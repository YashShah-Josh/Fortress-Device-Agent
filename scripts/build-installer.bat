@echo off
setlocal
set DIR=%~dp0..
set VERSION=%1
if "%VERSION%"=="" set VERSION=1.0.0
cd /d "%DIR%"
call mvn -q package -DskipTests
set JAR=%DIR%\target\fortress-device-agent.jar
set INPUT=%DIR%\target\jpackage-input
if exist "%INPUT%" rmdir /s /q "%INPUT%"
mkdir "%INPUT%"
copy /y "%JAR%" "%INPUT%\"
jpackage ^
  --name FortressDeviceAgent ^
  --input "%INPUT%" ^
  --main-jar fortress-device-agent.jar ^
  --main-class com.fortress.deviceagent.Main ^
  --type msi ^
  --app-version %VERSION% ^
  --vendor Fortress ^
  --dest "%DIR%\target\dist" ^
  --win-shortcut
echo Installer written to %DIR%\target\dist
