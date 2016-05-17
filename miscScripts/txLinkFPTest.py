#!/usr/bin/env python3

import re
import subprocess
import matplotlib.pyplot as plt

OUTPUT_PAT = re.compile("Depth: (.+) Accuracy: (.+) avgSetSize: (.+) non zero avg set size: (.+)")


def main():
    javacArgs = ["javac", "-cp", "../transactionGraph/src", "-s", "../transactionGraph/bin", "../transactionGraph/src/peerLink/PeerLinkTester.java"]
    javacProc = subprocess.Popen(javacArgs)
    javacProc.wait()
    for i in range(0, 3):
        fpSize = i * 100
        runGen(fpSize)
        runJava(fpSize)
    accFig = plt.figure()
    sizeFig = plt.figure()
    for i in range(0, 3):
        fpCount = i * 100
        setSizes = []
        acc = []
        depth = []
        fp = open("../temp/" + str(fpCount) + "-voting.txt", "r")
        for line in fp:
            mat = OUTPUT_PAT.search(line)
            if mat:
                depth.append(int(mat.group(1)))
                acc.append(float(mat.group(2)))
                setSizes.append(float(mat.group(4)))
        fp.close()
        plt.plot(depth, acc, figure=accFig, label = str(fpCount))
        plt.plot(depth, setSizes, figure=sizeFig, label = str(fpCount))
    accFig.savefig("acc.pdf", format="pdf")
    sizeFig.savefig("size.pdf", format="pdf")
        

def runJava(fpCount):
    javaArgs = ["java", "-Xmx10G", "-cp", "../transactionGraph/bin", "peerLink.PeerLinkTester", "../temp/" + str(fpCount) + "-txLinkSynth"]
    javaProc = subprocess.Popen(javaArgs)
    javaProc.wait()
    

def runGen(fpCount):
    genArgs = ["./synthTxLinker.py", "--out", "../temp/" + str(fpCount), "--max_fp", str(fpCount), "--min_fp", str(fpCount)]
    genProc = subprocess.Popen(genArgs)
    genProc.wait()
    

if __name__ == "__main__":
    main()
