@echo off
:: ──────────────────────────────────────────────────────────────────────────────
::  CS241 – Real Estate DBMS   |   Deliverable 3 – Build & Run (Windows)
::
::  Folder structure:
::    AA DBMSLAB\
::      lib\
::        mysql-connector-j-9.6.0.jar   <-- JAR is HERE (one level up)
::      RealEstateAdmin\
::        compile_and_run.bat           <-- THIS FILE
::        src\main\java\realestate\
::          AdminMenu.java
::          DBConnection.java
::          QueryRunner.java
::
::  Usage: double-click OR run from Command Prompt inside RealEstateAdmin\
:: ──────────────────────────────────────────────────────────────────────────────

setlocal enabledelayedexpansion

set "JAR=%~dp0lib\mysql-connector-j-9.6.0.jar"

if not exist "%JAR%" (
    echo.
    echo [ERROR] JAR not found at: %JAR%
    echo   Expected: AA DBMSLAB\lib\mysql-connector-j-9.6.0.jar
    echo   Make sure the connector JAR is in the lib\ folder next to RealEstateAdmin\
    echo.
    pause
    exit /b 1
)

echo.
echo [1/3] Cleaning previous build...
if exist out rmdir /s /q out
mkdir out

echo [2/3] Compiling Java sources...
javac -cp ".;%JAR%" -d out ^
    src\main\java\realestate\DBConnection.java ^
    src\main\java\realestate\QueryRunner.java ^
    src\main\java\realestate\AdminMenu.java

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Compilation failed. See errors above.
    pause
    exit /b 1
)
echo       Compilation successful.

echo [3/3] Launching Admin CLI...
echo ──────────────────────────────────────────────────────────────────────────
java -cp ".;%JAR%;out" realestate.AdminMenu

pause
