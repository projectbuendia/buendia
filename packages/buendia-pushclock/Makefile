export PACKAGE_VERSION := 0.1
export PACKAGE_NAME := $(shell basename $$(pwd))
export TOOLS := $(shell pwd)/../../tools
export FILES := $(shell find root)

PACKAGE=$(PACKAGE_NAME)_$(PACKAGE_VERSION).deb

EXTRA_ROOT=/tmp/buendia-pushclock

all: $(PACKAGE)

# buendia-pushclock is a slightly unusual; it contains source code that needs
# to be compiled on the target platform, so it Depends on gcc and performs the
# compilation as a postinst step.  An empty /usr/bin/buendia-pushclock file is
# included in data.tar.gz and later overwritten with the compiled binary.

$(PACKAGE): control/* $(FILES)
	mkdir -p $(EXTRA_ROOT)/usr/bin
	touch $(EXTRA_ROOT)/usr/bin/buendia-pushclock
	$(TOOLS)/mkdeb $@ control root $(EXTRA_ROOT)
