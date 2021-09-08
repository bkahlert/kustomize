FROM --platform=$BUILDPLATFORM ubuntu:bionic as docker-cli-getter
ARG DOCKER_VERSION=20.10.8
WORKDIR /tmp
RUN apt-get -qq update && apt-get -qq install wget
ARG TARGETARCH
RUN case ${TARGETARCH} in \
         arm|arm/v6|arm/v7) DOCKER_ARCH="armhf" ;; \
         arm64|arm/v8) DOCKER_ARCH="aarch64" ;;  \
         amd64) DOCKER_ARCH="x86_64" ;; \
    esac \
 && wget -q https://download.docker.com/linux/static/stable/${DOCKER_ARCH}/docker-${DOCKER_VERSION}.tgz \
 && tar xzvf docker-$DOCKER_VERSION.tgz


FROM openjdk:18-slim as app
WORKDIR /kustomize

# copy gradle
COPY gradlew .
COPY gradle ./gradle

# copy sources
COPY build.gradle.kts .
COPY src ./src

# compile
RUN ./gradlew installDist -v


FROM openjdk:18-slim
# install AWT dependencies
RUN apt-get update \
 && apt-get -qq install \
                fontconfig \
                libfreetype6

# Docker
COPY --from=docker-cli-getter '/tmp/docker/docker' '/usr/bin/docker'

# copy binaries
COPY --from=app /kustomize/build/install .
#RUN groupadd --gid ${GID} app
#RUN useradd --home-dir /work --create-home --no-log-init --uid ${UID} --gid ${GID} app
#USER app
WORKDIR /work
ENTRYPOINT ["/kustomize/bin/kustomize"]
CMD ["-h"]
