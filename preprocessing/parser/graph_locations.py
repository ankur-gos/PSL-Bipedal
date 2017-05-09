'''
    graph_locations
    Ankur Goswami, agoswam3@ucsc.edu
'''

import ijson
import numpy as np
import matplotlib.pyplot as plt

def parse_locations(filename):
    with open(filename, 'rb') as f:
        coordinates_y = []
        coordinates_x = []
        for id in ijson.items(f, 'item'):
            if 'loc' in id['data']:
                if 'coordinates' in id['data']['loc']:
                    coordinates_x.append(id['data']['loc']['coordinates'][0])
                    coordinates_y.append(id['data']['loc']['coordinates'][0])
        plt.plot(coordinates_x, coordinates_y)
        plt.show()

        

        print coordinates
        # parser = ijson.parse(f)
        # for prefix, event, value in parser:
        #     # print event
        #     if prefix.endswith('.coordinates'):
        #         print value

parse_locations('/Users/ankur/Coding/PSL-Bipedal/preprocessing/parser/jay_march_2016_2_march_2017.timeline')
