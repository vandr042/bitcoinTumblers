import bitcoinBloomTest
import numpy
import threading
class StepTest:
    def __init__(self,poolPercentage, stepSize):
        self.step = stepSize
        self.numAddresses = stepSize
        self.goalPercentage = poolPercentage
        self.testResults = {}

    def printResults(self):
        print("(testSize, [mean rounds]")
        for i in self.testResults.items():
            print(i)

    def writeResults(self):
        f = open('bloomTestStats.txt', 'w')
        f.write("(testSize,[mean rounds, std, median])\n")
        for i in self.testResults.items():
            f.write(str(i))
            f.write("\n")
        f.close()

    def threadFunc(self,numAddr):
        roundsList = []
        for i in range(0,1000):
            test = bitcoinBloomTest.BitcoinBloomTest(self.goalPercentage, numAddr)
            roundsList.append(test.runTest())
        meanRounds = numpy.mean(roundsList)
        std = numpy.std(roundsList)
        median = numpy.std(roundsList)
        self.testResults[numAddr] = [meanRounds,std,median]
        return

    def runTest(self):
        threads = []
        while (self.numAddresses <= 600000):
            threads.append(threading.Thread(target = self.threadFunc, args = (self.numAddresses,)))
            threads[len(threads)-1].start()
            self.numAddresses += self.step
        for i in threads:
            i.join()
        return


    
                
x = StepTest(90,25000)
x.runTest()
x.writeResults()

