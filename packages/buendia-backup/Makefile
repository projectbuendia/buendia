include ../Makefile.inc

$(EXTRA_ROOT): $(EXTRA_ROOT)/usr/bin/buendia-mysql-dump

$(EXTRA_ROOT)/usr/bin/buendia-mysql-dump:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/openmrs_dump $@
