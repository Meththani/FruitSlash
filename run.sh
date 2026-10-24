#!/bin/sh
# Linux/macOS equivalent of run.bat
[ -f out/Main.class ] || ./build.sh
java -cp out Main
