LAST_TAG := $(shell git log --format=format:"%d" | sed -e 's/.*tag: \(v[0-9.]*\).*/\1/' | grep '^v' | sed -e 's/^v//' | head -1)
ifeq ($(strip $(LAST_TAG)),)
  LAST_TAG := 0.0
endif

ifeq ($(strip $(BUILD_NUMBER)),)
  BUILD_NUMBER := _unknown
endif

export PACKAGE_VERSION := $(LAST_TAG).build$(BUILD_NUMBER)

# Execute the Makefiles in all the package subdirectories.
all:
	for i in buendia-*; do PACKAGE_NAME=$$i make -C $$i; done

clean:
	rm -rf */*.deb

pristine: clean
	for i in buendia-*; do rm -rf /tmp/$$i; done
