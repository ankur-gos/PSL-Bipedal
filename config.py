'''
    config.py
    Configure pipeline settings here
    Ankur Goswami, agoswam3@ucsc.edu
'''

import os

'''
    Data paths
'''

# Data Directory
data_directory = './data'

# Results Directory
results_directory = './results'

# Source data, change to point to data file
data_path = 'tom_processed'#'shankari_processed'#'jay_march_2016_2_march_2017.timeline' #'/Users/ankur/Coding/PSL-Bipedal/culler_processed'

anchor_truth_path = 'tom_places.tsv'

trip_truth_path = 'tom_trips.tsv'

def join_dir_path(directory, name):
    return '/'.join([directory, name])

def make_path(name):
    return join_dir_path(data_directory, name)

def make_results_path(name):
    return join_dir_path(results_directory, name)

'''
    Initial Parsed data observations (Shouldn't need to change)
'''

anchor_ground_path = make_path('anchor_truth.txt')

trip_ground_path = make_path('trip_truth.txt')

mode_ground_path = make_path('mode_truth.txt')

times_ground_path = make_path('times_truth.txt')

dataset_path = make_path('dataset.tsv')

mode_dataset_path = make_path('mode_dataset.tsv')

time_dataset_path = make_path('time_dataset.tsv')

anchor_dataset_path = make_path('anchor_dataset.tsv')

# Segment observations path
seg_path = make_path('segment_obs.txt')
# Mode observations path
mode_path = make_path('mode_obs.txt')

# Start observations path
start_loc_path = make_path('start_location_obs.txt')

# End Observations path
end_loc_path = make_path('end_location_obs.txt')

# Start time observations
start_time_path = make_path('start_time_obs.txt')

# End time observations
end_time_path = make_path('end_time_obs.txt')

# Segment day observations
segment_day_path = make_path('segment_days_obs.txt')

'''
    Grounded Nodes
'''

# Grounded anchors
anchors_path = make_path('grounded_anchors.txt')

# Grounded Frequent Trips
frequents_path = make_path('grounded_frequents.txt')

# Grounded FModes

grounded_modes = make_path('grounded_frequent_modes.txt')

grounded_times = make_path('grounded_frequent_times.txt')

trip_modes_times_path = make_path('trip_modes_times.txt')

num_anchors = 50

num_frequent_trips = 50

write_anchors = make_path('write_anchors.txt')

'''
    Results settings
'''

# Geosheets names and locations

mode_geosheet_path = make_results_path('mode_pujara_geosheet.csv')

time_geosheet_path = make_results_path('time_pujara_geosheet.csv')

mode_time_geosheet_path = make_results_path('mode_time_geosheet.csv')

markov_chains_results = make_results_path('frequency_results.csv')

# Frequent Trip Times

trip_times_path = make_results_path('trips_times.txt')

# Frequent Trip Modes

trip_modes_path = make_results_path('trips_modes.txt')

# Raw results paths

cleaned_frequent_results_path = make_results_path('cleaned_results.txt')

constructed_frequent_results_path = make_results_path('constructed_results.txt')

cleaned_grouped_results_path = make_results_path('cleaned_grouped_results.txt')

constructed_grouped_results_path = make_results_path('constructed_grouped_results.txt')
