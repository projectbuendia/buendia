stressdisk
==========

Runs a stress and integrity test on a prompted disk. Stressdisk comes with 2 different variations, the infinite_test and the single_file_test

###Infinite Test
The infinite test generates a random file with your specified block size and copies this file into the directory you want to check endlessly. If an error is thrown, a check if made whether the disk is full. If the disk is full, the first copied file (0) is checked in order to see if the disk being full overwrote any of the first bytes on the disk. (In order for this to work properly, your disk must be empty!). If the infinite setting is on (1) it will remove all data and start over. The outcome of this test is a percentage of transfers that have failed, plus an indication of the disk being full corrupted the first file.

```bash infinite_test.sh```
```Or if you want to run it in the background: bash infinite_test.sh &```
###Single file test
This test is mainly used to measure speed and integrity. You specify the size of the data in MB that your transfer file is, then stressdisk copies this file over to the remote disk, checks if files are the same, if they are - copy them back and check again locally, and then report the results. 

```bash single_file_test.sh```