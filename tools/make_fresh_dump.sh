#!/bin/bash
# Produces a cleaned snapshot of an OpenMRS MySQL database at /tmp/cleaned.zip.

cd $(dirname $0)
set -e

SRC_HOST=104.155.15.141
SRC_USER=jenkins  # a Unix account on SRC_HOST to which we have ssh access
SRC_DATABASE=openmrs  # the MySQL database on SRC_HOST to take a snapshot of
SRC=$SRC_USER@$SRC_HOST

WORK_HOST=104.155.15.141  # 104.155.14.26
WORK_USER=jenkins  # a Unix account on WORK_HOST to which we have ssh access
WORK_DATABASE=scratch  # a MySQL database on WORK_HOST to use as a scratch area
WORK=$WORK_USER@$WORK_HOST

# Check local values of username and password are set.  These must be valid
# for both the SRC_DATABASE and the WORK_DATABASE.
if [ -z "$MYSQL_USER" ]; then
    echo "Need to set \$MYSQL_USER"
    exit 2
fi
if [ -z "$MYSQL_PASSWORD" ]; then
    echo "Need to set \$MYSQL_PASSWORD"
    exit 2
fi

if [ $SRC:$SRC_DATABASE == $WORK:$WORK_DATABASE ]; then
    echo "Source database and working database should not be the same;"
    echo "the cleaning operation would mess up the source database."
    exit 2
fi

# Prepend two lines to the openmrs_dump script and run it on SRC_HOST.
cat <(echo "export MYSQL_USER='$MYSQL_USER' MYSQL_PASSWORD='$MYSQL_PASSWORD'") \
    <(echo "set -- $SRC_DATABASE dump.zip") \
    openmrs_dump \
    | ssh $SRC bash

# If necessary, copy the dump file over to the WORK_HOST.
if [ $SRC != $WORK ]; then
    scp $SRC:dump.zip /tmp/dump.zip
    scp /tmp/dump.zip $WORK:dump.zip
fi

# No more touching the SRC!
SRC_HOST=
SRC_USER=
SRC_DATABASE=
SRC=

# Load the dump into the scratch area.
cat <(echo "export MYSQL_USER='$MYSQL_USER' MYSQL_PASSWORD='$MYSQL_PASSWORD'") \
    <(echo "set -- $WORK_DATABASE dump.zip") \
    openmrs_load \
    | ssh $WORK bash

# Clear out dev/test data (patients, observations, etc.) to put the database
# back in a starting state.
cat clear_server.sql \
    | ssh $WORK "mysql -u'$MYSQL_USER' -p'$MYSQL_PASSWORD' $WORK_DATABASE"

# Dump the cleaned database.
cat <(echo "export MYSQL_USER='$MYSQL_USER' MYSQL_PASSWORD='$MYSQL_PASSWORD'") \
    <(echo "set -- $WORK_DATABASE cleaned.zip") \
    openmrs_dump \
    | ssh $WORK bash

# Copy the clean dump to the local machine.
scp $WORK:cleaned.zip /tmp/cleaned.zip
