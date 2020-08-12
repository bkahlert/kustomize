# Reflector

## Installation

```shell script
REFLECTOR=~/Development/com.bother-you/reflector/resources/install.sh && chmod +x $REFLECTOR && $REFLECTOR
```

```shell script
cd ~/Development/com.bother-you/reflector/resources
chmod +x install.sh
env CUSTOM_RASPIOS_BOOT='oldname="$(cat etc/hostname)" && for i in etc/host*; do sed -i "s/$oldname/bjorns-reflector/g" $i; done' ./install.sh
```

## To-Do

- Blueooth
  - As Internetconnection through host
  - As hotstop for host
- Locale (e.g. de_DE.UTF-8)
- Splash screen
- write CLI in Kotlin
  https://github.com/deepakshrma/kotlin-demos/tree/master/scripts  
