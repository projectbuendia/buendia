# Buendia shell scripts should all do ". /usr/share/buendia/utils.sh"

# All scripts should abort on error by default.
set -e

# Read all the settings.
for f in /usr/share/buendia/site/*; do . $f || true; done

# Ensure that essential directories exist.
mkdir -p /usr/share/buendia/config.d
mkdir -p /usr/share/buendia/diversions
mkdir -p /usr/share/buendia/names.d
mkdir -p /usr/share/buendia/packages.list.d
mkdir -p /usr/share/buendia/site

# Treats a "0" or "" as false and anything else as true.
function bool() {
    [ -n "$1" -a "$1" != "0" ]
}
