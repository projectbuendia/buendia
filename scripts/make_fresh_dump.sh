#!/bin/bash

SRC_ZONE="europe-west1-b"
SRC_INSTANCE="openmrs-2"
TMP_ZONE="europe-west1-b"
TMP_INSTANCE="jenkins-3"
dump="complete.zip"
cleandump="clean.zip"

# CHeck local values of username and password are set.
if [ -z "$MYSQL_USER" ]; then
    echo "Need to set \$MYSQL_USER"
    exit 2
fi
if [ -z "$MYSQL_PASSWORD" ]; then
    echo "Need to set \$MYSQL_PASSWORD"
    exit 2
fi

# Run OpenMRS dump on openmrs-2 VM instance, after setting username and password from local values
cat <(echo "export MYSQL_USER=\"$MYSQL_USER\"") \
<(echo "export MYSQL_PASSWORD=\"$MYSQL_PASSWORD\"") \
openmrs_dump \
  | gcloud compute ssh --zone $SRC_ZONE $SRC_INSTANCE --command "bash -s openmrs $dump"

# copy file to local computer
gcloud compute copy-files $SRC_INSTANCE:$dump /tmp/$dump --zone $SRC_ZONE

# copy dump over to our processing instance
gcloud compute copy-files /tmp/$dump clear_server.sql $TMP_INSTANCE:. --zone $TMP_ZONE

# Clear the server of added data, and add fresh data. Then dump again from the tmp instance.
# At the moment we don't add Kailahum locations, if we did, add in the following line
# <(echo "$runsql -e \"source /home/nfortescue/add_fresh_start.sql\" ") \

runsql="mysql -u $MYSQL_USER -p'$MYSQL_PASSWORD' openmrs "
cat <(echo "export MYSQL_USER=\"$MYSQL_USER\"") \
<(echo "export MYSQL_PASSWORD=\"$MYSQL_PASSWORD\"") \
openmrs_load \
<(echo "$runsql -e \"source clear_server.sql\" ") \
  | gcloud compute ssh --zone $TMP_ZONE $TMP_INSTANCE --command "bash -s openmrs $dump"

# run dump again to get a dump of the clean instance
cat <(echo "export MYSQL_USER=\"$MYSQL_USER\"") \
<(echo "export MYSQL_PASSWORD=\"$MYSQL_PASSWORD\"") \
openmrs_dump \
  | gcloud compute ssh --zone $TMP_ZONE $TMP_INSTANCE --command "bash -s openmrs $cleandump"

# copy the clean dump to local machine
gcloud compute copy-files $TMP_INSTANCE:$cleandump /tmp/$cleandump --zone $TMP_ZONE
