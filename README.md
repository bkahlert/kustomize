# kustomize — Kotlin-based customizer for IoT images like Raspberry Pi OS

This tool customizes an IoT image such as a [Raspberry Pi OS](https://www.raspberrypi.org/software/operating-systems) image using
a [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md) based config file and the tools
[koodies](https://github.com/bkahlert/koodies),
[virt-customize](https://libguestfs.org/virt-customize.1.html),
[guestfish](https://libguestfs.org/guestfish.1.html) and [dockerpi](https://github.com/lukechilds/dockerpi) to be readily usable on the target device such as
a [Raspberry Pi](https://www.raspberrypi.org/).

## run bash

docker run --rm -it --mount type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock --entrypoint /bin/bash kustomize

## build

docker build -t kustomize:latest .

## run

```shell
docker run --rm -it \
           --mount type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock \
           --mount type=bind,source=/tmp,target=/tmp \
           --mount type=bind,source=$(pwd),target=/work \
           -e TERM="$TERM" \
           -e TERM_PROGRAM="$TERM_PROGRAM" \
           -e COLORTERM="$COLORTERM" \
           kustomize \
           --cache-dir /work/cache \
           --config-file sample.conf \
           --env-file .env
```

**ONLY THOROUGLY TESTED WITH RASPBERRY PI OS IMAGES**

```text
░░░░░░░ IMG CSTMZR

▶ Configuring
· Configuration: file:///tmp/kustomize-test/IntegrationTest.should_apply_patches--h2ss/sample.conf (256 B)
· Name: .test
· OS: Raspberry Pi OS Lite
· Env: file:///home/john/sample/.env
· Cache: file:///home/john/sample/
✔︎
▶ Preparing
· Listing images ✔︎
· Pulling lukechilds/dockerpi:vm image ✔︎
· ▶ Deleting old working directories ✔︎
· ▶ Retrieving image
· · ▶ Downloading https://downloads.raspberrypi.org/raspios_lite_armhf_latest
· · · Using temporary directory file:///tmp/kustomize/download/dfAO--21E-tmp/
· · · Downloaded [file:///tmp/kustomize/download/dfAO--21E-tmp/raspios_lite_armhf_latest]
· · · Moving download to file:///tmp/kustomize/download/raspios_lite_armhf_latest
· · · Deleting file:///tmp/kustomize/download/dfAO--21E-tmp/
· · ✔︎
· · ▶ Moving download to file:///home/john/sample/download/raspios_lite_armhf_latest ✔︎
· · ▶ Unarchiving file:///home/john/sample … MB) ▶ Extracting found image 2021-05-07-raspios-buster-armhf-lite.img ✔︎
· · ▶ Moving download to file:///home/john/sample/raw/2021-05-07-raspios-buster-armhf-lite.img ✔︎
· ✔︎
✔︎
▶ Applying 2 patches to Raspberry Pi OS Lite ／ 2021-05-07-raspios-buster-armhf-lite.img
· ╭──╴Set Hostname to sample
· │   Set Time Zone to Central European Standard Time
· │   Change Username pi to john.doe
· │
· │   ◼ Disk Preparations
· │   ▶ Disk Customizations (17)
· │   · ▶ 17 virt-customize operations 🐳 bkahlert/libguestfs: file:///tmp/koodies/exec/WPG.sh
· │   · · [   0.0] Examining the guest ...
· │   · · [  31.8] Setting a random seed
· │   · · [  32.2] Setting the machine ID in /etc/machine-id
· │   · · [  32.2] Setting the hostname: sample--aTy0
· │   · · [  43.5] Setting the timezone: Europe/Berlin
· │   · · [  43.6] Appending line to /etc/sudoers.d/privacy
· │   · · [  43.7] Appending line to /etc/sudoers
· │   · · [  43.7] Making directory: /usr/lib/virt-sysprep/scripts
· │   · · [  43.7] Copying: usr/lib/virt-sysprep/scripts/0000---first-boot-order-fix to /usr/lib/virt-sysprep/scripts
· │   · · [  43.8] Changing permissions of /usr/lib/virt-sysprep/scripts/0000---first-boot-order-fix to 0755
· │   · · [  43.8] Making directory: /etc/systemd/system
· │   · · [  43.8] Copying: etc/systemd/system/firstboot-wait.service to /etc/systemd/system
· │   · · [  43.9] Making directory: /etc/systemd/system/multi-user.target.wants
· │   · · [  43.9] Linking: /etc/systemd/syst … wants/firstboot-wait.service -> /etc/systemd/system/firstboot-wait.service
· │   · · [  44.0] Making directory: /etc/systemd/scripts
· │   · · [  44.0] Copying: etc/systemd/scripts/firstboot-wait.sh to /etc/systemd/scripts
· │   · · [  44.1] Changing permissions of /etc/systemd/scripts/firstboot-wait.sh to 0755
· │   · · [  44.1] Installing firstboot command: usermod -l john.doe pi
· │   · · [  44.6] Installing firstboot command: groupmod -n john.doe pi
· │   · · [  45.0] Installing firstboot command: usermod -d /home/john.doe -m john.doe
· │   · · [  45.9] Finishing off
· │   · ✔︎
· │   ✔︎
· │   ◼ Disk Operations
· │   ◼ File Operations
· │   ▶ OS Preparations (1) ▶ Updating username of user pi to john.doe (1) ✔︎
· │   ◼ OS Boot
· │   ▶ OS Operations (1)
· │   · ▶ Running Raspberry Pi OS Lite ／ 202 … -raspios-buster-armhf-lite.img with ◀◀ finish rename
· │   · · image: /sdcard/filesystem.img                                             ◀◀ login ◀ finish rename ◀ shutdown
· ┊   · ·
· │   · · firstboot.sh[385]: === Running /usr … -john-doe-pi ===                    ◀◀ login❬1/4: wait … rename ◀ shutdown
· │   · · firstboot.sh[385]: === Running /usr … n-john-doe-pi ===                   ◀◀ login❬1/4: wait … rename ◀ shutdown
· │   · · firstboot.sh[385]: === Running /usr … --home-john-doe--m-john-doe ===     ◀◀ login❬1/4: wait … rename ◀ shutdown
· │   · · firstboot-wait.sh[387]: CHECKING SCRIPTS ⮕ SCRIPTS-DONE … COMPLETED      ◀◀ login❬1/4: wait … rename ◀ shutdown
· │   · · [  OK  ] Started libguestfs firstboot service completion.                 ◀◀ login❬1/4: wait … rename ◀ shutdown
· │   · · [  OK  ] Started Serial Getty on ttyAMA0.                                 ◀◀ login❬1/4: wait … rename ◀ shutdown
· │   · · [  OK  ] Reached target Login Prompts.                                    ◀◀ login❬1/4: wait … rename ◀ shutdown
· │   · ·                                                                           ◀◀ login❬1/4: wait … rename ◀ shutdown
· │   · ·                                                                           ◀◀ login❬1/4: wait … rename ◀ shutdown
· │   · · Raspbian GNU/Linux 10 sample--aTy0 ttyAMA0                                ◀◀ login❬1/4: wait … rename ◀ shutdown
· │   · ·                                                                           ◀◀ login❬1/4: wait … rename ◀ shutdown
· │   · · sample--aTy0 login:                                                       ◀◀ login❬1/4: wait … rename ◀ shutdown
· │   · ·                                                                           
· │   · ·         ̣ ˱ ❨ ( Entering "john.doe" )                                      
· │   · · {*≧∀≦}                                                                    
· │   · ·                                                                           
· │   · · sample--aTy0 login: john.doe                                              ◀◀ login❬2/4: conf … rename ◀ shutdown
· │   · ·                                                                           ◀◀ login❬2/4: conf … rename ◀ shutdown
· │   · · Password:                                                                 ◀◀ login❬3/4: pass … rename ◀ shutdown
· │   · ·                                                                           
· │   · ·         ̣ ˱ ❨ ( Entering "raspberry" )                                     
· │   · · ( ´ｰ`)                                                                    
· │   · ·                                                                           
· │   · · Password:                                                                 ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · · Linux sample--aTy0 4.19.50+ #1 Tue Nov 26 01:49:16 CET 2019 armv6l        ◀◀ login❬3/4: pass … rename ◀ shutdown
· │   · ·                                                                           ◀◀ login❬3/4: pass … rename ◀ shutdown
· │   · · The programs included with the Debian GNU/Linux system are free software; ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · · the exact distribution terms for each program are described in the        ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · · individual files in /usr/share/doc/*/copyright.                           ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · ·                                                                           ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · · Debian GNU/Linux comes with ABSOLUTELY NO WARRANTY, to the extent         ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · · permitted by applicable law.                                              ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · · [  OK  ] Created slice User Slice of UID 1000.                            ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · ·          Starting User Runtime Directory /run/user/1000...                ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · · [  OK  ] Started User Runtime Directory /run/user/1000.                   ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · ·          Starting User Manager for UID 1000...                            ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · · [  OK  ] Started User Manager for UID 1000.                               ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · · [  OK  ] Started Session c1 of user john.doe.                             ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · · john.doe@sample--aTy0:~$                                                  ◀◀ login❬4/4 confi … rename ◀ shutdown
· │   · ·                                                                           
· │   · ·               ̣ ˱ ❨ ( Logged in successfully )                             
· │   · · （；￣︶￣）                                                                
· │   · ·                                                                           
· │   · ·                                                                           
· │   · ·            ̣ ˱ ❨ ( Entering "ls /home/pi" )                                
· │   · · （´∀`）ｂ                                                                    
· │   · ·                                                                           
· │   · · john.doe@sample--aTy0:~$ ls /home/pi                                      ◀◀ finish rename❬1/3: ls …❭ ◀ shutdown
· │   · · ls: cannot access '/home/pi': No such file or directory                   ◀◀ finish rename❬2/3: id❭ ◀ shutdown
· │   · · john.doe@sample--aTy0:~$                                                  ◀◀ finish rename❬2/3: id❭ ◀ shutdown
· │   · ·                                                                           
· │   · ·      ̣ ˱ ❨ ( Entering "id pi" )                                            
· │   · · ▼ω▼                                                                       
· │   · ·                                                                           
· │   · · john.doe@sample--aTy0:~$ id pi                                            ◀◀ finish rename❬2/3: id …❭ ◀ shutdown
· │   · · id: ‘pi’: no such user                                                    ◀◀ finish rename❬3/3: id❭ ◀ shutdown
· │   · · john.doe@sample--aTy0:~$                                                  ◀◀ finish rename❬3/3: id❭ ◀ shutdown
· │   · ·                                                                           
· │   · ·         ̣ ˱ ❨ ( Entering "id john.doe" )                                   
· │   · · (^_^)v                                                                    
· │   · ·                                                                           
· │   · · john.doe@sample--aTy0:~$ id john.doe                                      ◀◀ finish rename❬3/3: id …❭ ◀ shutdown
· │   · · uid=1000(john.doe) gid=1000(john.doe) groups … (cdrom),27(sudo),29(audio) ◀◀ finish rename❬w … inish❭ ◀ shutdown
· │   · · john.doe@sample--aTy0:~$                                                  ◀◀ finish rename❬w … inish❭ ◀ shutdown
· │   · · Watchdog started. Timing out in 5.00s.                                    
· │   · · Watchdog stopped.                                                         
· │   · ·                                                                           
· │   · ·         ̣ ˱ ❨ ( Entering "'sudo' 'shutdown' '-h' 'now'" )                  
· │   · · (^^)ｂ                                                                    
· │   · ·                                                                           
· │   · · john.doe@sample--aTy0:~$ 'sudo' 'shutdown' '-h' 'now'                     ◀◀ shutdown❬shutting down❭
· │   · · [  OK  ] Stopped target Timers.                                           ◀◀ shutdown❬shutting down❭
· │   · · [  OK  ] Stopped Daily Cleanup of Temporary Directories.                  ◀◀ shutdown❬shutting down❭
· ┊   · ·
· │   · · [  OK  ] Reached target Power-Off.                                        ◀◀ shutdown❬shutting down❭
· │   · · reboot: System halted                                                     ◼
· │   · ✔︎
· │   ✔︎
· │
· ╰──╴✔︎
· ╭──╴Set Password of john.doe
· │
· │   ◼ Disk Preparations
· │   ▶ Disk Customizations (1)
· │   · ▶ 1 virt-customize operation 🐳 bkahlert/libguestfs: file:///tmp/koodies/exec/6z1.sh
· │   · · [   0.0] Examining the guest ...
· │   · · [  32.1] Setting a random seed
· │   · · [  32.6] Setting passwords
· │   · · [  43.9] Finishing off
· │   · ✔︎
· │   ✔︎
· │   ◼ Disk Operations
· │   ◼ File Operations
· │   ▶ OS Preparations (1) ▶ Updating password of user john.doe (1) ✔︎
· │   ◼ OS Boot
· │   ◼ OS Operations
· │
· ╰──╴✔︎
✔︎

(／￣‿￣)／~~☆’․･․･﹕☆ 2021-05-07-raspios-buster-armhf-lite.img @ file:///home/john/sample/2021-07-19T23-56-57--OJPy/
```

## Usage

```shell script
docker build --no-cache=true \
  --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
  --build-arg VCS_REF=$(git rev-parse --short HEAD) \
  --build-arg VERSION=$(./gradlew properties | grep ^version: | perl -pe 's/version:\s+//') \
  -t bkahlert/kustomize:latest .
```

1. Download
2. [Install](#installation) dependencies
3. `kustomize` --img=raspberry-pi-os-lite --version=LATEST --ssh.enabled=true --flash.auto=true

Alternatively run `--config-file=bother-you.json

## Installation

### GraalVM

- [Docker](https://docker.com): `docker pull oracle/graalvm-ce:20.2.0-java11`
- [sdkman](https://sdkman.io): `sdk install 20.2.0-r11-grl`
- [Homebrew](https://brew.sh): `brew cask install graalvm/tap/graalvm-ce-java11`

### Native Image Builder

- [gu](https://www.graalvm.org/docs/reference-manual/gu/): `gu install native-image`
- Run `bin/kustomize`

## Development

### Technologies Used

- [Kotlin](https://kotlinlang.org/) as the programming language
- [Config4k](https://github.com/config4k/config4k) to process HOCON configuration files
- [Gradle]() as the build tool
- [Gradle "Use Latest Versions Plugin"](https://github.com/patrikerdes/gradle-use-latest-versions-plugin)
  and [Gradle Versions Plugin](https://github.com/ben-manes/gradle-versions-plugin) to easily update all used version
- [JUnit 5](https://junit.org/junit5/) for testing
- [Gradle Shadow](https://github.com/johnrengelman/shadow) and [GraalVM Native Image Plugin](https://github.com/mike-neck/graalvm-native-image-plugin) to
  generate a native self-enclosed binary provided as a [Docker](https://www.docker.com/) image

### Testing

```text
                                                                                                                            
                        ●                                                                                                   
                       ╱ ╲                                                                                                  
                      ╱   ╲        __ \             |               __ )                      |                             
                     ╱     ╲       |   |  _ \   __| |  /  _ \  __|  __ \   _` |  __|  _ \  _` |                             
                    ╱       ╲      |   | (   | (      <   __/ |     |   | (   |\__ \  __/ (   |                             
                   ╱         ╲    ____/ \___/ \___|_|\_\\___|_|    ____/ \__,_|____/\___|\__,_|                             
                  ╱   ≤ 15'   ╲                                                                                             
                 ╱  @E2E @Test ╲                     __ __|         |                                                       
                ╱               ╲                       |  _ \  __| __|  __|                                                
               ╱ ─ ─ ─ ─ ─ ─ ─ ─ ╲                      |  __/\__ \ |  \__ \                                                
              ╱                   ╲                    _|\___|____/\__|____/                                                
             ╱                     ╲                                                                                        
            ╱          ≤ 2'         ╲                             ##        .                                               
           ╱  @DockerRequiring @Test ╲                      ## ## ##       ==                                               
          ╱                           ╲                  ## ## ## ##      ===                                               
         ╱                             ╲             /""""""""""""""""\___/ ===                                             
        ╱ ~~ ~~~ ~~~  ~~~~ ~~~~~ ~~~~ ~ ~~~~ ~  ~~~ {~~ ~~~~ ~~~ ~~~~ ~~ ~ /  ===- ~~~                                      
       ╱                                 ╲           \______ o          __/                                                 
      ╱                                   ╲            \    \        __/                                                    
     ╱                                     ╲            \____\______/                                                       
    ╱                 ≤ 10"                 ╲                                                                               
   ╱                  @Test                  ╲       _ \          |                |                 __ __|        |        
  ╱                                           ╲      |  | _ \  _| | /  -_)  _|____||     -_)(_-<(_-<    |  -_)(_-<  _|(_-<  
 ╱                                             ╲    ___/\___/\__|_\_\\___|_|      ____|\___|___/___/   _|\___|___/\__|___/  
●━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━●                                                                           
```

### Debugging

Running all tests will despite the number hardly leave any output.  
The reason for that the tests are run concurrently, and those relying on specific logging on the console would need non-parallel execution.

Therefore, logging is done in-memory using the component `InMemoryLogger`. Assertions on specific output can be done on that component's properties which give
access to the overall output and `in`, `out` and `err` separately—each with or without ANSI control sequences.

To actually see the output of a process, it suffices to run a single test only. `InMemoryLoggerResolver`, which is the component to provide instances
of `InMemoryLogger` will take notice and configure the logger to not only capture the output but to also actually forward it to the console.

Alternatively tests or test containers (= classes) can be annotated with `@Debug` which makes the corresponding logger also print to the console and which
temporarily deactivates all other tests. In contrast to run a single test this approach also allows multiple `@Debug` annotated tests to run while still seeing
output. Although that output is very likely mingled since those tests still run in parallel by default.

## TODO

- [ ] Replace curl based download with portable implementation
- [ ] 
  Consider [manually controlled](https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/) [TestContainer](https://www.testcontainers.org/features/startup_and_waits/#one-shot-startup-strategy-example)
  possibly [creating them on the fly](https://www.testcontainers.org/features/creating_images/); seems to even work
  with [consuming logs](https://www.testcontainers.org/features/container_logs/)
- [ ] Bluetooth PAN
- [ ] Serial connection using g_serial (`sudo systemctl enable serial-getty@ttyGS0.service`)
  , [based on INI files](https://www.digitalocean.com/community/tutorials/understanding-systemd-units-and-unit-files) so consider doing while increasing disk
  image
    - [ ] Connect on `/dev/tty.usbmodemNNNN` using "115200 baud (8N1 8- bit No-parity 1-stop if you need to set that)"
- [ ] Add docker check
- [ ] apt-get install -y git streamer (webcam tool)
- [ ] https://github.com/nmcclain/raspberian-firstboot
- [ ] https://github.com/kenfallon/fix-ssh-on-pi !!!
- [ ] use in memory filesystem

- name: Run the equivalent of "apt-get update" as a separate step apt:
  update_cache: true cache_valid_time: 3600
- name: Update all packages to the latest version apt:
  upgrade: dist

https://github.com/garthvh/ansible-raspi-playbooks/tree/master/tasks

https://github.com/garthvh/ansible-raspi-playbooks/blob/master/tasks/tzdata.yml

https://github.com/garthvh/ansible-raspi-playbooks/blob/master/tasks/internationalization.yml

https://github.com/garthvh/ansible-raspi-playbooks/blob/master/playbooks/new-default.yml

### Demo

- `FirstBootOrderFixTest.kt`
- `Size.kt`
- `NonBlockingReader.kt`
- `OperatingSystemTest.kt`
- `ArmRunnerTest.kt`
- `PatchesKtTest.kt`
- `RenderingLoggerIntTest.kt`
- `Program` class (e.g. login script)
- `ContainsExactlyInSomeOrder` (easily understandable builder pattern)
  -> now in koodies.builder
- `ImgFixture` (static builder)
- `ShellScriptBuilder` (with "echo 'this is a command'")
- `ReadOnlyFileSystem`
- `RegExDocument` (delegates properties)
- `PatchTest`, esp. GuestfishCommandBuilder (Complex builder pattern)

### Done and Deleted

… because it later was provided by Kotlin

- Java NIO extension functions
- generic builder (e.g. `buildList`)
