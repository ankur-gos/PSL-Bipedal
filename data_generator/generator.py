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
    generate_days(n)

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
    print 'Location1: %s, Location2: %s' % (location1, location2)
    with open('start_location_obs.txt', 'w') as f:
        for i in range(0, n):
            val = random.gauss(0, 1)
            # Adding some noise to the location values
            n1, n2 = random.uniform(-1, 1), random.uniform(-1, 1)
            if val > 0 and val < 0.5:
                f.write('%d\t%f-%f\n' % (i, location1[0] + n1, location1[1] + n2))
            elif val <= 0 and val > -0.5:
                f.write('%d\t%f-%f\n' % (i, location2[0] + n1, location2[1] + n2))
            else:
                f.write('%d\t%f-%f\n' % (i, location1[0] + float(random.randint(-10, 10)) + n1, location1[1] + float(random.randint(-10, 10)) + n2))
        f.close()
    with open('end_location_obs.txt', 'w') as f:
        for i in range(0, n):
            val = random.gauss(0, 1)
            n1, n2 = random.uniform(-1, 1), random.uniform(-1, 1)
            if val > 0 and val < 0.5:
                f.write('%d\t%f-%f\n' % (i, location1[0] + n1, location1[1] + n2))
            elif val <= 0 and val > -0.5:
                f.write('%d\t%f-%f\n' % (i, location2[0] + n1, location2[1] + n2))
            else:
                print location2[1] + float(random.randint(-10, 10))
                f.write('%d\t%f-%f\n' % (i, location2[1] + float(random.randint(-10, 10)) + n1, location2[1] + float(random.randint(-10, 10)) + n2))
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

'''
    generate_days
    3 trips per day for now
'''
def generate_days(n):
    month = 4
    day = 1
    lim = 30
    with open('segment_days_obs.txt', 'w') as f:
        for i in range(1, n+1):
            f.write('%d\t%d/%d/2017\n' % (i-1, month, day))
            if i % 3 == 0:
                day += 1
            if i == lim:
                # Switch between 30 and 31 day months
                lim = 30 if lim == 31 else 31
                month += 1

generate()