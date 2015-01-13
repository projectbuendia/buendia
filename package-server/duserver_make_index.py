#!/usr/bin/python
import os, sys, json
def split_into_components(version, boundary_chars="."):
    """Splits a string into numeric and string components."""
    components = []
    force_new_component = True
    for char in version:
        # Check if char is a boundary character
        if char in boundary_chars:
            force_new_component = True
            continue

        # Determine the type of character
        char_type = int if char.isdigit() else str

        # If a new component is forced due to a boundary character or
        #  an empty component list, create a new component
        if force_new_component:
            components.append([char_type, char])
            force_new_component = False
        # If the type of the character matches the current component,
        #  add the character to the current component.
        elif components[-1][0] == char_type:
            components[-1][1] += char
        # If the character is of another type, create a component.
        else:
            components.append([char_type, char])
    # Cast the components to their respective types
    return tuple([comp[0](comp[1]) for comp in components])

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
            {"version": str(version), "src": "%s/%s" % (base_url, filename)})

# Write each package index to their respective files
for module in packages:
    # Order the packages on their version label
    package_list = sorted(packages[module], key=lambda x:
            split_into_components(x['version']))
    # Write the index file
    open('%s/%s.json' % (package_directory, module), 'w').write(
        json.dumps(package_list))
