#!/usr/bin/env python3

import sys
import random

def main():
    fullNodeList = list(genIPSet(14000))
    pubList = fullNodeList[0:4000]
    privList = fullNodeList[4000:]
    privConnMap = genGroundTruth(pubList, privList)
    pubConnMap = buildPubConn(privConnMap)
    runSim(pubList, privList, privConnMap, pubConnMap)

def runSim(pubList, privList, privConnMap, pubConnMap):
    outFP = open("peer-finder-synth-out.log", "w")
    truthFP = open("peer-finder-synth-groundTruth.log", "w")
    currentTime = 0
    for tPub in pubList:
        outFP.write("conn," + tPub + "," + str(currentTime) + "\n")
        currentTime = currentTime + random.randint(5, 2000)
    for tPriv in privList:
        detectCount = random.randint(4, 7)
        tList = list(privConnMap[tPriv])
        for i in range(detectCount):
            outFP.write("remoteconn," + tPriv + "," + tList[i] + "," + str(currentTime) + "\n")
            currentTime = currentTime + random.randint(3, 500)
    for i in range(100):
        currentTime = currentTime + random.randint(1000, 5000)
        sendingPeer = privList[random.randint(0, len(privList) - 1)]
        txID = str(random.getrandbits(32))
        truthFP.write(sendingPeer + "," + txID + "," + str(currentTime) + "\n")
        currentTime = doTx(sendingPeer, privConnMap, pubConnMap, outFP, currentTime, txID)
    truthFP.close()
    outFP.close()
        

def doTx(sendingNode, privConn, pubConn, fp, time, txID):
    eventMap = {}
    for tPeer in privConn[sendingNode]:
        baseTime = time + random.randint(2,10)
        if tPeer in eventMap:
            eventMap[tPeer] = min(baseTime, eventMap[tPeer])
        else:
            eventMap[tPeer] = baseTime
        for tPrivPeer in pubConn[tPeer]:
            nextTime = baseTime + random.randint(2, 10)
            for tPubPeer in privConn[tPrivPeer]:
                nextNextTime = nextTime + random.randint(2,10)
                if tPubPeer in eventMap:
                    eventMap[tPubPeer] = min(nextNextTime, eventMap[tPubPeer])
                else:
                    eventMap[tPubPeer] = nextNextTime
    doneSet = set([])
    lastTime = 0
    while not len(doneSet) == len(eventMap):
        smallestTime = sys.maxsize
        curNode = None
        for tTalker in eventMap:
            if tTalker in doneSet:
                continue
            if eventMap[tTalker] < smallestTime:
                smallestTime = eventMap[tTalker]
                curNode = tTalker
        doneSet.add(curNode)
        fp.write("tx," + txID + "," + curNode + "," + str(smallestTime) + "\n")
        lastTime = smallestTime
    return lastTime
    
        
            
        
    
    
def buildPubConn(privConMap):
    retMap = {}
    for tKey in privConMap:
        for tPeer in privConMap[tKey]:
            if not tPeer in retMap:
                retMap[tPeer] = set([])
            retMap[tPeer].add(tKey)
    return retMap

def genGroundTruth(publicNodes, privateNodes):
    retMap = {}
    randomPubNodeList = publicNodes
    totalPubNodes = len(randomPubNodeList)
    for tNode in privateNodes:
        peerSet = set([])
        while len(peerSet) < 7:
            peerSet.add(randomPubNodeList[random.randint(0, totalPubNodes - 1)])
        retMap[tNode] = peerSet
    return retMap
    
def genIPSet(count):
    retSet = set([])
    while len(retSet) < count:
        ipStr = "[" + str(random.randint(1, 255)) + "." + str(random.randint(1, 255)) + "." + str(random.randint(1, 255)) + "." + str(random.randint(1, 255)) + "]:8333"
        retSet.add(ipStr)
    return retSet
        
    


if __name__ == "__main__":
    main()
