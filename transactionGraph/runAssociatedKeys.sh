#!/bin/bash

JAR_DIR="../jars/"
JAR_PATH=$JAR_DIR"bitcoinj.jar:"$JAR_DIR"slf4j.jar:"$JAR_DIR"commonsLogging.jar"

#TODO might need a bin dir first
rm -rf bin/
mkdir bin/
javac -cp $JAR_PATH:src/ -d bin/ src/bitblender/GetPool.java
#TODO update mem to your system's size
java -cp $JAR_PATH:bin/ -Xmx32G -d64 bitblender.GetPool
