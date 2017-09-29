import ClusterGPS

result = ClusterGPS.load_data(['location1.txt'])
assert(result == {(100.0, 100.0): 0, (200.0, 200.0): 0, (300.0, 300.0): 0})

lat_log = ClusterGPS.get_lat_long('100 100')
assert(lat_log == (100.0, 100.0))

dist = ClusterGPS.compute_haversine_distance((-77.037852, 38.898556),(-77.043934, 38.897147))
assert(round(dist,3) == 0.549)

mean = ClusterGPS.compute_marked_mean([(1, 1), (2, 2), (3, 3)])
assert(mean == (2, 2))

ClusterGPS.run(50, ['location2.txt'], ['location2_output.txt'])