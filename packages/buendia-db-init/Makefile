include ../Makefile.inc

$(EXTRA_ROOT): $(EXTRA_ROOT)/usr/share/buendia/db/init.zip

$(EXTRA_ROOT)/usr/share/buendia/db/init.zip: $(TOP)/db-snapshot/ID $(TOP)/db-snapshot/*.sql $(TOP)/db-snapshot/*.txt
	mkdir -p $$(dirname $@)
	zip -j $@ $^
