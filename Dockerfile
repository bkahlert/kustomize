ARG GRAALVM_VERSION=20.2.0-java11

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
FROM scratch
LABEL description="(Raspberry Pi) Image Customizer" url="https://imgcstmzr.com" maintainer="\"Björn Kahlert\"<mail@bkahlert.com>"
LABEL org.label-schema.schema-version="1.0"
LABEL org.label-schema.name="ImgCstmzr"
LABEL org.label-schema.description="(Raspberry Pi) Image Customizer"
LABEL org.label-schema.vendor="\"Björn Kahlert\"<mail@bkahlert.com>"
LABEL org.label-schema.usage="README.md"
LABEL org.label-schema.url="https://imgcstmzr.com"
LABEL org.label-schema.vcs-url="https://github.com/imgcstmzr/imgcstmzr.git"
LABEL org.label-schema.docker.cmd.help="docker run --rm $CONTAINER"
LABEL org.label-schema.docker.cmd.debug="docker run --rm -v $(PWD):/work --entrypoint /bin/bash -it $CONTAINER"
COPY --from=0 /project/build/native-image/imgcstmzr .
RUN groupadd -r app && useradd --no-log-init -r -g app app
# TODO USER appuser ENV ANSIBLE_USER=ansible SUDO_GROUP=wheel DEPLOY_GROUP=deployer
CMD ["/imgcstmzr"]
