@echo off
rem Builds (if needed) and starts the game.
setlocal
if not exist out\Main.class call build.bat
if errorlevel 1 exit /b 1
java -cp out Main
