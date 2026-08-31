@echo off
setlocal

cd /d "%~dp0"

rem This runs MCA Towns as its own Fabric/Loom project.
rem It does not build or run cmobs or smitherz_advaddon.
if "%~1"=="" (
    call "%~dp0gradlew.bat" runClient --no-daemon
) else (
    call "%~dp0gradlew.bat" %* --no-daemon
)

exit /b %ERRORLEVEL%
