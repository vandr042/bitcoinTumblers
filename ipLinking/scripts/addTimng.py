#!/usr/bin/env python3

import matplotlib.pyplot as plt
import sys

def main():
    delays = []
    fp = open(sys.argv[1], "r")
    for line in fp:
        if "addr timing" in line:
            tokens = line.split(",")[2:]
            for tVal in tokens:
                delays.append(int(tVal))
    fp.close()
    delays.sort()

    collapsedValues = []
    for i in range(len(delays)):
        if i % 1000 == 0:
            testVal = int(delays[i] / 100)
            if testVal > 0:
                collapsedValues.append(int(delays[i]))
    outFP = open("delays-10.txt", "w")
    for i in collapsedValues:
        outFP.write(str(i) + "\n")
    outFP.close()
    
    yVals = []
    count = len(delays)
    for i in range(count):
        yVals.append(float(1 + i) / float(count))
    curFig = plt.figure()
    ax = plt.subplot(111)
    ax.plot(delays, yVals, figure=curFig, lw=5.0)
    plt.xlim([0,100000])
    plt.xlabel("addr delay (ms)")
    plt.ylabel("CDF")
    curFig.savefig(sys.argv[1] + "-addrDelay.pdf", format= "pdf")


if __name__ == "__main__":
    main()
