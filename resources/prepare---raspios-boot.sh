#!/bin/sh
# shellcheck source=shared.sh
BASEDIR=$(dirname "$0") && . "$BASEDIR"/shared.sh

_p
if [ ! -f ssh ]; then
    _prompt "Activate SSH?"
    case $REPLY in
    n)
        _p "Skipping."
        ;;
    *)
        _p "Activating SSH... "
        (touch ssh)
        _p "Activated SSH successfully."
        ;;
    esac
fi

_p
if [ ! -b ./config.txt ] || [ -w ./config.txt ]; then
    if [ ! -b ./cmdline.txt ] || [ -w ./cmdline.txt ]; then
        _prompt "Activate USB OTG?" "Y n" "USB On-The-Go (provides ethernet, webcam, etc. via single USB port)"
        case $REPLY in
        n)
            _p "Skipping."
            ;;
        *)
            _p "Activating USB OTG... "
            _hasvalue "dwc2" "dtoverlay" <./config.txt || echo "dtoverlay=dwc2" >>./config.txt
            _hasvalue "dwc2" "modules-load" <./cmdline.txt || echo "modules-load=dwc2" >>./cmdline.txt
            _p "Activated USB OTG successfully."

            _p
            _prompt "Activate virtual ethernet?" "Y n" "g_ether"
            case $REPLY in
            n)
                _p "Skipping."
                ;;
            *)
                _p "Activating virtual ethernet... "
                _hasvalue "g_ether" "modules-load" <./cmdline.txt || cat ./cmdline.txt | _addvalue "g_ether" "dwc2" "modules-load" >./cmdline.txt
                _p "Activated virtual ethernet successfully."
                ;;
            esac

            _p
            _prompt "Activate virtual webcam?" "y N" "g_webcam"
            case $REPLY in
            y)
                _p "Activating virtual webcamt... "
                _hasvalue "g_webcam" "modules-load" <./cmdline.txt || cat ./cmdline.txt | _addvalue "g_webcam" "dwc2" "modules-load" >./cmdline.txt
                _p "Activated virtual ethernet successfully."
                ;;
            *)
                _p "Skipping."
                ;;
            esac
            ;;
        esac
    else
        _warn "Cannot write to cmdline.txt. Skipping OTG (USB on-the-go) setup."
    fi
else
    _warn "Cannot write to config.txt. Skipping OTG (USB on-the-go) setup."
fi
