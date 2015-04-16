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

#@author pim@whitespell.com

WORKSPACE="workspace/"

#delete workspace
rm -rf $WORKSPACE

echo "Which directory do you want to check for integrity? (Directory must exist)"
read CHECK_DIR

echo "Enter the size of the test file (MB)"
read FILE_SIZE_MB

echo "Enter data source (/dev/random, /dev/zero, /dev/urandom)"
read DATA_SOURCE

echo "Checking $CHECK_DIR with $FILE_SIZE_MB MB of random data"

#create workspace if it does not yet exist
mkdir $WORKSPACE


#create random file that we will run comparisons against
echo "Generating CORRECT compare file with a file size of 1 MB from $DATA_SOURCE.."

dd if=$DATA_SOURCE of=$WORKSPACE/compare_internal_base bs=1000000 count=1

$CORRECT_FILE_CREATION

#cat file into the internal file FILE_SIZE_MB times.
 COUNTER=0
         while [  $COUNTER -lt $FILE_SIZE_MB ]; do
            item=$COUNTER
			total=$FILE_SIZE_MB
			percent=$(printf '%i %i' $item $total | awk '{ pc=100*$1/$2; i=int(pc); print (pc-i<0.5)?i:i+1 }')
			echo -ne "  $COUNTER MB generated ($percent%) \r"
         	cat $WORKSPACE/compare_internal_base >> $WORKSPACE/compare_internal 
            let COUNTER=COUNTER+1 
         done

ls -lh $WORKSPACE

echo "Generating RANDOM FAULTY compare file.."
#always from urandom because /dev/zero will always be the same
dd if=/dev/urandom of=$WORKSPACE/compare_internal_faulty bs=1048576 count=1

$FAULTY_FILE_CREATION

#copy the file to the device
echo "Copying GOOD and FAULTY file file to $CHECK_DIR ...."
rsync -ah --progress $WORKSPACE/compare_internal $CHECK_DIR/compare_external
rsync -ah --progress $WORKSPACE/compare_internal_faulty $CHECK_DIR/compare_external_faulty

#compare internal with external file
echo "Starting integrity 1 by comparing local file with external file"
cmp --silent $WORKSPACE/compare_internal $CHECK_DIR/compare_external && echo "SUCCESS: Compare file was the same, integrity 1 success 1/2" || echo "FAIL: files that should be equal are different, integrity 1 failed 1/2"
cmp --silent $WORKSPACE/compare_internal $CHECK_DIR/compare_external_faulty && echo "FAIL: FAULTY Compare file was the same, failcheck for integrity 1 fail" || echo "SUCCESS: FAULTY Compare file was NOT the same, integrity 1 success 2/2"

echo "Moving external file back to workspace"

#move the file back to the workspace
mv $CHECK_DIR/compare_external workspace
mv $CHECK_DIR/compare_external_faulty workspace

#check if contents are still equal
echo "Starting integrity 2 by comparing local files"
cmp --silent $WORKSPACE/compare_internal $WORKSPACE/compare_external && echo "SUCCESS: Compare file was the same, integrity 2 success 1/2" || echo "FAIL: files that should be equal are different, integrity 2 failed 1/2"
cmp --silent $WORKSPACE/compare_internal $WORKSPACE/compare_external_faulty && echo "FAIL: FAULTY Compare file was the same, failcheck for integrity 2 fail" || echo "SUCCESS: FAULTY Compare file was NOT the same, integrity 2 success 2/2"
