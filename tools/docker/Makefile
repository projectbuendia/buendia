IMAGE := projectbuendia/debian-stretch
VERSION := 1.0.1
TAG := $(IMAGE):$(VERSION)
CONTEXT := $(shell mktemp -d)

all:
	cp -a Dockerfile ../apt $(CONTEXT)
	cd $(CONTEXT) && docker build -t $(TAG) .
	rm -rf $(CONTEXT)

push:
	docker push $(TAG) || echo "Do you need to run \`docker login\`?"

.DELETE_ON_ERROR:
	rm -rf $(CONTEXT)
