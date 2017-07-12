# PSL-Bipedal

Creating a model for finding frequent commute information.

## Requirements
- Python 2.7

## How to run

1. Setup config.py, set important paths
2. Install python packages from requirements.txt
```
pip install -r requirements.txt
```
3. Run build.py

```
usage: build.py [-h] [-n] [-g]

Run inference to find out frequent trips

optional arguments:
  -h, --help          show this help message and exit
  -n, --nopreprocess  Do not do preprocessing, just run inference
                      (preprocessing often needs to be done only once).
  -g, --geosheets     Create a geosheets friendly anchor output
```


