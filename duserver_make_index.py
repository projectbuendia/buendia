#!/bin/python
import os, sys, json

package_directory = os.environ.get('DUSERVER_PACKAGE_DIR')
if package_directory is None:
    print("Requires $DUSERVER_PACKAGE_DIR to be set to the package directory")
    exit()

base_url = os.environ.get('DUSERVER_PACKAGE_BASE_URL')
if base_url is None:
    print("Requires $DUSERVER_PACKAGE_BASE_URL to be set to the base url.")
    exit()

try:
    files = os.listdir(package_directory)
except OSError:
    print('Not a valid directory.')
    exit()

packages = {}
for filename in files:
    # Chop off the segment after the last period and split on '-'
    package_parts = '.'.join(filename.split(".")[:-1]).split("-")
    # Extract version string
    version = package_parts.pop()
    # Extract module string
    module = '-'.join(package_parts)

    # If both strings are non-empty
    if module and version:
        # Create package index
        packages[module] = packages.get(module, [])
        packages[module].append(
            {"version": version, "src": "%s/%s" % (base_url, filename)})

# Write each package index to their respective files
for module in packages:
    open('%s/%s.json' % (package_directory, module), 'w').write(
        json.dumps(packages[module]))
