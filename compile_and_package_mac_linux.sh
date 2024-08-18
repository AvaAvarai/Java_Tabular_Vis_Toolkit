#!/bin/bash

# Create the output directory if it doesn't exist
mkdir -p out

# Compile all Java files in the src directory and its subdirectories
javac -d out -cp "libs/*" $(find src -name "*.java")

# Copy resources to the output directory, excluding unnecessary files
rsync -av --exclude='.DS_Store' resources/ out/

# Package the compiled files and resources into a JAR
jar cfe out/JTabViz.jar src.Main -C out .

# Run the packaged Java application
java -jar out/JTabViz.jar
