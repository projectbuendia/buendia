#!/bin/bash

#delete workspace
rm -rf workspace

echo "Which directory do you want to check for integrity?"
read CHECK_DIR

echo "Enter the size of the test file (MB)"
read FILE_SIZE_MB

echo "Checking $CHECK_DIR with $FILE_SIZE_MB MB of random data"

#create workspace if it does not yet exist
mkdir workspace

#enter the workspace
cd workspace

#create random file that we will run comparisons against
echo "Generating file with a file size of $FILE_SIZE_MB MB from /dev/urandom..."

CORRECT_FILE_CREATION=$(dd if=/dev/urandom of=compare_internal bs=1048576 count=$FILE_SIZE_MB)

$CORRECT_FILE_CREATION

echo "Generating faulty compare file.."
FAULTY_FILE_CREATION=$(dd if=/dev/urandom of=compare_internal_faulty bs=1048576 count=1)

$FAULTY_FILE_CREATION

#copy the file to the device
echo "Copying compare and faulty file file to USB...."
cp compare_internal $CHECK_DIR/compare_external
cp compare_internal_faulty $CHECK_DIR/compare_external_faulty

#compare internal with external file
echo "Starting integrity 1 by comparing local file with external file"
cmp --silent compare_internal $CHECK_DIR/compare_external || echo "FAIL: files that should be equal are different, integrity 1 failed"
cmp --silent compare_internal $CHECK_DIR/compare_external_faulty || echo "/o/ success in comparing faulty files externally"

echo "Moving external file back to workspace"

#move the file back to the workspace
mv $CHECK_DIR/compare_external .
mv $CHECK_DIR/compare_external_faulty .

#check if contents are still equal
echo "Starting integrity 2 by comparing local files"
cmp --silent compare_internal compare_external || echo "FAIL: files are different, integrity 2 failed"
cmp --silent compare_internal compare_external_faulty || echo "/o/ success in comparing faulty files internally"
cd ../
