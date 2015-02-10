FROM google/debian:wheezy
RUN apt-get update
RUN apt-get install -y gdebi-core
ADD *.deb /tmp/
RUN gdebi -n /tmp/*.deb
