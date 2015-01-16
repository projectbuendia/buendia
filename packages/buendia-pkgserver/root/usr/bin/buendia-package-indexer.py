#!/usr/bin/python
import os, subprocess, json

def get_setting(setting, default=None, namespace='duserver'):
    """Get the value for a setting from the duserver settings file."""
    try:
        value = subprocess.check_output(
                "source /usr/share/buendia/site/%s; echo -n \"$%s\"" % (
                    namespace, setting,), shell=True)
    except subprocess.CalledProcessError:
        return default
    else:
        return value

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

def create_index(package_directory, base_url):
    """Creates the index of ```package_directory```."""
    try:
        files = os.listdir(package_directory)
    except OSError:
        print('Not a valid directory.')
        exit(2)

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
                {"version": str(version), "url": "%s/%s" % (base_url, filename)})

    # Write each package index to their respective files
    for module in packages:
        # Order the packages on their version label
        package_list = sorted(packages[module], key=lambda x:
                split_into_components(x['version']))
        # Write the index file
        open('%s/%s.json' % (package_directory, module), 'w').write(
            json.dumps(package_list))

if __name__ == '__main__':
    # Retrieve the package base url from the settings file
    base_url = get_setting('DUSERVER_PACKAGE_BASE_URL')
    if base_url is None:
        print("Requires $DUSERVER_PACKAGE_BASE_URL to be set to the base url.")
        exit(1)
    # Create the index of the default location for buendia packages
    create_index("/usr/share/buendia/packages/", base_url)

