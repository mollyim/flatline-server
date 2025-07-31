# 🚧 Work in Progress 🚧

Even in the `main` branch, resources from this repository are unstable in order to facilitate development.

During development, several features will be disabled or insecurely implemented.

Do not run this in production environments.

# Flatline Server

**Flatline Server** is a server prototype to which Signal-compatible clients can connect.

This version is forked from [signal-server](https://github.com/signalapp/Signal-Server).

## Development

Testing and building this project relies on [Docker](https://docs.docker.com/engine/install/).

This project is intended to be built with the [Temurin 21 JDK](https://adoptium.net/installation/).

For the following commands to succeed, ensure that `JAVA_HOME` points to a valid Temurin 21 JDK installation.

### Testing

#### Whisper Service

Requires a [FoundationDB client](https://apple.github.io/foundationdb/getting-started-linux.html).

```bash
./mvnw -f whisper-service/pom.xml clean verify -e \
-pl '!integration-tests' -Dsurefire.failIfNoSpecifiedTests=false -Dtest=\
\!org.whispersystems.textsecuregcm.controllers.VerificationControllerTest,\
\!org.whispersystems.textsecuregcm.controllers.SubscriptionControllerTest,\
\!org.whispersystems.textsecuregcm.registration.IdentityTokenCallCredentialsTest
```

Integration tests are excluded as they require an existing environment in which to run.

Tests for features that are disabled for the prototype are be excluded.

#### Storage Service

```bash
./mvnw -f storage-service/pom.xml clean test
```

#### Registration Service

```bash
./mvnw -f registration-service/pom.xml clean test
```

#### Contact Discovery Service

To test C dependencies:

```bash
make -C contact-discovery-service/c docker_tests
make -C contact-discovery-service/c docker_valgrinds
```

To run minimal tests without Intel SGX:

```bash
./mvnw -f contact-discovery-service/pom.xml verify -Dtest=\
\!org.signal.cdsi.enclave.**,\
\!org.signal.cdsi.IntegrationTest,\
\!org.signal.cdsi.JsonMapperInjectionIntegrationTest,\
\!org.signal.cdsi.limits.redis.RedisLeakyBucketRateLimiterIntegrationTest,\
\!org.signal.cdsi.util.ByteSizeValidatorTest
```

To run all tests with Intel SGX:

```bash
# Set up Intel SGX on Ubuntu 22.04.
sudo ./contact-discovery-service/c/docker/sgx_runtime_libraries.sh
./mvnw -f contact-discovery-service/pom.xml verify
```

### Building

In addition to the JAR artifacts, this stage will build and locally store container images with
[Jib](https://github.com/GoogleContainerTools/jib).

#### Whisper Service

```bash
./mvnw -f whisper-service/pom.xml clean deploy -Pexclude-spam-filter -Denv=dev -DskipTests
```

#### Storage Service

```bash
./mvnw -f storage-service/pom.xml clean package -Pdocker-deploy -Denv=dev -DskipTests
```

The `env` property is used as a prefix to fetch the relevant configuration files from `storage-service/config`.

#### Registration Service

```bash
./mvnw -f registration-service/pom.xml clean package \
  -Ddocker.repo=flatline-registration-service -Denv=dev -DskipTests
```

As configured for this prototype, the verification code is always the last six digits of the phone number.

#### Contact Discovery Service

```
./mvnw -f contact-discovery-service/pom.xml package -Dpackaging=docker \
  -Djib.to.image="flatline-contact-discovery-service:experimental" -DskipTests
```

### Running

The `dev/compose.yaml` file will deploy the containers built in the previous stage for local testing with Docker.

```bash
docker compose -f dev/compose.yaml up
```
