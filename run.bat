@echo off
rem Builds (when a JDK is available) and launches the simulator.
cd /d "%~dp0"
set "JAVA_CMD=java"
set "JAVAC_CMD=javac"

where java >nul 2>nul
if errorlevel 1 (
    for /d %%A in ("%USERPROFILE%\.antigravity-ide\extensions\redhat.java-*") do (
        for /d %%B in ("%%A\jre\*") do (
            if exist "%%B\bin\java.exe" (
                set "JAVA_CMD=%%B\bin\java.exe"
                set "JAVAC_CMD=%%B\bin\javac.exe"
            )
        )
    )
)

where javac >nul 2>nul
if %errorlevel%==0 (
    call build.bat
    if errorlevel 1 exit /b 1
) else (
    if not exist dist\LogicGateSimulator.jar (
        call build.bat
        if errorlevel 1 exit /b 1
    )
)
"%JAVA_CMD%" -jar dist\LogicGateSimulator.jar
