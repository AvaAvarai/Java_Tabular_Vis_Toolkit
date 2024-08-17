@echo off
REM Compile all Java files in the src directory and its subdirectories
for /r src %%f in (*.java) do (
    javac -d out -cp "libs/*" %%f
)

REM Run the compiled Java application
java -cp ".;libs/*;out" src.Main
pause
