'''
    generator.py
    Ankur Goswami, agoswam3@ucsc.edu
    Generate dummy data
'''

import random

def generate(n=20):
    generate_segments(n)
    generate_locations(n)
    generate_times(n)
    generate_modes(n)

'''
    generate_segments
    generate n segments
'''
def generate_segments(n):
    with open('segment_obs.txt', 'w') as f:
        for i in range(0, n):
            f.write('%d\n' % i)
        f.close()

'''
    generate_locations
    generate locations for our model
    n is the number of segments

    general idea: There is a probability distribution for locations
    generate the distribution and populate n entries based on it.
'''
def generate_locations(n):
    location1 = (float(random.randint(0, 100)), float(random.randint(0, 100)))
    location2 = (float(random.randint(0, 100)), float(random.randint(0, 100)))
    with open('start_location_obs.txt', 'w') as f:
        for i in range(0, n):
            val = random.gauss(0, 1)
            if val > 0 and val < 1:
                f.write('%d\t%f\t%f\n' % (i, location1[0], location1[1]))
            elif val <= 0 and val > -1:
                f.write('%d\t%f\t%f\n' % (i, location2[0], location2[1]))
            else:
                f.write('%d\t%f\t%f\n' % (i, float(random.randint(0, 100)), float(random.randint(0, 100))))
        f.close()
    with open('end_location_obs.txt', 'w') as f:
        for i in range(0, n):
            val = random.gauss(0, 1)
            if val > 0 and val < 1:
                f.write('%d\t%f\t%f\n' % (i, location1[0], location1[1]))
            elif val <= 0 and val > -1:
                f.write('%d\t%f\t%f\n' % (i, location2[0], location2[1]))
            else:
                f.write('%d\t%f\t%f\n' % (i, float(random.randint(0, 100)), float(random.randint(0, 100))))
        f.close()

'''
    generate_times
    generates times for the model
    simply iterate between Morning, Afternoon, Evening, and Night
'''
def generate_times(n):
    times = ['Morning', 'Afternoon', 'Evening', 'Night']
    with open('start_time_obs.txt', 'w') as f:
        for i in range(0, n):
            f.write('%d\t%s\n' % (i, times[i % 4]))
    with open('end_time_obs.txt', 'w') as f:
        for i in range(0, n):
            f.write('%d\t%s\n' % (i, times[i % 4]))

'''
    generate_modes
    randomly choose between Bike, Drive, and Walk for each segment
'''
def generate_modes(n):
    modes = ['Bike', 'Drive', 'Walk']
    with open('mode_obs.txt', 'w') as f:
        for i in range(0, n):
            f.write('%d\t%s\n' % (i, random.choice(modes)))
