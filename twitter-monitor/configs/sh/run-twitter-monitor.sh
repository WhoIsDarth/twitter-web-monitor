#!/bin/sh

gradle clean build -p web-monitor/twitter-monitor

export MONGODB_URI=mongodb://localhost:27017
export MONITOR_USER_NAME=admin
export MONITOR_USER_PASSWORD=admin
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export DICTIONARY_SERVICE_URL=http://localhost:8015/api/dictionary

nohup java -jar web-monitor/twitter-monitor/build/libs/twitter-monitor*.jar &
