'''
    preprocessing.py
    Preprocess locations by clustering them
    Ankur Goswami, agoswam3@ucsc.edu
'''

from sklearn.cluster import KMeans
from sklearn import mixture
import numpy as n
import itertools
from os import remove
import numpy as np
from scipy import linalg
import matplotlib.pyplot as plt
import matplotlib as mpl
import random

color_iter = itertools.cycle(['navy', 'c', 'cornflowerblue', 'gold',
                              'darkorange'])

'''
    truncate_locations
    read the start and end locations files, truncate past the first tab
'''
def truncate_locations(start, end):
    with open(start, 'r') as start_file, open(end, 'r') as end_file, open('truncated_locations.txt', 'w') as fout:
        for line in start_file:
            # Truncate past the tab (segment # and tab character)
            fout.write(line.split('\t', 1)[-1])
        for line in end_file:
            fout.write(line.split('\t', 1)[-1])

'''
    load_data
    Load data from the filename passed in and return that matrix
    returns np.matrix
'''
def load_data(filename):
    with open(filename, 'r') as file:
        locations = np.loadtxt(file, delimiter=' ')
        return locations

'''
Graph function taken from
http://scikit-learn.org/stable/auto_examples/mixture/plot_gmm.html#sphx-glr-auto-examples-mixture-plot-gmm-py
'''
def plot_results(X, Y_, means, covariances, index, title):
    splot = plt.subplot(1, 1, 1 + index)
    for i, (mean, covar, color) in enumerate(zip(
            means, covariances, color_iter)):
        v, w = linalg.eigh(covar)
        v = 2. * np.sqrt(2.) * np.sqrt(v)
        u = w[0] / linalg.norm(w[0])
        # as the DP will not use every component it has access to
        # unless it needs it, we shouldn't plot the redundant
        # components.
        if not np.any(Y_ == i):
            continue
        plt.scatter(X[Y_ == i, 0], X[Y_ == i, 1], 10, color=color)

        # Plot an ellipse to show the Gaussian component
        angle = np.arctan(u[1] / u[0])
        angle = 180. * angle / np.pi  # convert to degrees
        ell = mpl.patches.Ellipse(mean, v[0], v[1], 180. + angle, color=color)
        ell.set_clip_box(splot.bbox)
        ell.set_alpha(0.5)
        splot.add_artist(ell)

    plt.axis([-124.848974, -66.885444, 24.396308, 49.384358])
    plt.title('Gaussian Mixture with K=5')
    plt.xlabel('Longitude')
    plt.ylabel('Latitude')
    plt.xticks(())
    plt.yticks(())

'''
    run_gaussian_mixture
    Pass in location data, minimum number of gaussians, and maximum number of gaussians
    Find the Gaussian Mixture model for each # gaussians, then pick the model which minimizes the
    bayesian information criteria

    Finally, create a dictionary where each cluster key maps to the locations in that cluster

    TODO: Make sure min gaussians and max gaussians are valid parameters
'''
def run_gaussian_mixture(locations, min_gaussians, max_gaussians):
    mixtures = []
    for n in range(min_gaussians, max_gaussians):
        gmm = mixture.GaussianMixture(n_components=n).fit(locations)
        mixtures.append(gmm)
    min_gmm = min(mixtures, key=lambda m: m.bic(locations))
    predictions = min_gmm.predict(locations)
    predictions_list = list(predictions)
    # initialize cluster dict
    clusters = dict()
    for ind, prediction in enumerate(predictions_list):
        # First time seeing prediction, initialize location, ind list
        if prediction not in clusters:
            clusters[prediction] = [(tuple(locations[ind]), ind)]
        else:
            clusters[prediction].append((tuple(locations[ind]), ind))
    return clusters

'''
    plot_gaussian_mixture
    same as first part of run, but plot the optimal model
    TODO: Refactor first part
'''
def plot_gaussian_mixture(locations, min_gaussians, max_gaussians):
    mixtures = []
    for n in range(min_gaussians, max_gaussians):
        gmm = mixture.GaussianMixture(n_components=n).fit(locations)
        mixtures.append(gmm)
    min_gmm = min(mixtures, key=lambda m: m.bic(locations))
    predictions = min_gmm.predict(locations)
    plot_results(locations, predictions, min_gmm.means_, min_gmm.covariances_, 0, 'Results')
    plt.show()

'''
    predict_mixture
    Similar to run_gaussian_mixture, except instead of categorizing locations according to
    the cluster their in, reassign locations to the cluster mean and return that new list
'''
def predict_mixture(locations_with_index, min_gaussians, max_gaussians):
    mixtures = []
    locations = np.matrix([[lwi[0][0], lwi[0][1]] for lwi in locations_with_index])
    for n in range(min_gaussians, max_gaussians):
        gmm = mixture.GaussianMixture(n_components=n).fit(locations)
        mixtures.append(gmm)
    min_gmm = min(mixtures, key=lambda m: m.bic(locations))
    means = min_gmm.means_
    predictions = min_gmm.predict(locations)
    locations = [tuple(mean) for mean in means]
    predictions_list = list(predictions)
    return [(locations[p], locations_with_index[ind][1]) for ind, p in enumerate(predictions_list)]
    
'''
    write_locations
    Given a list of locations associated with a cluster (So not the entire list) and the length of the entire list
    (Where the first half of the entire list is the start locations and the second half the end locations),
    map and write each corresponding start and end locations
'''
def write_locations(locations_list, total_length, start_file, end_file):
    with open(start_file, 'w') as sf, open(end_file, 'w') as ef:
        table = dict()
        for location in locations_list:
            key = location[1] - total_length/2
            if location[1] < total_length/2:
                key = location[1] + total_length/2
            if location[1] in table:
                continue
            if key in table:
                print key
                print table
                print '--------'
                corresponding_location = table[key]
                segment = key if key < total_length/2 else location[1]
                start, end = None, None
                if key == segment:
                    start = location[0]
                    end = corresponding_location
                else:
                    start = corresponding_location
                    end = location[0]
                sf.write('%d\t%f %f\n' % (segment, start[0], start[1]))
                ef.write('%d\t%f %f\n' % (segment, end[0], end[1]))
            table[location[1]] = location[0]

'''
    Cleanup any intermediate files
'''
def cleanup(files):
    for file in files:
        remove(file)

'''
    sample_n_values
    sample n values from the first half (starting loctions)
    These will then pair later on
'''
def sample_n_values(n, array):
    print '%d samples' % n
    half = [val for ind, val in enumerate(array) if ind < len(array)/2]
    second_half = [val for ind, val in enumerate(array) if ind >= len(array)/2]
    # print len(half)
    # sampled_half = random.sample(half, n)
    return half[:n/2] + second_half[:n/2]

def run(start_file, end_file):
    truncate_locations(start_file, end_file)
    locations = load_data('truncated_locations.txt')
    new_locations = run_gaussian_mixture(locations, 2, 3)
    max_cluster = max(new_locations.itervalues(), key=lambda v: len(v))
    max_cluster = sample_n_values(int(0.8 * len(max_cluster)), max_cluster)
    write_locations(max_cluster, len(locations), start_file, end_file)
    cleanup(['truncated_locations.txt'])

def run_with_assignment(start_file, end_file, noise_est=0.7):
    truncate_locations(start_file, end_file)
    locations = load_data('truncated_locations.txt')
    new_locations = run_gaussian_mixture(locations, 2, 5)
    max_cluster = max(new_locations.itervalues(), key=lambda v: len(v))
    gaussians = int(noise_est * len(max_cluster))
    max_cluster = predict_mixture(max_cluster, gaussians, gaussians+1)
    max_cluster = sample_n_values(500, max_cluster)
    write_locations(max_cluster, len(locations), start_file, end_file)
    cleanup(['truncated_locations.txt'])


