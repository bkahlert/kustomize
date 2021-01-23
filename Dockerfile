ARG GRAALVM_VERSION=20.2.0-java11
#ARG VCS_REF
#ARG BUILD_DATE
#ARG VERSION
#ARG UID=1000
#ARG GID=1000
#ARG HOME_DIR=/app

# Download GraalVM and install Native Image Builder
FROM oracle/graalvm-ce:${GRAALVM_VERSION}
ENV GRAALVM_HOME="$JAVA_HOME"
RUN gu install native-image
WORKDIR /project

# Install Gradle
COPY gradlew .
COPY gradle ./gradle
RUN ./gradlew

# Compile and build native image
COPY build.gradle.kts .
COPY src ./src
RUN ./gradlew nativeImage installNativeImage

# Copy native binary
FROM busybox
#ARG VCS_REF
#ARG BUILD_DATE
#ARG VERSION
#ARG UID
#ARG GID
#ARG HOME_DIR
LABEL maintainer="Björn Kahlert <mail@bkahlert.com" \
      authors="Björn Kahlert <mail@bkahlert.com" \
      org.label-schema.schema-version="1.0" \
      org.label-schema.build-date=${BUILD_DATE} \
      org.label-schema.license=MIT \
      org.label-schema.name="bkahlert/imgcstmzr" \
      org.label-schema.description="Image Customizer for Raspberry Pi OS and alike" \
      org.label-schema.url="https://imgcstmzr.com/" \
      org.label-schema.vcs-type=Git \
      org.label-schema.vcs-url="https://github.com/imgcstmzr/imgcstmzr.git" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.version=$VERSION \
      org.label-schema.docker.cmd="docker run --rm --name imgcstmzr -it --mount type=bind,source=$(pwd),target=/work" \
      org.label-schema.usage="README.md"
COPY --from=0 /project/build/native-image/imgcstmzr .
#RUN groupadd --gid ${GID} app
#RUN useradd --home-dir ${HOME_DIR} --create-home --no-log-init --uid ${UID} --gid ${GID} app
#USER app
WORKDIR /app
ENTRYPOINT ["exec", "/project/build/native-image/imgcstmzr"]
CMD ["-h"]
