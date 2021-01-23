Booting QEMU machine "versatilepb" with kernel=/root/qemu-rpi-kernel/kernel-qemu-4.19.50-buster dtb=/root/qemu-rpi-kernel/versatile-pb.dtb
 vpb_sic_write: Bad register offset 0x2c
 Booting Linux on physical CPU 0x0
Linux version 4.19.50+ (niklas@ubuntu) (gcc version 9.2.1 20191008 (Ubuntu 9.2.1-9ubuntu2)) #1 Tue Nov 26 01:49:16 CET 2019
 CPU: ARMv6-compatible processor [410fb767] revision 7 (ARMv7), cr=00c5387d
 CPU: VIPT aliasing data cache, unknown instruction cache
 OF: fdt: Machine model: ARM Versatile PB
 Memory policy: Data cache writeback
 On node 0 totalpages: 65536
   Normal zone: 576 pages used for memmap
   Normal zone: 0 pages reserved
   Normal zone: 65536 pages, LIFO batch:15
 random: get_random_bytes called from start_kernel+0x88/0x414 with crng_init=0
 pcpu-alloc: s0 r0 d32768 u32768 alloc=1*32768
 pcpu-alloc: [0] 0
 Built 1 zonelists, mobility grouping on.  Total pages: 64960
Kernel command line: rw earlyprintk loglevel=8 console=ttyAMA0,115200 dwc_otg.lpm_enable=0 root=/dev/sda2 rootwait panic=1
 Dentry cache hash table entries: 32768 (order: 5, 131072 bytes)
 Inode-cache hash table entries: 16384 (order: 4, 65536 bytes)
Memory: 252040K/262144K available (5406K kernel code, 211K rwdata, 1360K rodata, 196K init, 142K bss, 10104K reserved, 0K cma-reserved) prompt❭
 Virtual kernel memory layout:
     vector  : 0xffff0000 - 0xffff1000   (   4 kB)
     fixmap  : 0xffc00000 - 0xfff00000   (3072 kB)
     vmalloc : 0xd0800000 - 0xff800000   ( 752 MB)
     lowmem  : 0xc0000000 - 0xd0000000   ( 256 MB)
     modules : 0xbf000000 - 0xc0000000   (  16 MB)
       .text : 0x(ptrval) - 0x(ptrval)   (5408 kB)
       .init : 0x(ptrval) - 0x(ptrval)   ( 196 kB)
       .data : 0x(ptrval) - 0x(ptrval)   ( 212 kB)
        .bss : 0x(ptrval) - 0x(ptrval)   ( 143 kB)
 NR_IRQS: 16, nr_irqs: 16, preallocated irqs: 16
 VIC @(ptrval): id 0x00041190, vendor 0x41
 FPGA IRQ chip 0 "intc" @ (ptrval), 20 irqs, parent IRQ: 47
clocksource: arm,sp804: mask: 0xffffffff max_cycles: 0xffffffff, max_idle_ns: 1911260446275 ns
 sched_clock: 32 bits at 1000kHz, resolution 1000ns, wraps every 2147483647500ns
 Failed to initialize '/amba/timer@101e3000': -22
 sched_clock: 32 bits at 24MHz, resolution 41ns, wraps every 89478484971ns
 Console: colour dummy device 80x30
 Calibrating delay loop... 769.63 BogoMIPS (lpj=3848192)
 pid_max: default: 32768 minimum: 301
 Mount-cache hash table entries: 1024 (order: 0, 4096 bytes)
 Mountpoint-cache hash table entries: 1024 (order: 0, 4096 bytes)
 CPU: Testing write buffer coherency: ok
 Setting up static identity map for 0x8220 - 0x827c
 devtmpfs: initialized
 VFP support v0.3: implementor 41 architecture 1 part 20 variant b rev 5
clocksource: jiffies: mask: 0xffffffff max_cycles: 0xffffffff, max_idle_ns: 19112604462750000 ns
 futex hash table entries: 256 (order: -1, 3072 bytes)
 NET: Registered protocol family 16
 DMA: preallocated 256 KiB pool for atomic coherent allocations
 OF: amba_device_add() failed (-19) for /amba/smc@10100000
 OF: amba_device_add() failed (-19) for /amba/mpmc@10110000
 OF: amba_device_add() failed (-19) for /amba/sctl@101e0000
 OF: amba_device_add() failed (-19) for /amba/watchdog@101e1000
 OF: amba_device_add() failed (-19) for /amba/sci@101f0000
 OF: amba_device_add() failed (-19) for /amba/ssp@101f4000
 OF: amba_device_add() failed (-19) for /amba/fpga/sci@a000
 Serial: AMBA PL011 UART driver
 101f1000.uart: ttyAMA0 at MMIO 0x101f1000 (irq = 28, base_baud = 0) is a PL011 rev1
 console [ttyAMA0] enabled
 101f2000.uart: ttyAMA1 at MMIO 0x101f2000 (irq = 29, base_baud = 0) is a PL011 rev1
 101f3000.uart: ttyAMA2 at MMIO 0x101f3000 (irq = 30, base_baud = 0) is a PL011 rev1
 10009000.uart: ttyAMA3 at MMIO 0x10009000 (irq = 54, base_baud = 0) is a PL011 rev1
 vgaarb: loaded
 SCSI subsystem initialized
 clocksource: Switched to clocksource arm,sp804
 NET: Registered protocol family 2
 tcp_listen_portaddr_hash hash table entries: 512 (order: 0, 4096 bytes)
 TCP established hash table entries: 2048 (order: 1, 8192 bytes)
 TCP bind hash table entries: 2048 (order: 1, 8192 bytes)
 TCP: Hash tables configured (established 2048 bind 2048)
 UDP hash table entries: 256 (order: 0, 4096 bytes)
 UDP-Lite hash table entries: 256 (order: 0, 4096 bytes)
 NET: Registered protocol family 1
 RPC: Registered named UNIX socket transport module.
 RPC: Registered udp transport module.
 RPC: Registered tcp transport module.
 RPC: Registered tcp NFSv4.1 backchannel transport module.
 PCI: CLS 0 bytes, default 32
 NetWinder Floating Point Emulator V0.97 (double precision)
 workingset: timestamp_bits=14 max_order=16 bucket_order=2
 Installing knfsd (copyright (C) 1996 okir@monad.swb.de).
 jffs2: version 2.2. (NAND) © 2001-2006 Red Hat, Inc.
 romfs: ROMFS MTD (C) 2007 Red Hat, Inc.
 9p: Installing v9fs 9p2000 file system support
 Block layer SCSI generic (bsg) driver version 0.4 loaded (major 252)
 io scheduler noop registered
 io scheduler deadline registered
 io scheduler cfq registered (default)
 io scheduler mq-deadline registered
 io scheduler kyber registered
 pl061_gpio 101e4000.gpio: PL061 GPIO chip @0x101e4000 registered
 pl061_gpio 101e5000.gpio: PL061 GPIO chip @0x101e5000 registered
 pl061_gpio 101e6000.gpio: PL061 GPIO chip @0x101e6000 registered
 pl061_gpio 101e7000.gpio: PL061 GPIO chip @0x101e7000 registered
 versatile-pci 10001000.pci: host bridge /amba/pci@10001000 ranges:
 versatile-pci 10001000.pci:    IO 0x43000000..0x4300ffff -> 0x00000000
 versatile-pci 10001000.pci:   MEM 0x50000000..0x5fffffff -> 0x50000000
 versatile-pci 10001000.pci:   MEM 0x60000000..0x6fffffff -> 0x60000000
 versatile-pci 10001000.pci: PCI core found (slot 11)
 versatile-pci 10001000.pci: PCI host bridge to bus 0000:00
 pci_bus 0000:00: root bus resource [bus 00-ff]
 pci_bus 0000:00: root bus resource [io  0x0000-0xffff]
 pci_bus 0000:00: root bus resource [mem 0x50000000-0x5fffffff]
 pci_bus 0000:00: root bus resource [mem 0x60000000-0x6fffffff pref]
 pci 0000:00:0c.0: [1000:0012] type 00 class 0x010000
 pci 0000:00:0c.0: reg 0x10: [io  0x0000-0x00ff]
 pci 0000:00:0c.0: reg 0x14: [mem 0x00000000-0x000003ff]
 pci 0000:00:0c.0: reg 0x18: [mem 0x00000000-0x00001fff]
 PCI: bus0: Fast back to back transfers disabled
 pci 0000:00:0c.0: BAR 2: assigned [mem 0x50000000-0x50001fff]
 pci 0000:00:0c.0: BAR 1: assigned [mem 0x50002000-0x500023ff]
 pci 0000:00:0c.0: BAR 0: assigned [io  0x1000-0x10ff]
 drm-clcd-pl111 dev:20: no max memory bandwidth specified, assume unlimited
 drm-clcd-pl111 dev:20: set up callbacks for Versatile PL110
 OF: graph: no port node found in /amba/display@10120000
 drm-clcd-pl111 dev:20: No bridge, exiting
 brd: module loaded
 loop: module loaded
 sym53c8xx 0000:00:0c.0: enabling device (0100 -> 0103)
 sym0: <895a> rev 0x0 at pci 0000:00:0c.0 irq 66
 sym0: No NVRAM, ID 7, Fast-40, LVD, parity checking
 sym0: SCSI BUS has been reset.
 scsi host0: sym-2.2.3
 scsi 0:0:0:0: Direct-Access     QEMU     QEMU HARDDISK    2.5+ PQ: 0 ANSI: 5
 scsi target0:0:0: tagged command queuing enabled, command queue depth 16.
 scsi target0:0:0: Beginning Domain Validation
 scsi target0:0:0: Domain Validation skipping write tests
 scsi target0:0:0: Ending Domain Validation
 scsi 0:0:2:0: CD-ROM            QEMU     QEMU CD-ROM      2.5+ PQ: 0 ANSI: 5
 scsi target0:0:2: tagged command queuing enabled, command queue depth 16.
 scsi target0:0:2: Beginning Domain Validation
 scsi target0:0:2: Domain Validation skipping write tests
 scsi target0:0:2: Ending Domain Validation
 random: fast init done
 sr 0:0:2:0: Power-on or device reset occurred
 sr 0:0:2:0: [sr0] scsi3-mmc drive: 16x/50x cd/rw xa/form2 cdda tray
 cdrom: Uniform CD-ROM driver Revision: 3.20
 sd 0:0:0:0: Power-on or device reset occurred
 sd 0:0:0:0: [sda] 3624960 512-byte logical blocks: (1.86 GB/1.73 GiB)
 sd 0:0:0:0: [sda] Write Protect is off
 sd 0:0:0:0: [sda] Mode Sense: 63 00 00 08
 sd 0:0:0:0: [sda] Write cache: enabled, read cache: enabled, doesn't support DPO or FUA
 sr 0:0:2:0: Attached scsi CD-ROM sr0
 of-flash 34000000.flash: versatile/realview flash protection
34000000.flash: Found 1 x32 devices at 0x0 in 32-bit bank. Manufacturer ID 0x000000 Chip ID 0x000000
 Intel/Sharp Extended Query Table at 0x0031
 Using buffer write method
 erase region 0: offset=0x0,size=0x40000,blocks=256
  sda: sda1 sda2
 sd 0:0:0:0: [sda] Attached SCSI disk
 smc91x.c: v1.1, sep 22 2004 by Nicolas Pitre <nico@fluxnic.net>
 smc91x 10010000.net eth0: SMC91C11xFD (rev 1) at (ptrval) IRQ 41
 smc91x 10010000.net eth0: Ethernet addr: 52:54:00:12:34:56
 rtc-ds1307 0-0068: registered as rtc0
 versatile reboot driver registered
 device-mapper: ioctl: 4.39.0-ioctl (2018-04-03) initialised: dm-devel@redhat.com
 mmci-pl18x fpga:05: /aliases ID not available
 mmci-pl18x fpga:05: mmc0: PL181 manf 41 rev0 at 0x10005000 irq 59,60 (pio)
 mmci-pl18x fpga:05: DMA channels RX none, TX none
 mmci-pl18x fpga:0b: /aliases ID not available
 mmci-pl18x fpga:0b: mmc1: PL181 manf 41 rev0 at 0x1000b000 irq 49,50 (pio)
 mmci-pl18x fpga:0b: DMA channels RX none, TX none
 leds-syscon 10000000.core-module:led@08.0: registered LED versatile:0
 leds-syscon 10000000.core-module:led@08.1: registered LED versatile:1
 leds-syscon 10000000.core-module:led@08.2: registered LED versatile:2
 leds-syscon 10000000.core-module:led@08.3: registered LED versatile:3
 leds-syscon 10000000.core-module:led@08.4: registered LED versatile:4
 leds-syscon 10000000.core-module:led@08.5: registered LED versatile:5
 leds-syscon 10000000.core-module:led@08.6: registered LED versatile:6
 leds-syscon 10000000.core-module:led@08.7: registered LED versatile:7
 ledtrig-cpu: registered to indicate activity on CPUs
 NET: Registered protocol family 17
 Bridge firewalling registered
 9pnet: Installing 9P2000 support
 rtc-ds1307 0-0068: setting system clock to 2020-10-11 04:39:03 UTC (1602391143)
 uart-pl011 101f1000.uart: no DMA platform data
input: AT Raw Set 2 keyboard as /devices/platform/amba/amba:fpga/10006000.kmi/serio0/input/input0
input: ImExPS/2 Generic Explorer Mouse as /devices/platform/amba/amba:fpga/10007000.kmi/serio1/input/input2
 EXT4-fs (sda2): mounted filesystem with ordered data mode. Opts: (null)
 VFS: Mounted root (ext4 filesystem) on device 8:2.
 devtmpfs: mounted
 Freeing unused kernel memory: 196K
 This architecture does not have kernel memory protection.
 Run /sbin/init as init process
 systemd[1]: Failed to lookup module alias 'autofs4': Function not implemented
systemd[1]: systemd 241 running in system mode. (+PAM +AUDIT +SELINUX +IMA +APPARMOR +SMACK +SYSVINIT +UTMP +LIBCRYPTSETUP +GCRYPT +GNUTLS +ACL +XZ +LZ4 +SECCOMP +BLKID +ELFUTIL
 systemd[1]: Detected architecture arm.
 Welcome to Raspbian GNU/Linux 10 (buster)!
 systemd[1]: Set hostname to <raspberrypi>.
 random: systemd: uninitialized urandom read (16 bytes read)
 systemd[1]: Initializing machine ID from random generator.
systemd[1]: File /lib/systemd/system/systemd-journald.service:12 configures an IP firewall (IPAddressDeny=any), but the local system does not support BPF/cgroup based firewallin
systemd[1]: Proceeding WITHOUT firewalling in effect! (This warning is only shown for the first loaded unit using IP firewalling.)
 random: systemd: uninitialized urandom read (16 bytes read)
 random: systemd: uninitialized urandom read (16 bytes read)
 systemd[1]: Created slice User and Session Slice.
 [  OK  ] Created slice User and Session Slice.
 random: systemd: uninitialized urandom read (16 bytes read)
 systemd[1]: Listening on Journal Socket.
 [  OK  ] Listening on Journal Socket.
 systemd[1]: Starting Load Kernel Modules...
          Starting Load Kernel Modules...
 systemd[1]: Listening on initctl Compatibility Named Pipe.
 [  OK  ] Listening on initctl Compatibility Named Pipe.
          Mounting RPC Pipe File System...
 [  OK  ] Started Forward Password R…uests to Wall Directory Watch.
          Mounting Kernel Debug File System...
 [  OK  ] Listening on Journal Socket (/dev/log).
 [  OK  ] Created slice system-serial\x2dgetty.slice.
 [  OK  ] Created slice system-getty.slice.
 [  OK  ] Listening on Syslog Socket.
          Mounting POSIX Message Queue File System...
 [  OK  ] Listening on udev Kernel Socket.
 [  OK  ] Created slice system-systemd\x2dfsck.slice.
 [  OK  ] Reached target Swap.
 [  OK  ] Started Dispatch Password …ts to Console Directory Watch.
 [  OK  ] Reached target Paths.
 systemd[1]: Condition check resulted in Journal Audit Socket being skipped.
systemd[1]: Condition check resulted in Arbitrary Executable File Formats File System Automount Point being skipped.
 systemd[1]: Condition check resulted in Set Up Additional Binary Formats being skipped.
 systemd[1]: Condition check resulted in Kernel Module supporting RPCSEC_GSS being skipped.
 systemd[1]: Listening on fsck to fsckd communication Socket.
 [  OK  ] Listening on fsck to fsckd communication Socket.
 systemd[1]: Starting Journal Service...
          Starting Journal Service...
 systemd[1]: Reached target Slices.
 [  OK  ] Reached target Slices.
 systemd[1]: Starting Set the console keyboard layout...
          Starting Set the console keyboard layout...
 systemd[1]: Reached target Local Encrypted Volumes.
 [  OK  ] Reached target Local Encrypted Volumes.
 systemd[1]: Listening on udev Control Socket.
 [  OK  ] Listening on udev Control Socket.
          Starting udev Coldplug all Devices...
          Starting Restore / save the current clock...
 [  OK  ] Started Load Kernel Modules.
 [  OK  ] Mounted RPC Pipe File System.
 [  OK  ] Mounted Kernel Debug File System.
 [  OK  ] Mounted POSIX Message Queue File System.
          Starting Apply Kernel Variables...
 [  OK  ] Started Restore / save the current clock.
          Starting Remount Root and Kernel File Systems...
 [  OK  ] Started Apply Kernel Variables.
 [  OK  ] Started Journal Service.
 [  OK  ] Started Remount Root and Kernel File Systems.
          Starting Flush Journal to Persistent Storage...
          Starting Create System Users...
          Starting Load/Save Random Seed...
 [  OK  ] Started Load/Save Random Seed.
 [  OK  ] Started Flush Journal to Persistent Storage.
 [  OK  ] Started Create System Users.
          Starting Create Static Device Nodes in /dev...
 [  OK  ] Started Create Static Device Nodes in /dev.
          Starting udev Kernel Device Manager...
 [  OK  ] Started udev Coldplug all Devices.
          Starting Helper to synchronize boot up for ifupdown...
 [  OK  ] Started Set the console keyboard layout.
 [  OK  ] Started Helper to synchronize boot up for ifupdown.
 [  OK  ] Reached target Local File Systems (Pre).
 [  OK  ] Started udev Kernel Device Manager.
 [*     ] (1 of 3) A start job is running for /dev/serial1 (20s / 1min 30s)
 M
 [  OK  ] Found device /dev/ttyAMA0.
 [  OK  ] Found device QEMU_HARDDISK boot.
          Starting File System Check…isk/by-partuuid/907af7d0-01...
 [  OK  ] Started File System Check Daemon to report status.
 [  OK  ] Started File System Check …/disk/by-partuuid/907af7d0-01.
          Mounting /boot...
 [  OK  ] Mounted /boot.
 [  OK  ] Reached target Local File Systems.
          Starting Raise network interfaces...
          Starting Create Volatile Files and Directories...
          Starting Preprocess NFS configuration...
          Starting Set console font and keymap...
 [  OK  ] Started Preprocess NFS configuration.
 [  OK  ] Reached target NFS client services.
 [  OK  ] Reached target Remote File Systems (Pre).
 [  OK  ] Reached target Remote File Systems.
 [  OK  ] Started Set console font and keymap.
 [  OK  ] Started Create Volatile Files and Directories.
          Starting Network Time Synchronization...
          Starting Update UTMP about System Boot/Shutdown...
 [  OK  ] Started Update UTMP about System Boot/Shutdown.
 [  OK  ] Started Network Time Synchronization.
 [  OK  ] Reached target System Time Synchronized.
 [  OK  ] Reached target System Initialization.
 [  OK  ] Listening on triggerhappy.socket.
 [  OK  ] Started Daily man-db regeneration.
 [  OK  ] Started Daily rotation of log files.
 [  OK  ] Started Daily apt download activities.
 [  OK  ] Listening on Avahi mDNS/DNS-SD Stack Activation Socket.
 [  OK  ] Started Daily Cleanup of Temporary Directories.
 [  OK  ] Listening on D-Bus System Message Bus Socket.
 [  OK  ] Reached target Sockets.
 [  OK  ] Reached target Basic System.
          Starting System Logging Service...
          Starting Regenerate SSH host keys...
          Starting Avahi mDNS/DNS-SD Stack...
          Starting rng-tools.service...
          Starting Check for Raspberry Pi EEPROM updates...
          Starting Login Service...
 [  OK  ] Started D-Bus System Message Bus.
          Starting LSB: Switch to on…nless shift key is pressed)...
          Starting LSB: Resize the r…ilesystem to fill partition...
 [  OK  ] Started Regular background program processing daemon.
          Starting WPA supplicant...
          Starting dhcpcd on all interfaces...
          Starting triggerhappy global hotkey daemon...
          Starting dphys-swapfile - …unt, and delete a swap file...
 [  OK  ] Started Daily apt upgrade and clean activities.
 [  OK  ] Reached target Timers.
 [  OK  ] Started System Logging Service.
 [  OK  ] Started triggerhappy global hotkey daemon.
 [  OK  ] Started Raise network interfaces.
 [FAILED] Failed to start rng-tools.service.
 See 'systemctl status rng-tools.service' for details.
 [  OK  ] Started Check for Raspberry Pi EEPROM updates.
 [  OK  ] Started Avahi mDNS/DNS-SD Stack.
 [  OK  ] Started WPA supplicant.
 [  OK  ] Started Login Service.
 [  OK  ] Started LSB: Switch to ond…(unless shift key is pressed).
 [  OK  ] Started dphys-swapfile - s…mount, and delete a swap file.
 [  OK  ] Started dhcpcd on all interfaces.
 [  OK  ] Started LSB: Resize the ro… filesystem to fill partition.
 [  OK  ] Reached target Network.
          Starting /etc/rc.local Compatibility...
          Starting Permit User Sessions...
 My IP address is 10.0.2.15
 [  OK  ] Started /etc/rc.local Compatibility.
 [  OK  ] Started Permit User Sessions.
 [  OK  ] Started Regenerate SSH host keys.
 [  OK  ] Started Serial Getty on ttyAMA0.
 [  OK  ] Started Getty on tty1.
 [  OK  ] Reached target Login Prompts.
 Raspbian GNU/Linux 10 raspberrypi ttyAMA0
 raspberrypi login:
 raspberrypi login: pi
 Password:
 raspberry
 Login incorrect
 raspberrypi login:
 raspberrypi login:
 raspberrypi login:
 raspberrypi login:
 raspberrypi login:
 raspberrypi login:
 raspberrypi login:
