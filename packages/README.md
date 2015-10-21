Package build scripts
=====================

Each directory builds one package.  Within each directory:

Files for the package go under `data`, e.g. `data/usr/bin/buendia-foo`.
There should be a `Makefile` beginning with the line `include ../Makefile.inc`.
If all the files can be statically checked in under `data`,
the `Makefile` can just be that one line.
Otherwise, write targets to generate each of the additional files
at a path under `$(EXTRA_DATA)`,
and add an `$(EXTRA_DATA)` target that depends on all these targets.

Declare dependencies in `control/control`,
which should be a valid **binary** package control file with Architecture "all".
See https://www.debian.org/doc/debian-policy/ch-controlfields.html for details.

Define pre-install, post-install, pre-removal, or post-removal actions
in `control/{pre,post}{inst,rm}`.
See https://www.debian.org/doc/debian-policy/ch-maintainerscripts.html
for details.

If your package adjusts the configuration of another package,
use `buendia-divert` to move the original configuration file aside.
Your package should provide the alternate configuration file at
`/usr/local/opt/buendia/diversions/`*original-path* and call `buendia-divert`
on the original configuration file in both `postinst` and `prerm`.
See [`buendia-sshd/control`](buendia-sshd/control) for an example.

If your package uses site-specific settings, they should be defined
as shell variables in a file in `/usr/local/opt/buendia/site`
and your package must provide a config script in `/usr/local/opt/buendia/config.d`
that reads the settings and applies them to the actual service or application
(e.g. by editing its configuration files and restarting the service).
Default values should be placed in `/usr/local/opt/buendia/site/10-[name]`;
these can be overridden by higher-numbered files (for more about settings
files, see buendia-utils/data/usr/local/opt/buendia/site/README).

The shell variables and config script should be named after the package;
for example, a package named `buendia-foo` should have variables with
names like `FOO_USER` and `FOO_PASSWORD`, default settings in
/usr/local/opt/buendia/site/10-foo, and a config script at
`/usr/local/opt/buendia/config.d/foo`.
