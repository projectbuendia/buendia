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
    if [ -e /etc/init.d/$1 && ! -e /etc/buendia-defer-reconfigure ]; then
        service $1 $2
    fi
}

# A handy shortcut, just for typing convenience.
usb=usr/share/buendia
