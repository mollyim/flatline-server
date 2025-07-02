# Docker Utilities

This directory contains scripts and utilities to build and test the Flatline server using Docker.

These utilities aim to improve the local development experience and abstract the building and testing from any specific CI environment.

# Testing

The `Dockerfile.test` builds a Docker image with the Flatline server source and additional dependencies required to run its tests. This image is used by the `compose.test.yaml` file, which also deploys a FoundationDB server to be used during the testing. This process can be triggered with the `test.sh` script.

# Running 

The main `Dockerfile` builds a minimal Docker image with the packaged Flatline server application and the Temurin runtime. It is used by the main `compose.yaml` file, which starts the server with the sample configuration and secrets bundle. This process can be triggered with the `run.sh` script.
