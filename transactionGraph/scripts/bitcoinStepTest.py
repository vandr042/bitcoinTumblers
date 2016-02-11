import bitcoinBloomTest
import numpy
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
        f = open('bloomTestStats', 'w')
        f.write("(testSize,[mean rounds, std, median])\n")
        for i in self.testResults.items():
            f.write(i)
            f.write("\n")

        

    def runTest(self):
        while (self.numAddresses <= 600000):
            roundsList = []
            for i in range(0,1000):
                test = bitcoinBloomTest.BitcoinBloomTest(self.goalPercentage, self.numAddresses)
                roundsList.append(test.runTest())
            meanRounds = numpy.mean(roundsList)
            std = numpy.std(roundsList)
            median = numpy.std(roundsList)
            self.testResults[self.numAddresses] = [meanRounds,std,median]
            self.numAddresses += self.step
                
x = StepTest(90,25000)
x.runTest()
x.printResults()
x.writeResults()

