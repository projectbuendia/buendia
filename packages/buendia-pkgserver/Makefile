export PACKAGE_VERSION := 1.0
export PACKAGE_NAME := $(shell basename $$(pwd))
export TOOLS := $(shell pwd)/../../tools
export FILES := $(shell find root)

PACKAGE=$(PACKAGE_NAME)_$(PACKAGE_VERSION).deb

all: $(PACKAGE)

$(PACKAGE): control/* $(FILES)
	$(TOOLS)/mkdeb $@ control root

clean: $(PACKAGE)
	rm $(PACKAGE)

docker: $(PACKAGE)
	docker build .
