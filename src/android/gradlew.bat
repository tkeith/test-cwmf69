@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Validate Java installation and version
:validate_java
if not "%JAVA_HOME%"=="" goto findJavaFromJavaHome

echo Error: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

exit /b 1

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto checkJavaVersion

echo Error: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

exit /b 1

:checkJavaVersion
"%JAVA_EXE%" -version 2>nul | findstr /i "version" > nul
if errorlevel 1 goto javaVersionError

@rem Validate minimum Java version (17)
"%JAVA_EXE%" -version 2>&1 | findstr /i "version \"17" > nul
if errorlevel 1 (
    echo Error: This version of Gradle requires Java 17 or later.
    echo Current Java version:
    "%JAVA_EXE%" -version
    exit /b 3
)

goto setup_gradle_environment

:javaVersionError
echo Error: Failed to determine Java version
exit /b 3

@rem Setup Gradle environment and classpath
:setup_gradle_environment
@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS
@rem to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

@rem Check for gradle-wrapper.jar
if exist "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" goto init

echo Error: Gradle wrapper jar file not found: %APP_HOME%\gradle\wrapper\gradle-wrapper.jar
echo Please ensure the Gradle wrapper is properly installed.
exit /b 2

:init
@rem Get command-line arguments, handling Windows variants
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal

:omega
@rem Set exit codes based on Gradle execution
exit /b %ERRORLEVEL%