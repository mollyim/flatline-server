#!/bin/sh

docker compose up -d --remove-orphans
docker logs -f flatline-server
