'''
    graph_locations
    Ankur Goswami, agoswam3@ucsc.edu
'''

import ijson
import json
import numpy as np
import os
import matplotlib.pyplot as plt

def parse_locations(filename):
    with open(filename, 'rb') as f, open('coordinates.txt', 'w') as c:
        coordinates = []
        for id in ijson.items(f, 'item'):
            if 'loc' in id['data']:
                if 'coordinates' in id['data']['loc']:
                    coordinates.append((id['data']['loc']['coordinates'][0], id['data']['loc']['coordinates'][1]))
        coordinates = [(float(x[0]), float(x[1])) for x in coordinates]
        for coordinate in coordinates:
            c.write('%f,%f\n' % coordinate)
        coordinates_set = set(coordinates)
        print coordinates_set
        coordinates_x = [x[0] for x in coordinates]
        coordinates_y = [y[1] for y in coordinates]
        plt.plot(coordinates_x, coordinates_y, 'ro')
        # Box of the USA
        plt.axis([-124.848974, -66.885444, 24.396308, 49.384358])
        plt.title('Locations')
        plt.xlabel('Longitude')
        plt.ylabel('Latitude')
        plt.show()

def filter_info(filename):
    with open(filename, 'rb') as f, open('filtered_locations.txt', 'w') as fl, open('motion_activity.txt', 'w') as ma:
        final_items = []
        for handle in [fl, ma]:
            handle.write('[\n')
        for item in ijson.items(f,'item'):
            if item['metadata']['key'] == 'background/filtered_location':
                fl.write(json.dumps(item, default=recursive_default))
                fl.write(',\n')
            elif item['metadata']['key'] == 'background/motion_activity':
                ma.write(json.dumps(item, default=recursive_default))
                ma.write(',\n')
        for handle in [fl, ma]:
            handle.seek(-2, os.SEEK_END)
            handle.truncate()
            handle.write(']\n')
        
def recursive_default(val):
    if isinstance(val, dict):
        return val.__dict__
    return '%f' % val

# filter_info('/Users/ankur/Coding/PSL-Bipedal/preprocessing/parser/jay_march_2016_2_march_2017.timeline')
parse_locations('/Users/ankur/Coding/PSL-Bipedal/preprocessing/parser/filtered_locations.txt')
