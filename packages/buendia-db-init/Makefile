include ../Makefile.inc

$(EXTRA_DATA): $(EXTRA_DATA)/usr/share/buendia/db/init.zip

$(EXTRA_DATA)/usr/share/buendia/db/init.zip: $(TOP)/db-snapshot/ID $(TOP)/db-snapshot/*.sql $(TOP)/db-snapshot/*.txt
	mkdir -p $$(dirname $@)
	zip -j $@ $^
