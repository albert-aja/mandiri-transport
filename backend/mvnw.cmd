@REM Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
@echo off
setlocal
set "MAVEN_VERSION=3.9.9"
set "MAVEN_DIST_DIR=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_HOME=%MAVEN_DIST_DIR%\apache-maven-%MAVEN_VERSION%"
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Apache Maven %MAVEN_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; New-Item -ItemType Directory -Force -Path '%MAVEN_DIST_DIR%' | Out-Null; $zip='%MAVEN_DIST_DIR%\maven.zip'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -Path $zip -DestinationPath '%MAVEN_DIST_DIR%' -Force; Remove-Item $zip -Force"
    if errorlevel 1 exit /b 1
)
call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%

