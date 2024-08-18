#!/bin/bash

# Compile all Java files in the src directory and its subdirectories
javac -d out -cp "libs/*" $(find src -name "*.java")

# Package the compiled files into a JAR
jar cfe out/JTabViz.jar src.Main -C out .

# Run the packaged Java application
java -jar out/JTabViz.jar
