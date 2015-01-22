export PACKAGE_VERSION := 0.1
include ../Makefile.vars

$(PACKAGE): control/* $(FILES)
	$(TOOLS)/mkdeb $@ control root
