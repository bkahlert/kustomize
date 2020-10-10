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
- [Config4k](https://github.com/config4k/config4k) to process HOCON configuration files
- [Gradle]() as the build tool
- [Gradle "Use Latest Versions Plugin"](https://github.com/patrikerdes/gradle-use-latest-versions-plugin) and [Gradle Versions Plugin](https://github.com/ben-manes/gradle-versions-plugin) to easily update all used version
- [JUnit 5](https://junit.org/junit5/) for testing
- [Gradle Shadow](https://github.com/johnrengelman/shadow) and [GraalVM Native Image Plugin](https://github.com/mike-neck/graalvm-native-image-plugin) to generate a native self-enclosed binary provided as a [Docker](https://www.docker.com/) image

## TODO
- [ ] Replace curl based download with portable implementation
- [ ] Consider [manually controlled](https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/) [TestContainer](https://www.testcontainers.org/features/startup_and_waits/#one-shot-startup-strategy-example) possibly [creating them on the fly](https://www.testcontainers.org/features/creating_images/); seems to even work with [consuming logs](https://www.testcontainers.org/features/container_logs/)
- [ ] Bluetooth PAN
- [ ] Serial connection using g_serial (`sudo systemctl enable getty@ttyGS0.service`), [based on INI files](https://www.digitalocean.com/community/tutorials/understanding-systemd-units-and-unit-files) so consider doing while increasing disk image
  - [ ] Connect on `/dev/tty.usbmodemNNNN` using "115200 baud (8N1 8- bit No-parity 1-stop if you need to set that)"
- [ ] Add docker check
- [ ] apt-get install -y git streamer (webcam tool)
- [ ] https://github.com/nmcclain/raspberian-firstboot
- [ ] https://github.com/kenfallon/fix-ssh-on-pi !!!

 - name: Run the equivalent of "apt-get update" as a separate step
    apt:
      update_cache: true
      cache_valid_time: 3600
  - name: Update all packages to the latest version
    apt:
      upgrade: dist

https://github.com/garthvh/ansible-raspi-playbooks/tree/master/tasks

https://github.com/garthvh/ansible-raspi-playbooks/blob/master/tasks/tzdata.yml

https://github.com/garthvh/ansible-raspi-playbooks/blob/master/tasks/internationalization.yml

https://github.com/garthvh/ansible-raspi-playbooks/blob/master/playbooks/new-default.yml
