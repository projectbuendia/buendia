Package build scripts
=====================

Each directory builds one package.
In most cases the Makefile is the same; you can copy Makefile.simple.

Declare dependencies in `control/control`,
which should be a valid **binary** package control file with Architecture "all".
See https://www.debian.org/doc/debian-policy/ch-controlfields.html for details.

Define pre-install, post-install, pre-removal, or post-removal actions
in `control/{pre,post}{inst,rm}`.
See https://www.debian.org/doc/debian-policy/ch-maintainerscripts.html
for details.

If your package adjusts the configuration of another package,
use `dpkg-divert` to move the original configuration file aside.
See [`buendia-sshd/control`](buendia-sshd/control) for an example.
Note that the `--package` argument is not needed,
as `dpkg-divert` will use `$DPKG_MAINTSCRIPT_PACKAGE` provided by dpkg.
When you divert configuration files,
replace them with symlinks to files installed by your package
in the appropriate subdirectory of `/usr/share/buendia/etc`
(e.g. a package named `buendia-foo` should place its alternate configuration
files in the directory `/usr/share/buendia/etc/foo`).

If your package uses site-specific settings, they should be defined
as shell variables in a file in `/usr/share/buendia/site`
and your package must provide a config script in `/usr/share/buendia/config.d`
that does two things:

1. Creates the settings file with default values, if the file does not exist.
   (Do **not** include the settings file in your package;
   only have it created by this script.)

2. Reads the settings file and applies the settings to the actual service
   or application (e.g. by editing its configuration files).

The settings file and config script should also be named after the package
(e.g. a package named `buendia-foo` should place its settings file
at `/usr/share/buendia/site/foo` and its config script at
`/usr/share/buendia/config.d/foo`).
