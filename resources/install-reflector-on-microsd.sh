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
(cd "${LOCATION}" && ls -tp | grep -v '/$' | grep -e '---' | tail -n +6 | xargs -I {} rm -- {})

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

    if [ ! -d "$DIR" ]; then mkdir -p "$DIR"; fi
    linux_partitions=$(diskutil info "$SRC" | grep -cie "type.*linux")
    if [ "$linux_partitions" = 1 ]; then
        _p "Mounting Linux partition using fuse-ext2... "
        cmd=$(sudo fuse-ext2 "$SRC" "$DIR" -o rw+ -o allow_other 2>&1)
        rt=$?
    else
        _p "Mounting Windows partition using diskutil... "
        cmd=$(sudo diskutil mount -mountPoint "$DIR" "$SRC" 2>&1)
        rt=$?
    fi
    _p $cmd
    [ $rt -gt 0 ] && {
        _die "Could not mount $DIR"
    }
    (cd "${DIR}" && "${SCRIPT}" .)

    _p
    _p "Preparation of $PARTITION finished."
    _p
    _prompt "Do you want to continue?" "Otherwise allows for manual changes, too."
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
}

cleanup() {
    _p
    _p "Cleaning up..."
    for vol in "$@"; do
        sleep 5
        sudo umount "$vol"
    done
    for last; do true; done
    sudo hdiutil detach -quiet "$last" || sudo hdiutil detach -force "$last"
    #    rm -rf "${LOCATION:?}/mnt/*"
}

flash() {
    _p
    IMG=$1
    TARGET=$(diskutil list | grep 'external' | grep 'physical' | tr -s ' ' | cut -d ' ' -f 1 | grep -m 1 '')
    if [ "$TARGET" = "" ]; then
        _p "⚡️🚫 No flashable medium found. You'll have to flash a memory card yourself."
        return 1
    fi
    INFO=$(diskutil info "$TARGET" | grep -ie "media name\|disk size" | tr -s ' ' | cut -d ':' -f2 | perl -pe 's/\n+/ /g')
    INFO=$(_grey "$INFO")
    _p "Found possible USB to flash: $TARGET $INFO"
    _prompt "Do you want to ⚡️ flash $TARGET now? [y/N] "
    case $REPLY in
    y)
        _p
        _p "Writing $IMG to $TARGET..."
        _p "$(sudo dd if="$IMG" of="$TARGET" bs=4m)"
        _p "Finished writing."
        _p
        eject "$TARGET"
        ;;
    *)
        _p "No. Skipping."
        ;;
    esac
}

eject() {
    _p
    _prompt "Eject $1?"
    case $REPLY in
    n) ;;
    *)
        _p
        _p "Ejecting..."
        retry55 sudo diskutil unmountDisk "$1"
        retry55 sudo hdiutil detach "$1"
        retry55 sudo hdiutil -force detach "$1"
        _p "$1 successfully ejected."
        _p
        ;;
    esac
}

download_image "$WORKING_COPY"
DISK=$(hdiutil attach -imagekey diskimage-class=CRawDiskImage -nomount "$WORKING_COPY" | grep -m 1 "" | tr -s ' ' | cut -d ' ' -f 1)
BOOT=$(hdiutil attach -imagekey diskimage-class=CRawDiskImage -nomount "$WORKING_COPY" | grep -m 1 "Windows" | tr -s ' ' | cut -d ' ' -f 1)
DATA=$(hdiutil attach -imagekey diskimage-class=CRawDiskImage -nomount "$WORKING_COPY" | grep -m 1 "Linux" | tr -s ' ' | cut -d ' ' -f 1)

(prepare_partition "raspios-boot" "$BOOT")
(prepare_partition "raspios-data" "$DATA")
(cleanup "$DATA" "$BOOT" "$DISK")
flash "$WORKING_COPY"

_p "$txBold""Patching completed.""$txReset"
_p "You can find the patched image at ""$WORKING_COPY"
