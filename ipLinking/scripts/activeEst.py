#!/usr/bin/env python3

import sys
import matplotlib.pyplot as plt

OK_WINDOW = 1.5
CONN_POINT_POS = 2
REMOTE_NODE_POS = 3

def main():


    times = []
    vals = []

    for i in range(3):
        startTS = -1
        currTS = 0.0
        
        onlineDict = {}

        times = []
        online = []
        logFP = open(sys.argv[1], "r")
        for line in logFP:
            if "CONNPOINT" in line:
                splits = line.split(",")
                ts = float(splits[0]) / (3600.0 * 1000.0)
                if startTS == -1:
                    startTS = ts
                ts = ts - startTS
                if ts > currTS:
                    if not currTS == 0.0:
                        online.append(countInWindow(onlineDict, currTS, OK_WINDOW + 0.5 * i))
                        times.append(currTS)
                        for tIP in onlineDict:
                            cleanMap(onlineDict[tIP], ts, OK_WINDOW + 0.5 * i)
                    currTS = currTS + 0.1
                    print("on " + str(currTS))
                if not splits[REMOTE_NODE_POS] in onlineDict:
                    onlineDict[splits[REMOTE_NODE_POS]] = {}
                onlineDict[splits[REMOTE_NODE_POS]][splits[CONN_POINT_POS]] = ts
        logFP.close()
        vals.append(online)

    curFig = plt.figure()
    for i in range(3):
        plt.plot(times, vals[i], figure=curFig, lw = 5.0, label = str(OK_WINDOW + 0.5 * i))
    plt.xlabel("Time since start (hrs)")
    plt.ylabel("Total Online Nodes (est)")
    plt.legend()
    curFig.savefig(sys.argv[1] + "-onlineGuess.pdf", format = "pdf")
            
                
def countInWindow(online, currTime, window):
    count = 0
    for tIP in online:
        for tCP in online[tIP]:
            if currTime - online[tIP][tCP] <= window:
                count = count + 1
                break
    return count

def cleanMap(nodeConnMap, currTime, window):
    toRemove = []
    for tCP in nodeConnMap:
        if currTime - nodeConnMap[tCP] > window:
            toRemove.append(tCP)
    for old in toRemove:
        del nodeConnMap[old]
    return nodeConnMap
        
if __name__ == "__main__":
    main()
