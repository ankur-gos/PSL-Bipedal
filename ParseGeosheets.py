'''
    Ankur Goswami
    agoswam3@ucsc.edu
    ParseGeosheets.py
'''

import re
import sys
import config

def filter_top_n_modes_trips():
    with open(config.trip_modes_path, 'r') as tmf, open(config.grounded_modes, 'w+') as gm_outf, open(config.grounded_times, 'w+') as gt_outf, open(config.trip_times_path, 'r') as ttf:
        i = 0
        for line in tmf:
            found = re.search(r'.*\'(.*) (.*)\'.*\'(.*) (.*)\'.*\'(.*)\'.*\[(.*)\].*', line, re.M|re.I)
            if found is not None:
                if float(found.group(6)) > 0.75:
                    gm_outf.write('%s %s\t%s %s\t%s\t%f\n' % (found.group(2), found.group(1), found.group(4), found.group(3), found.group(5), float(found.group(6))))
                    i += 2

        i = 0
        for line in ttf:
            found = re.search(r'.*\'(.*) (.*)\'.*\'(.*) (.*)\'.*\'(.*)\'.*\'(.*)\'.*\[(.*)\].*', line, re.M|re.I)
            if found is not None:
                if float(found.group(7)) > 0.75:
                    gt_outf.write('%s %s\t%s %s\t%s\t%s\t%f\n' % (found.group(2), found.group(1), found.group(4), found.group(3), found.group(5), found.group(6), float(found.group(7))))
                    i += 2

def write_final():
    with open(config.trip_modes_times_path, 'r') as tmtf, open(config.mode_time_geosheet_path, 'w+') as outf:
        i = 0
        for line in tmtf:
            found = re.search(r'.*\'(.*) (.*)\'.*\'(.*) (.*)\'.*\'(.*)\'.*\'(.*)\'.*\'(.*)\'.*\[(.*)\].*', line, re.M|re.I)
            if found is not None:
                if float(found.group(8)) > 0.85:
                    outf.write('Location\tType\tMode\tStart Time\tEnd Time\tConfidence\t\t=GEO_MAP(A%d:F%d, "Map%d")\n' % (i+1, i+2, i))
                    outf.write('%s,%s | %s,%s\tline\t%s\t%s\t%s\t%s\n' % (found.group(1), found.group(2), found.group(3), found.group(4), found.group(5), found.group(6), found.group(7), found.group(8)))
                    i += 2


def write():
    # # Export trip modes to geosheets

    colors = ((5, 'red'), (15, 'blue'), (float('inf'), 'black'))

    with open(config.trip_modes_path, 'r') as ttf, open(config.mode_geosheet_path, 'w+') as outf:
        outf.write('Location\tType\tMode\tColor\tConfidence\n')
        count, color_count = 0, 1
        current_color = colors[0]
        for line in ttf:
            found = re.search(r'.*\'(.*) (.*)\'.*\'(.*) (.*)\'.*\'(.*)\'.*\[(.*)\].*', line, re.M|re.I)
            if found is not None:
                if count == current_color[0]:
                    current_color = colors[color_count]
                    color_count += 1
                outf.write('%s,%s | %s,%s\tline\t%s\t%s\t%s\n' % (found.group(2), found.group(1), found.group(4), found.group(3), found.group(5), current_color[1], found.group(6)))
                count += 1


    # # Export trip times to geosheets

    with open(config.trip_times_path, 'r') as ttf, open(config.time_geosheet_path, 'w+') as outf:
        outf.write('Location\tType\tStart Time\tEnd Time\tColor\tConfidence\n')
        count, color_count = 0, 1
        current_color = colors[0]
        for line in ttf:
            found = re.search(r'.*\'(.*) (.*)\'.*\'(.*) (.*)\'.*\'(.*)\'.*\'(.*)\'.*\[(.*)\].*', line, re.M|re.I)
            if found is not None:
                if count == current_color[0]:
                    current_color = colors[color_count]
                    color_count += 1
                outf.write('%s,%s | %s,%s\tline\t%s\t%s\t%s\t%s\n' % (found.group(2), found.group(1), found.group(4), found.group(3), found.group(5), found.group(6), current_color[1], found.group(7)))
                count += 1
