#!/bin/bash

JAR_DIR="../jars/"
JAR_PATH=$JAR_DIR"bitcoinj.jar:"$JAR_DIR"slf4j.jar:"$JAR_DIR"commonsLogging.jar"

#TODO might need a bin dir first
rm -rf bin/
mkdir bin/
javac -cp $JAR_PATH:src/ -d bin/ src/bitcoinLink/FindAddress.java
#TODO update mem to your system's size
java -cp $JAR_PATH:bin/ -Xmx4G -d64 bitcoinLink.FindAddress $1