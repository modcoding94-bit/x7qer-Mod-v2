@echo off
setlocal
set "ROOT=%~dp0"
set "JAVA_HOME="

if not exist "%ROOT%.tools\jdk-home.txt" (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%ROOT%setup-jdk.ps1"
  if errorlevel 1 goto failed
)

for /f "usebackq delims=" %%J in ("%ROOT%.tools\jdk-home.txt") do set "JAVA_HOME=%%J"
if not exist "%JAVA_HOME%\bin\java.exe" (
  del "%ROOT%.tools\jdk-home.txt" 2>nul
  echo JDK nicht gefunden. Starte build.bat erneut, um es neu herunterzuladen.
  goto failed
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Baue 7xqer fuer Minecraft 1.21.11...
call "%ROOT%gradlew.bat" build --no-daemon
if errorlevel 1 goto failed

echo.
echo Fertig! Die Mod-JAR liegt in:
echo %ROOT%build\libs\7xqer-1.0.0.jar
start "" "%ROOT%build\libs"
pause
exit /b 0

:failed
echo.
echo Der Build ist fehlgeschlagen. Die Meldung oben zeigt den Grund.
pause
exit /b 1
