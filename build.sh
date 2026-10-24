#!/bin/sh
# Linux/macOS equivalent of build.bat
set -e
rm -rf out
mkdir out
javac -d out src/*.java test/*.java
echo "BUILD OK"
