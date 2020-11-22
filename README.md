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
- [ ] Serial connection using g_serial (`sudo systemctl enable getty@ttyGS0.service`)
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

- `Size.kt`
- `NonBlockingReader.kt`
- `OperatingSystemTest.kt`
- `ArmRunnerTest.kt`
- `PatchesKtTest.kt`
- `RenderingLoggerIntTest.kt`
- `Program` class (e.g. login script)
- `ContainsExactlyInSomeOrder` (easily understandable builder pattern)
- `ImgFixture` (static builder)
- `ShellScriptBuilder` (with !"echo 'this is a command'")
- `ReadOnlyFileSystem`
- `RegExDocument` (delegates properties)
- `PatchTest` (Complex builder pattern)
