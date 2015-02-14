#!/bin/bash
# Produces a cleaned snapshot of an OpenMRS database at /tmp/clean-dump.zip

cd $(dirname $0)
set -e

SRC_HOST=dev.projectbuendia.org
SRC_PORT=9022
SRC_USER=jenkins  # a Unix account on SRC_HOST to which we have ssh access
SRC_DATABASE=openmrs  # the MySQL database on SRC_HOST to take a snapshot of
SRC=$SRC_USER@$SRC_HOST

WORK_HOST=dev.projectbuendia.org
WORK_PORT=9022
WORK_USER=jenkins  # a Unix account on WORK_HOST to which we have ssh access
WORK_DATABASE=openmrs_clean  # a MySQL database on WORK_HOST as a scratch area
WORK=$WORK_USER@$WORK_HOST

if [ $SRC_HOST:$SRC_DATABASE = $WORK_HOST:$WORK_DATABASE ]; then
    echo "Source database and working database should not be the same;"
    echo "the cleaning operation would mess up the source database."
    exit 2
fi

# Dump the database on SRC_HOST.
cat <<EOF | ssh -p $SRC_PORT $SRC bash
. /usr/share/buendia/utils.sh
export MYSQL_USER=root MYSQL_PASSWORD=\$MYSQL_ROOT_PASSWORD
buendia-mysql-dump $SRC_DATABASE dump.zip
EOF

# If necessary, copy the dump file over to the WORK_HOST.
if [ $SRC != $WORK ]; then
    scp -P $SRC_PORT $SRC:dump.zip /tmp/dump.zip
    scp -P $WORK_PORT /tmp/dump.zip $WORK:dump.zip
fi

# No more touching the SRC after this point!
SRC_HOST= SRC_PORT= SRC_USER= SRC_DATABASE= SRC=

# Load the dump into the scratch area.
cat <<EOF | ssh -p $WORK_PORT $WORK bash
. /usr/share/buendia/utils.sh
export MYSQL_USER=root MYSQL_PASSWORD=\$MYSQL_ROOT_PASSWORD
buendia-mysql-load -f $WORK_DATABASE dump.zip
EOF

# Clear out dev/test data (patients, observations, etc.) to put the database
# back in a starting state.
scp -P $WORK_PORT clear_server.sql $WORK:
cat <<EOF | ssh -p $WORK_PORT $WORK bash
. /usr/share/buendia/utils.sh
mysql -uroot -p\$MYSQL_ROOT_PASSWORD $WORK_DATABASE < clear_server.sql
EOF

# Dump the cleaned database.
cat <<EOF | ssh -p $WORK_PORT $WORK bash
. /usr/share/buendia/utils.sh
export MYSQL_USER=root MYSQL_PASSWORD=\$MYSQL_ROOT_PASSWORD
buendia-mysql-dump $WORK_DATABASE clean-dump.zip
EOF

# Copy the clean dump to the local machine.
scp -P $WORK_PORT $WORK:clean-dump.zip /tmp/clean-dump.zip
