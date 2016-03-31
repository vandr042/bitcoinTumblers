#!/usr/bin/env python3

import sys
import numpy as np
import matplotlib.pyplot as plt
import re
from dateutil.parser import parse
from matplotlib.dates import HourLocator, DateFormatter

KNOWN_PAT = re.compile("(.+),total known nodes (\\d+)")
ACTIVE_PAT = re.compile("(.+),active connections (\\d+)")
KNOWN_OUT_FILE = "-knownCount.pdf"
ACTIVE_OUT_FILE = "-activeCount.pdf"

def main():
    print("on known")
    plotForMe(KNOWN_PAT, sys.argv[1], sys.argv[1] + KNOWN_OUT_FILE, "Known Nodes")
    print("on active")
    plotForMe(ACTIVE_PAT, sys.argv[1], sys.argv[1] + ACTIVE_OUT_FILE, "Actively Connected Nodes")

def plotForMe(pat, inFile, outFile, titleStr):
    dateArr = []
    countArr = []
    fp = open(inFile, "r")
    startTime = -1
    for line in fp:
        if "active" in line or "known" in line:
            matcher = pat.search(line)
            if matcher:
                ts = float(matcher.group(1)) / (3600.0 * 1000.0)
                if startTime == -1:
                    startTime = ts
                dateArr.append(ts - startTime)
                countArr.append(int(matcher.group(2)))
    fp.close()
    print("total lines " + str(len(dateArr)))

    curFig = plt.figure()
    ax = plt.subplot(111)
    ax.plot(dateArr, countArr, figure=curFig, lw=5.0)
#    hours = HourLocator()
#    hourFmt = DateFormatter('%H')
#    ax.xaxis.set_major_locator(hours)
#    ax.xaxis.set_major_formatter(hourFmt)
    plt.title(titleStr)
    plt.xlabel("Time (epoch)")
    plt.ylabel("Nodes")
    curFig.savefig(outFile, format="pdf")


if __name__ == "__main__":
    main()
