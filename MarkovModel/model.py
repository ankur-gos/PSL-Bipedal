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
                if segnum is in inputs:
                    inputs[segnum] += segnum[1]
                else:
                    inputs[segnum] = segnum[1]
    final_inputs = []
    for key in inputs:
        final_inputs.append(inputs[key])
    return final_inputs
        

def run(inputs):
    counts = {}
    for transition in inputs:
        if transition is in counts:
            counts[transition] += 1
        else:
            counts[transition] = 1
    sum = 0
    for key, val in counts.values():
        # There must be a minimum of 2 trips to be considered
        if counts[key] == 1:
            del counts[key]
        else:
            sum += counts[key]
    for key, val in counts.values():
        counts[key] = val / sum
    return counts

def output(counts, output_file):
    sorted_tuples = sorted(counts.items(), key=lambda x: x[1])
    with open(output_file, 'w+') as wf:
        for tup in sorted_tuples:
            wf.write("%s\t%f\n")
