#!/usr/bin/env bash
set -e

awslocal s3 mb s3://whisper-service-dynamic-config
awslocal s3 cp /tmp/dev.yml s3://whisper-service-dynamic-config/dev.yml

awslocal s3 mb s3://whisper-service-pre-key-store
