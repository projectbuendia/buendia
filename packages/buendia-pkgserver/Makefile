include ../Makefile.inc

SOURCE_DIR := $(TOP)/pkgserver/bin
TARGET_DIR := $(EXTRA_ROOT)/usr/bin

$(EXTRA_ROOT): $(TARGET_DIR)/buendia-package-apt-indexer $(TARGET_DIR)/buendia-package-indexer.py $(TARGET_DIR)/buendia-package-usb-import

$(TARGET_DIR)/buendia-package-apt-indexer: $(SOURCE_DIR)/buendia-package-apt-indexer
	mkdir -p $$(dirname $@)
	cp -p $^ $@

$(TARGET_DIR)/buendia-package-indexer.py: $(SOURCE_DIR)/buendia-package-indexer.py
	mkdir -p $$(dirname $@)
	cp -p $^ $@

$(TARGET_DIR)/buendia-package-usb-import: $(SOURCE_DIR)/buendia-package-usb-import
	mkdir -p $$(dirname $@)
	cp -p $^ $@

docker: $(PACKAGE)
	docker build .
