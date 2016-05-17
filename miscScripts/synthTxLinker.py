#!/usr/bin/env python3

import argparse
import sys
import random


DELAY_FILE = "delays-10.txt"

NUMBER_PUBLIC_PEERS = 6000
NUMBER_PRIVATE_PEERS = 30000

MIN_FALSE_POSITIVE = 300
MAX_FALSE_POSITIVE = 300
MIN_PEERS_KNOWN = 8
MAX_PEERS_KNOWN = 8
MAX_PEERS = 8

MAX_NET_JITTER = 100
MIN_NET_JITTER = 20

TX_SAMPLE_SIZE = 1000


#TODO add in connections between public nodes

def main(outFileBase):
    fullNodeList = list(genIPSet(NUMBER_PUBLIC_PEERS + NUMBER_PRIVATE_PEERS))
    pubList = fullNodeList[0:NUMBER_PUBLIC_PEERS]
    privList = fullNodeList[NUMBER_PUBLIC_PEERS:]
    print("done with node creation")
    privConnMap = genGroundTruth(pubList, privList)
    print("done with node linking")
    fpMap = genFPMap(pubList, privList, privConnMap)
    print("done with false positive generation")
    pubConnMap = buildPubConn(privConnMap)
    delayModel = buildDelayModel()
    print("starting sim")
    runSim(pubList, privList, privConnMap, pubConnMap, fpMap, delayModel, outFileBase)

def buildDelayModel():
    delays = []
    inFP = open(DELAY_FILE)
    for line in inFP:
        if len(line.strip()) > 0:
            #re-inflate to MS delay
            delays.append(int(line) * 100)
    inFP.close()
    return delays

def writeAll(outFPs, outStr):
    for outFP in outFPs:
        outFP.write(outStr)

def runSim(pubList, privList, privConnMap, pubConnMap, fpMap, delayModel, outBase):
    outFPs = []
    for i in range(3):
        outFPs.append(open(outBase + "-txLinkSynth-out" + str(i) + ".log", "w"))
    truthFP = open(outBase + "-txLinkSynth-groundTruth.log", "w")
    currentTime = 0
    for tPub in pubList:
        writeAll(outFPs, "conn," + tPub + "," + str(currentTime) + "\n")
        currentTime = currentTime + random.randint(5, 2000)
    for tPriv in privList:
        #TODO make detects independant
        detectCount = random.randint(MIN_PEERS_KNOWN, MAX_PEERS_KNOWN)
        tList = list(privConnMap[tPriv])
        for i in range(detectCount):
            writeAll(outFPs, "remoteconn," + tPriv + "," + tList[i] + "," + str(currentTime) + "\n")
            currentTime = currentTime + random.randint(3, 500)
    for tPriv in privList:
        for tFP in fpMap[tPriv]:
            #TODO should False Positives be independent?
            writeAll(outFPs, "remoteconn," + tPriv + "," + tFP + "," + str(currentTime) + "\n")
            currentTime = currentTime + random.randint(3, 500)
    for i in range(TX_SAMPLE_SIZE):
        if i % 100 == 0:
            print(str(i))
        currentTime = currentTime + random.randint(1000, 5000)
        sendingPeer = privList[random.randint(0, len(privList) - 1)]
        txID = str(random.getrandbits(32))
        truthFP.write(sendingPeer + "," + txID + "," + str(currentTime) + "\n")
        currentTime = doTx(sendingPeer, privConnMap, pubConnMap, outFPs, currentTime, txID, delayModel)
    truthFP.close()
    for outFP in outFPs:
        outFP.close()
        

def doTx(sendingNode, privConn, pubConn, outFPs, time, txID, delayModel):
    eventMap = {}
    reachTime = {}
    lastTime = 0
    for tPeer in privConn[sendingNode]:
        baseTime = time + genTxDelay(delayModel)
        eventMap[tPeer] = baseTime
    while len(eventMap) > 0:
        nextHost = None
        nextTime = 999999999999
        for tKey in eventMap:
            if eventMap[tKey] < nextTime:
                nextHost = tKey
                nextTime = eventMap[tKey]
        reachTime[nextHost] = nextTime
        del eventMap[nextHost]
        connMap = None
        if nextHost in privConn:
            connMap = privConn
        else:
            connMap = pubConn
        for tConnPeer in connMap[nextHost]:
            if not tConnPeer in reachTime:
                timeFromMe = nextTime + genTxDelay(delayModel)
                if tConnPeer in eventMap:
                    eventMap[tConnPeer] = min(timeFromMe, eventMap[tConnPeer])
                else:
                    eventMap[tConnPeer] = timeFromMe
    for outFP in outFPs:
        toMeMap = {}
        for tPeer in reachTime:
            if tPeer in pubConn:
                toMeMap[tPeer] = reachTime[tPeer] + genTxDelay(delayModel)
        myLastTime = 0
        while len(toMeMap) > 0:
            nextPeer = None
            smallest = 99999999999
            for tPeer in toMeMap:
                if toMeMap[tPeer] < smallest:
                    smallest = toMeMap[tPeer]
                    nextPeer = tPeer
            outFP.write("tx," + txID + "," + nextPeer + "," + str(smallest) + "\n")
            del toMeMap[nextPeer]
            myLastTime = smallest
        lastTime = max(lastTime, myLastTime)
    print("derp")
    return lastTime


#TODO add in "fast broadcast" chance    
def genTxDelay(delayModel):
    return random.choice(delayModel) + random.randint(MIN_NET_JITTER, MAX_NET_JITTER)
            
        

    
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

def genFPMap(publicNodes, privateNodes, connMap):
    retMap = {}
    for tNode in privateNodes:
        connSet = connMap[tNode]
        falseCount = random.randint(MIN_FALSE_POSITIVE, MAX_FALSE_POSITIVE)
        falseSet = set([])
        while len(falseSet) < falseCount:
            posible = random.choice(publicNodes)
            if not posible in connSet:
                falseSet.add(posible)
        retMap[tNode] = falseSet
    return retMap

def genIPSet(count):
    retSet = set([])
    while len(retSet) < count:
        ipStr = "[" + str(random.randint(1, 255)) + "." + str(random.randint(1, 255)) + "." + str(random.randint(1, 255)) + "." + str(random.randint(1, 255)) + "]:8333"
        retSet.add(ipStr)
    return retSet
        
    


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", help="Out base", required=True)
    parser.add_argument("--min_jitter", help = "Min net jitter", required=False, type=int, default=MIN_NET_JITTER)
    parser.add_argument("--max_jitter", help = "Max net jitter", required=False, type=int, default=MAX_NET_JITTER)
    parser.add_argument("--min_fp", help = "Min false positives", required = False, type=int, default = MIN_FALSE_POSITIVE)
    parser.add_argument("--max_fp", help = "Max false positives", required = False, type = int, default = MAX_FALSE_POSITIVE)
    parser.add_argument("--sample_size", help = "Sample size", required=False, type = int, default=TX_SAMPLE_SIZE)
    args = parser.parse_args()

    MIN_NET_JITTER = args.min_jitter
    MAX_NET_JITTER = args.max_jitter
    MIN_FALSE_POSITIVE = args.min_fp
    MAX_FALSE_POSITIVE = args.max_fp
    TX_SAMPLE_SIZE = args.sample_size
    
    main(args.out)
