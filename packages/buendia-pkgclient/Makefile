export PACKAGE_VERSION := 0.1
export PACKAGE_NAME := $(shell basename $$(pwd))
export TOOLS := $(shell pwd)/../../tools
export FILES := $(shell find root)

PACKAGE=$(PACKAGE_NAME)_$(PACKAGE_VERSION).deb

all: $(PACKAGE)

$(PACKAGE): control/* $(FILES)
	$(TOOLS)/mkdeb $@ control root

clean: $(PACKAGE)
	rm *.deb

docker: $(PACKAGE)
	docker build .
