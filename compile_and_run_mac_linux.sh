#!/bin/bash

# Requirements to run this script:
# - Java Development Kit (JDK) installed and configured
# - The script should be run from the project root directory

# Compile all Java files in the src directory and its subdirectories
javac -d out $(find src -name "*.java")

# Run the compiled Java application
java -cp "out" src.Main
