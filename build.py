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
def build_cleaned_default():
    cleaned_obs = parser.parse_cleaned_segments(config.data_path)
    parser.write_obs(cleaned_obs, config.seg_path, config.start_loc_path, config.end_loc_path, config.start_time_path, config.end_time_path, config.mode_path, config.segment_day_path)
    preprocesser.run(config.start_loc_path, config.end_loc_path)
    subprocess.call(['./run.sh'])
    ft.filter('./output/default/frequents_infer.txt', config.cleaned_frequent_results_path)

'''
    build_constructed_default
    run the default pipeline using the constructed segments
'''
def build_constructed_default():
    pass

build_cleaned_default()