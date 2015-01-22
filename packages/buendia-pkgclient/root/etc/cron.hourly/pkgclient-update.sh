#!/bin/bash
. /usr/share/buendia/config.d/pkgclient

apt-get -qqy --allow-unauthenticated install $(cat /usr/share/buendia/packages.list.d/*)
