#!/usr/bin/env bash
BUILD_DATE=$(date "+%Y%m%d%H%M")
SERVICE_NAME=oss
SERVICE_VER=1.0.7

#=======================================================================
# do not edit below
#=======================================================================
export SERVICE_NAME=${SERVICE_NAME}
export SERVICE_VER="${SERVICE_VER}-${BUILD_DATE}"

if [ "$1" = "up" ]; then
    if [ "${2:0:2}" = "-D" ]; then
        mvn -pl ${SERVICE_NAME}-server -P+layer -am "${2}" clean package
    else
        mvn -pl ${SERVICE_NAME}-server -P+layer -am clean package
    fi
    docker compose down
    docker rmi "apzda/${SERVICE_NAME}-server:latest"
fi

if [ -z "$1" ]; then
    exec docker compose up
else
    exec docker compose "${@:0:1}"
fi
