@echo off
rem Builds and runs the automated test harness. Exit code 0 = all tests passed.
setlocal
call build.bat
if errorlevel 1 exit /b 1
java -cp out TestRunner
