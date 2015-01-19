export PACKAGE_VERSION := 1.0
export PACKAGE_NAME := $(shell basename $$(pwd))
export TOOLS := $(shell pwd)/../../tools
export FILES := $(shell find root)
export SOURCE_DIR := $(shell pwd)/../../pkgserver
export BUILD_DIR :=$(shell mktemp -d)

PACKAGE=$(PACKAGE_NAME)_$(PACKAGE_VERSION).deb

all: $(PACKAGE)

$(PACKAGE): control/* $(FILES)
	mkdir -p $(BUILD_DIR)/usr/bin
	cp $(SOURCE_DIR)/bin/* $(BUILD_DIR)/usr/bin/
	$(TOOLS)/mkdeb $@ control root $(BUILD_DIR)
	rm -R $(BUILD_DIR)

clean: $(PACKAGE)
	rm *.deb

docker: $(PACKAGE)
	docker build .
