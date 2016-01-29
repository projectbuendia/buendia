Offline Flashing
Fabian Tamp
Jan 2016

Issues:
opkg.
debian packages.
Pings debian.org.


Downloads
Run the prep_for_offline_flash script

Debian server

brew install aptly gpg

gpg --no-default-keyring --keyring debian-archive-keyring.gpg --export | gpg --no-default-keyring --keyring trustedkeys.gpg --import

 gpg --gen-key

aptly mirror create wheezy-main http://ftp.ru.debian.org/debian/ wheezy main
aptly mirror create wheezy-updates http://ftp.ru.debian.org/debian/ wheezy-updates main

# This list was taken from install-buendia-base BASE_PACKAGES

FILTER="Priority (required) | Priority (important) | Priority (standard) | coreutils | less | curl | netcat | dnsutils | zip | unzip | git | screen | ssh | vim | strace | rsync | python2.7 | python-pip | openjdk-7-jdk | gcc | libc-dev | apt-utils | nginx | mysql-server | tomcat7 | tomcat7-admin | libsemanage1 | libept1.4.12 | cron-daemon | libnfnetlink0 | libxtables7 | linux-kernel-log-daemon | linux32 | schedutils | system-log-daemon | insserv | watch | fcgiwrap | dnsmasq | ntp | python-mysqldb"
aptly mirror edit -architectures=i386 -filter="$FILTER" -filter-with-deps wheezy-main
aptly mirror update wheezy-main
aptly publish drop wheezy
aptly snapshot drop wheezy-main-edison
aptly snapshot create wheezy-main-edison from mirror wheezy-main
aptly publish snapshot wheezy-main-edison

aptly publish drop buendia-stable
aptly repo drop buendia-release
aptly repo create buendia-release
aptly repo edit -distribution=buendia-stable -component=main buendia-release
aptly repo add buendia-release ~/Code/buendia/buendia/packages
aptly publish repo -architectures=all buendia-release

aptly serve # start server on :8080.

Bits

Setup static server for the “bits” folder on :6060.
e.g.
ruby -run -ehttpd . -p6060


# opkg

Copy the ipk files across.

```
scp *.ipk root@192.168.2.15
```

# ssh, and:

```
opkg install *.ipk
```
