@echo off

setlocal EnableDelayedExpansion

:getArg
if "%1%"=="" goto end
set input=!input! %1%
shift
goto getArg
:end

java -jar "!cd!/repl.jar" !input!

endlocal EnableDelayedExpansion