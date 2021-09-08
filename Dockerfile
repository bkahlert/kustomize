FROM gradle:jre11 as app

WORKDIR /home/gradle/kustomize
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY src ./src
RUN gradle installDist

WORKDIR /tmp
ARG DOCKER_VERSION=20.10.8
#RUN apt-get -qq update && apt-get -qq install wget
ARG TARGETARCH
RUN case ${TARGETARCH} in \
         arm|arm/v6|arm/v7) DOCKER_ARCH="armhf" ;; \
         arm64|arm/v8) DOCKER_ARCH="aarch64" ;;  \
         amd64) DOCKER_ARCH="x86_64" ;; \
    esac \
 && wget -q https://download.docker.com/linux/static/stable/${DOCKER_ARCH}/docker-${DOCKER_VERSION}.tgz \
 && tar xzvf docker-$DOCKER_VERSION.tgz


FROM openjdk:18-slim
# Install AWT dependencies
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get -qq install \
                fontconfig \
                libfreetype6

# Copy Docker CLI
COPY --from=app /tmp/docker/docker /usr/bin/docker

# Copy binaries
COPY --from=app /home/gradle/kustomize/build/install .
#RUN groupadd --gid ${GID} app
#RUN useradd --home-dir /work --create-home --no-log-init --uid ${UID} --gid ${GID} app
#USER app
WORKDIR /work
ENTRYPOINT ["/kustomize/bin/kustomize"]
CMD ["-h"]
