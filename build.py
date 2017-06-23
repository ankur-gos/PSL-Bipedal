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

'''
     build_cleaned_default
     run the default pipeline using the cleaned segments
'''

def build_default(cleaned=True):
    pass

def build_cleaned_default():
    cleaned_obs = parser.parse_cleaned_segments(config.data_path)
    parser.write_obs(cleaned_obs, config.seg_path, config.start_loc_path, config.end_loc_path, config.start_time_path, config.end_time_path, config.mode_path, config.segment_day_path)
    preprocesser.run(config.start_loc_path, config.end_loc_path, config.clusters_path)
    subprocess.call(['./run.sh'])
    ft.filter('./output/default/frequents_infer.txt', config.cleaned_frequent_results_path)

'''
    build_constructed_default
    run the default pipeline using the constructed segments
'''
def build_constructed_default():
    obs = parser.parse_segments(config.data_path)
    parser.write_obs(obs, config.seg_path, config.start_loc_path, config.end_loc_path, config.start_time_path, config.end_time_path, config.mode_path, config.segment_day_path)
    preprocesser.run(config.start_loc_path, config.end_loc_path, config.clusters_path)
    subprocess.call(['./run.sh'])
    ft.filter('./output/default/frequents_infer.txt', config.constructed_frequent_results_path)

def build_cleaned_clustered():
    cleaned_obs = parser.parse_cleaned_segments(config.data_path)
    parser.write_obs(cleaned_obs, config.seg_path, config.start_loc_path, config.end_loc_path, config.start_time_path, config.end_time_path, config.mode_path, config.segment_day_path)
    preprocesser.run_with_assignment(config.start_loc_path, config.end_loc_path)
    build_cleaned_clustered_nopreprocess()

def build_cleaned_clustered_nopreprocess():
    subprocess.call(['./run.sh'])
    ft.filter('./output/default/anchors.txt', './anchors_results')
    ft.filter_top_n('./output/default/anchors.txt', config.anchors_path, 50)
    subprocess.call(['./run_infer_frequents.sh'])
    ft.filter('./output/default/frequents_infer.txt', config.cleaned_grouped_results_path)
    ft.create_geosheets_csv(config.cleaned_grouped_results_path, 'geosheets_cleaned.txt')
    ft.filter_top_n_frequents('./output/default/frequents_infer.txt', config.frequents_path, 50)
    subprocess.call(['./run_infer_info.sh'])
    ft.filter('./output/default/frequent_times_infer.txt', config.trip_times_path)
    ft.filter('./output/default/frequent_modes_infer.txt', config.trip_modes_path)

def build_constructed_clustered():
    obs = parser.parse_segments(config.data_path)
    parser.write_obs(obs, config.seg_path, config.start_loc_path, config.end_loc_path, config.start_time_path, config.end_time_path, config.mode_path, config.segment_day_path)
    preprocesser.run_with_assignment(config.start_loc_path, config.end_loc_path)
    subprocess.call(['./run.sh'])
    ft.filter('./output/default/frequents_infer.txt', config.constructed_grouped_results_path)

# build_cleaned_clustered()
build_cleaned_clustered_nopreprocess()