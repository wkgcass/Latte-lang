#!/bin/bash
CMD_PATH=`dirname $0`
java -jar "$CMD_PATH/latte-build/target/latte-build-0.0.3-ALPHA-jar-with-dependencies.jar" $*
