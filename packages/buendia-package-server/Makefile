export PACKAGE_VERSION := 1.0
export PACKAGE_NAME := $(shell basename $$(pwd))
export TOOLS := $(shell pwd)/../../tools

PACKAGE=$(PACKAGE_NAME)_$(PACKAGE_VERSION).deb

all: $(PACKAGE)

$(PACKAGE): root/etc/nginx/sites-available/* root/usr/share/buendia/packages/* control/*
	$(TOOLS)/mkdeb $@ control root
