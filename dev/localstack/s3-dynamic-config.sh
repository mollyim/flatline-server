#!/usr/bin/env bash
set -e

awslocal s3 mb s3://flatline-server-dynamic-config
awslocal s3 cp /tmp/dev.yml s3://flatline-server-dynamic-config/dev.yml
