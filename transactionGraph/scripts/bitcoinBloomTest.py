import bloomHandler
import generation
import generationPool
import time

class BitcoinBloomTest:
    def __init__(self, poolPercentage, numAddresses):
        self.numAddresses = numAddresses
        self.goalPercentage = poolPercentage
        self.currentPercentage = 0
        self.bloomFilter = bloomHandler.BloomHandler(numAddresses,3)
        self.connectionSet = set()
        self.rounds = 0

    def __simRound(self):
        newAddrs = self.bloomFilter.getAddrs()
        for i in newAddrs:
            self.connectionSet.add(i)

    def getNumAddr(self):
        return len(self.connectionSet)

    def runTest(self):
        results = 0
        startTime = time.time()
        elapsedTime = 0
        while (self.currentPercentage < self.goalPercentage):
            self.rounds += 1
            self.__simRound()
            self.currentPercentage = (self.getNumAddr()/self.bloomFilter.poolSize)*100
        elapsedTime = time.time()-startTime
        results = self.rounds
        
        return(results)
