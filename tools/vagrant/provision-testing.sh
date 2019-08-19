#/bin/bash
apt-get install -y apt-transport-https
echo "deb [trusted=yes] https://projectbuendia.github.io/builds/packages unstable main java" >/etc/apt/sources.list.d/buendia-github.list
apt-get update
apt-get install -y buendia-server buendia-site-test buendia-dashboard

# Set hostname
sed -i -e "s/$(hostname)/buendia-testing/g" /etc/hosts /etc/hostname
hostname -F /etc/hostname
