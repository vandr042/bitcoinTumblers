import generation
import generationPool
import random
import math


class BloomHandler:
    MAX_GEN_SIZE = 2500
    def __init__(self, addressPoolSize, numGen):
        #self.addressPool = []
        self.genPool = generationPool.GenerationPool(numGen)
        self.poolSize = addressPoolSize
        #for i in range(0,self.poolSize):
            #self.addressPool.append(i)
        if (addressPoolSize <= 10869):
            self.testRange = math.floor(addressPoolSize*0.23)
        else:
            self.testRange = 2500

    #randomly retreives 2500 addresses from the address pool
    def __getAddresses(self):
        pool = []
        for i in range(0,self.testRange):
            newAddr = random.randint(1,self.poolSize)
            pool.append(newAddr)
        return pool

    #returns filtered 
    def getAddrs(self):
        pool = self.__getAddresses()
        pSize = 0
        retPool = []
        j = 0
        while (pSize < 1000 and j < 2500):
            result = self.genPool.insert(pool[j])
            if (result == 1):
                retPool.append(pool[j])
                pSize+=1
            j+=1
        return retPool
        
                
