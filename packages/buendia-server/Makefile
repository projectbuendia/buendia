export PACKAGE_VERSION := 0.1
include ../Makefile.noroot.inc

OMOD_DIR=../../projectbuendia.openmrs/omod/target
OPENMRS_MODULES_DIR=/usr/share/buendia/openmrs/modules

$(EXTRA_ROOT): $(EXTRA_ROOT)$(OPENMRS_MODULES_DIR)/buendia-server.omod $(EXTRA_ROOT)$(OPENMRS_MODULES_DIR)/xforms-4.3.1.omod

$(EXTRA_ROOT)$(OPENMRS_MODULES_DIR)/buendia-server.omod:
	mkdir -p $(EXTRA_ROOT)$(OPENMRS_MODULES_DIR)
	cp $(OMOD_DIR)/projectbuendia.openmrs-*.omod $@

$(EXTRA_ROOT)$(OPENMRS_MODULES_DIR)/xforms-4.3.1.omod:
	mkdir -p $(EXTRA_ROOT)$(OPENMRS_MODULES_DIR)
	cp ../../modules/xforms-4.3.1.jar $@
