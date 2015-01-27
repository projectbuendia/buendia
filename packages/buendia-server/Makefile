export PACKAGE_VERSION := 0.1
include ../Makefile.vars

OMAD_DIR="../../projectbuendia.openmrs/omod/target"

all: $(PACKAGE)

$(PACKAGE): control/* $(FILES)
	rm root/usr/share/buendia/openmrs/projectbuendia-*.omad
	cp $(OMAD_DIR)/projectbuendia-*.omad root/usr/share/buendia/openmrs/
	$(TOOLS)/mkdeb $@ control root
