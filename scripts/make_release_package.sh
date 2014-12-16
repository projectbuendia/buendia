#!/bin/bash

start=$(pwd)
cd $(dirname $0)
cd ..  # root directory is one up from scripts/
root=$(pwd)
cd $start

app_build_number=$1
server_build_number=$2
clean_dump_zip=$3
site=$4

if [ "$1" == "-h" -o -z "$site" ]; then
  echo "Usage: $0 <app-build-number> <server-build-number> <clean-dump.zip> <site>"
  echo
  echo "Constructs a zip file containing the specified builds of the app and"
  echo "the server module, using the specified cleaned database dump and the"
  echo "site-specific initialization SQL for the specified site, together with"
  echo "the scripts needed to set up the Edison.  Example:"
  echo
  echo "    ./make-release-package.sh 456 123 /tmp/dec14-clean.zip kailahun"
  echo
  echo "This produces buendia-a456-s123-dec14-kailahun.zip; then, to prepare"
  echo "a new Edison server, transfer the zip file to a workstation and do:"
  echo
  echo "    unzip buendia-a456-s123-dec14-kailahun.zip"
  echo "    cd buendia-a456-s123-dec14-kailahun"
  echo "    ./setup rootpassword GoogleGuest"
  exit 1
fi

host_port=jenkins.projectbuendia.org:1337
app_apk=buendia-app_${app_build_number}.apk
server_omod=buendia-server_${server_build_number}.omod
site_sql=site_$site.sql
dump=$(basename $clean_dump_zip)
dump=${dump%.zip}
dump=${dump%-clean*}
package_name=buendia-a${app_build_number}-s${server_build_number}-$dump-$site
work_dir=/tmp/buendia-package.$$/$package_name
clean_dump_name=$(basename $clean_dump_zip)

if [ ! -f $root/scripts/$site_sql ]; then
  echo Unknown site name: $site
  exit 1
fi

mkdir -p $work_dir
cp -pr $root/platforms $root/scripts $work_dir
mkdir $work_dir/omods
curl -o $work_dir/$app_apk http://$host_port/app/$app_apk
curl -o $work_dir/omods/$server_omod http://$host_port/server/$server_omod
cp $root/modules/xforms-4.3.1.jar $work_dir/omods/xforms-4.3.1.omod
cp $clean_dump_zip $work_dir/$clean_dump_name
cp $root/scripts/$site_sql $work_dir/

cat <<'EOF' > $work_dir/setup
#!/bin/bash

cd $(dirname $0)
root=$(pwd)
password=$1
ssid=$2
psk=$3

if [ "$1" == "-h" -o -z "$ssid" ]; then
  echo "Usage: $0 <new-admin-password> <ssid> [<psk-password>]"
  echo
  echo "Completely erases and sets up an Edison from scratch, first performing"
  echo "a firmware update and then installing all necessary applications,"
  echo "loading the database from a dump file, and installing OpenMRS modules."
  echo "The root password and all application administrator passwords will be"
  echo "set to <new-admin-password>."
  echo
  echo "Requires a wifi network with Internet access (which can be a wifi"
  echo "network with no password or with a PSK password)."
  exit 1
fi

EOF
cat <<EOF >> $work_dir/setup
clean_dump_name="$clean_dump_name"
site_sql="$site_sql"
EOF
cat <<'EOF' >> $work_dir/setup

platforms/edison-yocto/setup-new-edison "$password" "$root/omods" "$root/$clean_dump_name" "$root/$site_sql" "$ssid" "$psk"
EOF

chmod 755 $work_dir/setup

cd $work_dir/..
zip -r ${package_name}.zip $package_name
cd $start
mv $work_dir/../${package_name}.zip .
rm -r $(dirname $work_dir)
ls -l ${package_name}.zip
