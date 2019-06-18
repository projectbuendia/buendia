#/bin/bash
# Ensure that we can install OpenJDK 7 from Debian experimental and other prereqs from sid
cp -av /mnt/buendia/tools/apt/* /etc/apt/
apt-get update
# Install OpenJDK 7 *before* installing Maven; else Maven will pull in OpenJDK 8
apt-get install -y openjdk-7-jdk
apt-get install -y maven mysql-server python-mysqldb zip unzip git curl
