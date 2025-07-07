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

Testing the server requires a [FoundationDB client](https://apple.github.io/foundationdb/getting-started-linux.html).

```bash
./mvnw clean test
```

#### Storage Service

```bash
./mvnw -f storage-service/pom.xml clean test
```

### Building

In addition to the JAR artifacts, this stage will build and locally store container images with
[Jib](https://github.com/GoogleContainerTools/jib).

#### Server

```bash
./mvnw clean deploy -Pexclude-spam-filter -Denv=dev -DskipTests
```

#### Storage Service

```bash
./mvnw -f storage-service/pom.xml clean package -Pdocker-deploy -Denv=dev -DskipTests
```

The `env` property is used as a prefix to fetch the relevant configuration files from `storage-service/config`.

### Running

The `compose.yaml` file will deploy the containers built in the previous stage for local testing.

```bash
docker compose up
```
