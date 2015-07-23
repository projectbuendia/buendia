include ../Makefile.inc

# Add the openmrs.war file, fetched at build time.
WAR_URL=http://sourceforge.net/projects/openmrs/files/releases/OpenMRS_Platform_1.10.2/openmrs.war/download
WAR_MD5=7e8d4054d86d796018c9fca31f4d33b2

$(EXTRA_DATA): $(EXTRA_DATA)/var/lib/tomcat7/webapps/openmrs.war

$(EXTRA_DATA)/var/lib/tomcat7/webapps/openmrs.war:
	$(TOOLS)/fetch $(WAR_URL) $(WAR_MD5) $@
