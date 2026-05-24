@echo off
setlocal
set DIR=%~dp0..
set JAR=%DIR%\target\fortress-device-agent.jar
if not exist "%JAR%" (
  echo Building agent...
  pushd "%DIR%"
  call mvn -q package -DskipTests
  popd
)
java -jar "%JAR%" %*
