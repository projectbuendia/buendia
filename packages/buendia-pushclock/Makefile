export PACKAGE_VERSION := 1.0
export PACKAGE_NAME := $(shell basename $$(pwd))
export TOOLS := $(shell pwd)/../../tools
export FILES := $(shell find root)

PACKAGE=$(PACKAGE_NAME)_$(PACKAGE_VERSION).deb

all: $(PACKAGE)

# pushclock is a slightly unusual; it contains source code that needs to be
# compiled on the target platform, so it Depends on gcc and performs the
# compilation as a postinst step.  An empty /usr/bin/pushclock file is
# included in data.tar.gz and later overwritten with the compiled binary.

$(PACKAGE): control/* $(FILES)
	mkdir -p root/usr/bin
	touch root/usr/bin/pushclock
	$(TOOLS)/mkdeb $@ control root
