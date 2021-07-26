FROM openjdk:18-slim
WORKDIR /imgcstmzr

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
      org.label-schema.name="bkahlert/imgcstmzr" \
      org.label-schema.description="Image Customizer for Raspberry Pi OS and alike" \
      org.label-schema.url="https://imgcstmzr.com/" \
      org.label-schema.vcs-type=Git \
      org.label-schema.vcs-url="https://github.com/imgcstmzr/imgcstmzr.git" \
      org.label-schema.docker.cmd="docker run --rm --name imgcstmzr -it --mount type=bind,source=$(pwd),target=/work" \
      org.label-schema.usage="README.md"

# copy binaries
COPY --from=0 /imgcstmzr/build/install .
#RUN groupadd --gid ${GID} app
#RUN useradd --home-dir /work --create-home --no-log-init --uid ${UID} --gid ${GID} app
#USER app
WORKDIR /work
ENTRYPOINT ["/imgcstmzr/bin/imgcstmzr"]
CMD ["-h"]
