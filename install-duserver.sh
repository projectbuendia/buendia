#!/bin/bash
#
# Installation script for a dynamic update server.
# Run with sudo.

function print_success {
	if [ $1 -eq 0 ]; then
		echo "OK"
	else
		echo "FAIL"
		exit $STATUS
	fi
}

if [ ! -e "/etc/init.d/nginx" ]; then
	echo -n "Installing nginx..."
	apt-get -y install nginx > /dev/null
	STATUS=$?
	if [ $STATUS -eq 100 ]; then
		echo "deb http://nginx.org/packages/debian/ wheezy nginx" >> /etc/apt/sources.list
		echo "deb-src http://nginx.org/packages/debian/ wheezy nginx" >> /etc/apt/sources.list
		apt-get update > /dev/null
		apt-get -y install nginx > /dev/null
		print_success $?
	else
		print_success $STATUS
	fi
fi

echo -n "Adding configuration..."
cat <<EOF > /etc/nginx/sites-available/duserver.conf
server {
    root /var/www/modules;
    server_name packages.*;

    location / {
        allow all;
    }
}
EOF
print_success $?

echo -n "Enabling configuration..."
ln -s /etc/nginx/sites-available/duserver.conf /etc/nginx/sites-enabled/ > /dev/null
print_success $?

if [ ! -d "/var/www" ]; then
	echo -n "Create root directory for nginx-served files..."
	mkdir /var/www
	print_success $?
	chown www-data:www-data /var/www
fi

if [ ! -d "/var/www/packages" ]; then
	echo -n "Create root directory for packages..."
	mkdir /var/www/packages
	print_success $?
	chown www-data:www-data /var/www/packages
	chmod ug+rw /var/www/packages
fi

echo -n "Starting nginx..."
/etc/init.d/nginx start > /dev/null
print_success $?

#TODO: wget a script that needs to be run when a usb drive is entered
#TODO: add udev rule file that triggers the script to be run on 'add'
