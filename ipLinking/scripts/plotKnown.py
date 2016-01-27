#!/usr/bin/env python3

import numpy as np
import matplotlib.pyplot as plt
import re
from dateutil.parser import parse
from matplotlib.dates import HourLocator, DateFormatter

KNOWN_PAT = re.compile("(.+),total known nodes (\\d+)")
ACTIVE_PAT = re.compile("(.+),active connections (\\d+)")
KNOWN_IN_FILE = "../knownCount.txt"
KNOWN_OUT_FILE = "../knownCount.pdf"
ACTIVE_IN_FILE = "../activeCount.txt"
ACTIVE_OUT_FILE = "../activeCount.pdf"

def main():
    plotForMe(KNOWN_PAT, KNOWN_IN_FILE, KNOWN_OUT_FILE, "Known Nodes")
    plotForMe(ACTIVE_PAT, ACTIVE_IN_FILE, ACTIVE_OUT_FILE, "Actively Connected Nodes")

def plotForMe(pat, inFile, outFile, titleStr):
    dateArr = []
    countArr = []
    fp = open(inFile, "r")
    for line in fp:
        matcher = pat.search(line)
        if matcher:
            dateArr.append(parse(matcher.group(1)))
            countArr.append(int(matcher.group(2)))
    fp.close()
    print("total lines " + str(len(dateArr)))

    curFig = plt.figure()
    ax = plt.subplot(111)
    ax.plot(dateArr, countArr, figure=curFig, lw=5.0)
    hours = HourLocator()
    hourFmt = DateFormatter('%H')
    ax.xaxis.set_major_locator(hours)
    ax.xaxis.set_major_formatter(hourFmt)
    plt.title(titleStr)
    plt.xlabel("Time (Hours starting 13 Jan)")
    plt.ylabel("Nodes")
    curFig.savefig(outFile, format="pdf")


if __name__ == "__main__":
    main()
