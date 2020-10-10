#!/bin/sh

_p
HOSTNAME="$BASEDIR/custom---raspios-data/hostname"
if [ -r "${HOSTNAME}" ]; then
  if [ -r etc ] && [ -w etc/hostname ]; then
    HOSTNAME=$(cat "$HOSTNAME")
    _prompt "Change hostname to $HOSTNAME?"
    case $REPLY in
    n)
      _p "Skipping."
      ;;
    *)
      _p "Changing hostname to $HOSTNAME... "
      OLD_HOSTNAME=$(cat etc/hostname)
      echo "${HOSTNAME}" >etc/hostname
      sed -i -- "s'$OLD_HOSTNAME'$HOSTNAME'g" etc/host*
      NEW_HOSTNAME=$(cat etc/hostname)
      _p "Changed old hostname $OLD_HOSTNAME successfully to $NEW_HOSTNAME"
      ;;
    esac
  else
    _warn "Cannot write to etc/hostname. Skipping change of hostname."
  fi
else
  _p "No $HOSTNAME found. Skipping change of hostname."
fi

_p
if [ -r home ] && [ ! -b "home/pi/.ssh/authorized_keys" ] || [ -w "home/pi/.ssh/authorized_keys" ]; then
  _prompt "Copy your SSH keys?"
  case $REPLY in
  n)
    _p "Skipping."
    ;;
  *)
    _p "Copying SSH keys... "
    mkdir -p "home/${username}/.ssh"
    touch "home/${username}/.ssh/authorized_keys"
    for f in ~/.ssh/*.pub; do
      cat "${f}" >>"home/${username}/.ssh/authorized_keys"
      echo "" >>"home/${username}/.ssh/authorized_keys"
    done
    chown -R 1000:1000 "home/${username}/.ssh"
    chmod 700 "home/${username}/.ssh"
    chmod 644 "home/${username}/.ssh/authorized_keys"
    _p "Copied SSH keys successfully."
    ;;
  esac
else
  _warn "Cannot write to home/${username}/.ssh/authorized_keys. Skipping copy SSH keys."
fi

_p
if [ -r etc ] && [ ! -b "etc/samba/smb.conf" ] || [ -w "etc/samba/smb.conf" ]; then
  _prompt "Configure CIFS/SMB/Samba home share for ${username}?" "Y n" "/home/${username}"
  case $REPLY in
  n)
    _p "Skipping."
    ;;
  *)
    _p "Configuring home share for ${username}... "
    mkdir -p etc/samba
    cat <<EOF >>etc/samba/smb.conf

[home]
path = /home/${username}
writeable=Yes
create mask=0744
directory mask=0744
public=no
EOF
    echo -ne "$password\n$password\n" | smbpasswd -a -s "$username"
    ;;
  esac
else
  _warn "Cannot write to etc/samba/smb.conf. Skipping home share configuration."
fi

_p
if [ -r etc ] && [ ! -b "etc/samba/smb.conf" ] || [ -w "etc/samba/smb.conf" ]; then
  _prompt "Configure CIFS/SMB/Samba root share?" "r w N" "/home/${username}"
  case $REPLY in
  r)
    _p "Configuring read-only root share... "
    mkdir -p etc/samba
    cat <<EOF >>etc/samba/smb.conf

[/]
path = /
writeable=No
public=no
EOF
    echo -ne "$password\n$password\n" | smbpasswd -a -s "$username"
    ;;
  w)
    _p "Configuring writable root share... "
    mkdir -p etc/samba
    cat <<EOF >>/etc/samba/smb.conf

[/]
path = /
writeable=Yes
create mask=0740
directory mask=0740
public=no
EOF
    echo -ne "$password\n$password\n" | smbpasswd -a -s "$username"
    ;;
  *)
    _p "Skipping."
    ;;
  esac
else
  _warn "Cannot write to etc/samba/smb.conf. Skipping root share configuration."
fi

_p
WPA_SUPPLICANT_FILE="$BASEDIR/custom---raspios-data/wpa_supplicant.conf"
if [ -r "${WPA_SUPPLICANT_FILE}" ]; then
  if [ ! -b ./etc/wpa_supplicant/wpa_supplicant.conf ] || [ -w ./etc/wpa_supplicant/wpa_supplicant.conf ]; then
    _prompt "Copy wifi settings?" "Y n" "custom/wpa_supplicant.conf ➜ /etc/wpa_supplicant/wpa_supplicant.conf"
    case $REPLY in
    n)
      _p "Skipping."
      ;;
    *)
      _p "Copying wifi settings... "
      cat "${WPA_SUPPLICANT_FILE}" >./etc/wpa_supplicant/wpa_supplicant.conf
      _p "Copied wifi successfully."
      ;;
    esac
  else
    _warn "Cannot write to wpa_supplicant.conf. Skipping wifi setup."
  fi
else
  _p "No $WPA_SUPPLICANT_FILE found. Skipping wifi setup."
fi

_p
if [ -r etc ] && [ ! -b "etc/network/interfaces.d/usb0.network" ] || [ -w "etc/network/interfaces.d/usb0.network" ]; then
  _prompt "Configure OTG network device usb0 via interfaces.d?"
  case $REPLY in
  n)
    _p "Skipping."
    ;;
  *)
    _p "Configuring usb0... "
    mkdir -p etc/network/interfaces.d
    cat <<EOF >etc/network/interfaces.d/usb0.network
auto lo usb0

auth usb0
allow-hotplug usb0
iface usb0 inet static
address 192.168.168.192
netmask 255.255.255.0
network 192.168.168.0
broadcast 192.168.168.255
gateway 192.168.168.168
metric 999
dns-nameservers 192.168.168.168
EOF
    _p "Configured usb0 via interfaces.d."
    _p "  IP              192.168.168.192"
    _p "  default gateway 192.168.168.168"
    _p "  nameserver      192.168.168.192"
    ;;
  esac
else
  _warn "Cannot write to etc/network/interfaces.d/usb0.network. Skipping OTG for usb0/interfaces.d."
fi

_p
if [ -r etc ] && [ ! -b "etc/dhcpcd.conf" ] || [ -w "etc/dhcpcd.conf" ]; then
  _prompt "Configure OTG network device usb0 via dhcpcd.conf?"
  case $REPLY in
  n)
    _p "Skipping."
    ;;
  *)
    _p "Configuring usb0... "
    cat <<EOF >etc/dhcpcd.conf
interface usb0
static ip_address=192.168.168.192/24
static routers=192.168.168.168
static domain_name_servers=192.168.168.168
metric 999
fallback usb0
EOF
    _p "Configured usb0 via dhcpcd.conf"
    _p "  IP              192.168.168.192"
    _p "  default gateway 192.168.168.168"
    _p "  nameserver      192.168.168.192"
    ;;
  esac
else
  _warn "Cannot write to etc/dhcpcd.conf. Skipping OTG for usb0/dhcpcd.conf."
fi

_p
if [ -r etc ] && [ ! -b "etc/rc.local" ] || [ -w "etc/rc.local" ]; then
  _prompt "Deactivate power saving mode for wlan0?"
  case $REPLY in
  n)
    _p "Skipping."
    ;;
  *)
    _p "Deactivating power saving mode for wlan0... "
    sed -i -e '$s/exit 0//' etc/rc.local
    cat <<EOF >>etc/rc.local
/sbin/iw dev wlan0 set power_save off
EOF
    echo "exit 0" >>etc/rc.local
    ;;
  esac
else
  _warn "Cannot write to etc/rc.local. Skipping power saving mode deactivation."
fi

_p
if [ ! -e etc/apt/apt.conf.d/80-retries ] || [ -w etc/apt/apt.conf.d/80-retries ]; then
  _prompt "Set APT retries to 10?" "Y n"
  case $REPLY in
  n)
    _p "Skipping."
    ;;
  *)
    _p "Setting APT retries to 10... "
    echo "APT::Acquire::Retries \"10\";" >etc/apt/apt.conf.d/80-retries
    _p "Set APT retries to 10"
    ;;
  esac
else
  _warn "Cannot write to etc/apt/apt.conf.d/80-retries. Skipping APT retry configuration."
fi

_p
crontab_file="$BASEDIR/custom---raspios-data/crontab"
if [ -r "${crontab_file}" ]; then
  if [ -r etc ] && [ -w etc/crontab ]; then
    _prompt "Enable wifi auto re-connect?" "Y n" "custom---raspios-data/crontab"
    case $REPLY in
    n)
      _p "Skipping."
      ;;
    *)
      _p "Enabling wifi auto re-connect... "
      echo "*/5 * * * *      root     printf 'Periodic internet connection check... ' && (nc -4z -w5 google.com 443 1>/dev/null 2>&1 || nc -4z -w5 amazon.com 443 1>/dev/null 2>&1) && echo 'ok.' || printf 'failed. Trying to re-connect... ' && sudo /sbin/ip --force link set wlan0 down && sudo /sbin/ip link set wlan0 up && /bin/sleep 10 && (nc -4z -w5 google.com 443 1>/dev/null 2>&1 || nc -4z -w5 amazon.com 443 1>/dev/null 2>&1) && echo 'internet connection re-established.' || echo 'failed.'" >>etc/crontab
      _p "Configured crontab successfully"
      ;;
    esac
  else
    _warn "Cannot write to etc/crontab. Skipping enabling wifi auto re-connect."
  fi
else
  _p "No $crontab_file found. Skipping enabling wifi auto re-connect."
fi

_p
crontab_file="$BASEDIR/custom---raspios-data/crontab"
if [ -r "${crontab_file}" ]; then
  if [ -r etc ] && [ -w etc/crontab ]; then
    _prompt "Configure crontab?" "Y n" "custom---raspios-data/crontab"
    case $REPLY in
    n)
      _p "Skipping."
      ;;
    *)
      _p "Configuring crontab... "
      cat "${crontab_file}" >>etc/crontab
      _p "Configured crontab successfully"
      ;;
    esac
  else
    _warn "Cannot write to etc/crontab. Skipping crontab configuration."
  fi
else
  _p "No $crontab_file found. Skipping crontab configuration."
fi

_p
avahi_services_dir="$BASEDIR/custom---raspios-data/avahi-services"
if [ -r "${avahi_services_dir}" ]; then
  if [ -r etc ] && [ -w etc/init.d ]; then
    _prompt "Configure Avahi services?" "Y n" "custom---raspios-data/avahi-services"
    case $REPLY in
    n)
      _p "Skipping."
      ;;
    *)
      _p "Configuring Avahi services... "
      for f in ${avahi_services_dir}/*; do
        cp "$f" "etc/avahi/services"
      done
      chmod +x etc/avahi/services/*
      _p "Configured Avahi services successfully"
      ;;
    esac
  else
    _warn "Cannot write to etc/avahi/services. Skipping Avahi services configuration."
  fi
else
  _p "No $avahi_services_dir found. Skipping Avahi services configuration."
fi
