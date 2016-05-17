#!/usr/bin/env python3

import subprocess
import matplotlib.pyplot as plot

def main():
    javacArgs = ["javac", "-cp", "../transactionGraph/src", "-s", "../transactionGraph/bin", "../transactionGraph/src/peerLink/PeerLinkTester.java"]
    javacProc = subprocess.Popen(javacArgs)
    javacProc.wait()
    for i in range(0, 3):
        fpSize = i * 100
        runGen(fpSize)
        runJava(fpSize)

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
