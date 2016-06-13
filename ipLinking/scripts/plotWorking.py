#!/usr/bin/env python3

import sys
import re
import numpy as np
import matplotlib.pyplot as plt
from dateutil.parser import parse
from matplotlib.dates import HourLocator, DateFormatter

WORKED_PAT = re.compile("(.+),conn,.+")
FAILED_PAT = re.compile("(.+),tcpfailed,.+")

def main():
    print("on working")
    plotMe(WORKED_PAT, "../uniqueWorking.pdf", "Unique Working Nodes", ",conn,")
    print("on failed")
    plotMe(FAILED_PAT, "../failedConns.pdf", "Failed Connection Attempts", "tcpfailed")


def plotMe(pat, outFile, titleStr, sigWord):
    times = []
    values = []
    cur = 0
    startTime = -1
    fp = open(sys.argv[1], "r")
    for line in fp:
        if sigWord in line:
            matcher = pat.search(line)
            if matcher:
                cur = cur + 1
                ts = float(matcher.group(1)) / (3600.0 * 1000.0)
                if startTime == -1:
                    startTime = ts
                times.append(ts - startTime)
                values.append(cur)
    fp.close()
    print(str(len(times)))
    curFig = plt.figure()
    ax = plt.subplot(111)
    ax.plot(times, values, figure=curFig, lw=5.0)
#    hours = HourLocator()
#    hourFmt = DateFormatter('%H')
#    ax.xaxis.set_major_locator(hours)
#    ax.xaxis.set_major_formatter(hourFmt)
    plt.title(titleStr)
    plt.xlabel("Hours Since Start")
    plt.ylabel("Nodes")
    curFig.savefig(outFile, format="pdf")

if __name__ == "__main__":
    main()
