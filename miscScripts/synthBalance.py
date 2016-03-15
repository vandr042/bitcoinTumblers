#!/usr/bin/env python3

import random
import sys

def main():
    inputKeys = buildInputKeys(1000)
    outputKeyMap = buildOutputKeys(inputKeys)
    runSim(inputKeys, outputKeyMap)

def runSim(inputKeys, outputKeyMap):
    time = 0
    currBalance = {}
    outFP = open("balance-synth.log", "w")
    groundFP = open("balance-ground-truth.log", "w")
    eventMap = {}
    for tIn in inputKeys:
        currBalance[tIn] = 0
    for i in range(30000):
        time = time + random.randint(10, 600)
        flip = random.randint(0,3)
        if flip == 0 and canDoWithdrawl(currBalance):
            withKey = getRandomWithdrawer(currBalance)
            withAmount = random.randint(1, currBalance[withKey])
            currBalance[withKey] = currBalance[withKey] - withAmount
            groundFP.write(withKey + "," + str(withAmount) + "," + str(time) + "\n")
            outKeyList = outputKeyMap[withKey]
            for i in range(len(outKeyList) - 1):
                if withAmount == 0:
                    break
                myTime = time + random.randint(600, 3600)
                while myTime in eventMap:
                    myTime = myTime + 1
                thisAmount = random.randint(1, withAmount)
                withAmount = withAmount - thisAmount
                eventMap[myTime] = "withdraw," + outKeyList[i] + "," + str(thisAmount) + "," + str(myTime) + "\n"
            if withAmount > 0:
                myTime = time + random.randint(600, 3600)
                while myTime in eventMap:
                    myTime = myTime + 1
                eventMap[myTime] = "withdraw," + outKeyList[len(outKeyList) - 1] + "," + str(withAmount) + "," + str(myTime) + "\n"
        else:
            depKey = inputKeys[random.randint(0, len(inputKeys) - 1)]
            depAmount = random.randint(1, 1000)
            currBalance[depKey] = currBalance[depKey] + depAmount
            while time in eventMap:
                time = time + 1
            eventMap[time] = "deposit," + depKey + "," + str(depAmount) + "," + str(time) + "\n"

    doneSet = set([])
    while len(doneSet) < len(eventMap):
        curSmallest = sys.maxsize
        for tTime in eventMap:
            if tTime in doneSet:
                continue
            if curSmallest > tTime:
                curSmallest = tTime
        doneSet.add(curSmallest)
        outFP.write(eventMap[curSmallest])
    outFP.close()
    groundFP.close()

def getRandomWithdrawer(balanceMap):
    posList = []
    for tAcct in balanceMap:
        if balanceMap[tAcct] > 0.0:
            posList.append(tAcct)
    return posList[random.randint(0, len(posList) - 1)]
    
def canDoWithdrawl(balanceMap):
    for tAcct in balanceMap:
        if balanceMap[tAcct] > 0.0:
            return True
    return False
    
def buildInputKeys(count):
    retSet = set([])
    while len(retSet) < count:
        retSet.add(str(random.getrandbits(40)))
    return list(retSet)
        

def buildOutputKeys(inputKeys):
    retMap = {}
    outKeySet = set([])
    for tInput in inputKeys:
        numOutputKeys = random.randint(1, 4)
        myOutSet = set([])
        while len(myOutSet) < numOutputKeys:
            tmpStr = str(random.getrandbits(40))
            if tmpStr in inputKeys or tmpStr in outKeySet:
                continue
            myOutSet.add(tmpStr)
            outKeySet.add(tmpStr)
        retMap[tInput] = list(myOutSet)
    return retMap
            

if __name__ == "__main__":
    main()
