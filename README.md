# PSL-Bipedal

Creating a model for finding frequent commute information.

## How to run

1. Setup config.py, set important paths
2. Run build.py

```
usage: build.py [-h] [-n] [-g]

Run inference to find out frequent trips

optional arguments:
  -h, --help          show this help message and exit
  -n, --nopreprocess  Do not do preprocessing, just run inference
                      (preprocessing often needs to be done only once).
  -g, --geosheets     Create a geosheets friendly anchor output
```


