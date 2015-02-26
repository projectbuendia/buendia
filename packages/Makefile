# Check whether the current commit is tagged as a release.
CURRENT_TAG := $(shell git log -n 1 --format=format:%d | grep -o -w 'v[0-9][0-9.]*' | tr -d v | head -1)
ifeq ($(strip $(CURRENT_TAG)),)
    # Not a release.  "0.0.x" version numbers represent dev builds.
    ifeq ($(strip $(BUILD_NUMBER)),)
        BUILD_NUMBER := 0
    endif
    export PACKAGE_VERSION := 0.0.$(BUILD_NUMBER)
else
    # This is a release.  Use the version number in the release tag.
    export PACKAGE_VERSION := $(LAST_TAG)
endif

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

