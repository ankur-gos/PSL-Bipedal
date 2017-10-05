'''
    ClusterGPS.py
    Clustering Locations
'''

from math import sin, cos, sqrt, atan2, radians

def load_data(files):
    locations = {}
    for file in files:
        with open(file, 'r') as rf:
            for line in rf:
                lat_long = input_to_lat_long(line)
                locations[lat_long] = 0
    return locations

def input_to_lat_long(line):
    line = line[:-1]
    location = line.split('\t', 1)[-1]
    return get_lat_long(location)

def get_lat_long(location):
    spl = location.split(' ', 1)
    return (float(spl[0]), float(spl[1]))

def compute_haversine_distance(location1, location2):
    lon1 = radians(abs(location1[0]))
    lon2 = radians(abs(location2[0]))
    lat1  = radians(abs(location1[1]))
    lat2  = radians(abs(location2[1]))

    dlon = lon2 - lon1
    dlat = lat2 - lat1

    a = sin(dlat / 2)**2 + cos(lat1) * cos(lat2) * sin(dlon / 2)**2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return 6373.0 * c

def compute_marked_mean(locations):
    reduced = reduce(lambda x, y: (x[0] + y[0], x[1] + y[1]), locations)
    val = (round(reduced[0] / len(locations), 4), round(reduced[1] / len(locations), 4))
    return val

def cluster(radius, files, output_files):
    locations = load_data(files)
    clustered_locations = dict(locations)
    num_clusters = 0
    for location in locations:
        if clustered_locations[location] != 0:
            continue
        current_center = location
        unchanged_center = False
        marked = []
        while unchanged_center is False:
            for next_location in locations:
                if clustered_locations[next_location] != 0:
                    continue
                if compute_haversine_distance(current_center, next_location) <= radius:
                    marked.append(next_location)
            new_mean = compute_marked_mean(marked)
            if current_center == new_mean:
                unchanged_center = True
            else:
                current_center = new_mean
        for mark in marked:
            clustered_locations[mark] = current_center
        num_clusters += 1
        #clustered_locations[]
        #index_to_mean[current_cluster] = current_center
        #current_center += 1
    return clustered_locations, num_clusters

def run(radius, files, output_files):
    locations, num_clusters = cluster(radius, files, output_files)
    filenum = 0
    for file in files:
        with open(file, 'r') as rf, open(output_files[filenum], 'w+') as wf:
            for line in rf:
                lat_long = input_to_lat_long(line)
                segment_num = line.split('\t', 1)[0]
                wf.write('%s\t%f %f\n' % (segment_num, locations[lat_long][0], locations[lat_long][1]))
            filenum += 1
                