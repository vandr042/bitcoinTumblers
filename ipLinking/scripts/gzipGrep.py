#!/usr/bin/env python3

import os
import subprocess
import re
import sys




def main(dirPath, regexStr, printFlag):
    regexPat = re.compile(regexStr)
    filesInDir = os.listdir(dirPath)
    filesInDir.sort()
    for tFile in filesInDir:
        print("on " + tFile)
        if ".gz" in tFile:
            gzPath = os.path.join(dirPath, tFile)
            filePath = gzPath[:len(gzPath) - 3]
            gzProc = subprocess.Popen(["gunzip", gzPath])
            gzProc.wait()
            resultList = grepSingleFile(filePath, regexPat)
            gzProc = subprocess.Popen(["gzip", filePath])
            gzProc.wait()

def buildGrepList(dirPath):
    filesInDir = os.listdir(dirPath)
    filesInDir.sort()
    retList = []
    for tFile in filesInDir:
        if ".gz" in tFile:
            retList.append(os.path.join(dirPath, tFile))
    return retList

def handleSingleFile(gzPath, regexPat):
    filePath = gzPath[:len(gzPath) - 3]
    gzProc = subprocess.Popen(["gunzip", gzPath])
    gzProc.wait()
    resultList = grepSingleFile(filePath, regexPat)
    gzProc = subprocess.Popen(["gzip", filePath])
    gzProc.wait()
    return resultList
            
def grepSingleFile(filePath, regexPat):
    retList = []
    fp = open(filePath, "r")
    for line in fp:
        match = regexPat.search(line)
        if match:
            retList.append(line)
    fp.close()
    return retList


if __name__ == "__main__":
    if not len(sys.argv) == 3:
        print("Usage: ./gzipGrep.py <dir> <regex>")
    else:
        main(sys.argv[1], sys.argv[2], True)
