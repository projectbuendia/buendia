#!/bin/bash

function print_success {
	if [ $1 -eq 0 ]; then
		echo "OK"
	else
		echo "FAIL"
		exit $STATUS
	fi
}

if [ "$#" -ne 1 ]; then
	echo "Usage: $0 <archive>"
	echo "Extracts the contents of the archive to the versions folder on the duserver."
	echo "Requires the user to have public/private key access to the server."
	exit
fi

archive=$1

ext="${1##*.}"

extractdir=`mktemp -d /tmp/tmp.XXXXXXXXXX`

if [ "$ext" == "zip" ]; then
	echo -n "Extracting zip file..."
	unzip $archive -d $extractdir > /dev/null
	print_success $?
fi

if [ "$ext" == "tar" ]; then
	echo -n "Extracting tar file..."
	tar -xf $archive -C $extractdir > /dev/null
	print_success $?
fi

if [ "$ext" == "tgz" ] || [ "$ext" == "gz" ]; then
	echo -n "Extracting tar.gz file..."
	tar -xzf $archive -C $extractdir > /dev/null
	print_success $?
fi

if [ "$ext" == "bz2" ]; then
	echo -n "Extracting tar.bz2 file..."
	tar -xjf $archive -C $extractdir > /dev/null
	print_success $?
fi

uploaddir=`ssh duserver.projectbuendia.org "mktemp -d /tmp/tmp.XXXXXXXXXX"`

echo -n "Uploading extracted files to a temporary folder on the duserver..."
scp -r $extractdir/* duserver.projectbuendia.org:$uploaddir > /dev/null
print_success $?

echo -n "Removing extracted files locally..."
rm -R $extractdir
print_success $?

echo -n "Deploying the extracted files..."
ssh -q duserver.projectbuendia.org <<ENDSSH > /dev/null
sudo chown -R www-data:www-data $uploaddir
sudo su www-data
cp -R $uploaddir/* /var/www/versions/
exit
sudo rm -R $uploaddir
ENDSSH
print_success $?
