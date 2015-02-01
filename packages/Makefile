# Find the most recent release tag (consisting of a "v" followed by a version
# number) from which the current commit is descended.
LAST_TAG := $(shell git log --format=format:%d | grep -o -w 'v[0-9][0-9.]*' | tr -d v | head -1)
ifeq ($(strip $(LAST_TAG)),)
    LAST_TAG := 0.0
endif

# Check whether the current commit is tagged as a release.
CURRENT_TAG := $(shell git log -n 1 --format=format:%d | grep -o -w 'v[0-9][0-9.]*' | tr -d v | head -1)
ifeq ($(strip $(CURRENT_TAG)),)
    # Not a release.  Append the build number to produce a unique version.
    ifeq ($(strip $(BUILD_NUMBER)),)
        BUILD_NUMBER := _unknown
    endif
    export PACKAGE_VERSION := $(LAST_TAG).build$(BUILD_NUMBER)
else
    # This is a release.  Use the release tag.
    export PACKAGE_VERSION := $(LAST_TAG)
endif


# Execute the Makefiles in all the package subdirectories.
all:
	for i in buendia-*; do \
	    echo; \
	    echo === $$i:; \
	    PACKAGE_NAME=$$i make -C $$i || exit 1; \
	done

# Remove the built packages and intermediate build directories.
clean:
	rm -rf */*.deb /tmp/buendia-packages

# Remove everything including the cache of downloaded files.
pristine: clean
	rm -rf /tmp/buendia-fetched
