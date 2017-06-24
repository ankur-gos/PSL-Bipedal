'''
    build.py
    Script to build and run everything
    Ankur Goswami, agoswam3@ucsc.edu
'''

import config
import preprocessing.parser.parse as parser
import preprocessing.preprocessing as preprocesser
import subprocess
import output.filter_truth as ft
import sys

'''
    build_cleaned_clustered
    Run entire pipeline, with preprocessing
'''
def build_cleaned_clustered():
    # Parser cleaned segments, write them to data files
    cleaned_obs = parser.parse_cleaned_segments(config.data_path)
    parser.write_obs(cleaned_obs, config.seg_path, config.start_loc_path, config.end_loc_path, config.start_time_path, 
    config.end_time_path, config.mode_path, config.segment_day_path)
    # Cluster to coalesce locations
    preprocesser.run_with_assignment(config.start_loc_path, config.end_loc_path)
    build_cleaned_clustered_nopreprocess()

'''
    build_cleaned_clustered_nopreprocess
    Run the pipeline without parsing or preprocessing
'''
def build_cleaned_clustered_nopreprocess():
    # Infer anchors
    subprocess.call(['./run.sh'])
    # filter and ground anchors
    if config.write_anchors:
        ft.filter('./output/default/anchors.txt', './anchors_results')
    ft.filter_top_n('./output/default/anchors.txt', config.anchors_path, config.num_anchors)

    # Infer Frequent Trips
    subprocess.call(['./run_infer_frequents.sh'])
    ft.filter('./output/default/frequents_infer.txt', config.cleaned_grouped_results_path)

    if create_geosheets:
        ft.create_geosheets_csv(config.cleaned_grouped_results_path, 'geosheets_cleaned.txt')

    ft.filter_top_n_frequents('./output/default/frequents_infer.txt', config.frequents_path, num_frequent_trips)

    # Infer trip information
    subprocess.call(['./run_infer_info.sh'])

    # Filter and output results
    ft.filter('./output/default/frequent_times_infer.txt', config.trip_times_path)
    ft.filter('./output/default/frequent_modes_infer.txt', config.trip_modes_path)

# build_cleaned_clustered()
build_cleaned_clustered_nopreprocess()