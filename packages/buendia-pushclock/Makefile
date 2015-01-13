export PACKAGE_VERSION=1.0
export TOOLS := $(shell pwd)/../../tools

all: buendia-pushclock_$(PACKAGE_VERSION).deb

buendia-pushclock_$(PACKAGE_VERSION).deb: root/usr/src/pushclock.c control/*
	touch root/usr/bin/pushclock
	$(TOOLS)/mkdeb $@ control root
