libguestfs: create: flags = 0, handle = 0x1b19580, program = guestfish
libguestfs: launch: program=guestfish
libguestfs: launch: version=1.32.2
libguestfs: launch: backend registered: unix
libguestfs: launch: backend registered: uml
libguestfs: launch: backend registered: libvirt
libguestfs: launch: backend registered: direct
libguestfs: launch: backend=direct
libguestfs: launch: tmpdir=/tmp/libguestfsYmpbBu
libguestfs: launch: umask=0022
libguestfs: launch: euid=0
libguestfs: is_openable: /dev/kvm: No such file or directory
libguestfs: [00000ms] begin building supermin appliance
libguestfs: [00000ms] run supermin
libguestfs: command: run: /usr/bin/supermin
libguestfs: command: run: \ --build
libguestfs: command: run: \ --verbose
libguestfs: command: run: \ --if-newer
libguestfs: command: run: \ --lock /var/tmp/.guestfs-0/lock
libguestfs: command: run: \ --copy-kernel
libguestfs: command: run: \ -f ext2
libguestfs: command: run: \ --host-cpu x86_64
libguestfs: command: run: \ /usr/lib/x86_64-linux-gnu/guestfs/supermin.d
libguestfs: command: run: \ -o /var/tmp/.guestfs-0/appliance.d
supermin: version: 5.1.14
supermin: package handler: debian/dpkg
supermin: acquiring lock on /var/tmp/.guestfs-0/lock
supermin: build: /usr/lib/x86_64-linux-gnu/guestfs/supermin.d
supermin: reading the supermin appliance
supermin: build: visiting /usr/lib/x86_64-linux-gnu/guestfs/supermin.d/base.tar.gz type gzip base image (tar)
supermin: build: visiting /usr/lib/x86_64-linux-gnu/guestfs/supermin.d/daemon.tar.gz type gzip base image (tar)
supermin: build: visiting /usr/lib/x86_64-linux-gnu/guestfs/supermin.d/excludefiles type uncompressed excludefiles
supermin: build: visiting /usr/lib/x86_64-linux-gnu/guestfs/supermin.d/hostfiles type uncompressed hostfiles
supermin: build: visiting /usr/lib/x86_64-linux-gnu/guestfs/supermin.d/init.tar.gz type gzip base image (tar)
supermin: build: visiting /usr/lib/x86_64-linux-gnu/guestfs/supermin.d/packages type uncompressed packages
supermin: build: visiting /usr/lib/x86_64-linux-gnu/guestfs/supermin.d/packages-hfsplus type uncompressed packages
supermin: build: visiting /usr/lib/x86_64-linux-gnu/guestfs/supermin.d/packages-reiserfs type uncompressed packages
supermin: build: visiting /usr/lib/x86_64-linux-gnu/guestfs/supermin.d/packages-xfs type uncompressed packages
supermin: build: visiting /usr/lib/x86_64-linux-gnu/guestfs/supermin.d/udev-rules.tar.gz type gzip base image (tar)
supermin: mapping package names to installed packages
supermin: resolving full list of package dependencies
supermin: build: 196 packages, including dependencies
supermin: build: 6867 files
supermin: build: 3793 files, after matching excludefiles
supermin: build: 3795 files, after adding hostfiles
supermin: build: 3795 files, after removing unreadable files
supermin: build: 3799 files, after munging
supermin: kernel: picked kernel vmlinuz-4.4.0-119-generic
supermin: kernel: picked modules path /lib/modules/4.4.0-119-generic
supermin: kernel: kernel_version 4.4.0-119-generic
supermin: kernel: modules /lib/modules/4.4.0-119-generic
supermin: ext2: creating empty ext2 filesystem '/var/tmp/.guestfs-0/appliance.d.uonf4iae/root'
supermin: ext2: populating from base image
supermin: ext2: copying files from host filesystem
supermin: ext2: copying kernel modules
supermin: ext2: creating minimal initrd '/var/tmp/.guestfs-0/appliance.d.uonf4iae/initrd'
supermin: ext2: wrote 30 modules to minimal initrd
supermin: renaming /var/tmp/.guestfs-0/appliance.d.uonf4iae to /var/tmp/.guestfs-0/appliance.d
libguestfs: [02850ms] finished building supermin appliance
libguestfs: [02850ms] begin testing qemu features
libguestfs: command: run: /usr/bin/qemu-system-x86_64
libguestfs: command: run: \ -display none
libguestfs: command: run: \ -help
libguestfs: command: run: /usr/bin/qemu-system-x86_64
libguestfs: command: run: \ -display none
libguestfs: command: run: \ -version
libguestfs: qemu version 2.5
libguestfs: command: run: /usr/bin/qemu-system-x86_64
libguestfs: command: run: \ -display none
libguestfs: command: run: \ -machine accel=kvm:tcg
libguestfs: command: run: \ -device ?
libguestfs: [03139ms] finished testing qemu features
libguestfs: command: run: dmesg | grep -Eoh 'lpj=[[:digit:]]+'
dmesg: read kernel buffer failed: Operation not permitted
libguestfs: read_lpj_from_dmesg: external command exited with error status 1
libguestfs: command: run: grep
libguestfs: command: run: \ -Eoh lpj=[[:digit:]]+
libguestfs: command: run: \ /var/log/dmesg
libguestfs: read_lpj_from_files: external command exited with error status 1
[03285ms] /usr/bin/qemu-system-x86_64 \
global virtio-blk-pci.scsi=off \
nodefconfig \
enable-fips \
nodefaults \
display none \
machine accel=kvm:tcg \
m 500 \
no-reboot \
rtc driftfix=slew \
no-hpet \
global kvm-pit.lost_tick_policy=discard \
kernel /var/tmp/.guestfs-0/appliance.d/kernel \
initrd /var/tmp/.guestfs-0/appliance.d/initrd \
device virtio-scsi-pci,id=scsi \
drive file=/work/NpekTjxkXJESfSko1U0iOsQl-tmp.img,cache=writeback,format=raw,id=hd0,if=none \
device scsi-hd,drive=hd0 \
drive file=/var/tmp/.guestfs-0/appliance.d/root,snapshot=on,id=appliance,cache=unsafe,if=none \
device scsi-hd,drive=appliance \
device virtio-serial-pci \
serial stdio \
device sga \
chardev socket,path=/tmp/libguestfsYmpbBu/guestfsd.sock,id=channel0 \
device virtserialport,chardev=channel0,name=org.libguestfs.channel.0 \
append 'panic=1 console=ttyS0 udevtimeout=6000 udev.event-timeout=6000 no_timer_check acpi=off printk.time=1 cgroup_disable=memory root=/dev/sdb
selinux=0 guestfs_verbose=1 TERM=linux'
Could not access KVM kernel module: No such file or directory
failed to initialize KVM: No such file or directory
Back to tcg accelerator.
warning: TCG doesn't support requested feature: CPUID.01H:ECX.vmx [bit 5]
Could not open option rom 'sgabios.bin': No such file or directory
[    0.000000] Initializing cgroup subsys cpuset
[    0.000000] Initializing cgroup subsys cpu
[    0.000000] Initializing cgroup subsys cpuacct
[    0.000000] Linux version 4.4.0-119-generic (buildd@lcy01-amd64-013) (gcc version 5.4.0 20160609 (Ubuntu 5.4.0-6ubuntu1~16.04.9) ) #143-Ubuntu SMP
Mon Apr 2 16:08:24 UTC 2018 (Ubuntu 4.4.0-119.143-generic 4.4.114)
[    0.000000] Command line: panic=1 console=ttyS0 udevtimeout=6000 udev.event-timeout=6000 no_timer_check acpi=off printk.time=1 cgroup_disable=memor
y root=/dev/sdb selinux=0 guestfs_verbose=1 TERM=linux
[    0.000000] KERNEL supported cpus:
[    0.000000]   Intel GenuineIntel
[    0.000000]   AMD AuthenticAMD
[    0.000000]   Centaur CentaurHauls
[    0.000000] x86/fpu: Legacy x87 FPU detected.
[    0.000000] x86/fpu: Using 'lazy' FPU context switches.
[    0.000000] e820: BIOS-provided physical RAM map:
[    0.000000] BIOS-e820: [mem 0x0000000000000000-0x000000000009fbff] usable
[    0.000000] BIOS-e820: [mem 0x000000000009fc00-0x000000000009ffff] reserved
[    0.000000] BIOS-e820: [mem 0x00000000000f0000-0x00000000000fffff] reserved
[    0.000000] BIOS-e820: [mem 0x0000000000100000-0x000000001f3dffff] usable
[    0.000000] BIOS-e820: [mem 0x000000001f3e0000-0x000000001f3fffff] reserved
[    0.000000] BIOS-e820: [mem 0x00000000fffc0000-0x00000000ffffffff] reserved
[    0.000000] NX (Execute Disable) protection: active
[    0.000000] SMBIOS 2.8 present.
[    0.000000] Kernel/User page tables isolation: disabled
[    0.000000] e820: last_pfn = 0x1f3e0 max_arch_pfn = 0x400000000
[    0.000000] x86/PAT: Configuration [0-7]: WB  WC  UC- UC  WB  WC  UC- WT
[    0.000000] found SMP MP-table at [mem 0x000f6630-0x000f663f] mapped at [ffff8800000f6630]
[    0.000000] Scanning 1 areas for low memory corruption
[    0.000000] RAMDISK: [mem 0x1f10f000-0x1f3dffff]
[    0.000000] No NUMA configuration found
[    0.000000] Faking a node at [mem 0x0000000000000000-0x000000001f3dffff]
[    0.000000] NODE_DATA(0) allocated [mem 0x1f10a000-0x1f10efff]
[    0.000000] Zone ranges:
[    0.000000]   DMA [mem 0x0000000000001000-0x0000000000ffffff]
[    0.000000]   DMA32    [mem 0x0000000001000000-0x000000001f3dffff]
[    0.000000]   Normal   empty
[    0.000000]   Device   empty
[    0.000000] Movable zone start for each node
[    0.000000] Early memory node ranges
[    0.000000]   node   0: [mem 0x0000000000001000-0x000000000009efff]
[    0.000000]   node   0: [mem 0x0000000000100000-0x000000001f3dffff]
[    0.000000] Initmem setup node 0 [mem 0x0000000000001000-0x000000001f3dffff]
[    0.000000] SFI: Simple Firmware Interface v0.81 http://simplefirmware.org
[    0.000000] Intel MultiProcessor Specification v1.4
[    0.000000] MPTABLE: OEM ID: BOCHSCPU
[    0.000000] MPTABLE: Product ID: 0.1
[    0.000000] MPTABLE: APIC at: 0xFEE00000
[    0.000000] Processor #0 (Bootup-CPU)
[    0.000000] IOAPIC[0]: apic_id 0, version 17, address 0xfec00000, GSI 0-23
[    0.000000] Processors: 1
[    0.000000] smpboot: Allowing 1 CPUs, 0 hotplug CPUs
[    0.000000] PM: Registered nosave memory: [mem 0x00000000-0x00000fff]
[    0.000000] PM: Registered nosave memory: [mem 0x0009f000-0x0009ffff]
[    0.000000] PM: Registered nosave memory: [mem 0x000a0000-0x000effff]
[    0.000000] PM: Registered nosave memory: [mem 0x000f0000-0x000fffff]
[    0.000000] e820: [mem 0x1f400000-0xfffbffff] available for PCI devices
[    0.000000] Booting paravirtualized kernel on bare hardware
[    0.000000] clocksource: refined-jiffies: mask: 0xffffffff max_cycles: 0xffffffff, max_idle_ns: 7645519600211568 ns
[    0.000000] setup_percpu: NR_CPUS:512 nr_cpumask_bits:512 nr_cpu_ids:1 nr_node_ids:1
[    0.000000] PERCPU: Embedded 34 pages/cpu @ffff88001ee00000 s99544 r8192 d31528 u2097152
[    0.000000] Built 1 zonelists in Node order, mobility grouping on.  Total pages: 125848
[    0.000000] Policy zone: DMA32
[    0.000000] Kernel command line: panic=1 console=ttyS0 udevtimeout=6000 udev.event-timeout=6000 no_timer_check acpi=off printk.time=1 cgroup_disabl
e=memory root=/dev/sdb selinux=0 guestfs_verbose=1 TERM=linux
[    0.000000] PID hash table entries: 2048 (order: 2, 16384 bytes)
[    0.000000] Memory: 481380K/511480K available (8532K kernel code, 1313K rwdata, 3996K rodata, 1508K init, 1316K bss, 30100K reserved, 0K cma-reserv
ed)
[    0.000000] SLUB: HWalign=64, Order=0-3, MinObjects=0, CPUs=1, Nodes=1
[    0.000000] Hierarchical RCU implementation.
[    0.000000] \tBuild-time adjustment of leaf fanout to 64.
[    0.000000] \tRCU restricting CPUs from NR_CPUS=512 to nr_cpu_ids=1.
[    0.000000] RCU: Adjusting geometry for rcu_fanout_leaf=64, nr_cpu_ids=1
[    0.000000] NR_IRQS:33024 nr_irqs:256 16
[    0.000000] Console: colour *CGA 80x25
[    0.000000] console [ttyS0] enabled
[    0.000000] tsc: Unable to calibrate against PIT
[    0.000000] tsc: No reference (HPET/PMTIMER) available
[    0.000000] tsc: Marking TSC unstable due to could not calculate TSC khz
[    0.024000] Calibrating delay loop... 134.65 BogoMIPS (lpj=269312)
[    0.080000] pid_max: default: 32768 minimum: 301
[    0.084000] Security Framework initialized
[    0.084000] Yama: becoming mindful.
[    0.088000] AppArmor: AppArmor initialized
[    0.096000] Dentry cache hash table entries: 65536 (order: 7, 524288 bytes)
[    0.096000] Inode-cache hash table entries: 32768 (order: 6, 262144 bytes)
[    0.100000] Mount-cache hash table entries: 1024 (order: 1, 8192 bytes)
[    0.100000] Mountpoint-cache hash table entries: 1024 (order: 1, 8192 bytes)
[    0.116000] Initializing cgroup subsys io
[    0.120000] Initializing cgroup subsys memory
[    0.128000] Disabling memory control group subsystem
[    0.128000] Initializing cgroup subsys devices
[    0.128000] Initializing cgroup subsys freezer
[    0.128000] Initializing cgroup subsys net_cls
[    0.128000] Initializing cgroup subsys perf_event
[    0.128000] Initializing cgroup subsys net_prio
[    0.132000] Initializing cgroup subsys hugetlb
[    0.132000] Initializing cgroup subsys pids
[    0.136000] FEATURE SPEC_CTRL Not Present
[    0.136000] FEATURE IBPB Not Present
[    0.136000] mce: CPU supports 10 MCE banks
[    0.140000] Last level iTLB entries: 4KB 0, 2MB 0, 4MB 0
[    0.140000] Last level dTLB entries: 4KB 0, 2MB 0, 4MB 0, 1GB 0
[    0.140000] Spectre V2 mitigation: LFENCE not serializing. Switching to generic retpoline
[    0.144000] Spectre V2 mitigation: Mitigation: Full generic retpoline
[    0.144000] Spectre V2 mitigation: Speculation control IBPB not-supported IBRS not-supported
[    0.144000] Spectre V2 mitigation: Filling RSB on context switch
[    0.568000] Freeing SMP alternatives memory: 32K
[    0.580000] ftrace: allocating 32195 entries in 126 pages
[    0.780000] smpboot: APIC(0) Converting physical 0 to logical package 0
[    0.780000] smpboot: Max logical packages: 1
[    0.784000] ..TIMER: vector=0x30 apic1=0 pin1=2 apic2=-1 pin2=-1
[    0.908000] APIC timer disabled due to verification failure
[    0.908000] smpboot: CPU0: AMD QEMU Virtual CPU version 2.5+ (family: 0x6, model: 0x6, stepping: 0x3)
[    0.912000] Performance Events: Broken PMU hardware detected, using software events only.
[    0.912000] Failed to access perfctr msr (MSR c0010007 is 0)
[    0.928000] x86: Booted up 1 node, 1 CPUs
[    0.928000] smpboot: Total of 1 processors activated (134.65 BogoMIPS)
[    0.932000] NMI watchdog: disabled (cpu0): hardware events not enabled
[    0.932000] NMI watchdog: Shutting down hard lockup detector on all cpus
[    0.952000] devtmpfs: initialized
[    0.984000] evm: security.selinux
[    0.984000] evm: security.SMACK64
[    0.984000] evm: security.SMACK64EXEC
[    0.984000] evm: security.SMACK64TRANSMUTE
[    0.984000] evm: security.SMACK64MMAP
[    0.984000] evm: security.ima
[    0.984000] evm: security.capability
[    0.988000] clocksource: jiffies: mask: 0xffffffff max_cycles: 0xffffffff, max_idle_ns: 7645041785100000 ns
[    0.992000] futex hash table entries: 256 (order: 2, 16384 bytes)
[    0.996000] pinctrl core: initialized pinctrl subsystem
[    1.008000] RTC time:  0:03:31, date: 11/13/20
[    1.016000] NET: Registered protocol family 16
[    1.024000] cpuidle: using governor ladder
[    1.024000] cpuidle: using governor menu
[    1.028000] PCI: Using configuration type 1 for base access
[    1.068000] ACPI: Interpreter disabled.
[    1.076000] vgaarb: loaded
[    1.084000] SCSI subsystem initialized
[    1.084000] usbcore: registered new interface driver usbfs
[    1.088000] usbcore: registered new interface driver hub
[    1.088000] usbcore: registered new device driver usb
[    1.092000] PCI: Probing PCI hardware
[    1.096000] PCI host bridge to bus 0000:00
[    1.096000] pci_bus 0000:00: root bus resource [io  0x0000-0xffff]
[    1.096000] pci_bus 0000:00: root bus resource [mem 0x00000000-0xffffffffff]
[    1.096000] pci_bus 0000:00: No busn resource found for root bus, will use [bus 00-ff]
[    1.116000] pci 0000:00:01.1: legacy IDE quirk: reg 0x10: [io  0x01f0-0x01f7]
[    1.116000] pci 0000:00:01.1: legacy IDE quirk: reg 0x14: [io  0x03f6]
[    1.116000] pci 0000:00:01.1: legacy IDE quirk: reg 0x18: [io  0x0170-0x0177]
[    1.116000] pci 0000:00:01.1: legacy IDE quirk: reg 0x1c: [io  0x0376]
[    1.120000] pci 0000:00:01.3: quirk: [io  0x0600-0x063f] claimed by PIIX4 ACPI
[    1.120000] pci 0000:00:01.3: quirk: [io  0x0700-0x070f] claimed by PIIX4 SMB
[    1.152000] pci 0000:00:01.0: PIIX/ICH IRQ router [8086:7000]
[    1.168000] NetLabel: Initializing
[    1.168000] NetLabel:  domain hash size = 128
[    1.168000] NetLabel:  protocols = UNLABELED CIPSOv4
[    1.172000] NetLabel:  unlabeled traffic allowed by default
[    1.176000] amd_nb: Cannot enumerate AMD northbridges
[    1.176000] clocksource: Switched to clocksource refined-jiffies
[    1.328009] AppArmor: AppArmor Filesystem Enabled
[    1.332009] pnp: PnP ACPI: disabled
[    1.372012] NET: Registered protocol family 2
[    1.380012] TCP established hash table entries: 4096 (order: 3, 32768 bytes)
[    1.380012] TCP bind hash table entries: 4096 (order: 4, 65536 bytes)
[    1.380012] TCP: Hash tables configured (established 4096 bind 4096)
[    1.384013] UDP hash table entries: 256 (order: 1, 8192 bytes)
[    1.384013] UDP-Lite hash table entries: 256 (order: 1, 8192 bytes)
[    1.388013] NET: Registered protocol family 1
[    1.388013] pci 0000:00:00.0: Limiting direct PCI/PCI transfers
[    1.388013] pci 0000:00:01.0: PIIX3: Enabling Passive Release
[    1.388013] pci 0000:00:01.0: Activating ISA DMA hang workarounds
[    1.400014] Unpacking initramfs...
[    1.444016] Freeing initrd memory: 2884K
[    1.448017] platform rtc_cmos: registered platform RTC device (no PNP device found)
[    1.452017] Scanning for low memory corruption every 60 seconds
[    1.460017] audit: initializing netlink subsys (disabled)
[    1.460017] audit: type=2000 audit(1605225811.460:1): initialized
[    1.468018] Initialise system trusted keyring
[    1.472018] HugeTLB registered 2 MB page size, pre-allocated 0 pages
[    1.512021] zbud: loaded
[    1.520021] VFS: Disk quotas dquot_6.6.0
[    1.520021] VFS: Dquot-cache hash table entries: 512 (order 0, 4096 bytes)
[    1.528022] squashfs: version 4.0 (2009/01/31) Phillip Lougher
[    1.536022] fuse init (API version 7.23)
[    1.540022] Key type big_key registered
[    1.544023] Allocating IMA MOK and blacklist keyrings.
[    1.560024] Key type asymmetric registered
[    1.560024] Asymmetric key parser 'x509' registered
[    1.564024] Block layer SCSI generic (bsg) driver version 0.4 loaded (major 249)
[    1.568024] io scheduler noop registered
[    1.568024] io scheduler deadline registered (default)
[    1.568024] io scheduler cfq registered
[    1.576025] pci_hotplug: PCI Hot Plug PCI Core version: 0.5
[    1.576025] pciehp: PCI Express Hot Plug Controller Driver version: 0.4
[    1.580025] virtio-pci 0000:00:02.0: PCI->APIC IRQ transform: INT A -> IRQ 10
[    1.584025] virtio-pci 0000:00:02.0: virtio_pci: leaving for legacy driver
[    1.584025] virtio-pci 0000:00:03.0: PCI->APIC IRQ transform: INT A -> IRQ 11
[    1.584025] virtio-pci 0000:00:03.0: virtio_pci: leaving for legacy driver
[    1.592026] Serial: 8250/16550 driver, 32 ports, IRQ sharing enabled
[    1.596026] serial8250: ttyS0 at I/O 0x3f8 (irq = 4, base_baud = 115200) is a 16550A
[    1.644029] Linux agpgart interface v0.103
[    1.680031] loop: module loaded
[    1.696032] scsi host0: ata_piix
[    1.696032] scsi host1: ata_piix
[    1.700032] ata1: PATA max MWDMA2 cmd 0x1f0 ctl 0x3f6 bmdma 0xc060 irq 14
[    1.700032] ata2: PATA max MWDMA2 cmd 0x170 ctl 0x376 bmdma 0xc068 irq 15
[    1.704033] libphy: Fixed MDIO Bus: probed
[    1.704033] tun: Universal TUN/TAP device driver, 1.6
[    1.708033] tun: (C) 1999-2004 Max Krasnyansky <maxk@qualcomm.com>
[    1.708033] PPP generic driver version 2.4.2
[    1.708033] ehci_hcd: USB 2.0 'Enhanced' Host Controller (EHCI) Driver
[    1.708033] ehci-pci: EHCI PCI platform driver
[    1.712033] ehci-platform: EHCI generic platform driver
[    1.712033] ohci_hcd: USB 1.1 'Open' Host Controller (OHCI) Driver
[    1.712033] ohci-pci: OHCI PCI platform driver
[    1.712033] ohci-platform: OHCI generic platform driver
[    1.712033] uhci_hcd: USB Universal Host Controller Interface driver
[    1.716033] i8042: PNP: No PS/2 controller found. Probing ports directly.
[    1.720034] serio: i8042 KBD port at 0x60,0x64 irq 1
[    1.720034] serio: i8042 AUX port at 0x60,0x64 irq 12
[    1.724034] mousedev: PS/2 mouse device common for all mice
[    1.740035] rtc_cmos rtc_cmos: rtc core: registered rtc_cmos as rtc0
[    1.744035] input: AT Translated Set 2 keyboard as /devices/platform/i8042/serio0/input/input0
[    1.748035] rtc_cmos rtc_cmos: alarms up to one day, 114 bytes nvram
[    1.748035] i2c /dev entries driver
[    1.748035] device-mapper: uevent: version 1.0.3
[    1.752036] device-mapper: ioctl: 4.34.0-ioctl (2015-10-28) initialised: dm-devel@redhat.com
[    1.752036] ledtrig-cpu: registered to indicate activity on CPUs
[    1.760036] NET: Registered protocol family 10
[    1.772037] NET: Registered protocol family 17
[    1.772037] Key type dns_resolver registered
[    1.784038] registered taskstats version 1
[    1.784038] Loading compiled-in X.509 certificates
[    1.792038] Loaded X.509 cert 'Build time autogenerated kernel key: f854e06633b5f496ced5b2e02c7b14f761b69302'
[    1.796038] zswap: loaded using pool lzo/zbud
[    1.808039] Key type trusted registered
[    1.816040] Key type encrypted registered
[    1.816040] AppArmor: AppArmor sha1 policy hashing enabled
[    1.816040] ima: No TPM chip found, activating TPM-bypass!
[    1.820040] evm: HMAC attrs: 0x1
[    1.828040]   Magic number: 12:516:51
[    1.828040] rtc_cmos rtc_cmos: setting system clock to 2020-11-13 00:03:32 UTC (1605225812)
[    1.832041] BIOS EDD facility v0.16 2004-Jun-25, 0 devices found
[    1.832041] EDD information not available.
[    1.908045] Freeing unused kernel memory: 1508K
[    1.908045] Write protecting the kernel read-only data: 14336k
[    1.916046] Freeing unused kernel memory: 1696K
[    1.920046] Freeing unused kernel memory: 100K
supermin: mounting /proc
supermin: uptime: 2.01 0.76
supermin: ext2 mini initrd starting up: 5.1.14 zlib xz
supermin: cmdline: panic=1 console=ttyS0 udevtimeout=6000 udev.event-timeout=6000 no_timer_check acpi=off printk.time=1 cgroup_disable=memory root=/de
v/sdb selinux=0 guestfs_verbose=1 TERM=linux
supermin: mounting /sys
supermin: internal insmod crc32-pclmul.ko
[    2.060055] PCLMULQDQ-NI instructions are not detected.
insmod: init_module: crc32-pclmul.ko: No such device
supermin: internal insmod crct10dif-pclmul.ko
insmod: init_module: crct10dif-pclmul.ko: No such device
supermin: internal insmod crc32.ko
supermin: internal insmod virtio-rng.ko
supermin: internal insmod drm.ko
[    2.164061] [drm] Initialized drm 1.1.0 20060810
supermin: internal insmod fb_sys_fops.ko
supermin: internal insmod syscopyarea.ko
supermin: internal insmod sysfillrect.ko
supermin: internal insmod sysimgblt.ko
supermin: internal insmod drm_kms_helper.ko
supermin: internal insmod ttm.ko
supermin: internal insmod virtio-gpu.ko
supermin: internal insmod ideapad_slidebar.ko
[    2.296070] ideapad_slidebar: DMI does not match
insmod: init_module: ideapad_slidebar.ko: No such device
supermin: internal insmod video.ko
supermin: internal insmod sparse-keymap.ko
supermin: internal insmod wmi.ko
insmod: init_module: wmi.ko: No such device
supermin: internal insmod ideapad-laptop.ko
[    2.348073] ideapad_laptop: Unknown symbol wmi_remove_notify_handler (err 0)
[    2.348073] ideapad_laptop: Unknown symbol wmi_install_notify_handler (err 0)
insmod: init_module: ideapad-laptop.ko: Unknown symbol in module
supermin: internal insmod megaraid.ko
supermin: internal insmod megaraid_mm.ko
[    2.372074] megaraid cmm: 2.20.2.7 (Release Date: Sun Jul 16 00:01:03 EST 2006)
supermin: internal insmod megaraid_mbox.ko
[    2.388075] megaraid: 2.20.5.1 (Release Date: Thu Nov 16 15:32:35 EST 2006)
supermin: internal insmod megaraid_sas.ko
[    2.412077] megasas: 06.810.09.00-rc1
supermin: internal insmod scsi_transport_spi.ko
supermin: internal insmod sym53c8xx.ko
supermin: internal insmod virtio_scsi.ko
[    2.460080] scsi host2: Virtio SCSI HBA
[    2.468080] scsi 2:0:0:0: Direct-Access    QEMU QEMU HARDDISK    2.5+ PQ: 0 ANSI: 5
[    2.476081] scsi 2:0:1:0: Direct-Access    QEMU QEMU HARDDISK    2.5+ PQ: 0 ANSI: 5
[    2.752098] sd 2:0:0:0: [sda] 26624 512-byte logical blocks: (13.6 MB/13.0 MiB)
[    2.756098] sd 2:0:0:0: [sda] Write Protect is off
[    2.760099] sd 2:0:0:0: [sda] Write cache: enabled, read cache: enabled, doesn't support DPO or FUA
[    2.760099] sd 2:0:0:0: Attached scsi generic sg0 type 0
[    2.772099] sd 2:0:1:0: [sdb] 8388608 512-byte logical blocks: (4.29 GB/4.00 GiB)
[    2.772099] sd 2:0:1:0: [sdb] Write Protect is off
[    2.776100] sd 2:0:1:0: Attached scsi generic sg1 type 0
[    2.780100] sd 2:0:1:0: [sdb] Write cache: enabled, read cache: enabled, doesn't support DPO or FUA
[    2.816102] sd 2:0:0:0: [sda] Attached SCSI disk
[    2.816102] sd 2:0:1:0: [sdb] Attached SCSI disk
supermin: internal insmod virtio_input.ko
supermin: internal insmod crc-ccitt.ko
supermin: internal insmod crc-itu-t.ko
supermin: internal insmod crc7.ko
supermin: internal insmod crc8.ko
supermin: internal insmod libcrc32c.ko
supermin: picked /sys/block/sdb/dev as root device
supermin: creating /dev/root as block special 8:16
supermin: mounting new root on /root
[    2.904108] EXT4-fs (sdb): mounting ext2 file system using the ext4 subsystem
[    2.952111] EXT4-fs (sdb): mounted filesystem without journal. Opts:
supermin: chroot
Starting /init script ...
[    4.236191] random: systemd-tmpfile: uninitialized urandom read (16 bytes read, 100 bits of entropy available)
[    4.248192] random: systemd-tmpfile: uninitialized urandom read (16 bytes read, 103 bits of entropy available)
[    4.248192] random: systemd-tmpfile: uninitialized urandom read (16 bytes read, 103 bits of entropy available)
[/usr/lib/tmpfiles.d/journal-nocow.conf:26] Failed to replace specifiers: /var/log/journal/%m
[    4.260192] random: systemd-tmpfile: uninitialized urandom read (16 bytes read, 104 bits of entropy available)
[    4.260192] random: systemd-tmpfile: uninitialized urandom read (16 bytes read, 104 bits of entropy available)
[/usr/lib/tmpfiles.d/systemd.conf:26] Failed to replace specifiers: /run/log/journal/%m
[/usr/lib/tmpfiles.d/systemd.conf:28] Failed to replace specifiers: /run/log/journal/%m
[/usr/lib/tmpfiles.d/systemd.conf:29] Failed to replace specifiers: /run/log/journal/%m
[/usr/lib/tmpfiles.d/systemd.conf:32] Failed to replace specifiers: /var/log/journal/%m
[/usr/lib/tmpfiles.d/systemd.conf:33] Failed to replace specifiers: /var/log/journal/%m/system.journal
Failed to parse ACL "d:group:adm:r-x": No such file or directory. Ignoring
Failed to parse ACL "group:adm:r-x": No such file or directory. Ignoring
[/usr/lib/tmpfiles.d/systemd.conf:37] Failed to replace specifiers: /var/log/journal/%m
[/usr/lib/tmpfiles.d/systemd.conf:38] Failed to replace specifiers: /var/log/journal/%m
[/usr/lib/tmpfiles.d/systemd.conf:39] Failed to replace specifiers: /var/log/journal/%m/system.journal
starting version 229
[    4.432203] random: systemd-udevd: uninitialized urandom read (16 bytes read, 114 bits of entropy available)
[    4.468205] random: systemd-udevd: uninitialized urandom read (16 bytes read, 116 bits of entropy available)
[    4.468205] random: systemd-udevd: uninitialized urandom read (16 bytes read, 116 bits of entropy available)
[    4.476206] random: systemd-udevd: uninitialized urandom read (16 bytes read, 116 bits of entropy available)
[    4.476206] random: systemd-udevd: uninitialized urandom read (16 bytes read, 116 bits of entropy available)
[    4.628215] random: nonblocking pool is initialized
[    5.352261] piix4_smbus 0000:00:01.3: SMBus Host Controller at 0x700, revision 0
[    6.932359] input: ImExPS/2 Generic Explorer Mouse as /devices/platform/i8042/serio1/input/input2
[    7.012364] kvm: Nested Virtualization enabled
/init: 86: /init: cannot create /sys/block/{h,s,ub,v}d*/queue/scheduler: Directory nonexistent
mdadm: No arrays found in config file or automatically
  lvmetad is not active yet, using direct activation during sysinit
/init: 129: /init: ldmtool: not found
Linux (none) 4.4.0-119-generic #143-Ubuntu SMP Mon Apr 2 16:08:24 UTC 2018 x86_64 x86_64 x86_64 GNU/Linux
/dev:
total 0
crw------- 1 0 0  10, 235 Nov 13 00:03 autofs
drwxr-xr-x 2 0 0 240 Nov 13 00:03 block
drwxr-xr-x 2 0 0  80 Nov 13 00:03 bsg
crw------- 1 0 0  10, 234 Nov 13 00:03 btrfs-control
drwxr-xr-x 2 0 0    2900 Nov 13 00:03 char
crw------- 1 0 0   5,   1 Nov 13 00:03 console
lrwxrwxrwx 1 0 0  11 Nov 13 00:03 core -> /proc/kcore
crw------- 1 0 0  10,  59 Nov 13 00:03 cpu_dma_latency
crw------- 1 0 0  10, 203 Nov 13 00:03 cuse
drwxr-xr-x 5 0 0 100 Nov 13 00:03 disk
crw------- 1 0 0  10,  61 Nov 13 00:03 ecryptfs
lrwxrwxrwx 1 0 0  13 Nov 13 00:03 fd -> /proc/self/fd
crw-rw-rw- 1 0 0   1,   7 Nov 13 00:03 full
crw-rw-rw- 1 0 0  10, 229 Nov 13 00:03 fuse
crw------- 1 0 0  10, 183 Nov 13 00:03 hwrng
crw------- 1 0 0  89,   0 Nov 13 00:03 i2c-0
drwxr-xr-x 3 0 0 140 Nov 13 00:03 input
crw-r--r-- 1 0 0   1,  11 Nov 13 00:03 kmsg
crw------- 1 0 0  10, 232 Nov 13 00:03 kvm
drwxr-xr-x 2 0 0  60 Nov 13 00:03 lightnvm
crw------- 1 0 0  10, 237 Nov 13 00:03 loop-control
brw------- 1 0 0   7,   0 Nov 13 00:03 loop0
brw------- 1 0 0   7,   1 Nov 13 00:03 loop1
brw------- 1 0 0   7,   2 Nov 13 00:03 loop2
brw------- 1 0 0   7,   3 Nov 13 00:03 loop3
brw------- 1 0 0   7,   4 Nov 13 00:03 loop4
brw------- 1 0 0   7,   5 Nov 13 00:03 loop5
brw------- 1 0 0   7,   6 Nov 13 00:03 loop6
brw------- 1 0 0   7,   7 Nov 13 00:03 loop7
drwxr-xr-x 2 0 0  60 Nov 13 00:03 mapper
crw------- 1 0 0  10, 227 Nov 13 00:03 mcelog
crw------- 1 0 0  10,  55 Nov 13 00:03 megadev0
crw------- 1 0 0   1,   1 Nov 13 00:03 mem
crw------- 1 0 0  10,  56 Nov 13 00:03 memory_bandwidth
drwxr-xr-x 2 0 0  60 Nov 13 00:03 net
crw------- 1 0 0  10,  58 Nov 13 00:03 network_latency
crw------- 1 0 0  10,  57 Nov 13 00:03 network_throughput
crw-rw-rw- 1 0 0   1,   3 Nov 13 00:03 null
crw------- 1 0 0   1,   4 Nov 13 00:03 port
crw------- 1 0 0 108,   0 Nov 13 00:03 ppp
crw------- 1 0 0  10,   1 Nov 13 00:03 psaux
crw-rw-rw- 1 0 0   5,   2 Nov 13 00:03 ptmx
crw-rw-rw- 1 0 0   1,   8 Nov 13 00:03 random
crw------- 1 0 0  10,  62 Nov 13 00:03 rfkill
lrwxrwxrwx 1 0 0   4 Nov 13 00:03 rtc -> rtc0
crw------- 1 0 0 251,   0 Nov 13 00:03 rtc0
brw------- 1 0 0   8,   0 Nov 13 00:03 sda
brw------- 1 0 0   8,  16 Nov 13 00:03 sdb
crw------- 1 0 0  21,   0 Nov 13 00:03 sg0
crw------- 1 0 0  21,   1 Nov 13 00:03 sg1
crw------- 1 0 0  10, 231 Nov 13 00:03 snapshot
drwxr-xr-x 2 0 0  80 Nov 13 00:03 snd
lrwxrwxrwx 1 0 0  15 Nov 13 00:03 stderr -> /proc/self/fd/2
lrwxrwxrwx 1 0 0  15 Nov 13 00:03 stdin -> /proc/self/fd/0
lrwxrwxrwx 1 0 0  15 Nov 13 00:03 stdout -> /proc/self/fd/1
crw-rw-rw- 1 0 0   5,   0 Nov 13 00:03 tty
crw------- 1 0 0   4,   0 Nov 13 00:03 tty0
crw------- 1 0 0   4,   1 Nov 13 00:03 tty1
crw------- 1 0 0   4,  10 Nov 13 00:03 tty10
crw------- 1 0 0   4,  11 Nov 13 00:03 tty11
crw------- 1 0 0   4,  12 Nov 13 00:03 tty12
crw------- 1 0 0   4,  13 Nov 13 00:03 tty13
crw------- 1 0 0   4,  14 Nov 13 00:03 tty14
crw------- 1 0 0   4,  15 Nov 13 00:03 tty15
crw------- 1 0 0   4,  16 Nov 13 00:03 tty16
crw------- 1 0 0   4,  17 Nov 13 00:03 tty17
crw------- 1 0 0   4,  18 Nov 13 00:03 tty18
crw------- 1 0 0   4,  19 Nov 13 00:03 tty19
crw------- 1 0 0   4,   2 Nov 13 00:03 tty2
crw------- 1 0 0   4,  20 Nov 13 00:03 tty20
crw------- 1 0 0   4,  21 Nov 13 00:03 tty21
crw------- 1 0 0   4,  22 Nov 13 00:03 tty22
crw------- 1 0 0   4,  23 Nov 13 00:03 tty23
crw------- 1 0 0   4,  24 Nov 13 00:03 tty24
crw------- 1 0 0   4,  25 Nov 13 00:03 tty25
crw------- 1 0 0   4,  26 Nov 13 00:03 tty26
crw------- 1 0 0   4,  27 Nov 13 00:03 tty27
crw------- 1 0 0   4,  28 Nov 13 00:03 tty28
crw------- 1 0 0   4,  29 Nov 13 00:03 tty29
crw------- 1 0 0   4,   3 Nov 13 00:03 tty3
crw------- 1 0 0   4,  30 Nov 13 00:03 tty30
crw------- 1 0 0   4,  31 Nov 13 00:03 tty31
crw------- 1 0 0   4,  32 Nov 13 00:03 tty32
crw------- 1 0 0   4,  33 Nov 13 00:03 tty33
crw------- 1 0 0   4,  34 Nov 13 00:03 tty34
crw------- 1 0 0   4,  35 Nov 13 00:03 tty35
crw------- 1 0 0   4,  36 Nov 13 00:03 tty36
crw------- 1 0 0   4,  37 Nov 13 00:03 tty37
crw------- 1 0 0   4,  38 Nov 13 00:03 tty38
crw------- 1 0 0   4,  39 Nov 13 00:03 tty39
crw------- 1 0 0   4,   4 Nov 13 00:03 tty4
crw------- 1 0 0   4,  40 Nov 13 00:03 tty40
crw------- 1 0 0   4,  41 Nov 13 00:03 tty41
crw------- 1 0 0   4,  42 Nov 13 00:03 tty42
crw------- 1 0 0   4,  43 Nov 13 00:03 tty43
crw------- 1 0 0   4,  44 Nov 13 00:03 tty44
crw------- 1 0 0   4,  45 Nov 13 00:03 tty45
crw------- 1 0 0   4,  46 Nov 13 00:03 tty46
crw------- 1 0 0   4,  47 Nov 13 00:03 tty47
crw------- 1 0 0   4,  48 Nov 13 00:03 tty48
crw------- 1 0 0   4,  49 Nov 13 00:03 tty49
crw------- 1 0 0   4,   5 Nov 13 00:03 tty5
crw------- 1 0 0   4,  50 Nov 13 00:03 tty50
crw------- 1 0 0   4,  51 Nov 13 00:03 tty51
crw------- 1 0 0   4,  52 Nov 13 00:03 tty52
crw------- 1 0 0   4,  53 Nov 13 00:03 tty53
crw------- 1 0 0   4,  54 Nov 13 00:03 tty54
crw------- 1 0 0   4,  55 Nov 13 00:03 tty55
crw------- 1 0 0   4,  56 Nov 13 00:03 tty56
crw------- 1 0 0   4,  57 Nov 13 00:03 tty57
crw------- 1 0 0   4,  58 Nov 13 00:03 tty58
crw------- 1 0 0   4,  59 Nov 13 00:03 tty59
crw------- 1 0 0   4,   6 Nov 13 00:03 tty6
crw------- 1 0 0   4,  60 Nov 13 00:03 tty60
crw------- 1 0 0   4,  61 Nov 13 00:03 tty61
crw------- 1 0 0   4,  62 Nov 13 00:03 tty62
crw------- 1 0 0   4,  63 Nov 13 00:03 tty63
crw------- 1 0 0   4,   7 Nov 13 00:03 tty7
crw------- 1 0 0   4,   8 Nov 13 00:03 tty8
crw------- 1 0 0   4,   9 Nov 13 00:03 tty9
crw------- 1 0 0   4,  64 Nov 13 00:03 ttyS0
crw------- 1 0 0   4,  65 Nov 13 00:03 ttyS1
crw------- 1 0 0   4,  74 Nov 13 00:03 ttyS10
crw------- 1 0 0   4,  75 Nov 13 00:03 ttyS11
crw------- 1 0 0   4,  76 Nov 13 00:03 ttyS12
crw------- 1 0 0   4,  77 Nov 13 00:03 ttyS13
crw------- 1 0 0   4,  78 Nov 13 00:03 ttyS14
crw------- 1 0 0   4,  79 Nov 13 00:03 ttyS15
crw------- 1 0 0   4,  80 Nov 13 00:03 ttyS16
crw------- 1 0 0   4,  81 Nov 13 00:03 ttyS17
crw------- 1 0 0   4,  82 Nov 13 00:03 ttyS18
crw------- 1 0 0   4,  83 Nov 13 00:03 ttyS19
crw------- 1 0 0   4,  66 Nov 13 00:03 ttyS2
crw------- 1 0 0   4,  84 Nov 13 00:03 ttyS20
crw------- 1 0 0   4,  85 Nov 13 00:03 ttyS21
crw------- 1 0 0   4,  86 Nov 13 00:03 ttyS22
crw------- 1 0 0   4,  87 Nov 13 00:03 ttyS23
crw------- 1 0 0   4,  88 Nov 13 00:03 ttyS24
crw------- 1 0 0   4,  89 Nov 13 00:03 ttyS25
crw------- 1 0 0   4,  90 Nov 13 00:03 ttyS26
crw------- 1 0 0   4,  91 Nov 13 00:03 ttyS27
crw------- 1 0 0   4,  92 Nov 13 00:03 ttyS28
crw------- 1 0 0   4,  93 Nov 13 00:03 ttyS29
crw------- 1 0 0   4,  67 Nov 13 00:03 ttyS3
crw------- 1 0 0   4,  94 Nov 13 00:03 ttyS30
crw------- 1 0 0   4,  95 Nov 13 00:03 ttyS31
crw------- 1 0 0   4,  68 Nov 13 00:03 ttyS4
crw------- 1 0 0   4,  69 Nov 13 00:03 ttyS5
crw------- 1 0 0   4,  70 Nov 13 00:03 ttyS6
crw------- 1 0 0   4,  71 Nov 13 00:03 ttyS7
crw------- 1 0 0   4,  72 Nov 13 00:03 ttyS8
crw------- 1 0 0   4,  73 Nov 13 00:03 ttyS9
crw------- 1 0 0   5,   3 Nov 13 00:03 ttyprintk
crw------- 1 0 0  10, 239 Nov 13 00:03 uhid
crw------- 1 0 0  10, 223 Nov 13 00:03 uinput
crw-rw-rw- 1 0 0   1,   9 Nov 13 00:03 urandom
crw------- 1 0 0  10, 240 Nov 13 00:03 userio
crw------- 1 0 0   7,   0 Nov 13 00:03 vcs
crw------- 1 0 0   7,   1 Nov 13 00:03 vcs1
crw------- 1 0 0   7, 128 Nov 13 00:03 vcsa
crw------- 1 0 0   7, 129 Nov 13 00:03 vcsa1
drwxr-xr-x 2 0 0  60 Nov 13 00:03 vfio
crw------- 1 0 0  10,  63 Nov 13 00:03 vga_arbiter
crw------- 1 0 0  10, 137 Nov 13 00:03 vhci
crw------- 1 0 0  10, 238 Nov 13 00:03 vhost-net
drwxr-xr-x 2 0 0  60 Nov 13 00:03 virtio-ports
crw------- 1 0 0 248,   1 Nov 13 00:03 vport1p1
crw-rw-rw- 1 0 0   1,   5 Nov 13 00:03 zero
/dev/block:
total 0
lrwxrwxrwx 1 0 0 8 Nov 13 00:03 7:0 -> ../loop0
lrwxrwxrwx 1 0 0 8 Nov 13 00:03 7:1 -> ../loop1
lrwxrwxrwx 1 0 0 8 Nov 13 00:03 7:2 -> ../loop2
lrwxrwxrwx 1 0 0 8 Nov 13 00:03 7:3 -> ../loop3
lrwxrwxrwx 1 0 0 8 Nov 13 00:03 7:4 -> ../loop4
lrwxrwxrwx 1 0 0 8 Nov 13 00:03 7:5 -> ../loop5
lrwxrwxrwx 1 0 0 8 Nov 13 00:03 7:6 -> ../loop6
lrwxrwxrwx 1 0 0 8 Nov 13 00:03 7:7 -> ../loop7
lrwxrwxrwx 1 0 0 6 Nov 13 00:03 8:0 -> ../sda
lrwxrwxrwx 1 0 0 6 Nov 13 00:03 8:16 -> ../sdb
/dev/bsg:
total 0
crw------- 1 0 0 249, 0 Nov 13 00:03 2:0:0:0
crw------- 1 0 0 249, 1 Nov 13 00:03 2:0:1:0
/dev/char:
total 0
lrwxrwxrwx 1 0 0  6 Nov 13 00:03 108:0 -> ../ppp
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 10:1 -> ../psaux
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 10:183 -> ../hwrng
lrwxrwxrwx 1 0 0 10 Nov 13 00:03 10:200 -> ../net/tun
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 10:223 -> ../uinput
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 10:227 -> ../mcelog
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 10:229 -> ../fuse
lrwxrwxrwx 1 0 0 11 Nov 13 00:03 10:231 -> ../snapshot
lrwxrwxrwx 1 0 0  6 Nov 13 00:03 10:232 -> ../kvm
lrwxrwxrwx 1 0 0 17 Nov 13 00:03 10:236 -> ../mapper/control
lrwxrwxrwx 1 0 0 15 Nov 13 00:03 10:237 -> ../loop-control
lrwxrwxrwx 1 0 0 11 Nov 13 00:03 10:55 -> ../megadev0
lrwxrwxrwx 1 0 0 19 Nov 13 00:03 10:56 -> ../memory_bandwidth
lrwxrwxrwx 1 0 0 21 Nov 13 00:03 10:57 -> ../network_throughput
lrwxrwxrwx 1 0 0 18 Nov 13 00:03 10:58 -> ../network_latency
lrwxrwxrwx 1 0 0 18 Nov 13 00:03 10:59 -> ../cpu_dma_latency
lrwxrwxrwx 1 0 0 19 Nov 13 00:03 10:60 -> ../lightnvm/control
lrwxrwxrwx 1 0 0 11 Nov 13 00:03 10:61 -> ../ecryptfs
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 10:62 -> ../rfkill
lrwxrwxrwx 1 0 0 14 Nov 13 00:03 10:63 -> ../vga_arbiter
lrwxrwxrwx 1 0 0 15 Nov 13 00:03 13:32 -> ../input/mouse0
lrwxrwxrwx 1 0 0 13 Nov 13 00:03 13:63 -> ../input/mice
lrwxrwxrwx 1 0 0 15 Nov 13 00:03 13:64 -> ../input/event0
lrwxrwxrwx 1 0 0 15 Nov 13 00:03 13:65 -> ../input/event1
lrwxrwxrwx 1 0 0  6 Nov 13 00:03 1:1 -> ../mem
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 1:11 -> ../kmsg
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 1:3 -> ../null
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 1:4 -> ../port
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 1:5 -> ../zero
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 1:7 -> ../full
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 1:8 -> ../random
lrwxrwxrwx 1 0 0 10 Nov 13 00:03 1:9 -> ../urandom
lrwxrwxrwx 1 0 0  6 Nov 13 00:03 21:0 -> ../sg0
lrwxrwxrwx 1 0 0  6 Nov 13 00:03 21:1 -> ../sg1
lrwxrwxrwx 1 0 0 11 Nov 13 00:03 248:1 -> ../vport1p1
lrwxrwxrwx 1 0 0 14 Nov 13 00:03 249:0 -> ../bsg/2:0:0:0
lrwxrwxrwx 1 0 0 14 Nov 13 00:03 249:1 -> ../bsg/2:0:1:0
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 251:0 -> ../rtc0
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 4:0 -> ../tty0
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 4:1 -> ../tty1
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:10 -> ../tty10
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:11 -> ../tty11
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:12 -> ../tty12
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:13 -> ../tty13
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:14 -> ../tty14
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:15 -> ../tty15
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:16 -> ../tty16
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:17 -> ../tty17
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:18 -> ../tty18
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:19 -> ../tty19
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 4:2 -> ../tty2
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:20 -> ../tty20
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:21 -> ../tty21
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:22 -> ../tty22
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:23 -> ../tty23
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:24 -> ../tty24
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:25 -> ../tty25
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:26 -> ../tty26
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:27 -> ../tty27
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:28 -> ../tty28
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:29 -> ../tty29
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 4:3 -> ../tty3
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:30 -> ../tty30
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:31 -> ../tty31
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:32 -> ../tty32
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:33 -> ../tty33
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:34 -> ../tty34
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:35 -> ../tty35
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:36 -> ../tty36
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:37 -> ../tty37
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:38 -> ../tty38
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:39 -> ../tty39
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 4:4 -> ../tty4
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:40 -> ../tty40
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:41 -> ../tty41
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:42 -> ../tty42
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:43 -> ../tty43
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:44 -> ../tty44
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:45 -> ../tty45
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:46 -> ../tty46
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:47 -> ../tty47
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:48 -> ../tty48
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:49 -> ../tty49
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 4:5 -> ../tty5
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:50 -> ../tty50
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:51 -> ../tty51
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:52 -> ../tty52
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:53 -> ../tty53
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:54 -> ../tty54
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:55 -> ../tty55
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:56 -> ../tty56
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:57 -> ../tty57
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:58 -> ../tty58
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:59 -> ../tty59
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 4:6 -> ../tty6
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:60 -> ../tty60
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:61 -> ../tty61
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:62 -> ../tty62
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:63 -> ../tty63
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:64 -> ../ttyS0
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:65 -> ../ttyS1
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:66 -> ../ttyS2
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:67 -> ../ttyS3
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:68 -> ../ttyS4
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:69 -> ../ttyS5
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 4:7 -> ../tty7
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:70 -> ../ttyS6
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:71 -> ../ttyS7
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:72 -> ../ttyS8
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 4:73 -> ../ttyS9
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:74 -> ../ttyS10
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:75 -> ../ttyS11
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:76 -> ../ttyS12
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:77 -> ../ttyS13
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:78 -> ../ttyS14
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:79 -> ../ttyS15
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 4:8 -> ../tty8
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:80 -> ../ttyS16
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:81 -> ../ttyS17
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:82 -> ../ttyS18
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:83 -> ../ttyS19
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:84 -> ../ttyS20
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:85 -> ../ttyS21
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:86 -> ../ttyS22
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:87 -> ../ttyS23
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:88 -> ../ttyS24
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:89 -> ../ttyS25
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 4:9 -> ../tty9
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:90 -> ../ttyS26
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:91 -> ../ttyS27
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:92 -> ../ttyS28
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:93 -> ../ttyS29
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:94 -> ../ttyS30
lrwxrwxrwx 1 0 0  9 Nov 13 00:03 4:95 -> ../ttyS31
lrwxrwxrwx 1 0 0  6 Nov 13 00:03 5:0 -> ../tty
lrwxrwxrwx 1 0 0 10 Nov 13 00:03 5:1 -> ../console
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 5:2 -> ../ptmx
lrwxrwxrwx 1 0 0 12 Nov 13 00:03 5:3 -> ../ttyprintk
lrwxrwxrwx 1 0 0  6 Nov 13 00:03 7:0 -> ../vcs
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 7:1 -> ../vcs1
lrwxrwxrwx 1 0 0  7 Nov 13 00:03 7:128 -> ../vcsa
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 7:129 -> ../vcsa1
lrwxrwxrwx 1 0 0  8 Nov 13 00:03 89:0 -> ../i2c-0
/dev/disk:
total 0
drwxr-xr-x 2 0 0 80 Nov 13 00:03 by-id
drwxr-xr-x 2 0 0 80 Nov 13 00:03 by-path
drwxr-xr-x 2 0 0 60 Nov 13 00:03 by-uuid
/dev/disk/by-id:
total 0
lrwxrwxrwx 1 0 0 9 Nov 13 00:03 scsi-0QEMU_QEMU_HARDDISK_appliance -> ../../sdb
lrwxrwxrwx 1 0 0 9 Nov 13 00:03 scsi-0QEMU_QEMU_HARDDISK_hd0 -> ../../sda
/dev/disk/by-path:
total 0
lrwxrwxrwx 1 0 0 9 Nov 13 00:03 virtio-pci-0000:00:02.0-scsi-0:0:0:0 -> ../../sda
lrwxrwxrwx 1 0 0 9 Nov 13 00:03 virtio-pci-0000:00:02.0-scsi-0:0:1:0 -> ../../sdb
/dev/disk/by-uuid:
total 0
lrwxrwxrwx 1 0 0 9 Nov 13 00:03 a4279bc5-e8ef-4224-811b-440c6e49047e -> ../../sdb
/dev/input:
total 0
drwxr-xr-x 2 0 0    100 Nov 13 00:03 by-path
crw------- 1 0 0 13, 64 Nov 13 00:03 event0
crw------- 1 0 0 13, 65 Nov 13 00:03 event1
crw------- 1 0 0 13, 63 Nov 13 00:03 mice
crw------- 1 0 0 13, 32 Nov 13 00:03 mouse0
/dev/input/by-path:
total 0
lrwxrwxrwx 1 0 0 9 Nov 13 00:03 platform-i8042-serio-0-event-kbd -> ../event0
lrwxrwxrwx 1 0 0 9 Nov 13 00:03 platform-i8042-serio-1-event-mouse -> ../event1
lrwxrwxrwx 1 0 0 9 Nov 13 00:03 platform-i8042-serio-1-mouse -> ../mouse0
/dev/lightnvm:
total 0
crw------- 1 0 0 10, 60 Nov 13 00:03 control
/dev/mapper:
total 0
crw------- 1 0 0 10, 236 Nov 13 00:03 control
/dev/net:
total 0
crw-rw-rw- 1 0 0 10, 200 Nov 13 00:03 tun
/dev/snd:
total 0
crw------- 1 0 0 116,  1 Nov 13 00:03 seq
crw------- 1 0 0 116, 33 Nov 13 00:03 timer
/dev/vfio:
total 0
crw------- 1 0 0 10, 196 Nov 13 00:03 vfio
/dev/virtio-ports:
total 0
lrwxrwxrwx 1 0 0 11 Nov 13 00:03 org.libguestfs.channel.0 -> ../vport1p1
/dev/root / ext2 rw,noatime,block_validity,barrier,user_xattr,acl 0 0
/proc /proc proc rw,relatime 0 0
/sys /sys sysfs rw,relatime 0 0
tmpfs /run tmpfs rw,nosuid,relatime,size=97520k,mode=755 0 0
/dev /dev devtmpfs rw,relatime,size=240704k,nr_inodes=60176,mode=755 0 0
  /run/lvm/lvmetad.socket: connect failed: No such file or directory
  WARNING: Failed to connect to lvmetad. Falling back to internal scanning.
  /run/lvm/lvmetad.socket: connect failed: No such file or directory
  WARNING: Failed to connect to lvmetad. Falling back to internal scanning.
  /run/lvm/lvmetad.socket: connect failed: No such file or directory
  WARNING: Failed to connect to lvmetad. Falling back to internal scanning.
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1
ink/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
net 127.0.0.1/8 brd 127.255.255.255 scope host lo
  valid_lft forever preferred_lft forever
net6 ::1/128 scope host
  valid_lft forever preferred_lft forever
Module             Size  Used by
kvm_amd           65536  0
kvm              548864  1 kvm_amd
input_leds        16384  0
irqbypass         16384  1 kvm
mac_hid           16384  0
psmouse          131072  0
serio_raw         16384  0
i2c_piix4         24576  0
pata_acpi         16384  0
libcrc32c         16384  0
crc8              16384  0
crc7              16384  0
crc_itu_t         16384  0
crc_ccitt         16384  0
virtio_input      16384  0
virtio_scsi       20480  1
sym53c8xx         81920  0
scsi_transport_spi    32768  1 sym53c8xx
megaraid_sas     135168  0
megaraid_mbox     36864  0
megaraid_mm       20480  1 megaraid_mbox
megaraid          49152  0
sparse_keymap     16384  0
video             40960  0
virtio_gpu        53248  0
ttm               98304  1 virtio_gpu
drm_kms_helper   155648  1 virtio_gpu
sysimgblt         16384  1 drm_kms_helper
sysfillrect       16384  1 drm_kms_helper
syscopyarea       16384  1 drm_kms_helper
fb_sys_fops       16384  1 drm_kms_helper
drm              364544  3 ttm,drm_kms_helper,virtio_gpu
virtio_rng        16384  0
crc32             16384  0
Fri Nov 13 00:03:40 UTC 2020
clocksource: refined-jiffies
uptime: 10.11 1.38
guestfsd --verbose
trying to open virtio-serial channel '/dev/virtio-ports/org.libguestfs.channel.0'
udevadm --debug settle
calling: settle
libguestfs: recv_from_daemon: received GUESTFS_LAUNCH_FLAG
libguestfs: [16508ms] appliance is up
guestfsd: main_loop: new request, len 0x3c
udevadm --debug settle
calling: settle
commandrvf: stdout=e stderr=y flags=0x10000
commandrvf: parted -s -- /dev/sda mklabel msdos
[   11.328634]  sda:
udevadm --debug settle
calling: settle
num sectors:
guestfsd: main_loop: proc 208 (part_init) took 0.98 seconds
guestfsd: main_loop: new request, len 0x34
udevadm --debug settle
calling: settle
commandrvf: stdout=y stderr=y flags=0x0
commandrvf: blockdev --getsz /dev/sda
26624
guestfsd: main_loop: proc 62 (blockdev_getsz) took 0.11 seconds
guestfsd: main_loop: new request, len 0x4c
udevadm --debug settle
calling: settle
commandrvf: stdout=e stderr=y flags=0x10000
commandrvf: parted -s -- /dev/sda mkpart primary 2048s 22885s
[   11.892669]  sda:
[   12.388700]  sda: sda1
udevadm --debug settle
calling: settle
guestfsd: main_loop: proc 209 (part_add) took 0.91 seconds
guestfsd: main_loop: new request, len 0x4c
udevadm --debug settle
calling: settle
commandrvf: stdout=e stderr=y flags=0x10000
commandrvf: parted -s -- /dev/sda mkpart primary 22886s -2048s
Warning: The resulting partition is not properly aligned for best performance.
[   13.360761]  sda: sda1 sda2
udevadm --debug settle
calling: settle
guestfsd: main_loop: proc 209 (part_add) took 1.06 seconds
guestfsd: main_loop: new request, len 0x54
commandrvf: stdout=y stderr=y flags=0x0
commandrvf: wipefs --help
commandrvf: stdout=n stderr=n flags=0x0
commandrvf: wipefs -a --force /dev/sda1
commandrvf: stdout=n stderr=y flags=0x0
commandrvf: mkfs -t vfat -I /dev/sda1
guestfsd: main_loop: proc 278 (mkfs) took 0.26 seconds
guestfsd: main_loop: new request, len 0x54
commandrvf: stdout=n stderr=n flags=0x0
commandrvf: wipefs -a --force /dev/sda2
commandrvf: stdout=n stderr=y flags=0x0
commandrvf: mkfs -t vfat -I /dev/sda2
guestfsd: main_loop: proc 278 (mkfs) took 0.24 seconds
guestfsd: main_loop: new request, len 0x40
commandrvf: stdout=n stderr=y flags=0x0
commandrvf: mount -o  /dev/sda2 /sysroot/
guestfsd: main_loop: proc 1 (mount) took 0.14 seconds
guestfsd: main_loop: new request, len 0x34
guestfsd: main_loop: proc 32 (mkdir) took 0.01 seconds
guestfsd: main_loop: new request, len 0x44
commandrvf: stdout=n stderr=y flags=0x0
commandrvf: mount -o  /dev/sda1 /sysroot/boot
error: incorrect number of arguments
usage: tar-in tarfile directory [compress:..] [xattrs:true|false] [selinux:true|false] [acls:true|false]
type 'help tar-in' for more help on tar-in
guestfsd: main_loop: proc 1 (mount) took 0.12 seconds
guestfsd: main_loop: new request, len 0x28
umount-all: /proc/mounts: fsname=/dev/root dir=/ type=ext2 opts=rw,noatime,block_validity,barrier,user_xattr,acl freq=0 passno=0
umount-all: /proc/mounts: fsname=/proc dir=/proc type=proc opts=rw,relatime freq=0 passno=0
umount-all: /proc/mounts: fsname=/sys dir=/sys type=sysfs opts=rw,relatime freq=0 passno=0
umount-all: /proc/mounts: fsname=tmpfs dir=/run type=tmpfs opts=rw,nosuid,relatime,size=97520k,mode=755 freq=0 passno=0
umount-all: /proc/mounts: fsname=/dev dir=/dev type=devtmpfs opts=rw,relatime,size=240704k,nr_inodes=60176,mode=755 freq=0 passno=0
umount-all: /proc/mounts: fsname=/dev/sda2 dir=/sysroot type=vfat opts=rw,relatime,fmask=0022,dmask=0022,codepage=437,iocharset=iso8859-1,shortname=mi
xed,errors=remount-ro freq=0 passno=0
umount-all: /proc/mounts: fsname=/dev/sda1 dir=/sysroot/boot type=vfat opts=rw,relatime,fmask=0022,dmask=0022,codepage=437,iocharset=iso8859-1,shortna
me=mixed,errors=remount-ro freq=0 passno=0
commandrvf: stdout=n stderr=y flags=0x0
commandrvf: umount /sysroot/boot
commandrvf: stdout=n stderr=y flags=0x0
commandrvf: umount /sysroot
guestfsd: main_loop: proc 47 (umount_all) took 0.16 seconds
guestfsd: main_loop: new request, len 0x28
umount-all: /proc/mounts: fsname=/dev/root dir=/ type=ext2 opts=rw,noatime,block_validity,barrier,user_xattr,acl freq=0 passno=0
umount-all: /proc/mounts: fsname=/proc dir=/proc type=proc opts=rw,relatime freq=0 passno=0
umount-all: /proc/mounts: fsname=/sys dir=/sys type=sysfs opts=rw,relatime freq=0 passno=0
umount-all: /proc/mounts: fsname=tmpfs dir=/run type=tmpfs opts=rw,nosuid,relatime,size=97520k,mode=755 freq=0 passno=0
umount-all: /proc/mounts: fsname=/dev dir=/dev type=devtmpfs opts=rw,relatime,size=240704k,nr_inodes=60176,mode=755 freq=0 passno=0
fsync /dev/sda
libguestfs: sending SIGTERM to process 124
libguestfs: closing guestfs handle 0x1b19580 (state 0)
libguestfs: command: run: rm
libguestfs: command: run: \ -rf /tmp/libguestfsYmpbBu
