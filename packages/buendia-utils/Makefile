include ../Makefile.inc

$(EXTRA_DATA): $(EXTRA_DATA)/usr/bin/buendia-warmup

$(EXTRA_DATA)/usr/bin/buendia-warmup: $(TOP)/tools/warmup
	mkdir -p $$(dirname $@)
	cp $< $@
	chmod 755 $@
