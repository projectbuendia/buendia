# Set the environment variable PACKAGE_VERSION before running make.
ifeq ($(strip $(PACKAGE_VERSION)),)
    PACKAGE_VERSION := $(shell ../../tools/get_package_version)
endif
export PACKAGE_VERSION

# All paths are written as though the current directory is one level down,
# in one of the buendia-* subdirectories, because Makefile.inc is included
# by Makefiles in those subdirectories.

export PACKAGE_NAME := $(shell basename $$(pwd))
export TOP := $(shell cd ../.. && pwd)
export TOOLS := $(TOP)/tools
export DATA_FILES := $(shell [ -d data ] && find data)

# Everything under 'data' is automatically included in the package.  To add
# other files, write a rule to make each file (at a path under $(EXTRA_DATA)
# and add a rule for $(EXTRA_DATA): that depends on all of those files.
export EXTRA_DATA := /tmp/buendia-packages/$(PACKAGE_NAME)
$(shell mkdir -p $(EXTRA_DATA))

export PACKAGE := $(PACKAGE_NAME)_$(PACKAGE_VERSION)_all.deb
export DATA := $(shell [ -d data ] && echo data)

all: $(PACKAGE)

.FORCE:

# Remove the built package and intermediate build directory.
clean:
	rm -rf *.deb $(EXTRA_DATA)

# Remove everything including the cache of downloaded files.
pristine: clean
	rm -rf /tmp/buendia-fetched

tests:
	@failures=; for test in $$(find ../tests tests -type f 2>/dev/null); do $$test || failures="$$failures $$test"; done; if [ -n "$$failures" ]; then echo "Failed: $$failures"; exit 1; fi; echo "Tests passed!"

test: tests  # in case you mistype "make tests"

$(PACKAGE): tests control/* $(DATA_FILES) $(EXTRA_DATA)
	$(TOOLS)/mkdeb $@ control $(DATA) $(EXTRA_DATA)

# Site packages can just declare a need for this file and it will be made.
$(EXTRA_DATA)/usr/share/buendia/site/id:
	mkdir -p $$(dirname $@)
	name=$(PACKAGE_NAME); echo "SITE_ID=$${name#buendia-site-}" > $@
