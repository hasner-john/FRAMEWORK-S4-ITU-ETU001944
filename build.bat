@echo off
setlocal enabledelayedexpansion

set FRAMEWORK_NAME=sprint-mvc-framework
set SRC_DIR=src\main\java
set BUILD_DIR=build\classes
set BUILD_JAR=build\%FRAMEWORK_NAME%.jar
set DIST_DIR=dist
set LIB_DIR=lib
set SERVLET_API_JAR=%LIB_DIR%\jakarta.servlet-api-6.0.0.jar
set TEMP=%CD%\build
set TMP=%CD%\build

if not exist "%SERVLET_API_JAR%" (
    echo Erreur : %SERVLET_API_JAR% introuvable.
    echo Copiez jakarta.servlet-api-6.0.0.jar dans le dossier lib du framework.
    exit /b 1
)

if exist build rmdir /s /q build
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

if exist sources.txt del sources.txt
for /R "%SRC_DIR%" %%f in (*.java) do (
    set "sourceFile=%%f"
    set "sourceFile=!sourceFile:%CD%\=!"
    set "sourceFile=!sourceFile:\=/!"
    echo !sourceFile!>> sources.txt
)

echo Compilation du framework...
javac -cp "%SERVLET_API_JAR%" -d "%BUILD_DIR%" @sources.txt 2> compile_errors.txt
if errorlevel 1 (
    echo Erreur de compilation du framework !
    type compile_errors.txt
    del sources.txt
    exit /b 1
)

del sources.txt
del compile_errors.txt

echo Creation du JAR...
if exist "%DIST_DIR%\%FRAMEWORK_NAME%.jar" del /f /q "%DIST_DIR%\%FRAMEWORK_NAME%.jar"
if exist "%FRAMEWORK_NAME%.jar" del /f /q "%FRAMEWORK_NAME%.jar"
if exist "%BUILD_JAR%" del /f /q "%BUILD_JAR%"
jar -cvf "%BUILD_JAR%" -C "%BUILD_DIR%" .
if errorlevel 1 (
    echo Erreur de creation du JAR du framework !
    exit /b 1
)

copy /Y "%BUILD_JAR%" "%DIST_DIR%\%FRAMEWORK_NAME%.jar"
if errorlevel 1 (
    echo Erreur de copie du JAR dans le dossier dist !
    exit /b 1
)

copy /Y "%DIST_DIR%\%FRAMEWORK_NAME%.jar" "%FRAMEWORK_NAME%.jar"
if errorlevel 1 (
    echo Erreur de copie du JAR a la racine du framework !
    exit /b 1
)

echo Framework genere : %DIST_DIR%\%FRAMEWORK_NAME%.jar
