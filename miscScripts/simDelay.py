#!/usr/bin/env python3

import sys
import random
import math

#GENERATES BITCOIN DELAY MODEL IN MILLISECONDS
def main(avgDelay, count):
    for i in range(0, count):
        print(str(genNextSend(avgDelay) / 1000))

def genNextSend(avg):
    randVal = float(random.randint(0, math.pow(2, 48) - 1))
    frac = math.log1p(randVal * -0.0000000000000035527136788)
    return int(frac * avg * -1000000.0 + 0.5)

if __name__ == "__main__":
    main(int(sys.argv[1]), int(sys.argv[2]))
