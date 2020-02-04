include ../Makefile.inc

SERVER_ID=buendia
SOURCE_DIR=$(HOME)/openmrs/$(SERVER_ID)/modules
MAIN_MODULE_BUILD=$(SOURCE_DIR)/projectbuendia.openmrs-1.0-SNAPSHOT.omod

TARGET_DIR=$(EXTRA_DATA)/usr/share/buendia/openmrs/modules
MAIN_MODULE=$(TARGET_DIR)/buendia-server.omod
OTHER_MODULES=$(TARGET_DIR)/xforms-4.3.5.omod $(TARGET_DIR)/webservices.rest-2.6.omod

$(EXTRA_DATA): $(MAIN_MODULE) $(OTHER_MODULES) $(EXTRA_DATA)/usr/bin/buendia-mysql $(EXTRA_DATA)/usr/bin/buendia-openmrs-account-setup $(EXTRA_DATA)/usr/bin/buendia-openmrs-location-setup $(EXTRA_DATA)/usr/bin/buendia-openmrs-dump $(EXTRA_DATA)/usr/bin/buendia-openmrs-load $(EXTRA_DATA)/usr/bin/buendia-profile-apply $(EXTRA_DATA)/usr/bin/buendia-profile-validate $(EXTRA_DATA)/usr/bin/buendia-server-clear-cache

$(MAIN_MODULE_BUILD):
	../../tools/openmrs_build

$(MAIN_MODULE): $(MAIN_MODULE_BUILD)
	mkdir -p $(TARGET_DIR)
	cp -p $(MAIN_MODULE_BUILD) $@

$(OTHER_MODULES):
	mkdir -p $(TARGET_DIR)
	cp -p $(SOURCE_DIR)/$(notdir $@) $@

$(EXTRA_DATA)/usr/bin/buendia-mysql:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/mysql $@
	chmod 755 $@

$(EXTRA_DATA)/usr/bin/buendia-openmrs-account-setup:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/openmrs_account_setup $@
	chmod 755 $@

$(EXTRA_DATA)/usr/bin/buendia-openmrs-dump:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/openmrs_dump $@
	chmod 755 $@

$(EXTRA_DATA)/usr/bin/buendia-openmrs-location-setup:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/openmrs_location_setup $@
	chmod 755 $@

$(EXTRA_DATA)/usr/bin/buendia-openmrs-load:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/openmrs_load $@
	chmod 755 $@

$(EXTRA_DATA)/usr/bin/buendia-profile-apply:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/profile_apply $@
	chmod 755 $@

$(EXTRA_DATA)/usr/bin/buendia-profile-validate:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/profile_validate $@
	chmod 755 $@

$(EXTRA_DATA)/usr/bin/buendia-server-clear-cache:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/server_clear_cache $@
	chmod 755 $@
