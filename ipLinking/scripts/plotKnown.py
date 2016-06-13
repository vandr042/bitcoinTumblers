#!/usr/bin/env python3

import sys
import numpy as np
import matplotlib.pyplot as plt
import re
from dateutil.parser import parse
from matplotlib.dates import HourLocator, DateFormatter

KNOWN_PAT = re.compile("(.+),total known nodes (\\d+)")
ACTIVE_PAT = re.compile("(.+),active connections (\\d+)")
CONNSTAT_PAT = re.compile("(.+),CONNSTATUS,(\\d+),(\\d+),(\\d+)")
KNOWN_OUT_FILE = "-knownCount.pdf"
ACTIVE_OUT_FILE = "-activeCount.pdf"

def main():
    print("on known")
    plotForMe(KNOWN_PAT, sys.argv[1], sys.argv[1] + KNOWN_OUT_FILE, "Known Nodes")
    print("on active")
    plotForMe(ACTIVE_PAT, sys.argv[1], sys.argv[1] + ACTIVE_OUT_FILE, "Actively Connected Nodes")
    print("on conn status")
    connStat(sys.argv[1])

def connStat(inFile):
    startTime = -1
    timeArr = []
    activeArr = []
    priorityArr = []
    introducedArr = []
    fp = open(inFile, "r")
    for line in fp:
        if "CONNSTAT" in line:
            matcher = CONNSTAT_PAT.search(line)
            if matcher:
                ts = float(matcher.group(1)) / (3600.0 * 1000.0)
                if startTime == -1:
                    startTime = ts
                timeArr.append(ts - startTime)
                activeArr.append(int(matcher.group(2)))
                priorityArr.append(int(matcher.group(3)))
                introducedArr.append(int(matcher.group(4)))
    fp.close()
    curFig = plt.figure()
    ax = plt.subplot(111)
    ax.plot(timeArr, activeArr, figure=curFig, lw=5.0, label = "Running Tests")
    ax.plot(timeArr, priorityArr, figure=curFig, lw = 5.0, label = "Pending Priority")
#    plt.ylim(0, plt.ylim()[1])
    plt.title("Running/Pending Priority")
    plt.xlabel("Hours Since Start")
    plt.ylabel("Nodes")
    plt.legend(loc = "upper left")
    curFig.savefig(inFile + "-runningConn.pdf", format="pdf")
    curFig = plt.figure()
    ax = plt.subplot(111)
    ax.plot(timeArr, introducedArr, figure=curFig, lw=5.0)
#    plt.ylim(0, plt.ylim()[1])
    plt.title("Nodes Introduced To Connection")
    plt.xlabel("Hours Since Start")
    plt.ylabel("Nodes")
    curFig.savefig(inFile + "-intro.pdf", format="pdf")
        
    
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
    plt.ylim(0, plt.ylim()[1])
    plt.title(titleStr)
    plt.xlabel("Hours Since Start")
    plt.ylabel("Nodes")
    curFig.savefig(outFile, format="pdf")


if __name__ == "__main__":
    main()
