include ../Makefile.inc

$(EXTRA_DATA): $(EXTRA_DATA)/usr/share/buendia/db/init.zip

$(TOP)/db-snapshot/ID:
	cd $(TOP); git submodule update --init db-snapshot

$(EXTRA_DATA)/usr/share/buendia/db/init.zip: $(TOP)/db-snapshot/ID $(TOP)/db-snapshot/*.sql $(TOP)/db-snapshot/*.txt
	@mkdir -p $$(dirname $@)
	@echo Packing up init.zip...
	@zip -qj $@ $^
	@ls -l $@
