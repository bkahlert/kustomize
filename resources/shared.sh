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
cursor_left() { tput cub ${1:-1}; }                       # move cursor $1 times left
cursor_right() { tput cuf ${1:-1}; }                      # move cursor $1 times right
cursor_up() { tput cuu ${1:-1}; }                         # move cursor $1 times up
cursor_down() { tput cud ${1:-1}; }                       # move cursor $1 times down
cursor_line_start() { tput cr; }                          # move cursor to start of line
cursor_line_end() { tput hpa "$(tput cols)"; }            # move cursor to end of line
cursor_home() { tput home; }                              # move cursor to top left
cursor_end() { tput cup "$(tput lines)" "$(tput cols)"; } # move cursor to bottom right

# clear commands
clear_clear() { tput clear; }            # clear screen and home cursor
clear_to_line_start() { tput el1; }      # clear to beginning of line
clear_to_line_end() { tput el; }         # clear to end of line
clear_to_end() { tput ed; }              #clear to end of screen

# insert commands
edit_delete() { tput ech ${1:-1}; }      # deletes $1 characters
edit_insert() { tput ich ${1:-1}; }      # insert $1 characters (moves rest of line forward!)
edit_insert_lines() { tput il ${1:-1}; } # insert $1 lines

# miscelaneous  commands
beep() { printf "\a"; } # make a beep

readsingle() {
    local remainder_of_escape_sequence
    ESC=$(printf "\033")

    read -r -s -n1
    if [ "${REPLY:-}" = $ESC ]; then
        while read -r -s -n 1 -t 1 remainder_of_escape_sequence; do
            beep
            : # swallowing $remainder_of_escape_sequence"
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
    fold -w ${1:-$(tput cols)} -s
}

indent() {
    local terminal_width available_width
    terminal_width=$(tput cols)
    available_width=$((terminal_width - indent_width))
    #    wrap $available_width | paste /dev/null - | expand -$indent_width
    wrap $available_width | pr -to $indent_width
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
    if [ ! -z ${3+x} ]; then _p "$(cursor_right 6)""$description"; fi
}
_warn() { echo >&2 $bgBlack$fgYellow"  ! $* "$txReset; }
_die() {
    echo >&2 $bgBlack$fgRed" !! $* "$txReset
    exit 1
}
_run() { BASEDIR=$(dirname "$0") && SCRIPT="${BASEDIR}/$1.sh" && if [ ! -x "${SCRIPT}" ]; then chmod +x ${SCRIPT}; fi && "${SCRIPT}"; }
_prompt() {
    local text hint
    text=${1:?} && shift
    hint=${1:-} && shift
    REPLY=${1:-""} && shift

    _p "$text" "[""y""$fgBrightBlack""/n""$txReset""]" "$(_h $hint)"
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

_addvalue() {
    local addvalue value key
    addvalue=${1:?}
    value=${2:?}
    key=${3:-}
    echo "$addvalue"
    echo "$value"
    echo "$key"

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
            mount | grep -a " $mount_point " 1>/dev/null || break
            sudo umount $mount_point
            try=$((try + 1))
        done
        if [ -e "$DIR" ]; then sudo rm -rf "$DIR" 2>/dev/null 1>/dev/null; fi
    fi
}
