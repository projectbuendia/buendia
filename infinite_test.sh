#!/bin/bash

#@author pim@whitespell.com

WORKSPACE="workspace"

#delete workspace
rm -rf $WORKSPACE
mkdir $WORKSPACE

echo "Which directory do you want to check for integrity? (Directory must exist)"
read CHECK_DIR

echo "What block size (bytes) do you want to test with?"
read BLOCK_SIZE

echo "What data source (/dev/zero, /dev/random, /dev/urandom..) do you want to test with"
read DATA_SOURCE

#CHECK_DIR=/mnt/smallusb

echo "Inifinite (1) or until disk full(2) ?"
read INFINITE

echo "removing old data from checked directory"
rm -rf $CHECK_DIR/sample_data/
mkdir $CHECK_DIR/sample_data/

#create sample file of 40mb
echo "generating sample file"
dd if=$DATA_SOURCE of=$WORKSPACE/sample bs=$BLOCK_SIZE count=1

#start infinite loop that copies sample files to directed disk
 SUCCESS=0
 FAILS=0
 COUNTER=0
 DISKFULL=0
         while [  $COUNTER -ge 0 ]; do
         	cp $WORKSPACE/sample $CHECK_DIR/sample_data/$COUNTER.sample 
         	if [ $? -eq 0 ]; then
         		cmp --silent $WORKSPACE/sample $CHECK_DIR/sample_data/$COUNTER.sample && let SUCCESS=SUCCESS+1  || let FAILS=FAILS+1 
         	else 
         		# if copying fails, check if disk is more than or 99% full. Then check if first file is still in tact. If true, retry.
			df -h $CHECK_DIR | awk '{ print $5 }' | cut -d'%' -f1 | grep -o '[0-9]*' |  while read usep; do
				if [[ $usep = *[[:digit:]]* ]]; then
					#check if the error was thrown because the disk was full
				 	if [ $usep -ge 99 ]; then
				 		#enable to do disk ful write
				 		cmp --silent $WORKSPACE/sample $CHECK_DIR/sample_data/0.sample && echo "TEST SUCCESS $COUNTER files, 0.sample still in tact and disk full \n" >> diskfull.log && let SUCCESS=SUCCESS+1  || echo "TEST FAIL $COUNTER files, Index 0 corrupted when disk full \n" >> diskfull.log && let FAILS=FAILS+1
				 		cat diskfull.log
				 		let DISKFULL=DISKFULL+1 
				 		if [ $INFINITE -eq 1 ]; then 
				 			rm -rf $CHECK_DIR/sample_data/
							mkdir $CHECK_DIR/sample_data/
						else
							exit
						fi
					else 
						#if error wasn't thrown because disk as full, another error was thrown causing the test to fail
						let FAILS=FAILS+1 
					fi
				fi
			done        		
        	fi
        	let COUNTER=COUNTER+1 
        	item=$SUCCESS
			total=$COUNTER
			df -h $CHECK_DIR | awk '{ print $5 }' | cut -d'%' -f1 | grep -o '[0-9]*' |  while read usep; do
				if [[ $usep = *[[:digit:]]* ]]; then
					percent=$(printf '%i %i' $item $total | awk '{ pc=100*$1/$2; i=int(pc); print (pc-i<0.5)?i:i+1 }')
					echo -ne "  Success: Disk Full: $usep%, Fails: $FAILS ($percent%), Graceful fails (disk full): $DISKFULL,  $COUNTER rounds \r"
				fi
			done
         done