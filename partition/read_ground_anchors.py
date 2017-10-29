'''
    partition.py
    label anchor data
    TODO: Rename this file
    Ankur Goswami
'''

import pandas as pd
import numpy as np

def read_anchor_file(filename):
    df = pd.read_csv(filename, sep='\t')
    df_new = df[(df['Anchor Rating'] == '3') | (df['Anchor Rating'] == '2')]
    print df_new
    return df_new

def edit_location(df):
    df['Location'] = df['Location'].apply(rearrange)
    return df

def rearrange(val):
    spl = val.split(',')
    spl[1] = spl[1]
    return spl[1] + ' ' + spl[0]

def write_anchor_truth(read_file, write_file):
    df = read_anchor_file(read_file)
    df = edit_location(df)
    with open(write_file, 'w+') as wf:
        for index, row in df.iterrows():
            wf.write('%s\t1\n' % row[0])

def add_zeros(df):
    df['Location'] = df['Location'].apply(location_zeroes)
    return df

def location_zeroes(val):
    spl = val.split(' ')
    return spl[0] + '00 ' + spl[1] + '00'

def label_dataset(read_file, dataset):
    df = read_anchor_file(read_file)
    df = edit_location(df)
    dataset_df = pd.read_csv(dataset, sep='\t')
    #dataset_df = add_zeros(dataset_df)
    labels = pd.Series(np.zeros(dataset_df.shape[0]))
    for index, row in dataset_df.iterrows():
        if row[0] in df['Location'].values:
            labels.set_value(index, 1)
    dataset_df = dataset_df.assign(Labels=labels.values)
    dataset_df.to_csv(dataset, sep='\t', index=False)

def write_dataset(read_files, out_dataset):
    with open(out_dataset, 'w+') as out_f:
        out_f.write('Location\n')
        for file in read_files:
            with open(file, 'r') as rf:
                for line in rf:
                    spl = line.split('\t')[-1]
                    out_f.write('%s' % spl)




#df = read_anchor_file('tom_places.tsv')
#edit_location(df)
