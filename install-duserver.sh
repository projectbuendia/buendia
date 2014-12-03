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

echo "Adapting configuration..."
cat <<EOF > /etc/nginx/sites-available/default
server {
	root /var/www/;
    index index.html index.htm;

	server_name localhost;

	location /versions/ {
			allow all;
	}
}
EOF
print_success $?

echo "Create root directory for nginx-served files..."
if [ ! -d "/var/www" ]; then
	mkdir /var/www
fi
print_success $?

echo "Create root directory for versions..."
if [ ! -d "/var/www/versions" ]; then
	mkdir /var/www/versions
fi
print_success $?

echo "Create directory for openmrs versions..."
if [ ! -d "/var/www/versions/openmrs" ]; then
	mkdir /var/www/versions/openmrs
fi
print_success $?

echo "Create directory for androidclient versions..."
if [ ! -d "/var/www/versions/androidclient" ]; then
	mkdir /var/www/versions/androidclient
fi
print_success $?

echo "Starting nginx..."
/etc/init.d/nginx start > /dev/null
print_success $?


#TODO: wget a script that needs to be run when a usb drive is entered
#TODO: add udev rule file that triggers the script to be run on 'add'
