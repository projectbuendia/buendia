#!/bin/bash
# Copyright 2015 The Project Buendia Authors
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.  You may obtain a copy
# of the License at: http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distrib-
# uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
# OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
# specific language governing permissions and limitations under the License.

INSTANCE="openmrs-2"

if [ -z "$MYSQL_USER" ]; then
    echo "Need to set \$MYSQL_USER"
    exit 2
fi
if [ -z "$MYSQL_PASSWORD" ]; then
    echo "Need to set \$MYSQL_PASSWORD"
    exit 2
fi
if [ -z "$1" ]; then
    echo "Usage: prepare_gce_for_demo demo-0.2-patients-merged"
    exit 2
fi

# Turn the demo JSON data into an SQL script
python generate_site_sql.py $1

# Copy the SQL scripts to the GCE
gcloud compute copy-files $1.sql clear_server.sql add_fresh_start_data.sql $INSTANCE:/home/nfortescue --zone europe-west1-b

# Run the SQL script on the GCE instance
runsql="mysql -u $MYSQL_USER -p'$MYSQL_PASSWORD' openmrs "
cat <(echo "export MYSQL_USER=\"$MYSQL_USER\"") \
<(echo "export MYSQL_PASSWORD=\"$MYSQL_PASSWORD\"") \
<(echo "$runsql -e \"source /home/nfortescue/clear_server.sql\" ") \
<(echo "$runsql -e \"source /home/nfortescue/add_fresh_start_data.sql\" ") \
<(echo "$runsql -e \"source /home/nfortescue/$1.sql\" ") \
  | gcloud compute ssh --zone europe-west1-b $INSTANCE --command "bash -s"

