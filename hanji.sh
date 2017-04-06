#!/bin/bash

nohup java -Dprofiles=submodules/language-detection/profiles -jar build/libs/hanji-0.1.jar 2>hanji-error.log 1>hanji.log &
