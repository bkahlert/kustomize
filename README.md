# ImgCstmzr — Kotlin Based Image Customizer

**ONLY THOROUGLY TESTED WITH RASPBERRY PI OS IMAGES**

```text
╭──╴Running Raspberry Pi OS Lite ／ 2021-03-04-raspios-buster-armhf-lite.img with ◀◀ print HOME
│
│   image: /sdcard/filesystem.img                                                     ◀◀ login ◀ print HOME ◀ shutdown
│   file format: raw                                                                  ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   virtual size: 1.74 GiB (1866465280 bytes)                                         ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   disk size: 1.74 GiB                                                               ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   Booting QEMU machine "versatilepb" with kernel=/root/qemu-rpi-kernel/kernel-      ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   qemu-4.19.50-buster dtb=/root/qem
┊                                                                                    
│   Run /sbin/init as init process                                                    ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   systemd[1]: Failed to lookup module alias 'autofs4': Function not implemented     ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   systemd[1]: systemd 241 running in system mode. (+PAM +AUDIT +SELINUX +IMA        ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│    +APPARMOR +SMACK +SYSVINIT +UTMP +LIBCRYPTSETUP +GCRYPT +GNUTLS +ACL +XZ
│    +LZ4 +SECCOMP +BLKID +ELFUTILS +KMOD -IDN2 +IDN -PCRE2 default-hierarchy=hybrid)
│   systemd[1]: Detected architecture arm.                                            ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│                                                                                     ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   Welcome to Raspbian GNU/Linux 10 (buster)!                                        ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│                                                                                     ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   systemd[1]: Set hostname to <raspberrypi>.                                        ◀◀ login❬1/4: wait … t HOME ◀ shutdown
┊
│   systemd[1]: Initializing machine ID from random generator.                        ◀◀ login❬1/4: wait … t HOME ◀ shutdown
┊
│   [  OK  ] Started /etc/rc.local Compatibility.                                     ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   [  OK  ] Started Permit User Sessions.                                            ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   [  OK  ] Started Getty on tty1.                                                   ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   [  OK  ] Started Serial Getty on ttyAMA0.                                         ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   [  OK  ] Reached target Login Prompts.                                            ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│                                                                                     ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│                                                                                     ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   Raspbian GNU/Linux 10 raspberrypi ttyAMA0                                         ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│                                                                                     ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│   raspberrypi login: [  OK  ] Started Regenerate SSH host keys.                     ◀◀ login❬1/4: wait … t HOME ◀ shutdown
│                                                                                     
│            ̣ ˱ ❨ ( Entering "pi" )                                                   
│   (*^-ﾟ)v                                                                           
│                                                                                     
│   [ TIME ] Timed out waiting for device /dev/serial1.                               ◀◀ login❬2/4: conf … t HOME ◀ shutdown
│   [DEPEND] Dependency failed for Conf…ooth Modems connected by UART.                ◀◀ login❬3/4: pass … t HOME ◀ shutdown
│   pi                                                                                ◀◀ login❬3/4: pass … t HOME ◀ shutdown
│                                                                                     ◀◀ login❬3/4: pass … t HOME ◀ shutdown
│                                                                                     
│            ̣ ˱ ❨ ( Entering "raspberry" )                                            
│   (o^-`)b                                                                           
│                                                                                     
│   Password:                                                                         ◀◀ login❬4/4 confi … t HOME ◀ shutdown
│   Linux raspberrypi 4.19.50+ #1 Tue Nov 26 01:49:16 CET 2019 armv6l                 ◀◀ login❬3/4: pass … t HOME ◀ shutdown
│                                                                                     ◀◀ login❬4/4 confi … t HOME ◀ shutdown
│   The programs included with the Debian GNU/Linux system are free software;         ◀◀ login❬4/4 confi … t HOME ◀ shutdown
│   the exact distribution terms for each program are described in the                ◀◀ login❬4/4 confi … t HOME ◀ shutdown
│   individual files in /usr/share/doc/*/copyright.                                   ◀◀ login❬4/4 confi … t HOME ◀ shutdown
│                                                                                     ◀◀ login❬4/4 confi … t HOME ◀ shutdown
│   Debian GNU/Linux comes with ABSOLUTELY NO WARRANTY, to the extent                 ◀◀ login❬4/4 confi … t HOME ◀ shutdown
│   permitted by applicable law.                                                      ◀◀ login❬4/4 confi … t HOME ◀ shutdown
│   pi@raspberrypi:~$                                                                 ◀◀ login❬4/4 confi … t HOME ◀ shutdown
│                                                                                     
│            ̣ ˱ ❨ ( Logged in successfully )                                          
│   o(^▽^)o                                                                           
│                                                                                     
│                                                                                     
│            ̣ ˱ ❨ ( Entering "printenv HOME" )                                        
│   o(^▽^)o                                                                           
│                                                                                     
│   pi@raspberrypi:~$ printenv HOME                                                   ◀◀ print HOME❬1/1: … env …❭ ◀ shutdown
│   /home/pi                                                                          ◀◀ print HOME❬1/1: … env …❭ ◀ shutdown
│   pi@raspberrypi:~$                                                                 ◀◀ print HOME❬1/1: … env …❭ ◀ shutdown
│   pi@raspberrypi:~$                                                                 ◀◀ print HOME❬wait … inish❭ ◀ shutdown
│   Watchdog started. Timing out in 5.00s.                                            
│   Watchdog stopped.                                                                 
│                                                                                     
│               ̣ ˱ ❨ ( Entering "sudo shutdown -h now" )                              
│   v(*`-^*)ｂ                                                                        
│                                                                                     
│   pi@raspberrypi:~$ sudo shutdown -h now                                            ◀◀ shutdown❬shutting down❭
│            Unmounting RPC Pipe File System...                                       ◀◀ shutdown❬shutting down❭
│            Stopping Session c1 of user pi.                                          ◀◀ shutdown❬shutting down❭
│   [  OK  ] Stopped target Timers.                                                   ◀◀ shutdown❬shutting down❭
│   [  OK  ] Stopped Daily man-db regeneration.                                       ◀◀ shutdown❬shutting down❭
│   [  OK  ] Stopped Daily Cleanup of Temporary Directories.                          ◀◀ shutdown❬shutting down❭
│   [  OK  ] Stopped Daily apt upgrade and clean activities.                          ◀◀ shutdown❬shutting down❭
│   [  OK  ] Stopped Daily apt download activities.                                   ◀◀ shutdown❬shutting down❭
│   [  OK  ] Stopped target Graphical Interface.                                      ◀◀ shutdown❬shutting down❭
│   [  OK  ] Stopped target Multi-User System.                                        ◀◀ shutdown❬shutting down❭
┊
│   [  OK  ] Reached target Shutdown.                                                 ◀◀ shutdown❬shutting down❭
│   [  OK  ] Reached target Final Step.                                               ◀◀ shutdown❬shutting down❭
│   [  OK  ] Started Power-Off.                                                       ◀◀ shutdown❬shutting down❭
│   [  OK  ] Reached target Power-Off.                                                ◀◀ shutdown❬shutting down❭
│   reboot: System halted                                                             ◀◀ shutdown❬shutting down❭
│   Kernel panic - not syncing: Attempted to kill init! exitcode=0x00000000           ◀◀ shutdown❬shutting down❭
│                                                                                     ◀◀ shutdown❬shutting down❭
│   CPU: 0 PID: 1 Comm: systemd-shutdow Not tainted 4.19.50+ #1                       ◀◀ 
│   Hardware name: ARM-Versatile (Device Tree Support)                                ◀◀ 
│   [<c001d230>] (unwind_backtrace) from [<c00190ac>] (show_stack+0x10/0x14)          ◀◀ 
│   [<c00190ac>] (show_stack) from [<c0025f14>] (panic+0xc8/0x240)                    ◀◀ 
│   [<c0025f14>] (panic) from [<c0028444>] (do_exit+0x950/0x9fc)                      ◀◀ 
│   [<c0028444>] (do_exit) from [<c0042440>] (sys_reboot+0x1a0/0x1f0)                 ◀◀ 
│   [<c0042440>] (sys_reboot) from [<c0009000>] (ret_fast_syscall+0x0/0x54)           ◀◀ 
│   Exception stack(0xcf823fa8 to 0xcf823ff0)                                         ◀◀ 
│   3fa0:                   00000000 00000000 fee1dead 28121969 4321fedc 93372d00     ◀◀ 
│   3fc0: 00000000 00000000 00000000 00000058 00000fff beb14c08 00000000 00486b80     ◀◀ 
│   3fe0: 00498e3c beb14b88 004834a8 b6efca38                                         ◀◀ 
│   Rebooting in 1 seconds..                                                          ◀◀ 
│
╰──╴✔︎
```

## Usage

```shell script
docker build --no-cache=true \
  --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
  --build-arg VCS_REF=$(git rev-parse --short HEAD) \
  --build-arg VERSION=$(./gradlew properties | grep ^version: | perl -pe 's/version:\s+//') \
  -t bkahlert/imgcstmzr:latest .
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
