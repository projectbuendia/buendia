FROM debian:stretch-slim
LABEL Description="Project Buendia Debian build image" Vendor="Project Buendia" Version="1.1"
COPY apt/ /etc/apt/
# The extra mkdir step is a workaround for an error in update-alternatives
# running on stretch-slim:
#   https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=863199#28
RUN apt-get update && \
    apt-get -y upgrade && \
    mkdir -p /usr/share/man/man1 && \
    apt-get install -y openjdk-7-jdk && \
    apt-get install -y maven zip unzip git curl openssh-client make binutils git-restore-mtime
ENTRYPOINT /bin/bash
