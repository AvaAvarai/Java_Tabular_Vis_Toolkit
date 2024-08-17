@echo off
javac -d out -cp "libs/*" src/*.java
java -cp ".;libs/*;out" src.Main
pause