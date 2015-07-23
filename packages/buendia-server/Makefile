include ../Makefile.inc

SOURCE_DIR=../../openmrs/omod/target
TARGET_DIR=$(EXTRA_DATA)/usr/share/buendia/openmrs/modules

$(EXTRA_DATA): $(TARGET_DIR)/buendia-server.omod $(TARGET_DIR)/xforms-4.3.1.omod

$(TARGET_DIR)/buendia-server.omod:
	mkdir -p $(TARGET_DIR)
	cp $(SOURCE_DIR)/projectbuendia.openmrs-1.0-SNAPSHOT.omod $@

$(TARGET_DIR)/xforms-4.3.1.omod:
	mkdir -p $(TARGET_DIR)
	cp $(TOP)/modules/xforms-4.3.1.jar $@
