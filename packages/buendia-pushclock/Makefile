VERSION=1.0

export TOOLS := $(shell pwd)/../../tools
$(shell hash)

all: buendia-pushclock_$(VERSION).deb

buendia-pushclock_$(VERSION).deb: root/usr/src/pushclock.c control/*
	touch root/usr/bin/pushclock
	$(TOOLS)/mkdeb $@ control root
