ifeq ($(strip $(PACKAGE_VERSION)),)
    $(error PACKAGE_VERSION must be set before running make)
endif

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
	if [ -z "$$PACKAGE_VERSION" ]; then exit 1; fi

# Remove the built package and intermediate build directory.
clean:
	rm -rf *.deb $(EXTRA_DATA)

# Remove everything including the cache of downloaded files.
pristine: clean
	rm -rf /tmp/buendia-fetched

tests:
	@failures=; for test in ../tests/* tests/*; do if [ -f $$test ]; then $$test || failures="$$failures $$test"; fi; done; if [ -n "$$failures" ]; then echo "Failed: $$failures"; exit 1; fi

test: tests  # in case you mistype "make tests"

$(PACKAGE): tests control/* $(DATA_FILES) $(EXTRA_DATA)
	$(TOOLS)/mkdeb $@ control $(DATA) $(EXTRA_DATA)

.FORCE:
