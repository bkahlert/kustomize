FROM openjdk:18-slim
WORKDIR /kustomize

# copy gradle
COPY gradlew .
COPY gradle ./gradle

# copy sources
COPY build.gradle.kts .
COPY src ./src

# compile
RUN ./gradlew installDist

# new image
FROM openjdk:18-slim

# install Docker CLI & AWT dependencies
RUN apt-get update \
 && apt-get -y install \
      apt-transport-https \
      ca-certificates \
      curl \
      gnupg \
      lsb-release \
 && curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg \
 && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] \
          https://download.docker.com/linux/debian $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null \
 && apt-get update \
 && apt-get -y install docker-ce-cli \
 && apt-get -y install fontconfig libfreetype6

LABEL maintainer="Björn Kahlert <mail@bkahlert.com>" \
      authors="Björn Kahlert <mail@bkahlert.com>" \
      org.label-schema.schema-version="1.0" \
      org.label-schema.license=MIT \
      org.label-schema.name="bkahlert/kustomize" \
      org.label-schema.description="Kotlin-based customizer for IoT images like Raspberry Pi OS" \
      org.label-schema.url="https://github.com/bkahlert/kustomize" \
      org.label-schema.vcs-type=Git \
      org.label-schema.vcs-url="https://github.com/bkahlert/kustomize.git" \
      org.label-schema.docker.cmd="docker run --rm --name kustomize -it --mount type=bind,source=$(pwd),target=/work" \
      org.label-schema.usage="README.md"

# copy binaries
COPY --from=0 /kustomize/build/install .
#RUN groupadd --gid ${GID} app
#RUN useradd --home-dir /work --create-home --no-log-init --uid ${UID} --gid ${GID} app
#USER app
WORKDIR /work
ENTRYPOINT ["/kustomize/bin/kustomize"]
CMD ["-h"]
