include ../Makefile.inc

# Add the openmrs.war file, fetched at build time.
WAR_URL=http://sourceforge.net/projects/openmrs/files/releases/OpenMRS_Platform_1.10.1/openmrs.war/download
WAR_MD5=9fcad32a937bc433e4e866c56428b8be

$(EXTRA_ROOT): $(EXTRA_ROOT)/var/lib/tomcat7/webapps/openmrs.war

$(EXTRA_ROOT)/var/lib/tomcat7/webapps/openmrs.war:
	$(TOOLS)/fetch $(WAR_URL) $(WAR_MD5) $@
