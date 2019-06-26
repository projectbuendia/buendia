#/bin/bash
apt-get install -y apt-transport-https
echo "deb [trusted=yes] https://projectbuendia.github.io/builds/packages unstable main java" >/etc/apt/sources.list.d/buendia-github.list
apt-get update
apt-get install -y buendia-server buendia-site-test buendia-dashboard
