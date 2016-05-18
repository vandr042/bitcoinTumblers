#!/usr/bin/env python3

import re
import numpy as np
import matplotlib.pyplot as plt
from dateutil.parser import parse
from matplotlib.dates import HourLocator, DateFormatter

LOG_FILE = "../2016-01-13T17:16:01"
WORKED_PAT = re.compile("(.+),worked .+")
FAILED_PAT = re.compile("(.+),failed .+")

def main():
    plotMe(WORKED_PAT, "../uniqueWorking.pdf", "Unique Working Nodes")
    plotMe(FAILED_PAT, "../failedConns.pdf", "Failed Connection Attempts")


def plotMe(pat, outFile, titleStr):
    times = []
    values = []
    cur = 0
    fp = open(LOG_FILE, "r")
    for line in fp:
        matcher = pat.search(line)
        if matcher:
            cur = cur + 1
            times.append(parse(matcher.group(1))) 
            values.append(cur)
    fp.close()
    print(str(len(times)))
    curFig = plt.figure()
    ax = plt.subplot(111)
    ax.plot(times, values, figure=curFig, lw=5.0)
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
