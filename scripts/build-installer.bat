@echo off
setlocal
set DIR=%~dp0..
set VERSION=%1
if "%VERSION%"=="" set VERSION=1.0.0
cd /d "%DIR%"
call mvn -q package -DskipTests
set JAR=%DIR%\target\fortress-device-agent.jar
set INPUT=%DIR%\target\jpackage-input
set WIX_TEMP=%DIR%\target\jpackage-wix-temp
set WIX_OVERRIDE=%DIR%\target\wix-override
if exist "%INPUT%" rmdir /s /q "%INPUT%"
mkdir "%INPUT%"
copy /y "%JAR%" "%INPUT%\"
if exist "%WIX_TEMP%" rmdir /s /q "%WIX_TEMP%"
mkdir "%WIX_TEMP%"
if exist "%WIX_OVERRIDE%" rmdir /s /q "%WIX_OVERRIDE%"
mkdir "%WIX_OVERRIDE%"

set JPARGS=--name FortressDeviceAgent --input "%INPUT%" --main-jar fortress-device-agent.jar --main-class com.fortress.deviceagent.Main --app-version %VERSION% --vendor Fortress --win-shortcut

echo Extracting WiX template...
jpackage %JPARGS% --temp "%WIX_TEMP%" --type msi --dest "%DIR%\target\dist"
if errorlevel 1 exit /b 1

copy /y "%WIX_TEMP%\config\main.wxs" "%WIX_OVERRIDE%\main.wxs"
powershell -ExecutionPolicy Bypass -File "%DIR%\scripts\patch-main-wxs.ps1" -MainWxsPath "%WIX_OVERRIDE%\main.wxs"
if errorlevel 1 exit /b 1

echo Building MSI with login auto-start...
jpackage %JPARGS% --resource-dir "%WIX_OVERRIDE%" --type msi --dest "%DIR%\target\dist"
if errorlevel 1 exit /b 1

echo Installer written to %DIR%\target\dist
