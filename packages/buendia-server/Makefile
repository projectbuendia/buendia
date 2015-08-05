include ../Makefile.inc

SOURCE_DIR=$(HOME)/openmrs/server/modules
TARGET_DIR=$(EXTRA_DATA)/usr/share/buendia/openmrs/modules
MAIN_MODULE=$(TARGET_DIR)/buendia-server.omod
OTHER_MODULES=$(TARGET_DIR)/xforms-4.3.5.omod $(TARGET_DIR)/webservices.rest-2.6.omod

$(EXTRA_DATA): $(MAIN_MODULE) $(OTHER_MODULES)

$(MAIN_MODULE):
	mkdir -p $(TARGET_DIR)
	cp $(SOURCE_DIR)/projectbuendia.openmrs-1.0-SNAPSHOT.omod $@

$(OTHER_MODULES):
	mkdir -p $(TARGET_DIR)
	cp $(SOURCE_DIR)/$(notdir $@) $@
