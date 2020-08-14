#!/bin/bash
# shellcheck source=shared.sh
BASEDIR=$(dirname "$0") && . "$BASEDIR"/shared.sh

# Configuration
LOCATION=~/tmp/raspios

# Only-change-if-you-know-what-you-are-doing configuration
TIMESTAMP=$(date "+%Y-%m-%dT%H-%M-%S")
WORKING_COPY_BASE="${LOCATION}/raspios---"
WORKING_COPY="${WORKING_COPY_BASE}${TIMESTAMP}.img"

# delete old images if there's too many
(cd "${LOCATION}" && ls -tp | grep -v '/$' | grep -e '---' | tail -n +5 | xargs -I {} rm -- {})

download_image() {
    _p
    DEST=$1
    DIR=${2:-$LOCATION}
    IMG_RAW=${DIR}/raspios.img
    IMG_ZIP=${DIR}/raspios.zip
    IMG_UNZIP=${DIR}/raspios.unzip

    if [ -r "$DIR" ]; then
        _p "${DIR} already exists. Skipping creation."
    else
        mkdir -p "${DIR}"
    fi

    if [ -r "$IMG_ZIP" ]; then
        _p "${IMG_ZIP} already exists. Skipping download."
    else
        curl -L -o "${IMG_ZIP}" https://downloads.raspberrypi.org/raspios_lite_armhf_latest
        if [ -r "$IMG_ZIP" ]; then
            _p "${IMG_ZIP} successfully downloaded."
        else
            _p "Seems like ${IMG_ZIP} could not be downloaded. Aborting."
        fi
    fi

    if [ -r "$IMG_RAW" ]; then
        _p "${IMG_RAW} already exists. Skipping extraction."
    else
        rm -rf "${IMG_UNZIP}"
        mkdir -p "${IMG_UNZIP}"
        tar -xvf "${IMG_ZIP}" -C "${IMG_UNZIP}"
        find "${IMG_UNZIP}" -maxdepth 1 -type f | head -1000 | sed 's/.*/"&"/' | xargs -J % cp % "$IMG_RAW"
    fi

    if [ -e "$DEST" ]; then
        if [ -f "$DEST" ] && [ -w "$DEST" ]; then
            _p "$DEST already exists. Overwriting."
            rm -f "$DEST"
        else
            _p "$DEST must not exist or an overwritable file. Aborting."
        fi
    fi

    _p "Successfully created $DEST"

    cp "$IMG_RAW" "$DEST"
}

prepare_partition() {
    _p
    ABSDIR=$(_realpath "$0")
    BASEDIR=$(dirname "$ABSDIR")
    PARTITION="$1"

    _headline "Prepareing $PARTITION..."
    SCRIPT="$BASEDIR/prepare---${PARTITION}.sh"
    if [ ! -f "$SCRIPT" ]; then _die "Could not find $SCRIPT. Skipping."; fi
    if [ ! -x "$SCRIPT" ]; then chmod +x "$SCRIPT"; fi
    SRC=$2
    DIR=${3:-$LOCATION/mnt/$PARTITION}

    if [ -e "$DIR" ]; then _umount "$DIR"; fi
    _mount "$SRC" "$DIR" || _die "Could not mount $SRC"
    (cd "${DIR}" && "${SCRIPT}" .)

    _p
    _p "Preparation of $PARTITION finished."
    _p
    _prompt "Would you like to open ${PARTITION} to verify changes or adding manual ones?" "y N"
    case $REPLY in
    n)
        _p
        _p "Opening $DIR..."
        (open "$DIR")
        _p
        _prompt "Ready? Press any key to continue..."
        ;;
    *) ;;

    esac
    _umount "$DIR"
}

cleanup() {
    _p
    _p "Cleaning up..."
    for vol in "$@"; do
        if grep -qs '/mnt/foo ' /proc/mounts; then
            sleep 5
            sudo umount "$vol"
        else
            _p "$vol"" is not mounted. Skipping."
        fi
    done
    for last; do true; done
    cmd=$(sudo hdiutil detach -quiet "$last" 2>&1 || sudo hdiutil detach -force "$last" 2>&1)
    _p "$cmd"
}

flash() {
    _p
    IMG=$1
    TARGET=$(diskutil list | grep 'external' | grep 'physical' | tr -s ' ' | cut -d ' ' -f 1 | grep -m 1 '')
    if [ "$TARGET" = "" ]; then
        _p "⚡️🚫 No flashable medium found."
        _p "You'll have to flash a memory card yourself."
        return 1
    fi
    INFO=$(diskutil info "$TARGET" | grep -ie "media name\|disk size" | tr -s ' ' | cut -d ':' -f2 | perl -pe 's/\n+/ /g')
    INFO=$(_grey "$INFO")
    _p "Found possible USB to flash: $TARGET $INFO"
    _prompt "Do you want to ⚡️ flash $TARGET now?" "y N"
    case $REPLY in
    y)
        _p
        _flash "${IMG}" "${TARGET}"
        _p
        eject "$TARGET"
        ;;
    *)
        _p "Skipping."
        ;;
    esac
}

eject() {
    local answer=${2:-}
    _p
    if [ "${answer}" = "" ]; then _prompt "Eject $1?"; else REPLY=$answer; fi
    case $REPLY in
    n) ;;
    *)
        unmount_disk_output="$(retry55 sudo diskutil unmountDisk "$1")"
        detach_disk_output="$(retry55 sudo hdiutil detach "$1")"
        rt=$?
        _p "Ejecting... ""$(_h "${unmount_disk_output}"...)""$(_h "${detach_disk_output}")"
        if [ $rt ]; then _p "$1 successfully ejected."; else _warn "$1 could not be ejected"; fi
        ;;
    esac
}

cursor_up
download_image "$WORKING_COPY"
DISK=$(hdiutil attach -imagekey diskimage-class=CRawDiskImage -nomount "$WORKING_COPY" | grep -m 1 "" | tr -s ' ' | cut -d ' ' -f 1)
BOOT=$(hdiutil attach -imagekey diskimage-class=CRawDiskImage -nomount "$WORKING_COPY" | grep -m 1 "Windows" | tr -s ' ' | cut -d ' ' -f 1)
DATA=$(hdiutil attach -imagekey diskimage-class=CRawDiskImage -nomount "$WORKING_COPY" | grep -m 1 "Linux" | tr -s ' ' | cut -d ' ' -f 1)

(prepare_partition "raspios-boot" "$BOOT")
(prepare_partition "raspios-data" "$DATA")
eject "${DISK}" y
flash "${WORKING_COPY}"

_p
# shellcheck disable=SC2154
_p "${txBold}""Preparation completed.""${txReset}"
_p
_p "You can find the patched image at" "${WORKING_COPY}"
