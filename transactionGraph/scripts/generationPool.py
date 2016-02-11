import generation
class GenerationPool:
    maxGenSize = 2500

    def __init__(self, numOfGen):
        self.genPool = []
        self.numGen = numOfGen
        for i in range(0,numOfGen):
            self.genPool.append(generation.Generation())
        self.currentGenIndex = 0
    
    #attempts to insert into gen.  If current gen is becomes full, gen is cleared and swapped.
    def insert(self, i):
        for j in self.genPool:
            if (j.contains(i) == 1):
                return 0
        
        gen = self.genPool[self.currentGenIndex]
        if (gen.size == self.maxGenSize):
                self.currentGenIndex += 1
                if (self.currentGenIndex > (self.numGen - 1)):
                    self.currentGenIndex = 0
                gen = self.genPool[self.currentGenIndex]
                gen.clear()
        gen.insert(i)
        return 1
                

   
