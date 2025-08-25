# 🚧 Work in Progress 🚧

Even in the `main` branch, resources from this repository are unstable in order to facilitate development.

During development, several features will be disabled or insecurely implemented.

Do not run this in production environments.

# Flatline Server

**Flatline Server** is a server prototype to which Signal-compatible clients can connect.

It relies on various [components](#components) forked from their original [Signal repositories](https://github.com/signalapp).

## Components

The Flatline server is composed of multiple services organized in separate directories.

- **Whisper Service**
  - Directory: [whisper-service](whisper-service/)
  - Upstream: https://github.com/signalapp/Signal-Server
- **Storage Service**
  - Directory: [storage-service](storage-service/)
  - Upstream: https://github.com/signalapp/storage-service
- **Registration Service**
  - Directory: [registration-service](registration-service/)
  - Upstream: https://github.com/signalapp/registration-service
- **Contact Discovery Service**
  - Directory: [contact-discovery-service](contact-discovery-service/)
  - Upstream: https://github.com/signalapp/ContactDiscoveryService-Icelake

Additionally, the Flatline server relies on several other [infrastructure components](https://github.com/mollyim/flatline-server/blob/main/dev/compose.yaml).

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
./mvnw -f whisper-service/pom.xml clean deploy \
  -Pexclude-spam-filter -Denv=dev -DskipTests \
  -Djib.to.image="flatline-whisper-service:dev"
```

#### Storage Service

```bash
./mvnw -f storage-service/pom.xml clean package \
  -Pdocker-deploy -Denv=dev -DskipTests \
  -Djib.to.image="flatline-storage-service:dev"
```

The `env` property is used as a prefix to fetch the relevant configuration files from `storage-service/config`.

#### Registration Service

```bash
./mvnw -f registration-service/pom.xml clean package \
  -Denv=dev -DskipTests \
  -Djib.to.image="flatline-registration-service:dev"
```

As configured for this prototype, the verification code is always the last six digits of the phone number.

#### Contact Discovery Service

```bash
./mvnw -f contact-discovery-service/pom.xml package \
  -Dpackaging=docker -DskipTests \
  -Djib.to.image="flatline-contact-discovery-service:dev"
```

### Running with Kubernetes

The recommended method of installing Flatline is with [Helm](https://helm.sh) on [Kubernetes](https://kubernetes.io).

However, this method is currently still intended to provide a testing environment, not a production one.

Although the Helm chart can be installed on any cluster, it defaults to targeting single-node [`k3s`](https://docs.k3s.io/quick-start) clusters.

#### Install Kubernetes

If you do not have a Kubernetes cluster available, install a lightweight distribution such as [`k3s`](https://docs.k3s.io/quick-start).

After installing `k3s`, you may want to enable your non-root user to connect to the cluster:

```bash
mkdir -p $HOME/.kube
sudo cp /etc/rancher/k3s/k3s.yaml $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
chmod 600 $HOME/.kube/config

echo 'export KUBECONFIG=$HOME/.kube/config' >> $HOME/.profile
source $HOME/.profile
```

#### Deploy the Helm Chart

From a client configured to the target Kubernetes cluster, clone the repository and install the chart.

This process will install Flatline for local testing, with bundled sample configurations and "secrets".

To deviate from these steps, review the [`values.yaml`](helm/values.yaml) file for defaults and [customization](#customizing-the-installation) options.

```bash
HELM_RELEASE=dev # Optional: Replace "dev" with a different name to identify your release. 
git clone git@github.com:mollyim/flatline-server.git && cd flatline-server
helm install $HELM_RELEASE ./helm
```

When installation succeeds, follow the printed instructions to reach the deployed Flatline components.

#### Customizing the Installation

The Flatline chart provides several options to customize the installation by overriding default values.

Some common customization options are:

- Overriding the bundled configuration files for the Flatline components.
- Disabling bundled local cloud service emulators to rely on the actual cloud service providers instead.
- Disabling bundled infrastructure components (e.g. Traefik-specific resources, Redis Cluster, OpenTelemetry Collector, TUS...) to use existing ones.
- Using an existing StorageClass instead of the default [`k3s` local path provisioner](https://docs.k3s.io/storage).

These and other customizations are documented in the [`values.yaml`](helm/values.yaml) file.

You can read about [how to override values with Helm](https://helm.sh/docs/helm/helm_install/) in the official documentation.

#### Development on Kubernetes

Kubernetes expects container images to be served from a container registry.

You can deploy a simple container registry with the [Distribution Registry](https://distribution.github.io/distribution/) container image.

For example, to deploy an **insecure** registry for local testing:

```bash
docker run -d -p 5000:5000 --restart=always --name registry -v /tmp/registry:/var/lib/registry registry:3
```

When building with Maven, push the resulting container images to a registry. For example:

```bash
# Whisper Service
./mvnw -e \
  -f whisper-service/pom.xml \
  deploy \
  -Pexclude-spam-filter \
  -Denv=dev \
  -DskipTests \
  -Djib.goal=build \
  -Djib.to.image=localhost:5000/flatline-whisper-service:dev \
  -Djib.allowInsecureRegistries=true

# Storage Service
./mvnw -e \
  -f storage-service/pom.xml \
  clean package \
  -Pdocker-deploy \
  -Denv=dev \
  -DskipTests \
  -Djib.goal=build \
  -Djib.to.image=localhost:5000/flatline-storage-service:dev \
  -Djib.allowInsecureRegistries=true

# Registration Service
./mvnw -e \
  -f registration-service/pom.xml \
  clean package \
  -Denv=dev \
  -DskipTests \
  -Djib.goal=build \
  -Djib.to.image=localhost:5000/flatline-registration-service:dev \
  -Djib.allowInsecureRegistries=true

# Contact Discovery Service
./mvnw -e \
  -f contact-discovery-service/pom.xml \
  deploy \
  -Dpackaging=docker \
  -DskipTests \
  -Djib.to.image=localhost:5000/flatline-contact-discovery-service:dev \
  -Djib.allowInsecureRegistries=true
```

Once the images are on the registry, override the Helm image values to reference these images.

For example, to do this for every core Flatline component, create `local.yaml` with the following:

```yaml
whisperService:
  image:
    repository: localhost:5000/flatline-whisper-service
    tag: dev
storageService:
  image:
    repository: localhost:5000/flatline-storage-service
    tag: dev
registrationService:
  image:
    repository: localhost:5000/flatline-registration-service
    tag: dev
contactDiscoveryService:
  image:
    repository: localhost:5000/flatline-contact-discovery-service
    tag: dev
```

Finally, upgrade the Helm release to use these custom image values:

```bash
helm upgrade -f local.yaml $HELM_RELEASE ./helm
```

### Running with Docker

**WARNING:** This method will be deprecated soon in favor of using a local Kubernetes cluster.

The `dev/compose.yaml` file will use Docker to deploy the container images built from the `main` branch.

```bash
docker compose -f dev/compose.yaml up
```

When building the container images locally, edit the compose file to reference those images instead.
