include ../Makefile.inc

$(EXTRA_DATA): $(EXTRA_DATA)/usr/bin/buendia-pkgserver-index-debs

$(EXTRA_DATA)/usr/bin/buendia-pkgserver-index-debs:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/index_debs $@
	chmod 755 $@

docker: $(PACKAGE)
	docker build .
