'''
    filter_truth.py
    Ankur Goswami, agoswam3
'''

import sys
import re

def filter(filename, write_file):
    with open(filename, 'r') as read_f, open(write_file, 'w') as write_f:
        lines = []
        for line in read_f:
            if 'Truth=[0]' not in line:
                truth = re.search(r'(.*)Truth=[(.*)].*', line, re.M|re.I)
                lines.append((line, float(truth.group(2))))
        lines.sort(key=lambda x: x[1])
        for line in lines:
            write_f.write(line[0])

# if len(sys.argv) < 3:
#     raise Exception('Usage: python filter_truth.py FILTER_FILENAME WRITE_FILENAME')

# filter(sys.argv[1], sys.argv[2])
