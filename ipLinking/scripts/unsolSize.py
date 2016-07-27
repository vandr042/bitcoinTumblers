#!/usr/bin/env python3

import re
import matplotlib.pyplot as plt
import sys

UNSOL_PAT = re.compile("push of (\\d+) from")

def main():
    delays = []
    fp = open(sys.argv[1], "r")
    for line in fp:
        if "push of" in line:
            matcher = UNSOL_PAT.search(line)
            if matcher:
                delays.append(int(matcher.group(1)))
    fp.close()
    delays.sort()
    yVals = []
    count = len(delays)
    for i in range(count):
        yVals.append(float(1 + i) / float(count))
    curFig = plt.figure()
    ax = plt.subplot(111)
    ax.plot(delays, yVals, figure=curFig, lw=5.0)
    plt.xlim([0,5])
    plt.xlabel("unsolicit size")
    plt.ylabel("CDF")
    curFig.savefig(sys.argv[1] + "-unsol.pdf", format= "pdf")


if __name__ == "__main__":
    main()
