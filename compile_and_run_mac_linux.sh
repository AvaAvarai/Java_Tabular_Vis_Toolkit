#!/bin/bash
# Compile all Java files in the src directory and its subdirectories
javac -d out $(find src -name "*.java")

# Run the compiled Java application
java -cp "out" src.Main
