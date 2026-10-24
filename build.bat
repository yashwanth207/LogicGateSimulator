@echo off
rem Compiles the project and packages dist\LogicGateSimulator.jar (needs a JDK 11+).
cd /d "%~dp0"
set "JAVAC_CMD=javac"
set "JAR_CMD=jar"

where javac >nul 2>nul
if errorlevel 1 (
    for /d %%A in ("%USERPROFILE%\.antigravity-ide\extensions\redhat.java-*") do (
        for /d %%B in ("%%A\jre\*") do (
            if exist "%%B\bin\javac.exe" (
                set "JAVAC_CMD=%%B\bin\javac.exe"
                set "JAR_CMD=%%B\bin\jar.exe"
            )
        )
    )
)

if exist out rmdir /s /q out
mkdir out
if not exist dist mkdir dist
"%JAVAC_CMD%" -encoding UTF-8 -sourcepath src -d out src\logicsim\LogicGateSimulator.java
if errorlevel 1 exit /b 1
"%JAR_CMD%" --create --file dist\LogicGateSimulator.jar --main-class logicsim.LogicGateSimulator -C out .
if errorlevel 1 exit /b 1
echo Built dist\LogicGateSimulator.jar
