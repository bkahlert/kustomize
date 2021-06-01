[ -z "$(grep modules-load=dwc2,g_ether cmdline.txt)" ] && sed -i 's/\<rootwait\>/& modules-load=dwc2,g_ether/' cmdline.txt

echo ""

OLD

#!/bin/sh
cd /Users/bkahlert/.imgcstmzr.test/guestfish
docker rm guestfish
docker 2>&1 run \
  --env LIBGUESTFS_DEBUG=0 \
  --env LIBGUESTFS_TRACE=0 \
  --env GUESTFISH_PS1='(〜￣△￣)〜o/￣￣￣<゜)))彡 ' \
  --name "guestfish" \
  -it \
  --rm \
  --volume $(PWD)/raspi.img:/work/raspi.img \
  --volume $(PWD)/guestfish.shared:/work/guestfish.shared \
  bkahlert/libguestfs \
  --rw \
  --add /work/raspi.img \
  --mount /dev/sda2:/ \
  --mount /dev/sda1:/boot \
  <<HERE
docker
HERE

NEW

1. Bash
docker run --rm --name "guestfish" -it \
  --mount type=bind,source="$(pwd)/shared/",target=/shared \
  --mount type=bind,source="$(pwd)/disk.img",target=/images/disk.img \
  --entrypoint /bin/bash bkahlert/libguestfs

docker run --rm -v $(PWD)/shared:/shared -v $(PWD)/disk.img:/images/disk.img --entrypoint /bin/bash -it bkahlert/libguestfs
docker run --privileged --rm -v $(PWD)/shared:/shared -v $(PWD)/disk.img:/images/disk.img --entrypoint /bin/bash -it bkahlert/libguestfs

2. Guestfish

#!/bin/sh
cd /Users/bkahlert/.imgcstmzr.test/guestfish
docker 2>&1 run \
  --name "guestfish" \
  --interactive \
  --tty \
  --rm \
  --mount type=bind,source="$(pwd)/shared/",target=/shared \
  --mount type=bind,source="$(pwd)/disk.img",target=/images/disk.img \
  bkahlert/libguestfs \
  --rw \
  --add /images/disk.img \
  --mount /dev/sda2:/ \
  --mount /dev/sda1:/boot \
  <<HERE
…
HERE

docker 2>&1 run \
  --env LIBGUESTFS_DEBUG="1" \
  --env LIBGUESTFS_TRACE="1" \
  --workdir / \
  --rm \
  --interactive \
  --tty \
  --mount type=bind,source="$(pwd)/shared/",target=/shared \
  --mount type=bind,source="$(pwd)/disk.img",target=/images/disk.img \
  bkahlert/libguestfs \
  --ro \
  --add /images/disk.img \
  --mount /dev/sda2:/ \
  --mount /dev/sda1:/boot \
  <<COMMANDS
!mkdir -p "shared/bin"
-copy-out "/bin" "shared/bin"
umount-all
exit
COMMANDS

3. virt-customiz

--entrypoint virt-customize \
  --rw \
  --add /images/disk.img \
  --mount /dev/sda2:/ \
  --mount /dev/sda1:/boot
#--firstBoot
#--firstboot-install
#--hostname
#--password
#--root-password
#--run SCRIPT
--ssh-inject USER:file:FILENAME
#    Read the ssh key from FILENAME. FILENAME is usually a .pub file.
--ssh-inject USER:string:KEY_STRING
#    Use the specified KEY_STRING. KEY_STRING is usually a public string like ssh-rsa AAAA.... user@localhost.
--ssh-inject USER[:SELECTOR]
--timezone TIMEZONE
--touch FILE
--colors

3.1 Copy SSH-KEY
cd /Users/bkahlert/.imgcstmzr.test/guestfish
docker 2>&1 run \
  --entrypoint virt-customize \
  --name "guestfish" \
  -it \
  --rm \
  --mount type=bind,source="$(pwd)/shared/",target=/shared \
  --mount type=bind,source="$(pwd)/disk.img",target=/images/disk.img \
  bkahlert/libguestfs \
  --add /images/disk.img \
  --ssh-inject pi:string:"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDFqErnUaNo+UYn3dITTebC6mZMVtsUapSl4lGzG3HWKbqmYwfQUO7JBgwENmrNQMH7+F6YNvArnnezRk6iwBP8Z08cqsedq0zXJA16MaiegAWhcoEAidRqWXx8xml0gc80X3yzXP6PyP7/xbMIcIMGpX6rGnUSo8zaLTLOiEQ3LL1nXSPi1j+BAEOVMhIPe84V31HAvnBby37Ii4wfdBOEILBxBRAuYo7NM3C4gLZJyqqJZH1z1iocxXydxehlOApKDSIqZPCgCaMqw7WVJKTrg1SIcjAJqiepWq8oWPf3dMHf9Z+Yi+p/1DX4s/CUV6NOvBpE2f1+DZnf7r0o/CCtxkXwl1G7ur4DF0T+M+veNMBiqTGHBSKR+3j4HvM3Bs8AnDNxqS9ywxeeWuKHKJIbruZmYuEMZH0ubDSgViBbZ79nOZPN5SyqfRDD7Y4/X47JnLoMO7htDilDvn/KU3niEoRsV2wrtLhdO9kfM8m3/yJw27VuHRQxTF/iZN7qRKr+hW9O3OKmnwV5GXScqX0dyP2Eb12xmwyCdnyHbkdyNe0gqgDtpFRrFHoaonlM7iT1tuenX5ot5rkJbJIBHRevAhRcVXZG+jZwOvyoJMmUH2qmITchmJ+9SRTvdt3pyOFkQMwXXD7VA9ENbwhJIJbS5A5nWJ4/SLLesHzcw4exuQ== 'Dr. Björn Kahlert, RSA key, 2020-08'"

4. virt-resize

--align-first auto
--colors
--alignment 2048
--resize /dev/sda2=10G
--expand PART

UID
cat /etc/passwd | awk -F: '$1 == "pi" { print $3 }'

GID
cat /etc/passwd | awk -F: '$1 == "pi" { print $3 }'

HOME_DIR
cat /etc/passwd | awk -F: '$1 == "pi" { print $7 }'

mkdir docker-build
cd docker-build
git clone https://github.com/bkahlert/libguestfs
cd guestfish
# add augeas to dockerfile // http://augeas.net/tour.html
docker build -t bkahlert/libguestfs .

docker run --rm -v $(PWD):/work --entrypoint /bin/bash -it bkahlert/libguestfs
guestfish
add /work/raspi.img
mount /dev/sda2:/
mount /dev/sda1:/boot

guestfish
add /work/raspi.img
run
mount "/dev/sda1" "/"
mount-local /work/guestfish.shared readonly:true
mount-local-run

guestmount -a disk.img -i /shared -o nonempty
rm -rf guestfish.shared/* && guestmount -a disk.img -i /shared -o allow_other
guestmount -a disk.img -m -i /shared -o allow_other
