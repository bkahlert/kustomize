#!/bin/bash
# shellcheck source=shared.sh
BASEDIR=$(dirname "$0") && . "$BASEDIR"/shared.sh
if command -v fuse-ext2 &>/dev/null; then macosPrepared=$(_h " alreay prepared"); fi

_title "Reflector Image Preparation"

_p "This tools aims to facilitate setting up your reflector as much as possible."
_p "Currently only the current lite version of Raspberry Pi OS is offered."
_option 1 "Raspberry Pi OS (32-bit) Lite" "Suitable for Raspberry Pi B+/2/3/4 & Zero Rev.1.3/W/WH"
_option 9 "Mac OS X$macosPrepared" "Installs the required packages OSX Fuse / Fuse for macOS and Fuse Ext2 in order to mount the images to be prepared."
_option ESC "Quit"
_p

_prompt "What would you like to do?" "1 9 ESC" "Press ESC or any other key to quit."

_p
case $REPLY in
1)
    _p "$(_grey Preparing image...)"
    _p
    _run "prepare---image"
    ;;
9)
    _p "$(_grey Preparing Mac OS X...)"
    _p
    _run "prepare---mac-os"
    _p
    _run "prepare"
    ;;
*)
    _p "$(_red Quitting...)"
    _p
    ;;
esac
