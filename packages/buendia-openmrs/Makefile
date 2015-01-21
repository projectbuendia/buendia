export PACKAGE_VERSION := 0.1
include ../Makefile.vars

EXTRA_ROOT=/tmp/buendia-openmrs
WAR_URL=http://sourceforge.net/projects/openmrs/files/releases/OpenMRS_Platform_1.10.1/openmrs.war/download
WAR_PATH=$(EXTRA_ROOT)/var/lib/tomcat7/webapps/openmrs.war
WAR_MD5=9fcad32a937bc433e4e866c56428b8be

all: $(PACKAGE)

.FORCE:

$(WAR_PATH): .FORCE
	$(TOOLS)/fetch_or_skip $(WAR_URL) $(WAR_PATH) $(WAR_MD5)

$(PACKAGE): control/* $(FILES) $(WAR_PATH)
	$(TOOLS)/mkdeb $@ control root $(EXTRA_ROOT)
