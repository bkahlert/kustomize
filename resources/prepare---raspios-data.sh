#!/bin/sh
# shellcheck source=shared.sh
BASEDIR=$(dirname "$0") && . "$BASEDIR"/shared.sh

if [ "${EUID}" -ne 0 ]; then
    exec sudo /bin/sh "$0" "$@"
fi

_p
password_file="$BASEDIR/custom---raspios-data/password"
if [ -r "${password_file}" ]; then
    if [ -r etc ] && [ -w etc/shadow ]; then
        _prompt "Change password according to custom password file?" "custom---raspios-data/password"
        case $REPLY in
        n)
            _p "Skipping."
            ;;
        *)
            _p "Changing password... "
            password_hash=$(openssl passwd -1 -in "$password_file")
            sed -i.bak -E "s/^(pi:)([^:]*)(:.*)$/\1""${password_hash}""\3/" etc/shadow
            _p "Changed password successfully"
            ;;
        esac
    else
        _warn "Cannot write to etc/shadow. Skipping change of password."
    fi
else
    _p "No $password_file found. Skipping change of password."
fi

_p
username_file="$BASEDIR/custom---raspios-data/username"
if [ -r "${username_file}" ]; then
    if [ -r etc ] && [ -w etc/shadow ]; then
        username=$(cat "$username_file")
        _prompt "Change username pi to $username?" "custom---raspios-data/username"
        case $REPLY in
        n)
            _p "Skipping."
            ;;
        *)
            _p "Changing username... "
            username_files="etc/passwd etc/shadow etc/subuid etc/group etc/gshadow etc/subgid"
            for uf in ${username_files}; do
                if [ -e "${uf}" ]; then
                    #                    if [ ! -w $uf ]; then
                    sed -i.bak -E "s/^(.*)(pi)(.*)$/\1""${username}""\3/" "${uf}"
                    #                    else
                    #                        _warn "Could not change $uf. Skipping."
                    #                    fi
                else
                    _p "Could not find $uf. Skipping."
                fi
            done
            _p "Changed username pi to $username successfully"
            ;;
        esac
    else
        _warn "Cannot write to etc/shadow. Skipping change of password."
    fi
else
    _p "No $password_file found. Skipping change of password."
fi

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
            NEW_HOSTNAME=$(cat etc/hostname | tr -s ' ')
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
        mkdir -p "home/pi/.ssh"
        touch "home/pi/.ssh/authorized_keys"
        for f in ~/.ssh/*.pub; do cat "${f}" >>"home/pi/.ssh/authorized_keys"; done
        chmod 700 "home/pi/.ssh"
        chmod 644 "home/pi/.ssh/authorized_keys"
        _p "Copied SSH keys successfully."
        ;;
    esac
else
    _warn "Cannot write to home/pi/.ssh/authorized_keys. Skipping copy SSH keys."
fi

_p
WPA_SUPPLICANT_FILE="$BASEDIR/custom---raspios-data/wpa_supplicant.conf"
if [ -r "${WPA_SUPPLICANT_FILE}" ]; then
    if [ ! -b ./etc/wpa_supplicant/wpa_supplicant.conf ] || [ -w ./etc/wpa_supplicant/wpa_supplicant.conf ]; then
        _prompt "Copy wifi settings?" "custom/wpa_supplicant.conf ➜ /etc/wpa_supplicant/wpa_supplicant.conf"
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
auth usb0
allow-hotplug usb0
iface usb0 inet static
address 192.168.168.192
netmask 255.255.255.0
network 192.168.168.0
broadcast 192.168.168.255
gateway 192.168.168.168
dns-nameservers 192.168.168.168
EOF
        _p "Configured usb0 via interfaces.d."
        _p "  IP              192.168.168.192"
        _p "  default gateway 192.168.168.168"
        _p "  nameserver      192.168.168.168"
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
EOF
        _p "Configured usb0 via dhcpcd.conf"
        _p "  IP              192.168.168.192"
        _p "  default gateway 192.168.168.168"
        _p "  nameserver      192.168.168.168"
        ;;
    esac
else
    _warn "Cannot write to etc/dhcpcd.conf. Skipping OTG for usb0/dhcpcd.d."
fi
