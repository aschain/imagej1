@echo off
setlocal

set FX_VERSION=21.0.4
set M2_REPO=%USERPROFILE%\.m2\repository
set FX_BASE=%M2_REPO%\org\openjfx\javafx-base\%FX_VERSION%\javafx-base-%FX_VERSION%-win.jar
set FX_GRAPHICS=%M2_REPO%\org\openjfx\javafx-graphics\%FX_VERSION%\javafx-graphics-%FX_VERSION%-win.jar
set FX_CONTROLS=%M2_REPO%\org\openjfx\javafx-controls\%FX_VERSION%\javafx-controls-%FX_VERSION%-win.jar

echo [1/3] Ensuring project classes are compiled...
call mvn -q -DskipTests compile
if errorlevel 1 goto :error

echo [2/3] Fetching OpenJFX runtime jars (Windows)...
call mvn -q dependency:get -Dartifact=org.openjfx:javafx-base:%FX_VERSION%:jar:win
if errorlevel 1 goto :error
call mvn -q dependency:get -Dartifact=org.openjfx:javafx-graphics:%FX_VERSION%:jar:win
if errorlevel 1 goto :error
call mvn -q dependency:get -Dartifact=org.openjfx:javafx-controls:%FX_VERSION%:jar:win
if errorlevel 1 goto :error

if not exist "%FX_BASE%" goto :missing
if not exist "%FX_GRAPHICS%" goto :missing
if not exist "%FX_CONTROLS%" goto :missing

set FX_MODULE_PATH=%FX_BASE%;%FX_GRAPHICS%;%FX_CONTROLS%

echo [3/3] Launching ImageJ with JavaFX backend...
java --module-path "%FX_MODULE_PATH%" --add-modules javafx.controls,javafx.graphics -cp target\classes ij.ImageJ -gui=javafx %*
if errorlevel 1 goto :error

goto :eof

:missing
echo Required OpenJFX jars were not found after download attempt.
echo Expected:
echo   %FX_BASE%
echo   %FX_GRAPHICS%
echo   %FX_CONTROLS%
goto :error

:error
echo JavaFX launch failed.
exit /b 1
