#!/usr/bin/env python3

import os
import subprocess
import re
import sys


def main(dirPath, regexStr):
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
            grepSingleFile(filePath, regexPat)
            gzProc = subprocess.Popen(["gzip", filePath])
            gzProc.wait()

def grepSingleFile(filePath, regexPat):
    fp = open(filePath, "r")
    for line in fp:
        match = regexPat.search(line)
        if match:
            print(line)
    fp.close()


if __name__ == "__main__":
    if not len(sys.argv) == 3:
        print("Usage: ./gzipGrep.py <dir> <regex>")
    else:
        main(sys.argv[1], sys.argv[2])
