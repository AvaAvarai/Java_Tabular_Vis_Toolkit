#!/bin/bash
# Compile the Java files
javac -d out -cp "libs/*" src/*.java

# Run the compiled Java application
java -cp "out:libs/*" src.Main