'''
    filter_truth.py
    Ankur Goswami, agoswam3
'''

import sys

def filter(filename, write_file):
    with open(filename, 'r') as read_f, open(write_file, 'w') as write_f:
        for line in read_f:
            if 'Truth=[0]' not in line:
                write_f.write(line)

if len(sys.argv) < 3:
    raise Exception('Usage: python filter_truth.py FILTER_FILENAME WRITE_FILENAME')

filter(sys.argv[1], sys.argv[2])
