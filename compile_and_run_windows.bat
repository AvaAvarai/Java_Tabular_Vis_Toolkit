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

REM Compile all Java files explicitly
echo Compiling Java files...
javac -d out -cp "libs/*" src/Main.java src/utils/*.java src/managers/*.java src/table/*.java src/plots/*.java src/*.java

REM Check if compilation was successful
if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b %errorlevel%
)

REM Run the compiled Java application
echo Running the application...
java -cp "out;libs/*" src.Main
pause
