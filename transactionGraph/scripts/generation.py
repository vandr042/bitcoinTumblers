
import math

class Generation:
    def __init__(self):
        self.genPool = set()
        self.size = 0

    #insert will return 1 if appended, 0 if address already contained
    def insert(self, i):
        self.genPool.add(i)
        self.size+=1

    def clear(self):
        self.genPool.clear()
        self.size = 0

    def contains(self,i):
        if i in self.genPool:
            return 1
        else:
            return 0

        
                
