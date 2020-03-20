# Copyright 2015 The Project Buendia Authors
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.  You may obtain a copy
# of the License at: http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distrib-
# uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
# OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
# specific language governing permissions and limitations under the License.

# Buendia shell scripts should all do ". /usr/share/buendia/utils.sh"

# Read all the settings.
for file in /usr/share/buendia/site/*; do [ -f $file ] && . $file || true; done

# Writes stdin to a file, creating any parent directories as needed.
function create() {
    mkdir -p $(dirname "$1")
    cat > "$1"
}

# Skips the first line and removes leading whitespace from each subsequent line
# of a block of text; handy for indenting a literal text blocks in a script.
function unindent() {
    sed -ne '2,$ s/^ *// p'
}

# Treats a "0" or "" as false and anything else as true.
function bool() {
    [ -n "$1" -a "$1" != "0" ]
}

# Starts, stops, or restarts a service, without failing if it doesn't exist.
# Skips the service operation if a reconfiguration is pending.
function service_if_exists() {
    if [ -e /etc/init.d/$1 -a ! -e /etc/buendia-defer-reconfigure ]; then
        service $1 $2
    fi
}

# Print a list of external file systems. If $EXTERNAL_BLOCK_DEVICES is set in
# /usr/share/buendia/site/*, list the partitions on those devices only.
# Otherwise, default to listing partitions on all block devices connected via
# USB.
function external_file_systems() {
    if [ -z "$EXTERNAL_BLOCK_DEVICES" ]; then
        EXTERNAL_BLOCK_DEVICES=$(lsblk -Sno NAME,TRAN | grep usb | cut -d' ' -f1)
    fi
    for device in $EXTERNAL_BLOCK_DEVICES; do
        ls /dev/${device}[0-9]
    done
}

# Encrypt a file using the system key, given the input file name.
function encrypt_file() {
    openssl enc -aes-256-cbc -md sha256 -salt -in $1 -out $1.enc \
        -pass file:/usr/share/buendia/system.key
}

# Decrypt a file using the system key, given the expected output filename. The
# input filename is assumed to be the output filename with '.enc' appended.
function decrypt_file() {
    openssl enc -d -aes-256-cbc -md sha256 -in $1.enc -out $1 \
        -pass file:/usr/share/buendia/system.key
}

# A handy shortcut, just for typing convenience.
usb=usr/share/buendia
