'''
    build.py
    Script to build and run everything
    Ankur Goswami, agoswam3@ucsc.edu
'''

import config
import preprocessing.parser.parse as parser
import subprocess
import output.filter_truth as ft

'''
    First run through, no preprocessing needed, just raw output
'''
cleaned_obs = parser.parse_cleaned_segments(config.data_path)
parser.write_cleaned_obs(cleaned_obs, config.seg_path, config.start_loc_path, config.end_loc_path, config.start_time_path, config.end_time_path, config.mode_path)
subprocess.call(['./run.sh'])
ft.filter('./output/default/frequents_infer.txt', config.cleaned_frequent_results_path)