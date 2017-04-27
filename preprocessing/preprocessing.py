'''
    preprocessing.py
    Preprocess locations by clustering them
    Ankur Goswami, agoswam3@ucsc.edu
'''

from sklearn.cluster import KMeans
import numpy as np

def truncate_locations(start, end):
    with open(start, 'r') as start_file, open(end, 'r') as end_file, open('truncated_locations.txt', 'w') as fout:
        for line in start_file:
            # Truncate past the first two characters (segment # and tab character)
            fout.write(line[2:])
        for line in end_file:
            fout.write(line[2:])

def load_data(filename):
    pass

