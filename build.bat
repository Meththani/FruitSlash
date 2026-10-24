@echo off
rem Compiles the game and the test harness into the "out" folder.
setlocal
if exist out rmdir /s /q out
mkdir out
javac -d out src\*.java test\*.java
if errorlevel 1 (
    echo.
    echo BUILD FAILED
    exit /b 1
)
echo BUILD OK
