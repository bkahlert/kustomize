#!/bin/bash
# shellcheck source=shared.sh
BASEDIR=$(dirname "$0") && . "$BASEDIR"/shared.sh

_title "Reflector Installation"

_p "This tools aims to facilitate setting up your reflector as much as possible."
_p "Currently only the current lite version of Raspberry Pi OS is offered."
_option 1 "Raspberry Pi OS (32-bit) Lite" "Suitable for Raspberry Pi B+/2/3/4 & Zero Rev.1.3/W/WH"
_option ESC "Quit"
_p

_prompt "What's your choice?" "Press ESC or any other key to quit."

case $REPLY in
1)
    _p
    _p "$(_grey Installing...)"
    _p
    _run "install-reflector-on-microsd"
    ;;
*)
    _p "$(_red Quitting...)"
    ;;
esac
