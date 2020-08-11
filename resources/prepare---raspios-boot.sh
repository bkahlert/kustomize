#!/bin/sh
# shellcheck source=shared.sh
BASEDIR=$(dirname "$0") && . "$BASEDIR"/shared.sh

add_or_patch() {
    FILE=$1
    PROBE=$2
    PATCH=$3
    NAME=$4
    SUPPRESS=${5:-}

    (grep -qF "$PROBE" $FILE && _p && _pstart "$NAME is already activated.") && perl -p -i -e 's`^.*\Q'"$2"'\E.*$`'"$PATCH"'`' $FILE || {
        if [ "$SUPPRESS" = "" ]; then _p && _prompt "Activate $NAME?"; else REPLY=y; fi
        case $REPLY in
        n)
            _p
            _p "No. Continue."
            return 0
            ;;
        *)
            _pstart "Activating $NAME... "
            _p "$PROBE" >>"$FILE"
            perl -p -i -e 's`^.*\Q'"$2"'\E.*$`'"$PATCH"'`' $FILE
            _p "Activated $NAME successfully."
            return 1
            ;;
        esac
    }
    return 1
}

_p
if [ ! -f ssh ]; then
    _prompt "Activate SSH?"
    case $REPLY in
    n)
        _p
        _p "No. Continue."
        ;;
    *)
        _pstart "Activating SSH... "
        (touch ssh)
        _p "Activated SSH successfully."
        ;;
    esac
fi

if [ ! -b ./config.txt ] || [ -w ./config.txt ]; then
    if [ ! -b ./cmdline.txt ] || [ -w ./cmdline.txt ]; then
        perl -i.bak -pe 's/ /\n/g' cmdline.txt
        add_or_patch config.txt "dtoverlay=dwc2" "dtoverlay=dwc2" "OTG (USB on-the-go)" || (add_or_patch cmdline.txt "modules-load=dwc2" "modules-load=dwc2" "OTG (cmdline.txt)" "suppress") 1>/dev/null
        add_or_patch cmdline.txt "modules-load=dwc2" "modules-load=dwc2,g_ether" "Ethernet over OTG"
        #add_or_patch cmdline.txt "modules-load=dwc2,g_ether" "modules-load=dwc2,g_ether,g_webcam" "Webcam over OTG"
        perl -i.bak -pe 's/\n+/ /g' cmdline.txt
    else
        _warn "Cannot write to cmdline.txt. Skipping OTP (USB on-the-go) setup."
    fi
else
    _warn "Cannot write to config.txt. Skipping OTP (USB on-the-go) setup."
fi
