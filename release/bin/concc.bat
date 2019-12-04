@echo off

rem ##########################################################################
rem # Adapted from scripts included with Scala (https://www.scala-lang.org)
rem ##########################################################################

setlocal enableextensions enabledelayedexpansion

set _LINE_TOOLCP=

if not [%~1]==[-toolcp] (
  goto :notoolcp
)
shift
set _LINE_TOOLCP=%~1
shift

:notoolcp

rem We keep in _JAVA_PARAMS all -J-prefixed and -D-prefixed arguments
set _JAVA_PARAMS=

if [%1]==[] goto param_afterloop
set _TEST_PARAM=%~1
if not "%_TEST_PARAM:~0,1%"=="-" goto param_afterloop

set _TEST_PARAM=%~1
if "%_TEST_PARAM:~0,2%"=="-J" (
  set _JAVA_PARAMS=%_TEST_PARAM:~2%
)

if "%_TEST_PARAM:~0,2%"=="-D" (
  rem Only match beginning of the -D option. The relevant bit is 17 chars long.
  rem test if this was double-quoted property "-Dprop=42"
  for /F "delims== tokens=1-2" %%G in ("%_TEST_PARAM%") DO (
    if not "%%G" == "%_TEST_PARAM%" (
      rem double quoted: "-Dprop=42" -> -Dprop="42"
      set _JAVA_PARAMS=%%G="%%H"
    ) else if [%2] neq [] (
      rem it was a normal property: -Dprop=42 or -Drop="42"
      set _JAVA_PARAMS=%_TEST_PARAM%=%2
      shift
    )
  )
)

:param_loop
shift

if [%1]==[] goto param_afterloop
set _TEST_PARAM=%~1
if not "%_TEST_PARAM:~0,1%"=="-" goto param_afterloop 

set _TEST_PARAM=%~1
if "%_TEST_PARAM:~0,2%"=="-J" (
  set _JAVA_PARAMS=%_JAVA_PARAMS% %_TEST_PARAM:~2%
)

if "%_TEST_PARAM:~0,2%"=="-D" (
  rem test if this was double-quoted property "-Dprop=42"
  for /F "delims== tokens=1-2" %%G in ("%_TEST_PARAM%") DO (
    if not "%%G" == "%_TEST_PARAM%" (
      rem double quoted: "-Dprop=42" -> -Dprop="42"
      set _JAVA_PARAMS=%_JAVA_PARAMS% %%G="%%H"
    ) else if [%2] neq [] (
      rem it was a normal property: -Dprop=42 or -Drop="42"
      set _JAVA_PARAMS=%_JAVA_PARAMS% %_TEST_PARAM%=%2
      shift
    )
  )
)
goto param_loop
:param_afterloop

@setlocal
call :set_home

rem We use the value of the JAVACMD environment variable if defined
set _JAVACMD=%JAVACMD%

if not defined _JAVACMD (
  if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
  )
)

if "%_JAVACMD%"=="" set _JAVACMD=java

rem We use the value of the JAVA_OPTS environment variable if defined
set _JAVA_OPTS=%JAVA_OPTS%
if not defined _JAVA_OPTS set _JAVA_OPTS=-Xmx512M -Xms64M

rem We append _JAVA_PARAMS java arguments to JAVA_OPTS if necessary
if defined _JAVA_PARAMS set _JAVA_OPTS=%_JAVA_OPTS% %_JAVA_PARAMS%

set _TOOL_CLASSPATH=
if "%_TOOL_CLASSPATH%"=="" (
  for %%f in ("!_CONC_HOME!\lib\*.jar") do call :add_cpath "%%f"
  for /d %%f in ("!_CONC_HOME!\lib\*") do call :add_cpath "%%f"
)

if not "%_LINE_TOOLCP%"=="" call :add_cpath "%_LINE_TOOLCP%"

set _PROPS=-Dcom.concurnas.home="!_CONC_HOME!"

setlocal DisableDelayedExpansion
rem echo "%_JAVACMD%" %_JAVA_OPTS% %_PROPS% -cp "%_TOOL_CLASSPATH%" com.concurnas.conc.ConcWrapper concc %*
"%_JAVACMD%" %_JAVA_OPTS% %_PROPS% -cp "%_TOOL_CLASSPATH%" com.concurnas.conc.ConcWrapper concc  %*
goto end

rem ##########################################################################
rem # subroutines

:add_cpath
  if "%_TOOL_CLASSPATH%"=="" (
    set _TOOL_CLASSPATH=%~1
  ) else (
    set _TOOL_CLASSPATH=%_TOOL_CLASSPATH%;%~1
  )
goto :eof

:set_home
  set _BIN_DIR=
  for %%i in ("%~sf0") do set _BIN_DIR=%_BIN_DIR%%%~dpsi
  set _CONC_HOME=%_BIN_DIR%..
goto :eof

:end
@endlocal

REM exit code fix, see http://stackoverflow.com/questions/4632891/exiting-batch-with-exit-b-x-where-x-1-acts-as-if-command-completed-successfu
@"%COMSPEC%" /C exit %errorlevel% >nul
