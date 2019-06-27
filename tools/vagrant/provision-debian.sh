#/bin/bash

# Incredibly, as of 25 June 2019, openjdk-7 seems to have been removed from experimental.
# Ensure that we can install OpenJDK 7 from Debian experimental and other prereqs from sid
# cp -av /mnt/buendia/tools/apt/* /etc/apt/

# Get apt-transport-https so that we can apt-get install from an https server
apt-get update
apt-get install apt-transport-https

# Use our private archive for openjdk-7 and tomcat
echo "deb [trusted=yes] https://projectbuendia.github.io/builds/packages stable java" > /etc/apt/sources.list.d/openjdk-7.list
apt-get update

# Install OpenJDK 7 *before* installing Maven; else Maven will pull in OpenJDK 8
apt-get install -y openjdk-7-jdk
apt-get install -y maven mysql-server python-mysqldb zip unzip git curl
