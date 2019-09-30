#/bin/bash

# Incredibly, as of 25 June 2019, openjdk-7 seems to have been removed from experimental.
# Ensure that we can install OpenJDK 7 from Debian experimental and other prereqs from sid
# cp -av /mnt/buendia/tools/apt/* /etc/apt/

# Get apt-transport-https so that we can apt-get install from an https server
# apt-get update
# apt-get install apt-transport-https

# Use our private archive for openjdk-7 and tomcat
echo "deb [trusted=yes] http://download.buendia.org/deb stable java" > /etc/apt/sources.list.d/openjdk-7.list
echo "deb http://security.debian.org/debian-security stretch/updates experimental" > /etc/apt/sources.list.d/wpasupplicant.list
apt-get update

export DEBIAN_FRONTEND=noninteractive

# Install OpenJDK 7 *before* installing Maven; else Maven will pull in OpenJDK 8
apt-get install -y openjdk-7-jdk
apt-get install -y maven mysql-server python-mysqldb zip unzip git curl

# Set hostname
sed -i -e "s/$(hostname)/buendia-build/g" /etc/hosts /etc/hostname
hostname -F /etc/hostname
