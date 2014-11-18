stressdisk
==========

Runs a stress and integrity test on a prompted disk.

Instructions:

##1: Clone stressdisk in any directory
##2: run: bash run_tests.sh
##3: Enter the directory you'd like to test (make sure your device is mounted, e.g. /mnt/sd or /mnt/usb).
##4. Enter the file size in MB you'd like to generate and copy and check for integrity (1GB file on edison takes ~ 5 minutes)
##5. stressdisk will first generate a file of the size you've specified, and it will also generate a 1MB file of random data to compare to that is NOT equal to the initial file.
##6. stressdisk copies both generated files to the specified directory and check if the files are equal (and also checks if file is not equal with faulty file)
##7. stressdisk moves the files back to the workspace and checks if files are still equal (also checks again if wile is not equal with faulty file)
##8. If all tests pass, you will have 4X SUCCEED: print out in your terminal, otherwise you will see a fail. 

Will elaborate on this further at a later stage

- Pim
