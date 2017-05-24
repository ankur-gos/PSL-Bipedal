'''
    parse.py
    Ankur Goswami, agoswam3@ucsc.edu
'''

import ijson

seg_path = '/Users/ankur/Coding/PSL-Bipedal/data/segment_obs.txt'
mode_path = '/Users/ankur/Coding/PSL-Bipedal/data/mode_obs.txt'
start_loc_path = '/Users/ankur/Coding/PSL-Bipedal/data/start_location_obs.txt'
end_loc_path = '/Users/ankur/Coding/PSL-Bipedal/data/end_location_obs.txt'
start_time_path = '/Users/ankur/Coding/PSL-Bipedal/data/start_time_obs.txt'
end_time_path = '/Users/ankur/Coding/PSL-Bipedal/data/end_time_obs.txt'
data_path = '/Users/ankur/Coding/PSL-Bipedal/preprocessing/parser/jay_march_2016_2_march_2017.timeline'

class LocationObs(object):

    def __init__(self):
        self.start_location = None
        self.start_time = None
        self.end_location = None
        self.end_time = None
        self.mode = None

def get_key(item):
    return item['metadata']['key']

def is_motion_activity(item):
    return get_key(item) == 'background/motion_activity'

def is_end_segment(item):
    if get_key(item) != 'background/motion_activity':
        return False
    return not item['data']['stationary'] and not item['data']['cycling'] and not item['data']['walking'] and not item['data']['running'] and not item['data']['automotive']

def is_location(item):
    return get_key(item) == 'background/location' or get_key(item) == 'background/filtered_location'

def is_start_segment(item):
    if get_key(item) != 'background/motion_activity':
        return False
    # print item['data']
    return item['data']['cycling'] or item['data']['walking'] or item['data']['running'] or item['data']['automotive']

def get_cleaned_mode(item):
    val = item['data']['sensed_mode']
    if val == 0:
        return 'automotive'
    if val == 1:
        return 'cycling'
    if val == 2:
        return 'walking'
    return None

def is_cleaned_segment(item):
    return get_key(item) == 'analysis/cleaned_section'

def get_location_obs_from_csegment(item):
    location = LocationObs()
    start_loc = item['data']['start_loc']['coordinates']
    end_loc = item['data']['end_loc']['coordinates']
    start_hour = item['data']['start_local_dt']['hour']
    end_hour = item['data']['end_local_dt']['hour']
    location.start_location = (start_loc[0], start_loc[1])
    location.end_location = (end_loc[0], start_loc[1])
    location.start_time = get_time_string(start_hour)
    location.end_time = get_time_string(end_hour)
    location.mode = get_cleaned_mode(item)
    return location


def get_mode(item):
    for mode in ['cycling', 'walking', 'running', 'automotive']:
        if item['data'][mode]:
            return mode

def get_time_string(hour):
    if hour >= 3 and hour <= 10:
        return 'Morning'
    if hour > 10 and hour < 16:
        return 'Afternoon'
    if hour >= 16 and hour < 20:
        return 'Evening'
    return 'Night'

def get_time(item):
    time = item['data']['local_dt']['hour']
    return get_time_string(hour)

def parse_segments(filename):
    with open(filename, 'rb') as rawfile:
        buffer = None
        current_location = None
        final_obs = []
        for item in ijson.items(rawfile, 'item'):
            if is_start_segment(item) and current_location is None:
                # print 'Found start segment'
                current_location = LocationObs()
                current_location.start_time = get_time(item)
                current_location.mode = get_mode(item)
                buffer = None
            elif current_location is not None:
                if is_location(item) and current_location is not None and buffer is None:
                    current_location.start_location = (item['data']['longitude'], item['data']['latitude'])
                elif buffer is None:
                    current_location = None
                elif is_end_segment(item):
                    current_location.end_location = (buffer['data']['longitude'], buffer['data']['latitude'])
                    current_location.end_time = get_time(item)
                    final_obs.append(current_location)
                    # print final_obs
                    current_location = None
                if is_location(item):
                    buffer = item
        return final_obs

'''
    Literally same as write_obs, except mode, probably should refactor later
'''
def write_cleaned_obs(observations, segment_path, start_loc_path, end_loc_path, start_time_path, end_time_path, mode_path, segment_day_path):
    with open(segment_path, 'w+') as sf, open(start_loc_path, 'w+') as start_lf, open(end_loc_path, 'w+') as end_lf, open(start_time_path, 'w+') as start_tf, open(end_time_path, 'w+') as end_tf, open(mode_path, 'w+') as mode_f, open(segment_day_path, 'w+') as day_f:
        for ind, obs in enumerate(observations):
            sf.write('%d\n' % ind)
            mode_f.write('%d\tcycling\n' % ind)
            start_lf.write('%d\t%0.3f %0.3f\n' % (ind, obs.start_location[0], obs.start_location[1]))
            end_lf.write('%d\t%0.3f %0.3f\n' % (ind, obs.end_location[0], obs.end_location[1]))
            start_tf.write('%d\t%s\n' % (ind, obs.start_time))
            end_tf.write('%d\t%s\n' % (ind, obs.end_time))

def parse_cleaned_segments(filename):
    with open(filename, 'rb') as rawfile:
        obs = []
        for item in ijson.items(rawfile, 'item'):
            if is_cleaned_segment(item):
                obs.append(get_location_obs_from_csegment(item))
        return obs


def write_obs(observations, segment_path, start_loc_path, end_loc_path, start_time_path, end_time_path, mode_path):
    with open(segment_path, 'w+') as sf, open(mode_path, 'w+') as mode_f, open(start_loc_path, 'w+') as start_lf, open(end_loc_path,'w+') as end_lf, open(start_time_path, 'w+') as start_tf, open(end_time_path, 'w+') as end_tf:
        for ind, obs in enumerate(observations):
            sf.write('%d\n' % ind)
            if obs.mode is not None:
                mode_f.write('%d\t%s\n' % (ind, obs.mode))
            start_lf.write('%d\t%0.3f %0.3f\n' % (ind, obs.start_location[0], obs.start_location[1]))
            end_lf.write('%d\t%0.3f %0.3f\n' % (ind, obs.end_location[0], obs.end_location[1]))
            start_tf.write('%d\t%s\n' % (ind, obs.start_time))
            end_tf.write('%d\t%s\n' % (ind, obs.end_time))


# obs = parse_segments(data_path)
# write_obs(obs, seg_path, start_loc_path, end_loc_path, start_time_path, end_time_path, mode_path)

# obs = parse_cleaned_segments(data_path)
# write_cleaned_obs(obs, seg_path, start_loc_path, end_loc_path, start_time_path, end_time_path, mode_path)

