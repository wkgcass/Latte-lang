@echo off

setlocal EnableDelayedExpansion

:getArg
if "%1%"=="" goto end
set input=!input! %1%
shift
goto getArg
:end

java -jar "!cd!/latte-build/target/latte-build-0.0.3-ALPHA-jar-with-dependencies.jar" !input!

endlocal EnableDelayedExpansion