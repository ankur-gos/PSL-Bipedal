'''
    build.py
    Script to build and run everything
    Ankur Goswami, agoswam3@ucsc.edu
'''

import config
import preprocessing.parser.parse as ps
import preprocessing.preprocessing as preprocesser
import cluster.ClusterGPS as cluster
import subprocess
import output.filter_truth as ft
import sys
import os
import argparse
import ParseGeosheets as pg
import shutil
import MarkovModel.model as mc

def preprocess():
    # Parser cleaned segments, write them to data files
    cleaned_obs = ps.parse_cleaned_segments(config.data_path)
    ps.write_obs(cleaned_obs, config.seg_path, config.start_loc_path, config.end_loc_path, config.start_time_path,
    config.end_time_path, config.mode_path, config.segment_day_path)
    # Cluster to coalesce locations
    cluster.run(0.2, [config.start_loc_path, config.end_loc_path], ['temp1', 'temp2'])
    shutil.copy('temp1', config.start_loc_path)
    shutil.copy('temp2', config.end_loc_path)
    os.remove('temp1')
    os.remove('temp2')

def build_markov_chains():
    preprocess()
    build_markov_chains_nopreprocess()

def build_markov_chains_nopreprocess():
    mc.model([config.start_loc_path, config.end_loc_path], config.markov_chains_results)

'''
    build_cleaned_clustered
    Run entire pipeline, with preprocessing
'''
def build_cleaned_clustered(create_geosheets):
    preprocess()
    build_cleaned_clustered_nopreprocess(create_geosheets)

'''
    build_cleaned_clustered_nopreprocess
    Run the pipeline without parsing or preprocessing
'''
def build_cleaned_clustered_nopreprocess(create_geosheets):
    # Infer anchors
    subprocess.call(['./run.sh'])
    # filter and ground anchors
    if config.write_anchors:
        ft.filter('./output/default/anchors.txt', './anchors_results')
    ft.filter_top_n('./output/default/anchors.txt', config.anchors_path, config.num_anchors)

    if create_geosheets:
        ft.anchor_geosheets('./output/default/anchors.txt', './anchors_geosheet.csv')

    # Infer Frequent Trips
    subprocess.call(['./run_infer_frequents.sh'])
    ft.filter('./output/default/frequents_infer.txt', config.cleaned_grouped_results_path)

    if create_geosheets:
        ft.create_geosheets_csv(config.cleaned_grouped_results_path, 'geosheets_cleaned.txt')

    ft.filter_top_n_frequents('./output/default/frequents_infer.txt', config.frequents_path, config.num_frequent_trips)

    #Infer trip information
    subprocess.call(['./run_infer_info.sh'])

    filter_and_merge(create_geosheets)

def filter_and_merge(create_geosheets):
    # Filter and output results
    ft.filter('./output/default/frequent_times_infer.txt', config.trip_times_path)
    ft.filter('./output/default/frequent_modes_infer.txt', config.trip_modes_path)

    if create_geosheets:
        pg.write()

    pg.filter_top_n_modes_trips()
    subprocess.call(['./run_infer_merge.sh'])

    ft.filter('./output/default/frequent_modes_times_infer.txt', config.trip_modes_times_path)
    if create_geosheets:
        pg.write_final()

parser = argparse.ArgumentParser(description='Run inference to find out frequent trips')
parser.add_argument('-n', '--nopreprocess', action='store_true', help='Do not do preprocessing, just run inference (preprocessing often needs to be done only once).')
parser.add_argument('-g', '--geosheets', action='store_true', help='Create a geosheets friendly anchor output')
parser.add_argument('-f', '--filtermerge', action='store_true', help='Run the filtering and merging process after inferring trip information')
parser.add_argument('-m', '--markov', action='store_true', help='Run the Markov chains baseline and output results')
args = parser.parse_args()
geosheets = args.geosheets
if args.nopreprocess:
    if args.markov:
        build_markov_chains_nopreprocess()
    else:
        build_cleaned_clustered_nopreprocess(geosheets)
elif args.filtermerge:
    filter_and_merge(geosheets)
elif args.markov:
    build_markov_chains()
else:
    build_cleaned_clustered(geosheets)

