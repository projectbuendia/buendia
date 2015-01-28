#!/bin/bash

CATALINA_PID=/var/run/tomcat7.pid

if ! service tomcat7 status > /dev/null 2>&1 ; then
	# Ensure we keep timestamps
	cp="cp --preserve=timestamps"
	tar="tar --atime-preserve"

	# Create directory to store dump
	DUMPDIR=/var/log/tomcat_watchdog/$(date +"%Y-%m-%d.%H.%M")
	mkdir -p $DUMPDIR

	# Notify that we are dumping
	now=$(date); echo "$now >> Tomcat7 is not running, dumping to $DUMPDIR"

	# Store the PID file, if we have one
	if [ -f "$CATALINA_PID" ]; then
		$cp $CATALINA_PID $DUMPDIR/tomcat7.pid > /dev/null
	fi
	# Save a copy of the tomcat logs
	$tar -czf $DUMPDIR/tomcat7_logs.tar.gz /var/log/tomcat7/* > /dev/null 2>&1
	# Save any tomcat module logs, if any can be found
	MODULE_LOGS=$(find -L /usr/share/tomcat7 -name *.log)
	if [ -n "$MODULE_LOGS" ]; then
		$tar -czhf $DUMPDIR/tomcat7_module_logs.tar.gz $MODULE_LOGS \
			> /dev/null 2>&1
	fi
	# Save a copy of the system logs
	$tar -czf $DUMPDIR/system_logs.tar.gz /var/log/syslog /var/log/*.log \
		/var/log/dmesg /var/log/messages > /dev/null 2>&1

	# Try to restart tomcat
	if service tomcat7 restart; then
		now=$(date); echo "$now >> Tomcat7 has been restarted."
	else
		now=$(date); echo "$now >> Tomcat7 could not be restarted."
	fi
fi

