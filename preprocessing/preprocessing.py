'''
    preprocessing.py
    Preprocess locations by clustering them
    Ankur Goswami, agoswam3@ucsc.edu
'''

from sklearn.cluster import KMeans
from sklearn import mixture
import numpy as np

'''
    truncate_locations
    read the start and end locations files, truncate the first two characters and write them to one file
'''
def truncate_locations(start, end):
    with open(start, 'r') as start_file, open(end, 'r') as end_file, open('truncated_locations.txt', 'w') as fout:
        for line in start_file:
            # Truncate past the tab (segment # and tab character)
            fout.write(line.split('\t', 1)[-1])
        for line in end_file:
            fout.write(line.split('\t', 1)[-1])

def load_data(filename):
    with open(filename, 'r') as file:
        locations = np.loadtxt(file, delimiter=' ')
        return locations

def run_gaussian_mixture(locations):
    mixtures = []
    bics = []
    for n in range(1, len(locations)/8):
        gmm = mixture.GaussianMixture(n_components=n).fit(locations)
        mixtures.append(gmm)
    min_gmm = min(mixtures, key=lambda m: m.bic(locations))
    means = min_gmm.means_
    print len(means)
    predictions = min_gmm.predict(locations)
    locations = [tuple(mean) for mean in means]
    predictions_list = list(predictions)
    # Return the mapped location to the prediction
    return [locations[p] for p in predictions_list]
    
def write_locations(locations_list):
    with open('start_location_obs_post.txt', 'w') as start, open('end_location_obs_post.txt', 'w') as end:
        # First half of the list is the start locations
        for i in range(0, len(locations_list)/2):
            start.write('%d\t%f %f\n' % (i, locations_list[i][0], locations_list[i][1]))
        for i in range(0, len(locations_list)/2):
            end.write('%d\t%f %f\n' % (i, locations_list[i][0], locations_list[i][1]))

def run():
    truncate_locations('start_location_obs.txt', 'end_location_obs.txt')
    locations = load_data('truncated_locations.txt')
    new_locations = run_gaussian_mixture(locations)
    write_locations(new_locations)

run()

'''
    Not actually needed
'''
def run_kmeans(locations):
     kmeans = KMeans(n_clusters=5).fit(locations)
     print kmeans.cluster_centers_
     return kmeans.cluster_centers_


