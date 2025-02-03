#!/bin/sh
set -e
set -m

echo "=====================";
echo "===Start Connector===";
echo "=====================";
/docker-entrypoint.sh start & CONNECTOR_PID=$!

echo "=====================";
echo "===Retry connection===";
echo "=====================";
curl -f --retry-all-errors --retry 10 --retry-delay 5 --retry-max-time 40 http://localhost:8083/connectors; 

echo "=====================";
echo "=Init Order connector=";
echo "=====================";

curl -X POST -H "Content-Type: application/json" -d @/saga-order-cdc-config.json http://localhost:8083/connectors

echo "=====================";
echo "Wait for main process";
echo "=====================";
echo "Wait for PID" $CONNECTOR_PID;
wait $CONNECTOR_PID
echo "Exit status: $?"
