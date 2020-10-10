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
LABEL description="Downloads and Customizes Raspberry Pi Images" url="https://imgcstmzr.com" maintainer="\"Björn Kahlert\"<mail@bkahlert.com>"
COPY --from=0 /project/build/native-image/imgcstmzr .
RUN groupadd -r app && useradd --no-log-init -r -g app app
# TODO USER appuser ENV ANSIBLE_USER=ansible SUDO_GROUP=wheel DEPLOY_GROUP=deployer
CMD ["/imgcstmzr"]
