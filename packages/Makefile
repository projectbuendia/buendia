# Set the environment variable PACKAGE_VERSION before running make.
ifeq ($(strip $(PACKAGE_VERSION)),)
    PACKAGE_VERSION := $(shell ../tools/get_package_version)
endif
export PACKAGE_VERSION

# Execute the Makefiles in all the package subdirectories.
all:
	@for i in buendia-*; do \
	    echo; \
	    echo === $$i:; \
	    PACKAGE_NAME=$$i make -C $$i || exit 1; \
	done

tests: .FORCE
	@for i in buendia-*; do \
	    echo; \
	    echo === $$i:; \
	    PACKAGE_NAME=$$i make -C $$i tests || exit 1; \
	done
	@echo
	@echo "=== All tests passed!"

test: tests  # in case you mistype "make tests"

# Remove the built packages and intermediate build directories.
clean:
	rm -rf */*.deb /tmp/buendia-packages

# Remove everything including the cache of downloaded files.
pristine: clean
	rm -rf /tmp/buendia-fetched

.FORCE:

