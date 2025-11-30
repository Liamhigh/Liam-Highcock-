@rem Gradle Wrapper for Verum Omnis Android Forensic Engine
@rem Windows batch script

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem  Gradle startup script for Windows
@rem ##########################################################################

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

echo.
echo ERROR: JAVA_HOME is not set. Please set it to your JDK 17+ installation.
echo.
goto fail

:findJavaFromJavaHome
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
goto fail

:execute
@rem Check for Gradle
set GRADLE_VERSION=8.4
set GRADLE_USER_HOME=%USERPROFILE%\.gradle

echo Running Gradle...
call "%GRADLE_USER_HOME%\wrapper\dists\gradle-%GRADLE_VERSION%-bin\gradle-%GRADLE_VERSION%\bin\gradle.bat" %*

:fail
exit /b 1

:end
