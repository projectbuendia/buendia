#!/bin/bash

CATALINA_PID=/var/run/tomcat7.pid

if service tomcat7 status > /dev/null 2>&1 ; then
	# Ensure we keep timestamps
	cp="cp --preserve=timestamps"
	tar="tar --atime-preserve"

	# Create directory to store dump
	DUMPDIR=/var/log/tomcat_watchdog/$(date +"%Y-%m-%d.%H.%M")
	mkdir -p $DUMPDIR

	# Notify that we are dumping
	now=$(date); echo "$now >> Tomcat7 is not running, dumping to $DUMPDIR"

	# Store the datetime information
	date > $DUMPDIR/datetime
	# Store the PID file, if we have one
	if [ -f "$CATALINA_PID" ]; then
		$cp $CATALINA_PID $DUMPDIR/tomcat7.pid
	fi
	# Save a copy of the tomcat logs
	$tar -czf $DUMPDIR/tomcat7_logs.tar.gz /var/log/tomcat7/*
	# Save a copy of the tomcat files
	$tar -czf $DUMPDIR/tomcat7_files.tar.gz /usr/share/tomcat7/*
	# Save a copy of the syslog
	$tar -czf $DUMPDIR/syslog.tar.gz /var/log/syslog

	# Try to restart tomcat
	now=$(date); echo "Restarting tomcat7..."
	if service tomcat7 restart; then
		now=$(date); echo "$now >> Tomcat7 has been restarted."
	else:
		now=$(date); echo "$now >> Tomcat7 could not be restarted."
	fi
fi

