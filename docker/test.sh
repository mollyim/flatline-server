#!/bin/sh

docker compose -f compose.test.yaml up --build --remove-orphans --abort-on-container-exit --exit-code-from server 
