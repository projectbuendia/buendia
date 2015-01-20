export PACKAGE_VERSION := 0.1
include ../Makefile.vars

export SOURCE_DIR := $(TOP)/pkgserver
export BUILD_DIR :=$(shell mktemp -d /tmp/buendia-pkgserver.XXXXXXXX)

$(PACKAGE): control/* $(FILES)
	mkdir -p $(BUILD_DIR)/usr/bin
	cp $(SOURCE_DIR)/bin/* $(BUILD_DIR)/usr/bin/
	$(TOOLS)/mkdeb $@ control root $(BUILD_DIR)
	rm -R $(BUILD_DIR)

clean: $(PACKAGE)
	rm *.deb

docker: $(PACKAGE)
	docker build .
