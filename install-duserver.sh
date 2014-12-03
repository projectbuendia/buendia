function print_success {
	if [ $1 -eq 0 ]; then
		echo "OK"
	else
		echo "FAIL"
		exit $STATUS
	fi
}

echo -n "Installing nginx..."
apt-get -y install nginx > /dev/null
print_success $?

echo -n "Adapting configuration..."
cat <<EOF > /etc/nginx/sites-available/default
server {
    root /var/www/;
    index index.html index.htm versions.json;

    server_name localhost;

    location /versions/ {
        allow all;
    }
}
EOF
print_success $?

if [ ! -d "/var/www" ]; then
	echo -n "Create root directory for nginx-served files..."
	mkdir /var/www
	print_success $?
	chown www-data:www-data /var/www
fi

if [ ! -d "/var/www/versions" ]; then
	echo -n "Create root directory for versions..."
	mkdir /var/www/versions
	print_success $?
	chown www-data:www-data /var/www/versions
	chmod ug+rw /var/www/versions
fi

if [ ! -d "/var/www/versions/openmrs" ]; then
	echo -n "Create directory for openmrs versions..."
	mkdir /var/www/versions/openmrs
	print_success $?
	chown www-data:www-data /var/www/versions/openmrs
	chmod ug+rw /var/www/versions/openmrs
fi

if [ ! -d "/var/www/versions/androidclient" ]; then
	echo -n "Create directory for androidclient versions..."
	mkdir /var/www/versions/androidclient
	print_success $?
	chown www-data:www-data /var/www/versions/androidclient
	chmod ug+rw /var/www/versions/androidclient
fi

echo -n "Starting nginx..."
/etc/init.d/nginx start > /dev/null
print_success $?


#TODO: wget a script that needs to be run when a usb drive is entered
#TODO: add udev rule file that triggers the script to be run on 'add'
