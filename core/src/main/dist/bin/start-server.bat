@echo off

rem Set the maximum memory heap size as a JVM option
set JAVA_OPT=-server -mx512m

rem Set the lib dir relative to the batch file's directory
set LIB_DIR=%~dp0\..\lib
rem echo LIB_DIR = %LIB_DIR%

rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=%1
if ""%1""=="""" goto setupArgsEnd
shift
:setupArgs
if ""%1""=="""" goto setupArgsEnd
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setupArgs

:setupArgsEnd

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
goto javaHome

:noJavaHome
set JAVA=java
goto javaHomeEnd

:javaHome
set JAVA=%JAVA_HOME%\bin\java

:javaHomeEnd

:checkJdk14
"%JAVA%" -version 2>&1 | findstr "1.4" >NUL
IF ERRORLEVEL 1 goto checkJdk15
echo Java 5 or newer required to run the server
goto end

:checkJdk15
"%JAVA%" -version 2>&1 | findstr "1.5" >NUL
IF ERRORLEVEL 1 goto java6
rem use java.ext.dirs hack
rem echo Using java.ext.dirs to set classpath
"%JAVA%" %JAVA_OPT% -Djava.ext.dirs="%LIB_DIR%" org.openrdf.http.server.Start %CMD_LINE_ARGS%
goto end

:java6
rem use java 6 wildcard feature
rem echo Using wildcard to set classpath
"%JAVA%" %JAVA_OPT% -cp "%LIB_DIR%\*" org.openrdf.http.server.Start %CMD_LINE_ARGS%
goto end

:end
