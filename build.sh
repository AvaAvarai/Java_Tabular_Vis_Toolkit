#!/bin/bash

# Requirements to run this script:
# - Java Development Kit (JDK) installed and configured
# - The script should be run from the project root directory

echo "Building JTabViz..."

# Create the output directory if it doesn't exist
mkdir -p out

# Compile all Java files
echo "Compiling Java files..."
javac -d out $(find src -name "*.java")

# Check if compilation was successful
if [ $? -ne 0 ]; then
    echo "Compilation failed."
    exit 1
fi

# Copy resources to the output directory, excluding unnecessary files
echo "Copying resources..."
rsync -av --exclude='.DS_Store' resources/ out/

# Copy the README.md file to the output directory
cp README.md out/

# Remove any existing META-INF directory to avoid conflicts
rm -rf out/META-INF

# Create the manifest file
echo "Main-Class: src.Main" > out/MANIFEST.MF

# Package everything into a single JAR file
echo "Creating JAR file..."
jar cfm out/JTabViz.jar out/MANIFEST.MF -C out .

# Check if JAR creation was successful
if [ $? -ne 0 ]; then
    echo "JAR creation failed."
    exit 1
fi

echo "Build successful! JAR file created at out/JTabViz.jar"
echo "Running application..."
java -jar out/JTabViz.jar 