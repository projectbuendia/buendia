export PACKAGE_VERSION := 1.0
export PACKAGE_NAME := $(shell basename $$(pwd))
export TOOLS := $(shell pwd)/../../tools
export FILES := $(shell find root)

PACKAGE=$(PACKAGE_NAME)_$(PACKAGE_VERSION).deb

all: $(PACKAGE)

/tmp/openmrs.war:
	$(TOOLS)/fetch_unless_fetched http://sourceforge.net/projects/openmrs/files/releases/OpenMRS_Platform_1.10.1/openmrs.war/download /tmp/openmrs.war 9fcad32a937bc433e4e866c56428b8be

$(PACKAGE): control/* $(FILES) /tmp/openmrs.war
	$(TOOLS)/fetch_unless_fetched http://sourceforge.net/projects/openmrs/files/releases/OpenMRS_Platform_1.10.1/openmrs.war/download /tmp/openmrs.war 9fcad32a937bc433e4e866c56428b8be
	mkdir -p root/var/lib/tomcat7/webapps
	cp /tmp/openmrs.war root/var/lib/tomcat7/webapps/openmrs.war
	$(TOOLS)/mkdeb $@ control root
	rm root/var/lib/tomcat7/webapps/openmrs.war
	rmdir -p root/var/lib/tomcat7/webapps

