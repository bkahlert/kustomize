/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)" && brew update && brew install qemu
export QEMU=$(which qemu-system-arm)

# Dowload kernel and export location
curl -OL
https://github.com/dhruvvyas90/qemu-rpi-kernel/raw/master/kernel-qemu-4.4.34-jessie
export RPI_KERNEL=./kernel-qemu-4.4.34-jessie

# Download filesystem and export location
curl -o 2017-03-02-raspbian-jessie.zip -L http://downloads.raspberrypi.org/raspbian/images/raspbian-2017-03-03/2017-03-02-raspbian-jessie.zip
unzip 2017-03-02-raspbian-jessie.zip
export RPI_FS=./2017-03-02-raspbian-jessie.zip

# Tweak filesystem: start qemu with init flag, switch to guest window to execute tweak and close window afterwards
$QEMU -kernel $RPI_KERNEL -cpu arm1176 -m 256 -M versatilepb -no-reboot -serial stdio -append "root=/dev/sda2 panic=1 rootfstype=ext4 rw init=/bin/bash" -drive "file=2017-03-02-raspbian-jessie.img,index=0,media=disk,format=raw"

# enter these on the qemu terminal and exit after
sed -i -e 's/^/#/' /etc/ld.so.preload
sed -i -e 's/^/#/' /etc/ld.so.conf
sed -i -e 's/^/#/' /etc/fstab

# Emulate Raspberry Pi

$QEMU -kernel $RPI_KERNEL -cpu arm1176 -m 256 -M versatilepb -no-reboot -serial stdio -append "root=/dev/sda2 panic=1 rootfstype=ext4 rw" -drive "file=2017-03-02-raspbian-jessie.img,index=0,media=disk,format=raw" -net user,hostfwd=tcp::5022-:22

# Login to Raspberry Pi
ssh -p 5022 pi@localhost
