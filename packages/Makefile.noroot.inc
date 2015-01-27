# Makefile base for packages without a root folder in their source directory

# The PACKAGE_VERSION environment variable must be set before running make.

# All paths are written as though the current directory is one level down,
# in one of the buendia-* subdirectories, because Makefile.inc is included
# by Makefiles in those subdirectories.

export PACKAGE_NAME := $(shell basename $$(pwd))
export TOP := $(shell cd ../.. && pwd)
export TOOLS := $(TOP)/tools

# Everything under 'root' is automatically included in the package.  To add
# other files, write a rule to make each file (at a path under $(EXTRA_ROOT)
# and add a rule for $(EXTRA_ROOT): that depends on all of those files.
export EXTRA_ROOT := /tmp/$(PACKAGE_NAME)
$(shell mkdir -p $(EXTRA_ROOT))

export PACKAGE := $(PACKAGE_NAME)_$(PACKAGE_VERSION)_all.deb

all: $(PACKAGE)

clean:
	rm -rf *.deb

pristine: clean
	rm -rf $(EXTRA_ROOT)

$(PACKAGE): control/* $(EXTRA_ROOT)
	$(TOOLS)/mkdeb $@ control $(EXTRA_ROOT)

.FORCE:
