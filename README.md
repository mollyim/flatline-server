# 🚧 Work in Progress 🚧

# Flatline Server

**Flatline Server** is a server prototype to which Signal-compatible clients can connect.

This version is forked from [signal-server](https://github.com/signalapp/Signal-Server).

## Development

Testing and building this project relies on [Docker](https://docs.docker.com/engine/install/).

This project is intended to be built with the [Temurin 21 JDK](https://adoptium.net/installation/).

For the following commands to succeed, ensure that `JAVA_HOME` points to a valid Temurin 21 JDK installation.

### Testing

#### Server

Testing requires the [FoundationDB client](https://apple.github.io/foundationdb/getting-started-linux.html).

```bash
./mvnw clean test
```

#### Storage Service

```bash
./mvnw -f storage-service/pom.xml clean test
```

### Building

In addition to the JAR artifacts, this stage will build container images, which will be stored locally.

#### Server

```bash
./mvnw clean deploy -Pexclude-spam-filter -DskipTests
```

#### Storage Service

```bash
./mvnw -f storage-service/pom.xml clean package -Pdocker-deploy -Denv=dev -DskipTests
```

### Running

The `compose.yaml` file will deploy the containers built in the previous stage for local testing.

```bash
docker compose up
```
