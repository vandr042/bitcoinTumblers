#!/usr/bin/env python3

import io
import sys

def main():
    logFile = sys.argv[1]
    ip = sys.argv[2]
    tsMap = uniqueTS(logFile, ip)
    connPoints = ["104.236.129.178:8333", "213.167.17.6:8333", "24.207.98.44:8333", "100.38.11.146:8333", "37.1.202.134:8333", "62.63.157.78:8333", "193.0.109.3:8333", "195.154.174.226:8333"]
    print("peers who know me " + str(len(tsMap)))
    multCount = 0
    totalTSSet = set([])
    for tKey in tsMap:
        totalTSSet = totalTSSet.union(tsMap[tKey])
        if len(tsMap[tKey]) > 1:
            mutlCount = multCount + 1
    print("mult count " + str(multCount))
    print("unique TS " + str(len(totalTSSet)))
    foundCount = 0
    for tPoint in connPoints:
        if tPoint in tsMap:
            foundCount = foundCount + 1
            print(tPoint + " -> " + str(tsMap[tPoint]))
    print("conn points found " + str(foundCount))
    tsToNode = {}
    for tNode in tsMap:
        for tTs in tsMap[tNode]:
            if not tTs in tsToNode:
                tsToNode[tTs] = set([])
            tsToNode[tTs].add(tNode)

    for tTs in tsToNode:
        print(tTs + " -> " + str(len(tsToNode[tTs])))
            
def uniqueTS(logFile, ip):
    tsCount = {}
    derpCount = 0
    fp = open(logFile, "r")
    for line in fp:
        if ",INTIP," in line:
            tokens = line.split(",")
            ts = tokens[4]
            lineip = tokens[3]
            remPeer = tokens[2]
            if not ip == lineip:
                derpCount = derpCount + 1
            else:
                if not remPeer in tsCount:
                    tsCount[remPeer] = set([])
                tsCount[remPeer].add(ts)
    print("derp count is " + str(derpCount))
    return tsCount

if __name__ == "__main__":
    main()
