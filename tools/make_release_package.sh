#!/bin/bash

# TODO(kpy): This currently builds the zip file from a combination of
# externally built binaries (downloaded from Jenkins by build number)
# and files in the current working repo (platforms/ and tools/).
# This is bad -- it will be inconsistent when HEAD in the current working
# repo doesn't match the build numbers specified as arguments.  Instead,
# this should be a "make" target that builds everything based on the current
# working repo.  To enable this, we need to add android-client as a submodule
# so that this repo records the version of android-client that we've selected
# to go with this release, and then we can configure Jenkins to build the
# entire package whenever we apply an "rc" tag to this repo.

start=$(pwd)
cd $(dirname $0)
cd ..  # root directory is one up from tools/
root=$(pwd)
cd $start

client_rc_number=$1
server_rc_number=$2
clean_dump_zip=$3
site=$4

if [ "$1" == "-h" -o -z "$site" ]; then
  echo "Usage: $0 <client-rc-number> <server-rc-number> <clean-dump.zip> <site>"
  echo
  echo "Constructs a zip file containing the specified RC builds of the client"
  echo "the server module, using the specified cleaned database dump and the"
  echo "site-specific initialization SQL for the specified site, together with"
  echo "the scripts needed to set up the Edison.  Example:"
  echo
  echo "    ./make-release-package.sh 456 123 /tmp/dec14-clean.zip kailahun"
  echo
  echo "This produces buendia-c456-s123-dec14-kailahun.zip; then, to prepare"
  echo "a new Edison server, transfer the zip file to a workstation and do:"
  echo
  echo "    unzip buendia-c456-s123-dec14-kailahun.zip"
  echo "    cd buendia-c456-s123-dec14-kailahun"
  echo "    ./setup rootpassword GoogleGuest"
  exit 1
fi

host_port=jenkins.projectbuendia.org:1337
client_apk=buendia-client-rc_${client_rc_number}.apk
server_omod=buendia-server-rc_${server_rc_number}.omod
site_sql=site_$site.sql
dump=$(basename $clean_dump_zip)
dump=${dump%.zip}
dump=${dump%-clean*}
package_name=buendia-c${client_rc_number}-s${server_rc_number}-$dump-$site
work_dir=/tmp/buendia-package.$$/$package_name
clean_dump_name=$(basename $clean_dump_zip)

if [ ! -f $root/tools/$site_sql ]; then
  echo Unknown site name: $site
  exit 1
fi

set -e
mkdir -p $work_dir
cp -pr $root/platforms $root/tools $work_dir
mkdir $work_dir/omods
curl -o $work_dir/$client_apk http://$host_port/client-rc/$client_apk
unzip -tq $work_dir/$client_apk
curl -o $work_dir/omods/$server_omod http://$host_port/server-rc/$server_omod
unzip -tq $work_dir/omods/$server_omod
cp $root/modules/xforms-4.3.1.jar $work_dir/omods/xforms-4.3.1.omod
cp $clean_dump_zip $work_dir/$clean_dump_name
cp $root/tools/$site_sql $work_dir/

cat <<'EOF' > $work_dir/setup
#!/bin/bash

cd $(dirname $0)
root=$(pwd)
password=$1
setup_ssid_psk=$2
production_ssid_psk=$3

if [ "$1" == "-h" -o -z "$setup_ssid_psk" ]; then
  echo "Usage: $0 <new-admin-password> <setup-ssid>[:<psk-password>] <production-ssid>[:<psk-password>]"
  echo
  echo "Completely erases and sets up an Edison from scratch, first performing"
  echo "a firmware update and then installing all necessary applications,"
  echo "loading the database from a dump file, and installing OpenMRS modules."
  echo "The root password and all application administrator passwords will be"
  echo "set to <new-admin-password>."
  echo
  echo "<setup-ssid> should be a network with Internet access that the Edison"
  echo "will use during this setup process to download and install software."
  echo "<production-ssid> is the network that the Edison will be configured"
  echo "to join automatically on boot, in production.  Each network can be"
  echo "a wifi network with no password or with a PSK password."
  exit 1
fi

EOF
cat <<EOF >> $work_dir/setup
clean_dump_name="$clean_dump_name"
site_sql="$site_sql"
EOF
cat <<'EOF' >> $work_dir/setup

platforms/edison-yocto/setup-new-edison "$password" "$root/omods" "$root/$clean_dump_name" "$root/$site_sql" "$setup_ssid_psk" "$production_ssid_psk"
EOF

chmod 755 $work_dir/setup

cd $work_dir/..
zip -r ${package_name}.zip $package_name
cd $start
mv $work_dir/../${package_name}.zip .
rm -r $(dirname $work_dir)
ls -l ${package_name}.zip
