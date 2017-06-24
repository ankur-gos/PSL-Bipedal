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
data_path = '/Users/ankur/Coding/PSL-Bipedal/preprocessing/parser/data/jay_march_2016_2_march_2017.timeline'

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






'''
    Intermediate Results settings
'''

'''
    Processing Settings
'''

write_anchors

data_directory = './data/'
seg_path = '/Users/ankur/Coding/PSL-Bipedal/data/segment_obs.txt'
mode_path = '/Users/ankur/Coding/PSL-Bipedal/data/mode_obs.txt'
start_loc_path = '/Users/ankur/Coding/PSL-Bipedal/data/start_location_obs.txt'
end_loc_path = '/Users/ankur/Coding/PSL-Bipedal/data/end_location_obs.txt'
start_time_path = '/Users/ankur/Coding/PSL-Bipedal/data/start_time_obs.txt'
end_time_path = '/Users/ankur/Coding/PSL-Bipedal/data/end_time_obs.txt'
segment_day_path = '/Users/ankur/Coding/PSL-Bipedal/data/segment_days_obs.txt'
clusters_path = '/Users/ankur/Coding/PSL-Bipedal/data/clusters.txt'
anchors_path = '/Users/ankur/Coding/PSL-Bipedal/data/grounded_anchors.txt'
frequents_path = '/Users/ankur/Coding/PSL-Bipedal/data/grounded_frequents.txt'
trip_times_path = '/Users/ankur/Coding/PSL-Bipedal/results/trips_times.txt'
trip_modes_path = '/Users/ankur/Coding/PSL-Bipedal/results/trips_modes.txt'

cleaned_frequent_results_path = '/Users/ankur/Coding/PSL-Bipedal/results/cleaned_results.txt'
constructed_frequent_results_path = '/Users/ankur/Coding/PSL-Bipedal/results/constructed_results.txt'
cleaned_grouped_results_path = '/Users/ankur/Coding/PSL-Bipedal/results/cleaned_grouped_results.txt'
constructed_grouped_results_path = '/Users/ankur/Coding/PSL-Bipedal/results/constructed_grouped_results.txt'

