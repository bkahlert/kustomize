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

ARG TARGETPLATFORM
RUN wget -q https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini-static-${TINI_ARCH} -O /tini \
 && chmod +x /tini

# install Docker CLI & AWT dependencies
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get -qq install \
      apt-transport-https \
      ca-certificates \
      curl \
      gnupg \
      lsb-release \
 && curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg \
 && case ${TARGETPLATFORM} in \
         "linux/amd64")     DOCKER_ARCH=amd64  ;; \
         "linux/arm64/v8")  DOCKER_ARCH=arm64  ;; \
    esac \
 && echo "deb [arch=$DOCKER_ARCH signed-by=/usr/share/keyrings/debian-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null \
 && apt-get update \
 && apt-get -qq install \
                docker-ce-cli \
                fontconfig \
                libfreetype6

# copy binaries
COPY --from=0 /kustomize/build/install .
#RUN groupadd --gid ${GID} app
#RUN useradd --home-dir /work --create-home --no-log-init --uid ${UID} --gid ${GID} app
#USER app
WORKDIR /work
ENTRYPOINT ["/kustomize/bin/kustomize"]
CMD ["-h"]
