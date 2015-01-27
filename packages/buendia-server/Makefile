export PACKAGE_VERSION := 0.1
include ../Makefile.inc

OMOD_DIR="../../projectbuendia.openmrs/omod/target"

$(EXTRA_ROOT): $(EXTRA_ROOT)/usr/share/buendia/openmrs/buendia-server.omod

$(EXTRA_ROOT)/usr/share/buendia/openmrs/buendia-server.omod:
	mkdir -p $(dirname $@)
	cp $(OMOD_DIR)/projectbuendia-*.omod $@
