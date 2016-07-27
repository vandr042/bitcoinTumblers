#!/usr/bin/env python3

import io
import gzipGrep
import re
import pickle

CONN_PAT = re.compile("CONNPOINT")
MAX_GAP = 60 * 60 * 3

def main(firstDir, secondDir, outFile):
    seenMap = clearDir(firstDir)
    kinSeenMap = clearDir(secondDir)
    seenMapList = [seenMap, kinSeenMap]
    workMap = listify(seenMapList)
    onlineCount(workMap, outFile)

def clearDir(dirPath):
    seenMap = {}
    filesToParse = gzipGrep.buildGrepList(dirPath)
    for tFile in filesToParse:
        print("on " + tFile)
        onlineStrs = gzipGrep.handleSingleFile(tFile, CONN_PAT)
        for tStr in onlineStrs:
            tokens = tStr.split(",")
            if len(tokens) < 5:
                continue
            if not tokens[3] in seenMap:
                seenMap[tokens[3]] = set([])
            seenMap[tokens[3]].add(int(tokens[4]))
    return seenMap


def listify(mapList):
    fullSets = {}
    for tMap in mapList:
        for tKey in tMap:
            if not tKey in fullSets:
                fullSets[tKey] = tMap[tKey]
            else:
                fullSets[tKey] = fullSets[tKey] | tMap[tKey]
    listMap = {}
    for tKey in fullSets:
        tempList = []
        for tVal in fullSets[tKey]:
            tempList.append(tVal)
        tempList.sort()
        listMap[tKey] = tempList
    return listMap

def onlineCount(listMap, outFile):
    durationList = []
    outFP = open(outFile, "w")
    firstSeen = {}
    lastSeen = {}
    while len(listMap) > 0:
        curSmallest = 99999999999999999
        curLargest = None
        curKey = None
        for tKey in listMap:
            if listMap[tKey][0] < curSmallest:
                curSmallest = listMap[tKey][0]
                curKey = tKey
        stopPos = 0
        tempList = listMap[curKey]
        for i in range(1, len(tempList)):
            if tempList[i] - tempList[i - 1] > MAX_GAP:
                stopPos = i - 1
                break
        curLargest = tempList[stopPos]
        tempList = tempList[stopPos + 1:]
        if len(tempList) == 0:
            del listMap[curKey]
            print(str(len(listMap)))
        else:
            listMap[curKey] = tempList
        if not curKey in firstSeen:
            firstSeen[curKey] = curSmallest
        lastSeen[curKey] = curLargest
        toDel = []
        for tKey in lastSeen:
            if curSmallest - lastSeen[tKey] > MAX_GAP:
                durationList.append(lastSeen[tKey] - firstSeen[tKey])
                toDel.append(tKey)
        for tKey in toDel:
            del firstSeen[tKey]
            del lastSeen[tKey]
        outFP.write(str(curSmallest) + "," + str(len(firstSeen)) + "\n")
    outFP.close()
    return durationList

if __name__ == "__main__":
    main("../parsed", "../kin-parsed", "both.csv")
