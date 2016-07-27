#!/usr/bin/env python3

import sys
import matplotlib.pyplot as plt

def main(inputFile, outputFile):
    inFP = open(inputFile, "r")
    xVals = []
    yVals = []
    for line in inFP:
        splits = line.split(",")
        if len(splits) == 2:
            xVals.append(float(splits[0]))
            yVals.append(float(splits[1]))
    inFP.close()

    curFig = plt.figure()
    plt.plot(xVals, yVals, figure = curFig, lw = 5.0)
    curFig.savefig(outputFile, format = "pdf")

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
