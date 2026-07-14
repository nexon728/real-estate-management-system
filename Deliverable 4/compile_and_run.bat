@echo off
:: ──────────────────────────────────────────────────────────────────────────────
::  CS241 – Real Estate DBMS   |   Deliverable 4 – Build & Run (Windows)
::
::  Folder structure:
::    AA DBMSLAB\
::      lib\
::        mysql-connector-j-9.6.0.jar   <-- JAR lives here
::      RealEstateD4\
::        compile_and_run.bat           <-- THIS FILE
::        src\realestate\
::          Main.java
::          DBConnection.java
::          Util.java
::          OfficeReports.java
::          AgentInterface.java
:: ──────────────────────────────────────────────────────────────────────────────

setlocal enabledelayedexpansion

set "JAR=%~dp0lib\mysql-connector-j-9.6.0.jar"

if not exist "%JAR%" (
    echo.
    echo [ERROR] JAR not found at: %JAR%
    echo   Expected: AA DBMSLAB\lib\mysql-connector-j-9.6.0.jar
    pause
    exit /b 1
)

echo.
echo [1/3] Cleaning previous build...
if exist out rmdir /s /q out
mkdir out

echo [2/3] Compiling Java sources...
javac -cp ".;%JAR%" -d out ^
    src\realestate\DBConnection.java ^
    src\realestate\Util.java ^
    src\realestate\OfficeReports.java ^
    src\realestate\AgentInterface.java ^
    src\realestate\Main.java

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Compilation failed. See errors above.
    pause
    exit /b 1
)
echo       Compilation successful.

echo [3/3] Launching application...
echo ──────────────────────────────────────────────────────────────────────────
java -cp ".;%JAR%;out" realestate.Main

pause
