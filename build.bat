@echo off

REM Requirements to run this script:
REM - Java Development Kit (JDK) installed and configured
REM - The script should be run from the project root directory

echo Building JTabViz...

REM Create the output directory if it doesn't exist
if not exist out (
    mkdir out
)

REM Check if JAR file already exists
if exist out\JTabViz.jar (
    echo JAR file exists. Running the application...
    java -jar out\JTabViz.jar
    pause
    exit /b 0
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

echo Build successful! JAR file created at out\JTabViz.jar
echo Running the application...
java -jar out\JTabViz.jar
pause 