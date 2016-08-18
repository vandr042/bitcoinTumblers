#!/usr/bin/env python3

import sys
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

def main(plMode, vpMode):
    compile()
    runPrune(plMode)
    if os.listdir("recovery"):
        runProc = run(True, plMode, vpMode)
    else:
        runProc = run(False, plMode, vpMode)
    while True:
        runProc.wait()
        print("PROCESS FAILED")
        time.sleep(10)
        print("PROCESS RESTARTED")
        runProc = run(True, plMode, vpMode)
    
def compile():
    print("Starting compile")
    shutil.rmtree(BUILD_DEST)
    os.mkdir(BUILD_DEST)
    compileArgs = ["javac", "-cp", JAR_STR + ":" + SRC_DEST, "-d", BUILD_DEST]
    compProc = subprocess.Popen(compileArgs + [BASE_SOURCE_FILE])
    compProc.wait()
    compProc = subprocess.Popen(compileArgs + ["src/analysis/SpyLogCleaner.java"])
    compProc.wait()
    print("done compiling")

def runPrune(plMode):
    procArgs = ["java", "-Xmx6G", "-cp", JAR_STR + ":" + BUILD_DEST, "-d64", "analysis.SpyLogCleaner"]
    if plMode:
        procArgs = procArgs + ["--pl"]
    subprocess.Popen(procArgs)
    
def run(restart, plMode, vpMode):
    procArgs = ["java", "-XX:+UseG1GC","-cp", JAR_STR + ":" + BUILD_DEST, "-Xmx7G", "-d64", BASE_CLASS]
    if plMode:
        procArgs = procArgs + ["--plMan"]
    if vpMode:
        procArgs = procArgs + ["--vantagepoint"]
    if restart:
        procArgs = procArgs + ["--recovery"]
    runProc = subprocess.Popen(procArgs)
    return runProc

if __name__ == "__main__":
    main("--pl" in sys.argv, "--vp" in sys.argv)
