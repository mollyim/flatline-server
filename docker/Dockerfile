FROM debian:latest AS builder
# Pin dependency versions.
ARG TEMURIN_VER=21.0.7+6
# Install build dependencies.
RUN apt update && apt install -y wget curl docker.io
# Install the Tamarin OpenJDK v21.
RUN wget https://github.com/adoptium/temurin21-binaries/releases/download/jdk-${TEMURIN_VER}/OpenJDK21U-jdk_x64_linux_hotspot_$(echo $TEMURIN_VER | tr '+' '_').tar.gz && \
      tar xzf OpenJDK21U-jdk_x64_linux_hotspot_$(echo $TEMURIN_VER | tr '+' '_').tar.gz && \
      mv jdk-${TEMURIN_VER} /opt/
ENV JAVA_HOME=/opt/jdk-${TEMURIN_VER}
ENV PATH=$JAVA_HOME/bin:$PATH
# Copy the source to the container image.
COPY ../ /root/src/
WORKDIR /root/src
# Build the JAR.
RUN ./mvnw -e -B -Pexclude-spam-filter clean package -DskipTests=true
RUN cp $(ls service/target/TextSecureServer-*-dirty-SNAPSHOT.jar) /root/TextSecureServer.jar

FROM eclipse-temurin:21-alpine
COPY --from=builder /root/TextSecureServer.jar /TextSecureServer.jar
