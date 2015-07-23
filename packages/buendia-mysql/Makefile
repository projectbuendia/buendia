include ../Makefile.inc

$(EXTRA_DATA): $(EXTRA_DATA)/usr/bin/buendia-mysql-dump $(EXTRA_DATA)/usr/bin/buendia-mysql-load $(EXTRA_DATA)/usr/bin/buendia-openmrs-account-setup

$(EXTRA_DATA)/usr/bin/buendia-mysql-dump:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/openmrs_dump $@
	chmod 755 $@

$(EXTRA_DATA)/usr/bin/buendia-mysql-load:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/openmrs_load $@
	chmod 755 $@

$(EXTRA_DATA)/usr/bin/buendia-openmrs-account-setup:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/openmrs_account_setup $@
	chmod 755 $@
