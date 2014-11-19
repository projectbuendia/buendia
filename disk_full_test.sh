#!/bin/bash

#@author pim@whitespell.com

WORKSPACE="workspace"

#delete workspace
rm -rf $WORKSPACE
mkdir $WORKSPACE

echo "Which directory do you want to check for integrity? (Directory must exist)"
read CHECK_DIR

#CHECK_DIR=/mnt/smallusb

#echo "Inifinite (1) or until disk full(2) ?"
#read INFINITE

rm -rf $CHECK_DIR/sample_data/
mkdir $CHECK_DIR/sample_data/

#create sample file of 40mb
dd if=/dev/urandom of=$WORKSPACE/sample bs=40000000 count=1

#start infinite loop that copies sample files to directed disk
 SUCCESS=0
 FAILS=0
 COUNTER=1
         while [  $COUNTER > 0 ]; do
         	cp $WORKSPACE/sample $CHECK_DIR/sample_data/$COUNTER.sample 
         	if [ $? -eq 0 ]; then
         		cmp --silent $WORKSPACE/sample $CHECK_DIR/sample_data/$COUNTER.sample && let SUCCESS=SUCCESS+1  || let FAILS=FAILS+1 
         	else 
         		# if copying fails, check if disk is more than or 99% full. Then check if first file is still in tact. If true, retry.
			df -h $CHECK_DIR | awk '{ print $5 }' | cut -d'%' -f1 | grep -o '[0-9]*' |  while read usep; do
			if [[ $usep = *[[:digit:]]* ]]; then
			 	if [ $usep -ge 99 ]; then
			 		#enable to do disk ful write
			 		cmp --silent $WORKSPACE/sample $CHECK_DIR/sample_data/0.sample && echo "TEST SUCCESS $COUNTER files, Index 0 still in tact and disk full \n" >> diskfull.log  || echo "TEST FAIL $COUNTER files, Index 0 corrupted when disk full \n" >> diskfull.log
			 		rm -rf $CHECK_DIR/sample_data/
					mkdir $CHECK_DIR/sample_data/
				fi
			fi
			done
         		let FAILS=FAILS+1 
        	fi
        	let COUNTER=COUNTER+1 
        	item=$FAILS
			total=$COUNTER
			percent=$(printf '%i %i' $item $total | awk '{ pc=100*$1/$2; i=int(pc); print (pc-i<0.5)?i:i+1 }')
			echo -ne "  $FAILS fails in $COUNTER files ($percent%) \r"
			echo  "  $FAILS fails in $COUNTER files ($percent%)" > output.log 
         done

         	item=$FAILS
			total=$COUNTER
			percent=$(printf '%i %i' $item $total | awk '{ pc=100*$1/$2; i=int(pc); print (pc-i<0.5)?i:i+1 }')
			echo "  $FAILS fails in $COUNTER files ($percent%)"


 

#check whether write succeeded, if not ( check if error message contains the word full or space) if not, write error to error.log and add a fail +1