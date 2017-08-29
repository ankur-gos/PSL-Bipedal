'''
    filter_truth.py
    Ankur Goswami, agoswam3
'''

import sys
import re

def filter_top_n(filename, write_file, n):
    with open(write_file, 'w+') as write_f:
        lines = filter_lines(filename)
        for line in lines:
            if line[1] < 0.7:
                break
            anchor = re.search(r'.*\(\'(.*)\'.*', line[0], re.M|re.I)
            if anchor is not None:
                write_f.write('%s\n' % anchor.group(1))

def anchor_geosheets(filename, write_file):
    with open(write_file, 'w+') as write_f:
        i = 0
        lines = filter_lines(filename)
        for line in lines:
            anchor = re.search(r'.*\(\'(.*) (.*)\'.*', line[0], re.M|re.I)
            if anchor is not None:
                write_f.write('Location\tMap\n')
                write_f.write('%s,%s\t=GEO_MAP(A%d:A%d, \"MAP%d\")\n' % (anchor.group(2), anchor.group(1), i+1, i+2, i))
                i += 2

def filter_top_n_frequents(filename, write_file, n):
    with open(write_file, 'w+') as write_f:
        lines = filter_lines(filename)
        for line in lines:
            if line[1] < 0.9:
                break
            frequent_trip = re.search(r'.*\'(.*)\', \'(.*)\'.*', line[0], re.M|re.I)
            if frequent_trip is not None:
                write_f.write('%s\t%s\n' % (frequent_trip.group(1), frequent_trip.group(2)))

def filter_top_n_modes_trips(times_filename, modes_filename,  times_wf, modes_wf, n):
    with open(write_file, 'w+') as write_f:
        lines = filter_lines(filename)
        for line in lines:
            if line[1] < 0.70:
                break
            frequent_trip = re.search(r'.*\'(.*)\', \'(.*)\'.*', line[0], re.M|re.I)
            if frequent_trip is not None:
                write_f.write('%s\t%s\n' % (frequent_trip.group(1), frequent_trip.group(2)))

def create_geosheets_csv(locations_file, write_file):
    with open(locations_file, 'r') as lf, open(write_file, 'w+') as wf:
        lines = filter_lines(locations_file)
        wf.write('Location\tType\tTruth\n')
        for line in lines:
            found = re.search(r'.*\'(.*) (.*)\'.*\'(.*) (.*)\'.*', line[0], re.M|re.I)
            if line[1] < 0.7:
                break
            if found is not None:
                wf.write('%s,%s | %s,%s\tline\t%f\n' % (found.group(2), found.group(1), found.group(4), found.group(3), line[1]))


def filter(filename, write_file):
    with open(write_file, 'w+') as write_f:
        lines = filter_lines(filename)
        for line in lines:
            write_f.write(line[0])

def filter_lines(filename):
    with open(filename, 'r') as read_f:
        lines = []
        for line in read_f:
            if 'Truth=[0]' not in line:
                truth = re.search(r'(.*)Truth=\[(.*)\].*', line, re.M|re.I)
                if truth is not None:
                    lines.append((line, float(truth.group(2))))
        lines.sort(key=lambda x: x[1])
        lines.reverse()
        return lines
