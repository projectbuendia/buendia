# Custom Docker images for build automation

Since OpenMRS 1.x relies on Java 7, we maintain our own custom Docker base
images for build automation.

`make` builds a Debian stretch image with OpenJDK 7 and other build prereqs for
Buendia. Additional packages have been included to satisfy [CircleCI
requirements](https://circleci.com/docs/2.0/custom-images/#required-tools-for-primary-containers).

`make push` will push the tagged image to Docker Hub.
