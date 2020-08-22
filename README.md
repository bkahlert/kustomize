# ImgCstmzr — Downloads and Customizes Raspberry Pi Images

## Usage

```shell script
docker build -t imgcstmzr .
```

1. Download
2. [Install](#installation) dependencies
3. `imgcstmzr` --img=raspberry-pi-os-lite --version=LATEST --ssh.enabled=true --flash.auto=true

Alternatively run `--config=bother-you.json

## Installation

### GraalVM
- [Docker](https://docker.com): `docker pull oracle/graalvm-ce:20.2.0-java11`
- [sdkman](https://sdkman.io): `sdk install 20.2.0-r11-grl`
- [Homebrew](https://brew.sh): `brew cask install graalvm/tap/graalvm-ce-java11`

### Native Image Builder
- [gu](https://www.graalvm.org/docs/reference-manual/gu/): `gu install native-image`
- Run `bin/imgcstmzr`


## Technologies Used
- [Kotlin](https://kotlinlang.org/) as the programming language
- [Config4k](https://github.com/config4k/config4k) to process configuration files of various formats
- [Gradle]() as the build tool
- [Gradle Use Latest Versions Plugin](https://github.com/patrikerdes/gradle-use-latest-versions-plugin) and [Gradle Versions Plugin](https://github.com/ben-manes/gradle-versions-plugin) to easily update all used version
- [JUnit 5](https://junit.org/junit5/) for testing
- [Gradle Shadow](https://github.com/johnrengelman/shadow) and [GraalVM Native Image Plugin](https://github.com/mike-neck/graalvm-native-image-plugin) to generate a native self-enclosed binary provided as a [Docker](https://www.docker.com/) image

## TODO
- [ ] Replace curl based download with portable implementation
