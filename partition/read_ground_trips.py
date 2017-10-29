'''
    read_ground_trips.py
    Put ground trips into the truth table for evaluation
'''

import pandas as pd
import numpy as np

def read_trips_file(filename):
    df = pd.read_csv(filename, sep='\t')
    df_new = df[(df['Trip Rating'] == '3') | (df['Trip Rating'] == '2')]
    return df_new

def process_values(df):
    df['Location'] = df['Location'].apply(rearrange)
    df['Estimated Start Time'] = df['Estimated Start Time'].apply(remap_time)
    df['Estimated End Time'] = df['Estimated End Time'].apply(remap_time)
    return df

def remap_time(val):
    if pd.isnull(val):
        return val
    
    spl = val.split(' ')
    hour = 0
    if spl[1] == 'PM':
        hour += 12
    spl2 = spl[0].split(':')
    hour += int(spl2[0])
    '''
    minutes = str(int(60 * round(float(spl2[1])/60))).zfill(2)
    if minutes == '60':
        hour += 1
        minutes = '00'
    return str(hour) + ':' + minutes
    '''
    if hour >= 3 and hour <= 10:
        return 'Morning'
    if hour > 10 and hour < 16:
        return 'Afternoon'
    if hour >= 16 and hour < 20:
        return 'Evening'
    return 'Night'


def rearrange(val):
    spl = val.split('|')
    spl[0] = spl[0][:-1]
    spl[1] = spl[1][1:]
    spl2 = spl[0].split(',')
    spl3 = spl[1].split(',')
    return spl2[1] + ' ' + spl2[0] + '\t' + spl3[1] + ' ' + spl3[0]

def add_zeros(df):
    df['Start Location'] = df['Start Location'].apply(location_zeroes)
    df['End Location'] = df['End Location'].apply(location_zeroes)
    return df

def location_zeroes(val):
    spl = val.split(' ')
    return spl[0] + ' ' + spl[1]

def write_trip_truth(read_file, write_file, write_times, write_modes):
    df = read_trips_file(read_file)
    df = process_values(df)
    with open(write_file, 'w+') as wf, open(write_modes, 'w+') as wm, open(write_times, 'w+') as wt:
        store = []
        store1 = []
        store2 = []
        for index, row in df.iterrows():
            if row[0] not in store:
                wf.write('%s\t1\n' % row[0])
            store.append(row[0])
            if not pd.isnull(row[6]):
                if (row[0], row[6]) not in store1:
                    wm.write('%s\t%s\t1\n' % (row[0], row[6]))
                store1.append((row[0], row[6]))
            if not pd.isnull(row[7]) and not pd.isnull(row[8]):
                if (row[0], row[7], row[8]) not in store2:
                    wt.write('%s\t%s\t%s\t1\n' % (row[0], row[7], row[8]))
                store2.append((row[0], row[7], row[8]))

def label_anchor_dataset(read_file, dataset):
    df = pd.read_csv(read_file, sep='\t')
    dataset_df = pd.read_csv(dataset, sep='\t', names=list('12345'))
    dataset_df = dataset_df.dropna(axis=0,how='any')
    indices = dataset_df.index.values
    labels = pd.Series(np.zeros(dataset_df.shape[0]))
    labels = labels.reindex(indices, fill_value=0)
    for index, row in dataset_df.iterrows():
        for ind in range(df.shape[0]):
            if row[0] == df.ix[:,0][ind] or row[1] == df.ix[:,0][ind]:
                labels.set_value(index, 1)
    dataset_df = dataset_df.assign(Labels=labels.values)
    dataset_df.to_csv(dataset, sep='\t', index=False, header=False)

def label_dataset(read_file, dataset):
    df = pd.read_csv(read_file, sep='\t')
    dataset_df = pd.read_csv(dataset, sep='\t', names=list('12345'))
    dataset_df = dataset_df.dropna(axis=0,how='any')
    indices = dataset_df.index.values
    labels = pd.Series(np.zeros(dataset_df.shape[0]))
    labels = labels.reindex(indices, fill_value=0)
    for index, row in dataset_df.iterrows():
        for ind in range(df.shape[0]):
            if row[0] == df.ix[:,0][ind] and row[1] == df.ix[:,1][ind]:
                labels.set_value(index, 1)
    dataset_df = dataset_df.assign(Labels=labels.values)
    dataset_df.to_csv(dataset, sep='\t', index=False, header=False)

def label_mode_set(read_file, dataset):
    df = pd.read_csv(read_file, sep='\t')
    dataset_df = pd.read_csv(dataset, sep='\t',names=list('12345'))
    dataset_df = dataset_df.dropna(axis=0,how='any')
    indices = dataset_df.index.values
    labels = pd.Series(np.zeros(dataset_df.shape[0]))
    labels = labels.reindex(indices, fill_value=0)
    for index, row in dataset_df.iterrows():
        for ind in range(df.shape[0]):
            if row[0] == df.ix[:,0][ind] and row[1] == df.ix[:,1][ind] and row[2] == df.ix[:,2][ind]:
                labels.set_value(index, 1)
    dataset_df = dataset_df.assign(Labels=labels.values)
    dataset_df.to_csv(dataset, sep='\t', index=False, header=False)

def label_time_set(read_file, dataset):
    df = pd.read_csv(read_file, sep='\t')
    dataset_df = pd.read_csv(dataset, sep='\t', names=list('12345'))
    dataset_df = dataset_df.dropna(axis=0,how='any')
    indices = dataset_df.index.values
    labels = pd.Series(np.zeros(dataset_df.shape[0]))
    labels = labels.reindex(indices, fill_value=0)
    for index, row in dataset_df.iterrows():
        for ind in range(df.shape[0]):
            if row[0] == df.ix[:,0][ind] and row[1] == df.ix[:,1][ind] and row[2] == df.ix[:,2][ind] and row[3] == df.ix[:,3][ind]:
                labels.set_value(index, 1)
    dataset_df = dataset_df.assign(Labels=labels.values)
    dataset_df.to_csv(dataset, sep='\t', index=False, header=False)

def write_dataset(read_files, out_dataset):
    with open(out_dataset, 'w+') as out_f:
        seg_map = {}
        for file in read_files:
            with open(file, 'r') as rf:
                for line in rf:
                    spl = line.split('\t')
                    if spl[0] in seg_map:
                        seg_map[spl[0]].append(spl[-1][:-1])
                    else:
                        seg_map[spl[0]] = [spl[-1][:-1]]
        for value in seg_map.itervalues():
            concat = ""
            for val in value:
                concat += val + '\t'
            concat = concat[:-1]
            concat += '\n'
            out_f.write(concat)

