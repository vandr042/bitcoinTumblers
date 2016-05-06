#!/usr/bin/env python3

import sys
import random

OUT_FILE_BASE = "mjsFull"
DELAY_FILE = "delays-10.txt"

NUMBER_PUBLIC_PEERS = 6000
NUMBER_PRIVATE_PEERS = 30000

MIN_PEERS_KNOWN = 4
MAX_PEERS_KNOWN = 7
MAX_PEERS = 7

def main():
    fullNodeList = list(genIPSet(NUMBER_PUBLIC_PEERS + NUMBER_PRIVATE_PEERS))
    pubList = fullNodeList[0:NUMBER_PUBLIC_PEERS]
    privList = fullNodeList[NUMBER_PUBLIC_PEERS:]
    privConnMap = genGroundTruth(pubList, privList)
    pubConnMap = buildPubConn(privConnMap)
    delayModel = buildDelayModel()
    runSim(pubList, privList, privConnMap, pubConnMap, delayModel)

#TODO re-inflate (mult by 100)
def buildDelayModel():
    delays = []
    inFP = open(DELAY_FILE)
    for line in inFP:
        if len(line.strip()) > 0:
            delays.append(int(line))
    inFP.close()
    return delays
    
def runSim(pubList, privList, privConnMap, pubConnMap, delayModel):
    outFP = open(OUT_FILE_BASE + "-peer-finder-synth-out.log", "w")
    truthFP = open(OUT_FILE_BASE + "-peer-finder-synth-groundTruth.log", "w")
    currentTime = 0
    for tPub in pubList:
        outFP.write("conn," + tPub + "," + str(currentTime) + "\n")
        currentTime = currentTime + random.randint(5, 2000)
    for tPriv in privList:
        detectCount = random.randint(MIN_PEERS_KNOWN, MAX_PEERS_KNOWN)
        tList = list(privConnMap[tPriv])
        for i in range(detectCount):
            outFP.write("remoteconn," + tPriv + "," + tList[i] + "," + str(currentTime) + "\n")
            currentTime = currentTime + random.randint(3, 500)
    for i in range(1000):
        if i % 100 == 0:
            print(str(i))
        currentTime = currentTime + random.randint(1000, 5000)
        sendingPeer = privList[random.randint(0, len(privList) - 1)]
        txID = str(random.getrandbits(32))
        truthFP.write(sendingPeer + "," + txID + "," + str(currentTime) + "\n")
        currentTime = doTx(sendingPeer, privConnMap, pubConnMap, outFP, currentTime, txID, delayModel)
    truthFP.close()
    outFP.close()
        

def doTx(sendingNode, privConn, pubConn, fp, time, txID, delayModel):
    eventMap = {}
    for tPeer in privConn[sendingNode]:
        baseTime = time + genTxDelay(delayModel)
        if tPeer in eventMap:
            eventMap[tPeer] = min(baseTime, eventMap[tPeer])
        else:
            eventMap[tPeer] = baseTime
        for tPrivPeer in pubConn[tPeer]:
            nextTime = baseTime + genTxDelay(delayModel)
            for tPubPeer in privConn[tPrivPeer]:
                nextNextTime = nextTime + genTxDelay(delayModel)
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
    
#TODO add network "jitter" (random number between 20 ms and 500 ms)
def genTxDelay(delayModel):
    return random.choice(delayModel)
            
        

    
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
        while len(peerSet) < MAX_PEERS:
            peerSet.add(random.choice(randomPubNodeList))
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
