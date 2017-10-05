'''
    Markov Model for transportation
    Ankur Goswami
'''

def load_inputs(datafiles):
    inputs = {}
    for file in datafiles:
        with open(file, 'r') as rf:
            for line in rf:
                split = line.split('\t', 1)
                segnum = split[0]
                if segnum in inputs:
                    inputs[segnum] += ' ' + split[1][:-1]
                else:
                    inputs[segnum] = split[1][:-1]
    final_inputs = []
    for key in inputs:
        final_inputs.append(inputs[key])
    return final_inputs
        

def run(inputs):
    counts = {}
    anchors = {}
    for transition in inputs:
        if transition in counts:
            counts[transition] += 1
        else:
            counts[transition] = 1
    sum = 0
    for key, val in counts.items():
        parts = key.split(' ')
        # There must be a minimum of 5 trips to be considered
        #if counts[key] < 4:
        #    del counts[key]
        if len(parts) > 2 and parts[0] == parts[2] and parts[1] == parts[3]:
            del counts[key]
        else:
            loc1 = parts[1] + ',' + parts[0]
            loc2 = parts[3] + ',' + parts[2]
            if loc1 in anchors:
                anchors[loc1] += 1
            else:
                anchors[loc1] = 1
            if loc2 in anchors:
                anchors[loc2] += 1
            else:
                anchors[loc2] = 1
            sum += counts[key]
    #for key, val in counts.items():
    #    counts[key] = float(val) / float(sum)
    return counts, anchors

def output(counts, output_file):
    sorted_tuples = reversed(sorted(counts.items(), key=lambda x: x[1]))
    with open(output_file, 'w+') as wf:
        for tup in counts:
            wf.write("%s\t%f\n" % (tup[0], tup[1]))

def model(files, outputfile):
    inputs = load_inputs(files)
    counts, anchors = run(inputs)
    counts = reversed(sorted(counts.items(), key=lambda x: x[1]))
    anchors = reversed(sorted(anchors.items(), key=lambda x: x[1]))
    write_anchors(anchors, './results/anchor_results.csv')
    write_geosheet(counts, outputfile)

def write_anchors(anchors, output_file):
    i = 0
    with open(output_file, 'w+') as wf:
        for anchor in anchors:
            wf.write('Location\tTruth\tMap\tAnchor Rating\tLabel\tOff Location\n')
            wf.write('%s\t%f\t=GEO_MAP(A%d:B%d, \"MAP%d\")\n' % (anchor[0], anchor[1], i+1, i+2, i))
            i += 2
def write_geosheet(counts, output_file):
    i = 0
    with open(output_file, 'w+') as wf:
        for count in counts:
            locations = count[0]
            splits = locations.split(' ')
            wf.write('Location\tType\tTruth\tMap\tTrip Rating\tFragment\tTrip Mode\tTrip Time\tOld\tOff Locations\n')
            wf.write('%s,%s | %s,%s\tline\t%f\t=GEO_MAP(A%d:C%d, \"MAP%d\")\n' % (splits[1], splits[0], splits[3], splits[2], count[1], i+1, i+2, i))
            i += 2
