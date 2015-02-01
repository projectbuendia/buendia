include ../Makefile.inc

$(EXTRA_ROOT): $(EXTRA_ROOT)/usr/bin/buendia-mysql-dump $(EXTRA_ROOT)/usr/bin/buendia-mysql-load

$(EXTRA_ROOT)/usr/bin/buendia-mysql-dump:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/openmrs_dump $@
	chmod 755 $@

$(EXTRA_ROOT)/usr/bin/buendia-mysql-load:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/openmrs_load $@
	chmod 755 $@
