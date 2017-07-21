#!/bin/bash

if [ "$1" == "start" ]; then 
  nohup java -Dhanji.profilesDir=submodules/language-detection/profiles -jar build/libs/hanji-0.2.jar 2>hanji-error.log 1>hanji.log & echo $! > hanji.pid
elif [ "$1" == "stop" ]; then
  kill `cat hanji.pid`
fi 
