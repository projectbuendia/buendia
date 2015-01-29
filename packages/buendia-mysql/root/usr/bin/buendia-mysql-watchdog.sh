#!/bin/bash

MYSQL_PID=/var/run/mysqld/mysqld.pid

if ! service mysqld status > /dev/null 2>&1 ; then
	# Ensure we keep timestamps
	cp="cp --preserve=timestamps"
	tar="tar --atime-preserve"

	# Create directory to store dump
	DUMPDIR=/var/log/mysql_watchdog/$(date +"%Y-%m-%d.%H.%M")
	mkdir -p $DUMPDIR

	# Notify that we are dumping
	now=$(date); echo "$now >> MySQL is not running, dumping to $DUMPDIR"

	# Store the PID file, if we have one
	if [ -f "$MYSQL_PID" ]; then
		$cp $MYSQL_PID $DUMPDIR/mysqld.pid > /dev/null
	fi
	# Save a copy of the mysql logs
	$tar -czf $DUMPDIR/mysql_logs.tar.gz /var/log/mysql.* /var/log/mysql/* > /dev/null 2>&1
	# Save a copy of the system logs
	$tar -czf $DUMPDIR/system_logs.tar.gz /var/log/syslog /var/log/*.log \
		/var/log/dmesg /var/log/messages > /dev/null 2>&1

	# Try to restart MySQL
	if service mysql restart; then
		now=$(date); echo "$now >> MySQL has been restarted."
	else
		now=$(date); echo "$now >> MySQL could not be restarted."
	fi
fi

