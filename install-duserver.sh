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

echo -e "Welcome to the installation script for the package server.\n"
echo "What is the domain at which the package server will be made available?"
echo -n "(Default: packages.local) "
read base_url

echo -n "Updating apt-get..."
apt-get update
print_success $?

echo -n "Installing nginx..."
if [ ! -e "/etc/init.d/nginx" ]; then
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
else
	echo "SKIP (nginx is already installed)"
fi

echo -n "Installing python..."
which python > /dev/null
if [ $? -eq 1 ]; then
	apt-get -y install python > /dev/null
	print_success $?
else
	echo "SKIP (python is already installed)"
fi

echo -n "Adding configuration..."
if [ ! -e "/etc/nginx/sites-available/duserver.conf" ]; then
	cat <<EOF > /etc/nginx/sites-available/duserver.conf
	server {
		root /var/www/packages;
		server_name $base_url;

		location / {
			allow all;
		}
	}
EOF
	print_success $?
else
	echo "SKIP (file already exists)"
fi

echo -n "Enabling configuration..."
if [ ! -e "/etc/nginx/sites-enabled/duserver.conf" ]; then
	ln -s /etc/nginx/sites-available/duserver.conf /etc/nginx/sites-enabled/ > /dev/null
	print_success $?
else
	echo "SKIP (link already exists)"
fi

echo -n "Create root directory for nginx-served files..."
if [ ! -d "/var/www" ]; then
	mkdir /var/www
	print_success $?
	chown www-data:www-data /var/www
else
	echo "SKIP (directory already exists)"
fi

echo -n "Create root directory for packages..."
if [ ! -d "/var/www/packages" ]; then
	mkdir /var/www/packages
	print_success $?
	chown www-data:www-data /var/www/packages
	chmod ug+rw /var/www/packages
else
	echo "SKIP (directory already exists)"
fi

echo -n "(Re)starting nginx..."
/etc/init.d/nginx restart > /dev/null
print_success $?

echo -n 'Add DUSERVER_PACKAGE_DIR variable to the environment...'
grep 'export DUSERVER_PACKAGE_DIR' /etc/profile > /dev/null
if [ $? -eq 1 ]; then
	echo "export DUSERVER_PACKAGE_DIR=/var/www/packages" >> /etc/profile
	print_success $?
else
	echo "SKIP (variable is already added)"
fi

echo -n 'Add DUSERVER_PACKAGE_BASE_URL variable to the environment...'
grep 'export DUSERVER_PACKAGE_BASE_URL' /etc/profile > /dev/null
if [ $? -eq 1 ]; then
	echo "export DUSERVER_PACKAGE_BASE_URL=\"http://$base_url\"" >> /etc/profile
	print_success $?
else
	echo "SKIP (variable is already added)"
fi

echo -n "Reload environment..."
source /etc/profile
print_success $?

echo -n "Downloading duserver_make_index.py..."
if [ ! -e "/usr/local/bin/duserver_make_index.py" ]; then
	curl "https://raw.githubusercontent.com/ProjectBuendia/buendia-scripts/master/duserver_make_index.py" \
		-o "/usr/local/bin/duserver_make_index.py" > /dev/null
	print_success $?
	chmod +x "/usr/local/bin/duserver_make_index.py"
else
	echo "SKIP (script already exists)"
fi

echo -n "Creating usb-import script..."
if [ ! -e "/usr/local/bin/import-updates-from-usb" ]; then
	cat <<EOF > /usr/local/bin/import-updates-from-usb
	#!/bin/bash
	mkdir /tmp/usb
	mount /dev/duserver_usb /tmp/usb
	ls -l /tmp/usb/*
	cp -R /tmp/usb/* /var/www/packages/
	umount /tmp/usb
	rmdir /tmp/usb
	su -u www-data duserver_make_index.py
EOF
	print_success $?
else
	echo "SKIP (script already exists)"
fi

echo -n "Adding udev rule for usb trigger..."
if [ ! -e "/etc/udev/rules.d/80-usb-add.rules" ]; then
	cat <<EOF > /etc/udev/rules.d/80-usb-add.rules
	KERNEL=="sd?1", SUBSYSTEMS=="usb", DRIVERS=="usb-storage", ACTION=="add", SYMLINK+="duserver_usb", RUN+="/usr/local/bin/import-updates-from-usb"
EOF
	print_success $?
else
	echo "SKIP (rule already exists)"
fi

echo -n "Restarting udev"
/etc/init.d/udev restart > /dev/null
print_success $?
