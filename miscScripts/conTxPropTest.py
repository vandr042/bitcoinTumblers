#!/usr/bin/env python3

import argparse
import sys
import random


DELAY_FILE = "delay-5Avg.txt"

NUMBER_PUBLIC_PEERS = 6000
NUMBER_PRIVATE_PEERS = 17000

MIN_FALSE_POSITIVE = 0
MAX_FALSE_POSITIVE = 0
MIN_PEERS_KNOWN = 8
MAX_PEERS_KNOWN = 8
MAX_PEERS = 8

MAX_NET_JITTER = 80
MIN_NET_JITTER = 20

TX_SAMPLE_SIZE = 1

VANTAGE_POINT_COUNT = 3

secondDirConnect = 0.0
top4DirConnect = 0.0
top10DirConnect = 0.0

#TODO add in connections between public nodes

def main(outFileBase):
    fullNodeList = list(genIPSet(NUMBER_PUBLIC_PEERS + NUMBER_PRIVATE_PEERS))
    pubList = fullNodeList[0:NUMBER_PUBLIC_PEERS]
    privList = fullNodeList[NUMBER_PUBLIC_PEERS:]
    #print("done with node creation")
    privConnMap = genGroundTruth(pubList, privList)
    #print("done with node linking")
    fpMap = genFPMap(pubList, privList, privConnMap)
    #print("done with false positive generation")
    pubConnMap = buildPubConn(privConnMap)
    delayModel = buildDelayModel()
    #print("starting sim")
    runSim(pubList, privList, privConnMap, pubConnMap, fpMap, delayModel, outFileBase)
    #print("second: " + str(secondDirConnect/TX_SAMPLE_SIZE) + " top4: " + str(top4DirConnect/TX_SAMPLE_SIZE) + " top10: " + str(top10DirConnect/TX_SAMPLE_SIZE))
def buildDelayModel():
    delays = []
    inFP = open(DELAY_FILE)
    for line in inFP:
        if len(line.strip()) > 0:
            #re-inflate to MS delay
            delays.append(int(float(line)))
    inFP.close()
    return delays

def writeAll(outFPs, outStr):
    for outFP in outFPs:
        outFP.write(outStr)

def runSim(pubList, privList, privConnMap, pubConnMap, fpMap, delayModel, outBase):
    outFPs = []
    for i in range(VANTAGE_POINT_COUNT):
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
    frontList = []
    for i in range(TX_SAMPLE_SIZE):
        currentTime = currentTime + random.randint(1000, 5000)
        sendingPeer = privList[random.randint(0, len(privList) - 1)]
        txID = str(random.getrandbits(32))
        truthFP.write(sendingPeer + "," + txID + "," + str(currentTime) + "\n")
        frontList.append(doTx(sendingPeer, privConnMap, pubConnMap, outFPs, txID, delayModel))
        print("**")
    truthFP.close()
    for outFP in outFPs:
        outFP.close()
   # print(str(frontList))
        

def doTx(sendingNode, privConn, pubConn, outFPs, txID, delayModel):
    eventMap = {}
    reachTime = {}
    lastTime = 0
    #print("******Init Prop Time******")
    for tPeer in privConn[sendingNode]:
        baseTime = genTxDelay(delayModel)
        eventMap[tPeer] = baseTime
      # print(tPeer + " " + str(eventMap[tPeer]))
    while len(eventMap) > 0:
        nextHost = None
        nextTime = 999999999999
        for tKey in eventMap:
            if eventMap[tKey] < nextTime:
                nextHost = tKey
                nextTime = eventMap[tKey]
        reachList = []
        for tKey in eventMap:
            if eventMap[tKey] - MIN_NET_JITTER <= nextTime:
                reachList.append(tKey)
        for tKey in reachList:
            myTime = eventMap[tKey]
            reachTime[tKey] = myTime
            del eventMap[tKey]
            connMap = None
            if tKey in privConn:
                connMap = privConn
            else:
                connMap = pubConn
            for tConnPeer in connMap[tKey]:
                if not (tConnPeer in reachTime or tConnPeer in reachList):
                    timeFromMe =  myTime + genTxDelay(delayModel)
                    if tConnPeer in eventMap:
                        eventMap[tConnPeer] = min(timeFromMe, eventMap[tConnPeer])
                    else:
                        eventMap[tConnPeer] = timeFromMe
    #print("*******Direct Node Reach*****")
#    for tPeer in privConn[sendingNode]:
       #print(tPeer + " " + str(reachTime[tPeer]))
    #print("******First ten reached*****")
    firstTenSet = set([])
    numFromFront = 0
    currentlyGoing = True
    top4Found = False
    top10Found = False
    global secondDirConnect
    global top4DirConnect
    global top10DirConnect
    numDirConnect = 0
    for i in range(0, 10):
        smallest = 999999999999
        nextPeer = None
        for tPeer in reachTime:
            if tPeer in firstTenSet:
                continue
            if tPeer in privConn:
                continue
            if reachTime[tPeer] < smallest:
                smallest = reachTime[tPeer]
                nextPeer = tPeer
        toPrintStr = str(i) + ": " + nextPeer + " " + str(reachTime[nextPeer])
        if nextPeer in privConn[sendingNode]:
            toPrintStr = toPrintStr + " !!!!!"
          #  if (i == 1):
           #     secondDirConnect += 1
            #if (i > 0 and i < 4 and top4Found == False): 
             #   top4DirConnect += 1
   	#	top4DirConnect = True
	 #   if (i > 0): 
	    numDirConnect += 1
	    top10Found = True	
            #counts the number of public nodes, starting with the first, to see the message first
            if currentlyGoing:
                numFromFront = numFromFront + 1
        elif currentlyGoing:
            currentlyGoing = False
        firstTenSet.add(nextPeer)
   	#print(toPrintStr)
    #monitor points
    for i in range(0, VANTAGE_POINT_COUNT):
        #print("******VP: " + str(i) + "*****")
        reachList = []
        toMeMap = {}
        for tPeer in reachTime:
            if tPeer in pubConn:
                toMeMap[tPeer] = reachTime[tPeer] + genTxDelay(delayModel)
	tenCount = 0
        while len(toMeMap) > 0:
            nextPeer = None
            smallest = 99999999999
            for tPeer in toMeMap:
                if toMeMap[tPeer] < smallest:
                    smallest = toMeMap[tPeer]
                    nextPeer = tPeer
            inGT = nextPeer in privConn[sendingNode]
            if len(reachList) < 10 or inGT:
                toPrintStr = str(len(reachList) + 1) + " " + nextPeer + " " + str(toMeMap[nextPeer])
                if inGT:
                    toPrintStr = toPrintStr + " !!!!"
		if (tenCount < 10):
	                print(toPrintStr)
            reachList.append(nextPeer)
            del toMeMap[nextPeer]
	    tenCount += 1
    return numFromFront



#TODO add in "fast broadcast" chance    
def genTxDelay(delayModel):
    return random.choice(delayModel) + random.randint(MIN_NET_JITTER, MAX_NET_JITTER)
    #return random.randint(MIN_NET_JITTER, MAX_NET_JITTER)
            
        

    
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
    parser.add_argument("--vp_count", help = "Vantage Point Count", required=False, type = int, default=VANTAGE_POINT_COUNT)
    parser.add_argument("--pub_count", help = "Number of publically reachable pers", required=False, type = int, default = NUMBER_PUBLIC_PEERS)
    parser.add_argument("--priv_count", help = "Number of private peers", required = False, type = int, default = NUMBER_PRIVATE_PEERS)
    args = parser.parse_args()

    MIN_NET_JITTER = args.min_jitter
    MAX_NET_JITTER = args.max_jitter
    MIN_FALSE_POSITIVE = args.min_fp
    MAX_FALSE_POSITIVE = args.max_fp
    TX_SAMPLE_SIZE = args.sample_size
    VANTAGE_POINT_COUNT = args.vp_count
    NUMBER_PUBLIC_PEERS = args.pub_count
    NUMBER_PRIVATE_PEERS = args.priv_count
    
    main(args.out)
