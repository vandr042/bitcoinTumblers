#!/bin/bash

rm -rf bin/
mkdir bin/
javac -cp commons-logging-1.2/commons-logging-1.2.jar:bitcoinj-core-0.13-bundled.jar:slf4j-jcl-1.7.12.jar:src/ -d bin/ src/bitcoinLink/PeerFinder.java
#TODO update mem to your system's size
java -cp commons-logging-1.2/commons-logging-1.2.jar:bitcoinj-core-0.13-bundled.jar:slf4j-jcl-1.7.12.jar:bin/ -Xmx4G -d64 bitcoinLink.PeerFinder