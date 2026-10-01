@echo off
setlocal
cd /d "%~dp0"
if not exist build mkdir build
javac -encoding UTF-8 --release 17 -d build src\*.java
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp build Main %*
exit /b %errorlevel%
