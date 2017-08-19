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
data_path = '/Users/ankur/Coding/PSL-Bipedal/preprocessing/parser/data/jay_march_2016_2_march_2017.timeline' #'/Users/ankur/Coding/PSL-Bipedal/culler_processed'

def join_dir_path(directory, name):
    return '/'.join([directory, name])

def make_path(name):
    return join_dir_path(data_directory, name)

def make_results_path(name):
    return join_dir_path(results_directory, name)

'''
    Initial Parsed data observations (Shouldn't need to change)
'''
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

num_anchors = 50

num_frequent_trips = 50

write_anchors = make_path('write_anchors.txt')

'''
    Results settings
'''

# Geosheets names and locations

mode_geosheet_path = make_results_path('mode_pujara_geosheet.csv')

time_geosheet_path = make_results_path('time_pujara_geosheet.csv')

# Frequent Trip Times

trip_times_path = make_results_path('trips_times.txt')

# Frequent Trip Modes

trip_modes_path = make_results_path('trips_modes.txt')

# Raw results paths

cleaned_frequent_results_path = make_results_path('cleaned_results.txt')

constructed_frequent_results_path = make_results_path('constructed_results.txt')

cleaned_grouped_results_path = make_results_path('cleaned_grouped_results.txt')

constructed_grouped_results_path = make_results_path('constructed_grouped_results.txt')
