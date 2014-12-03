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

out=`mktemp -d tmp.XXXXXXXXXX`

if [ "$ext" == "zip" ]; then
	echo -n "Extracting zip file..."
	unzip $archive -d $out > /dev/null
	print_success $?
fi

if [ "$ext" == "tar" ]; then
	echo -n "Extracting tar file..."
	tar -xf $archive -C $out > /dev/null
	print_success $?
fi

if [ "$ext" == "tgz" ] || [ "$ext" == "gz" ]; then
	echo -n "Extracting tar.gz file..."
	tar -xzf $archive -C $out > /dev/null
	print_success $?
fi

if [ "$ext" == "bz2" ]; then
	echo -n "Extracting tar.bz2 file..."
	tar -xjf $archive -C $out > /dev/null
	print_success $?
fi

echo -n "Uploading extracted files to 104.155.15.141..."
scp -r $out/* duserver.projectbuendia.org:/var/www/versions/ > /dev/null
print_success $?

echo -n "Removing extracted files..."
rm -R $out
print_success $?
