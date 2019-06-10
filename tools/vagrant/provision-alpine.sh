#!/bin/bash

MAVEN_VERSION=3.6.1
# FIXME: this mirror link shouldn't be hardcoded
MAVEN_DOWNLOAD=http://mirror.cogentco.com/pub/apache/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.zip

apk update
apk add openjdk7 zip unzip git mariadb mariadb-client py-mysqldb

# Alpine 3.8 provides Maven but it insists on pulling in OpenJDK 8, so we'll just install it manually
wget -O/tmp/maven.zip ${MAVEN_DOWNLOAD}
mkdir -p /opt
cd /opt && unzip /tmp/maven.zip
rm -f /tmp/maven.zip
echo "export PATH=\$PATH:/opt/apache-maven-${MAVEN_VERSION}/bin" > /etc/profile.d/maven.sh

mysql_install_db --user=mysql --datadir=/var/lib/mysql
rc-service mariadb start
rc-update add mariadb default
