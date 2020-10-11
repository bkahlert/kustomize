export bgBlack bgRed bgGreen bgYellow bgBlue bgMagenta bgCyan bgWhite bgBrightBlack bgBrightRed bgBrightGreen bgBrightYellow bgBrightBlue bgBrightMagenta \
    bgBrightCyan bgBrightWhite fgBlack fgRed fgGreen fgYellow fgBlue fgMagenta fgCyan fgWhite fgBrightBlack fgBrightRed fgBrightGreen fgBrightYellow \
    fgBrightBlue fgBrightMagenta fgBrightCyan fgBrightWhite txBold txHalf txUnderline txEndUnder txReverse txStandout txEndStand txReset

indent_width=4
_times() {
    local i count
    i=0
    count=$1
    shift
    until [ "$i" -ge "$count" ]; do
        ("$@")
        i=$((i + 1))
    done
}

# background color using ANSI escape
bgBlack=$(tput setab 0)                                   # black
bgRed=$(tput setab 1)                                     # red
bgGreen=$(tput setab 2)                                   # green
bgYellow=$(tput setab 3)                                  # yellow
bgBlue=$(tput setab 4)                                    # blue
bgMagenta=$(tput setab 5)                                 # magenta
bgCyan=$(tput setab 6)                                    # cyan
bgWhite=$(tput setab 7)                                   # white
bgBrightBlack=$(tput setab 8)                             # bright black
bgBrightRed=$(tput setab 9)                               # bright red
bgBrightGreen=$(tput setab 10)                            # bright green
bgBrightYellow=$(tput setab 11)                           # bright yellow
bgBrightBlue=$(tput setab 12)                             # bright blue
bgBrightMagenta=$(tput setab 13)                          # bright magenta
bgBrightCyan=$(tput setab 14)                             # bright cyan
bgBrightWhite=$(tput setab 15)                            # bright white

# foreground color using ANSI escape
fgBlack=$(tput setaf 0)                                   # black
fgRed=$(tput setaf 1)                                     # red
fgGreen=$(tput setaf 2)                                   # green
fgYellow=$(tput setaf 3)                                  # yellow
fgBlue=$(tput setaf 4)                                    # blue
fgMagenta=$(tput setaf 5)                                 # magenta
fgCyan=$(tput setaf 6)                                    # cyan
fgWhite=$(tput setaf 7)                                   # white
fgBrightBlack=$(tput setaf 8)                             # bright black
fgBrightRed=$(tput setaf 9)                               # bright red
fgBrightGreen=$(tput setaf 10)                            # bright green
fgBrightYellow=$(tput setaf 11)                           # bright yellow
fgBrightBlue=$(tput setaf 12)                             # bright blue
fgBrightMagenta=$(tput setaf 13)                          # bright magenta
fgBrightCyan=$(tput setaf 14)                             # bright cyan
fgBrightWhite=$(tput setaf 15)                            # bright white

# text editing options
txBold=$(tput bold)                                       # bold
txHalf=$(tput dim)                                        # half-bright
txUnderline=$(tput smul)                                  # underline
txEndUnder=$(tput rmul)                                   # exit underline
txReverse=$(tput rev)                                     # reverse
txStandout=$(tput smso)                                   # standout
txEndStand=$(tput rmso)                                   # exit standout
txReset=$(tput sgr0)                                      # reset attributes

# cursor commands
cursor_save() { tput sc; }                                # save cursor position
cursor_load() { tput rc; }                                # load cursor position
cursor_hide() { tput civis; }                             # hide cursor
cursor_show() { tput cnorm; }                             # show cursor
cursor_restore() { tput sc; }                             # load cursor position
cursor_left() { tput cub "${1:-1}"; }                     # move cursor $1 times left
cursor_right() { tput cuf "${1:-1}"; }                    # move cursor $1 times right
cursor_up() { tput cuu "${1:-1}"; }                       # move cursor $1 times up
cursor_down() { tput cud "${1:-1}"; }                     # move cursor $1 times down
cursor_line_start() { tput cr; }                          # move cursor to start of line
cursor_line_end() { tput hpa "$(tput cols)"; }            # move cursor to end of line
cursor_home() { tput home; }                              # move cursor to top left
cursor_end() { tput cup "$(tput lines)" "$(tput cols)"; } # move cursor to bottom right

# clear commands
clear_clear() { tput clear; }              # clear screen and home cursor
clear_to_line_start() { tput el1; }        # clear to beginning of line
clear_to_line_end() { tput el; }           # clear to end of line
clear_to_end() { tput ed; }                #clear to end of screen

# insert commands
edit_delete() { tput ech "${1:-1}"; }      # deletes $1 characters
edit_insert() { tput ich "${1:-1}"; }      # insert $1 characters (moves rest of line forward!)
edit_insert_lines() { tput il "${1:-1}"; } # insert $1 lines

# miscelaneous  commands
beep() { printf "\a"; } # make a beep

readsingle() {
    ESC=$(printf "\033")

    read -r -s -n1
    if [ "${REPLY:-}" = "${ESC}" ]; then
        while read -r -s -n 1 -t 1; do
            beep
            : # swallowing remainder of escape sequence
        done
        readsingle
    fi

    #    stty -icanon -echo
    #    REPLY=$(dd bs=1 count=1 2>/dev/null)
    #    stty icanon echo
}

prefix() {
    local end=${1:-$(tput colors)}
    local start=${2:-0}
    until [ "$start" -ge "$end" ]; do
        printf "$(tput setab $start) $(tput sgr0)"
        start=$((start + 1))
    done
}

error() { echo "$@" >&2; }
_realpath() {
    [[ $1 == /* ]] && echo "$1" || echo "$PWD/${1#./}"
}
_standout() {
    sed "s/\(.*\)/${txHalf}\1${txReset}/"
}
_underline() {
    sed "s/\(.*\)/${txUnderline}\1${txEndUnder}/"
}
_red() { echo "\e[31m$*\e[m"; }
_green() { echo "\e[32m$*\e[m"; }
_blue() { echo "\e[34m$*\e[m"; }
_cyan() { echo "\e[36m$*\e[m"; }
_grey() { echo "\e[90m$*\e[m"; }
_title() {
    _p
    echo "$(prefix 7) $1" | _standout | indent
    _p
}
_headline() {
    _p
    echo "$(prefix 15 8) $1" | _standout | indent
    _p
}
wrap() {
    fold -w "${1:-$(tput cols)}" -s
}

indent() {
    local rt terminal_width available_width
    rt=$?
    terminal_width=$(tput cols)
    available_width=$((terminal_width - indent_width))
    #    wrap $available_width | paste /dev/null - | expand -$indent_width
    wrap $available_width | pr -to $indent_width
    return $rt
}
_printf() { printf "${1:-}"; }
_p() { _printf "$*\n" | indent; }
_h() {
    _printf "$fgBrightBlack""$*""$txReset"
}
_option() {
    local short_label long_label description
    short_label=$1
    long_label=$2
    description=${3:-}
    _p
    _p "${fgGreen}""[""${short_label}""]""${txReset}""\r""$(cursor_right 10)""${fgCyan}""${long_label}""${txReset}"
    if [ -n "${3+x}" ]; then _p "$(cursor_right 6)""$description"; fi
}
_warn() { echo >&2 "$bgBlack$fgYellow""  ! $* ""$txReset"; }
_die() {
    echo >&2 "$bgBlack$fgRed"" !! $* ""$txReset"
    exit 1
}
_run() { BASEDIR=$(dirname "$0") && SCRIPT="${BASEDIR}/$1.sh" && if [ ! -x "${SCRIPT}" ]; then chmod +x "${SCRIPT}"; fi && "${SCRIPT}"; }
_prompt() {
    local text hint
    text=${1:?} && shift
    options=${1:-"Y n"} && shift
    hint=${1:-} && shift
    REPLY=${1:-""} && shift

    formatted_options=$(printf "%s" "${options}" |
        tr -s ' ' |
        tr "[:space:]" "/" |
        perl -pe "s/^(.*?)([A-Z]+)(.*?)$/${fgBrightBlack}\1${txReset}\2${fgBrightBlack}\3${txReset}/" |
        tr "[:upper:]" "[:lower:]")
    _p "$text" "[""${formatted_options}""]" "$(_h "${hint}")"
    cursor_hide
    readsingle
    printf "%s %s" ">" "$txBold""${REPLY:-ENTER}""$txReset" | indent
    cursor_show
}

retry55() {
    local n max wait rt
    n=0
    max=2
    wait=2
    rt=0
    until [ "$n" -ge "$max" ]; do
        "$@" && return $?
        rt=$?
        n=$((n + 1))
        sleep "$wait"
    done
    return $rt
}

_hasvalue() {
    local value key
    value=${1:?}
    key=${2:-}
    if [ "$(sed -E "/^(""$key""[^#]*)\s*=\s*(([^,]*,)*)\s*($value)(,.*)*$/!d" | wc -l)" -gt 0 ]; then
        return 0
    else
        return 1
    fi
}

_addvalue() { # not working
    local addvalue value key
    addvalue=${1:?}
    value=${2:?}
    key=${3:-}
    sed -E "s/^(""$key""[^#]*)\s*=\s*(([^,]*,)*)\s*($value)(,.*)*$/\1=\2\4,$addvalue\5/"
}

_mount() {
    local device mount_point
    device=${1:?}
    mount_point=${2:?}

    if [ ! -e "$mount_point" ]; then mkdir -p "$mount_point"; fi
    linux_partitions=$(diskutil info "$device" | grep -cie "type.*linux")
    if [ "$linux_partitions" = 1 ]; then
        _p "Mounting Linux partition using fuse-ext2... "
        cmd=$(sudo fuse-ext2 "$device" "$mount_point" -o rw+ -o allow_other 2>&1)
        rt=$?
    else
        _p "Mounting Windows partition using diskutil... "
        cmd=$(diskutil mount -mountPoint "$mount_point" "$device" 2>&1)
        rt=$?
    fi
    _p "$cmd"
    return $rt
}

_umount() {
    local mount_point try max_tries
    mount_point=${1:?}
    try=0
    max_tries=20

    if [ -e "$mount_point" ]; then
        until [ "$try" -ge "$max_tries" ]; do
            mount | grep -a "[^\s]$mount_point " 1>/dev/null || break
            sudo umount "$mount_point" || sudo diskutil unmount "$mount_point"
            try=$((try + 1))
        done
        if [ -e "$mount_point" ] && [ -d "$mount_point" ]; then sudo rm -rf "$mount_point" 2>/dev/null 1>/dev/null; fi
    fi
}

_debug() {
    if [ -e "$1" ]; then echo "if [ -e \"$1\" ]; then echo \"file exists\"; fi"; fi
    if [ -r "$1" ]; then echo "if [ -r \"$1\" ]; then echo \"file is regular\"; fi"; fi
    if [ -s "$1" ]; then echo "if [ -s \"$1\" ]; then echo \"file is not zero size\"; fi"; fi
    if [ -d "$1" ]; then echo "if [ -d \"$1\" ]; then echo \"file is a directory\"; fi"; fi
    if [ -b "$1" ]; then echo "if [ -b \"$1\" ]; then echo \"file is a block device\"; fi"; fi
    if [ -c "$1" ]; then echo "if [ -c \"$1\" ]; then echo \"file is a character device\"; fi"; fi
    if [ -p "$1" ]; then echo "if [ -p \"$1\" ]; then echo \"file is a pipe\"; fi"; fi
    if [ -h "$1" ]; then echo "if [ -h \"$1\" ]; then echo \"file is a symbolic link\"; fi"; fi
    if [ -L "$1" ]; then echo "if [ -L \"$1\" ]; then echo \"file is a symbolic link\"; fi"; fi
    if [ -S "$1" ]; then echo "if [ -S \"$1\" ]; then echo \"file is a socket\"; fi"; fi
    if [ -t "$1" ]; then echo "if [ -t \"$1\" ]; then echo \"file descriptor is associated with a terminal device\"; fi"; fi
    if [ -r "$1" ]; then echo "if [ -r \"$1\" ]; then echo \"file has read permissions\"; fi"; fi
    if [ -w "$1" ]; then echo "if [ -w \"$1\" ]; then echo \"file has write permissions\"; fi"; fi
    if [ -x "$1" ]; then echo "if [ -x \"$1\" ]; then echo \"file has execute permissions\"; fi"; fi
    if [ -g "$1" ]; then echo "if [ -g \"$1\" ]; then echo \"file has set 'set-group-id' flag\"; fi"; fi
    if [ -u "$1" ]; then echo "if [ -u \"$1\" ]; then echo \"file has set 'set-user-id' flag\"; fi"; fi
    if [ -k "$1" ]; then echo "if [ -k \"$1\" ]; then echo \"file has set 'sticky-bit' flag\"; fi"; fi
    if [ -O "$1" ]; then echo "if [ -O \"$1\" ]; then echo \"file has same owner-id as you\"; fi"; fi
    if [ -G "$1" ]; then echo "if [ -G \"$1\" ]; then echo \"file has same group-id as you\"; fi"; fi
    if [ -N "$1" ]; then echo "if [ -N \"$1\" ]; then echo \"file was modified since it was last read\"; fi"; fi
}

_pv_dd() {
    local filesize
    if [ "${1:-}" == "" ]; then _die "Input file parameter missing"; fi
    filesize="$(wc -c "$1" | awk '{print $1}')"

    diskutil unmountDisk "$2"
    dd if="$1" | pv -s ${filesize} | dd of="$2" bs=4m
}

_dd() {
    local cmd
    if [ "${1:-}" == "" ]; then _die "Input file parameter missing"; fi
    if [ "${2:-}" == "" ]; then _die "Output file parameter missing"; fi
    if [ ! -e "$1" ]; then _die "Cannot dd since $1 does not exist."; fi
    if [ ! -r "$1" ]; then _die "Cannot dd since $1 is not reable."; fi
    if [ ! -e "$2" ]; then _die "Cannot dd since $2 does not exist."; fi
    if [ ! -b "$2" ]; then _die "Cannot dd since $2 is no block device."; fi

    diskutil unmountDisk "$2"
    dd if="$1" of="$2" bs=4m
}

_flash() {
    local source destination
    source=${1:?}
    destination=${2:?}

    _p "Flashing ${source} to ${destination}..."
    if command -v pv &>/dev/null; then
        _pv_dd "${source}" "${destination}"
    else
        if brew install pv; then
            _pv_dd "${source}" "${destination}"
        else
            _dd "${source}" "${destination}"
        fi
    fi
    if [ $? ]; then _p "Finished flashing."; else _warn "Writing failed."; fi
}