#!/bin/bash

JARPATH="../jars"
#TODO might need a bin dir first
rm -rf bin/
mkdir bin/
javac -cp $JARPATH/commonsLogging.jar:$JARPATH/mjsBCJ.jar:$JARPATH/slf4j.jar:src/ -d bin/ src/control/Manager.java
#TODO update mem to your system's size
java -cp $JARPATH/commonsLogging.jar:$JARPATH/mjsBCJ.jar:$JARPATH/slf4j.jar:bin/ -Xmx50G -d64 control.Manager
