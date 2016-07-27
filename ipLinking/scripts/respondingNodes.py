#!/usr/bin/env python3

import sys
import matplotlib.pyplot as plt

def main():


    seen = set([])
    solSeen = set([])

    
    startTS = -1
    currTS = 0.0
    
    times = []
    online = []
    responsive = []
    
    logFP = open(sys.argv[1], "r")
    for line in logFP:
        if "ADDRRCV" in line:
            splits = line.split(",")
            ts = float(splits[0]) / (3600.0 * 1000.0)
            if startTS == -1:
                startTS = ts
            ts = ts - startTS
            if ts > currTS:
                if not currTS == 0.0:
                    online.append(len(seen))
                    responsive.append(len(solSeen))
                    times.append(currTS)
                    seen = set([])
                    solSeen = set([])
                currTS = currTS + 0.1
                print("on " + str(currTS))
            host = splits[3]
            seen.add(host)
            if int(splits[2]) >= 500:
                solSeen.add(host)
    logFP.close()


    curFig = plt.figure()
    plt.plot(times, online, figure=curFig, lw = 5.0, label = "online")
    plt.plot(times, responsive, figure=curFig, lw = 5.0, label = "responsive")
    plt.xlabel("Time since start (hrs)")
    plt.ylabel("Nodes responding (Past 6 mins)")
    plt.legend()
    curFig.savefig(sys.argv[1] + "-respondingPeers.pdf", format = "pdf")
            
                
if __name__ == "__main__":
    main()
