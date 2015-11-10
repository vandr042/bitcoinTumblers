#!/usr/bin/env python3

import re
import math
import numpy as np
import matplotlib.pyplot as plt

def contains(l,n): #check if already in list
    try:
        l.index(n)
        return 0
    except ValueError as e:
        return -1


if __name__ == "__main__":
    addrDict = dict()
    LOG_FILE = "/scratch/waterhouse/schuch/git/bitcoinTumblers/bitcoinT/harvest.log"
    f = open(LOG_FILE, 'r')
    wFile = open('testWrite.txt','w')
    goodCount = 0
    badCount = 0
    z = 0
    
#extract addresses from file
    for s in f:
        m = re.findall(r'(\d{0,5}\.\d{0,5}\.\d{0,5}\.\d{0,5}\]\:\d{4})', s)
        if (len(m) < 2): # check if bad address
            badCount += 1
        else:
            goodCount += 1
            for i in range(len(m)):
                m[i] = re.sub(r'\]',"",m[i])
            if m[0] in addrDict:
            #Is peer already listed as knowing about this address?
                knownAddr = contains(addrDict[m[0]], m[1])      
                if knownAddr == -1:         # if not in list append
                    addrDict[m[0]].append(m[1])
            else:
            #if first time seeing address, add to dict
                addrDict[m[0]] = [m[1]]
        z+=1

    print("done")
    print("bad " + str(badCount)) #bad addresses
    print("good " + str(goodCount))

#writing values to file for test
    greaterThanOne = 0
    myVals = []
    for key in addrDict:
        wFile.write(str(key) + "," + str(len(addrDict[key])) + "\n")
        myVals.append(len(addrDict[key]))
        if len(addrDict[key]) > 1:
            greaterThanOne += 1
    wFile.close()
    print("greater than one " + str(greaterThanOne))


    x = np.sort(myVals)
    y = np.array(range(len(myVals))) / len(myVals)

    curFig = plt.figure()
    plt.plot(x, y, figure=curFig)
    curFig.savefig("test.pdf", format="pdf")

    










