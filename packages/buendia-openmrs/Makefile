include ../Makefile.inc

SERVER_ID=server

$(EXTRA_DATA): $(EXTRA_DATA)/var/lib/tomcat7/webapps/openmrs.war

$(EXTRA_DATA)/var/lib/tomcat7/webapps/openmrs.war:
	$(TOOLS)/openmrs_ensure_sdk
	mkdir -p $(shell dirname $@)
	cp -av $(HOME)/openmrs/$(SERVER_ID)/*.war $@
