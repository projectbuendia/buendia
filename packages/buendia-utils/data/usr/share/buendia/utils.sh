# Buendia shell scripts should all do ". /usr/share/buendia/utils.sh"

# Read all the settings.
for f in /usr/share/buendia/site/*; do [ -f $f ] && . $f || true; done

# Ensure that essential directories exist.
mkdir -p /usr/share/buendia/config.d
mkdir -p /usr/share/buendia/diversions
mkdir -p /usr/share/buendia/names.d
mkdir -p /usr/share/buendia/packages.list.d
mkdir -p /usr/share/buendia/site

# Writes stdin to a file, creating any parent directories as needed.
function create() {
    mkdir -p $(dirname "$1")
    cat > "$1"
}

# Skips the first line and removes leading whitespace from each subsequent line
# of a block of text; handy for indenting a literal text blocks in a script.
function unindent() {
    echo -n "$1" | sed -ne '2,$ s/^ *// p'
}

# Treats a "0" or "" as false and anything else as true.
function bool() {
    [ -n "$1" -a "$1" != "0" ]
}

# Starts, stops, or restarts a service, without failing if it doesn't exist.
function service_if_exists() {
    if [ -e /etc/init.d/$1 ]; then
        service $1 $2
    fi
}
