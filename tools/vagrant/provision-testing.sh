#/bin/bash
# apt-get install -y apt-transport-https
echo "deb [trusted=yes] http://download.buendia.org/deb unstable main java" >/etc/apt/sources.list.d/buendia-github.list

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -f -y
apt-get upgrade -y
apt-get install -y buendia-site-test-integration buendia-dashboard buendia-backup buendia-monitoring buendia-server buendia-pkgserver

# Set hostname
sed -i -e "s/$(hostname)/buendia-testing/g" /etc/hosts /etc/hostname
hostname -F /etc/hostname
