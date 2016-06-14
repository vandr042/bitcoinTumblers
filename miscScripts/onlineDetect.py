#!/usr/bin/env python3

import random
import sys

DELAY_FILE = "announce-delay.txt"

def fullTest(number):
    for i in range(0, 10):
        fract = float(i) * 0.05 + 0.5
        print(str(fract) + "," + str(singleTest(10000, fract, number)))

def singleTest(sampleCount, confidence, numberBCast):
    delays = loadDelays()
    minutesReq = []
    for i in range(0, sampleCount):
        myTimes = []
        while len(myTimes) < 8:
            time = 0
            while time < 60.0:
                time = time + random.choice(delays)
            myTimes.append(time)
        myTimes.sort()
        minutesReq.append(myTimes[numberBCast - 1])
    minutesReq.sort()
    return minutesReq[int(len(minutesReq) * confidence)]

# Fetches the delays, converting them into MINUTES for ease of use
def loadDelays():
    fp = open(DELAY_FILE, "r")
    delayVals = []
    for line in fp:
        delayVals.append(float(line) / 60000)
    fp.close()
    return delayVals

if __name__ == "__main__":
    fullTest(int(sys.argv[1]))
#    print(str(singleTest(int(sys.argv[1]), float(sys.argv[2]), int(sys.argv[3]))))
