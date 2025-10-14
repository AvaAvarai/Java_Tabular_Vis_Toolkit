@echo off
setlocal enabledelayedexpansion

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Java is not installed or not in PATH. Please install Java and try again.
    pause
    exit /b 1
)

REM Detect Java version (major only)
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VERSION=%%~i"
)

REM Strip quotes and split major version
set "JAVA_VERSION=%JAVA_VERSION:"=%"
for /f "tokens=1 delims=." %%j in ("%JAVA_VERSION%") do (
    set "JAVA_MAJOR=%%j"
)

REM Proceed with build
echo Building JTabViz...

if not exist out (
    mkdir out
)

echo Copying resources...
xcopy /E /I /Y resources\graphics out\graphics >nul
xcopy /E /I /Y resources\icons out\icons >nul

if exist README.md (
    copy README.md out\ >nul
)

echo Compiling Java files using --release %JAVA_MAJOR%...
javac --enable-preview --release %JAVA_MAJOR% -d out -cp "libs/*" src/Main.java src/classifiers/*.java src/utils/*.java src/managers/*.java src/table/*.java src/plots/*.java src/*.java

if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b %errorlevel%
)

echo Main-Class: src.Main> out\MANIFEST.MF

echo Creating JAR file...
jar cfm out\JTabViz.jar out\MANIFEST.MF -C out .

if %errorlevel% neq 0 (
    echo JAR creation failed.
    pause
    exit /b %errorlevel%
)

echo Build successful! Running application...
start /min java --enable-preview -jar out\JTabViz.jar
