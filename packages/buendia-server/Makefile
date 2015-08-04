include ../Makefile.inc

SOURCE_DIR=$(HOME)/openmrs/server/modules
TARGET_DIR=$(EXTRA_DATA)/usr/share/buendia/openmrs/modules

$(EXTRA_DATA): $(TARGET_DIR)/buendia-server.omod $(TARGET_DIR)/xforms-4.3.1.omod $(TARGET_DIR)/webservices.rest-2.6.omod

$(TARGET_DIR)/buendia-server.omod:
	mkdir -p $(TARGET_DIR)
	cp $(SOURCE_DIR)/projectbuendia.openmrs-1.0-SNAPSHOT.omod $@

$(TARGET_DIR)/xforms-4.3.1.omod $(TARGET_DIR)/webservices.rest-2.6.omod:
	mkdir -p $(TARGET_DIR)
	cp $(SOURCE_DIR)/$(notdir $@) $@
