@echo off

REM Requirements to run this script:
REM - Java Development Kit (JDK) installed and configured
REM - The script should be run from the project root directory

REM Create the output directory if it doesn't exist
if not exist out (
    mkdir out
)

REM Copy all resources to the output directory if they don't exist
echo Copying resources...
xcopy /E /I /Y resources\graphics out\graphics
xcopy /E /I /Y resources\icons out\icons

REM Copy the README.md file to the output directory if it exists
if exist README.md (
    copy README.md out\
)

REM Compile all Java files explicitly
echo Compiling Java files...
javac -d out -cp "libs/*" src/Main.java src/utils/*.java src/managers/*.java src/table/*.java src/plots/*.java src/*.java

REM Check if compilation was successful
if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b %errorlevel%
)

REM Create the manifest file
echo Main-Class: src.Main> out\MANIFEST.MF

REM Package everything into a single JAR file
echo Creating JAR file...
jar cfm out\JTabViz.jar out\MANIFEST.MF -C out .

REM Check if JAR creation was successful
if %errorlevel% neq 0 (
    echo JAR creation failed.
    pause
    exit /b %errorlevel%
)

REM Run the packaged Java application
echo Running the application...
java -jar out\JTabViz.jar
pause
