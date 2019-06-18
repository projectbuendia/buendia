# Setting up Buendia on a fresh Debian stretch install

## [Optional]: Set up a fresh Debian stretch install in Vagrant

    vagrant init debian/stretch64
    vagrant up
    vagrant ssh

## Install apt configuration for OpenJDK 7

Copy [this file](../tools/apt/preferences.d/openjdk-7) to `/etc/apt/preferences.d/openjdk-7`,
[this file](../tools/apt/sources.list.d/openjdk-7.list) to `/etc/apt/sources.list.d/openjdk-7.list`,
and [this file](../tools/apt/apt.conf.d/99norecommends) to `/etc/apt/apt.conf.d/99norecommends`.

If you're running Debian in Vagrant, and you've done `vagrant up` in your `buendia/` directory, you can simply:

    sudo cp -a /vagrant/tools/apt/* /etc/apt/

## Install system requirements

    sudo apt update
    sudo apt upgrade
    sudo apt install -y openjdk-7-jdk
    sudo apt install -y maven mysql-server python-mysqldb git build-essential zip unzip curl

The last couple steps will download and install about 250 MB of packages. Note,
we download and install OpenJDK 7 before installing Maven; otherwise apt will
try to pull in OpenJDK 8 as well.

## Get the server source code

    git clone https://github.com/projectbuendia/buendia
    cd buendia

## Set up the server database as a dev site

    tools/openmrs_setup dev

This process takes several minutes, and has a number of interactive steps:

1. Hit return when prompted for a MySQL password.
2. Select `n` to decline sending SDK stats to OpenMRS. (or hit return to accept the default if you're feeling nice)
3. Hit return when prompted ready by the script.
4. Select `2` for "platform".
5. Select `7` for "other" platform version, and then manually enter the correct platform version (`1.10.2` as of this note)
6. Hit enter to accept the default port number.
7. Hit enter to accept no debugging by default.
8. Select `2` to use pre-installed MySQL 5.6
9. Select `1` to use current `JAVA_HOME` (but doublecheck that it's `/usr/lib/jvm/java-7-openjdk-amd64/jre`)

## Build the Buendia server

    tools/openmrs_build

This process takes >5 minutes to download and build a bunch of Java packages.
At some point, you should see some unit tests run and pass.

## Run the server

    tools/openmrs_run &

After about a minute, you should see a log entry reading `INFO: Starting ProtocolHandler ["http-bio-9000"]`.

## Confirm working

    curl -L http://localhost:9000/openmrs/

You should see some HTML. You can shutdown the server with `kill -HUP %1`.
