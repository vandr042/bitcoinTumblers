#!/usr/bin/env python3

import os
import shutil
import subprocess
import time

JARPATH = "../jars"
BUILD_DEST = "bin/"
SRC_DEST = "src/"
BASE_SOURCE_FILE = "src/control/Manager.java"
BASE_CLASS = "control.Manager"
JAR_STR = JARPATH + "/commonsLogging.jar:" + JARPATH + "/mjsBCJ.jar:" + JARPATH + "/slf4j.jar:" + JARPATH + "/argparse4j-0.5.0.jar"

def main():
    compile()
    if os.listdir("recovery"):
        runProc = run(True)
    else:
        runProc = run(False)
    while True:
        runProc.wait()
        print("PROCESS FAILED")
        time.sleep(30)
        print("PROCESS RESTARTED")
        runProc = run(True)
    
def compile():
    print("Starting compile")
    shutil.rmtree(BUILD_DEST)
    os.mkdir(BUILD_DEST)
    compileArgs = ["javac", "-cp", JAR_STR + ":" + SRC_DEST, "-d", BUILD_DEST, BASE_SOURCE_FILE]
    compProc = subprocess.Popen(compileArgs)
    compProc.wait()
    print("done compiling")

def run(restart):
    procArgs = ["java", "-XX:+UseG1GC","-cp", JAR_STR + ":" + BUILD_DEST, "-Xmx19G", "-d64", BASE_CLASS]
    if restart:
        procArgs = procArgs + ["--recovery"]
    runProc = subprocess.Popen(procArgs)
    return runProc

if __name__ == "__main__":
    main()
