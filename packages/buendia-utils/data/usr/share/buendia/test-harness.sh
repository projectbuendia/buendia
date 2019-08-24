BUENDIA_TEST_SUITE_PATH=/usr/share/buendia/tests

. /usr/share/buendia/utils.sh

begin_suite="--------"
begin_test="[ ???? ]"
ok="[  \033[32mOK\033[0m  ]"
pass="[ \033[92mPASS\033[0m ]"
bad="[ \033[31mBAD\033[0m  ]"
fail_suite="[ \033[91mFAIL\033[0m ]"
skip_suite="[ \033[33mSKIP\033[0m ]"
warning="\033[33mWARNING\033[0m"

# execute_cron_right_now extracts the shell commands from a given buendia
# crontab, and executes them all right now, in the order found. This gives us
# the ability to perform with some verisimilitude what the crontab will wind up
# doing eventually, without having to wait for it.
execute_cron_right_now () {
    cron_file=/etc/cron.d/buendia-$1
    script=cron.sh
    # Keep the environment settings at the top of the file.
    grep "^[A-Za-z0-9_]*=" $cron_file > $script
    # Strip away any comments and all cron timings and just run the entirety of
    # the script now.
    grep "^[^#]" $cron_file | cut -d' ' -f7- -s >> $script
    # From man 5 crontab:
    #   Percent-signs (%) in the command, unless escaped with backslash (\),
    #   will be changed into newline characters, and all data after the first %
    #   will be sent to the command as standard input.
    if grep "%" $script; then
        echo -e "$warning: cron file contains a %; this script probably won't do what you want!"
        echo "See 'man 5 crontab' for details."
        return 1
    fi
    . $script
}

# mount_loopback creates an ext2-formatted loopback device, and mounts it
# inside the current working directory, to simulate the insertion of an
# external USB storage device.
mount_loopback () {
    # 1st arg sets the size of the loopback device; default is 250M.
    size=${1:-250M}

    # Choose a mount point inside the current working directory
    # (which is assumed to be a temp dir created by the suite harness).
    # 
    # If this is already mounted, then return success early (and
    # assume that the existing loopback device is big enough).
    mnt_point=$(pwd)/loop
    grep $mnt_point /etc/mtab && return 0

    # Create an empty file of the appropriate size for the loopback device.
    loop_img=$(pwd)/loopback.img
    dd if=/dev/zero of=$loop_img bs=$size count=1

    # Set up the first loopback device available.
    loop_dev=$(losetup -f)
    losetup $loop_dev $loop_img

    # Format the loopback device as ext2 and mount it.
    mkfs.ext2 $loop_dev
    mkdir -p $mnt_point
    mount $loop_dev $mnt_point

    # Tell Buendia that we have an external block device under /dev/loop?
    echo "EXTERNAL_BLOCK_DEVICES=loop" > /usr/share/buendia/site/88-testing-loopback
    . /usr/share/buendia/utils.sh
}

# umount_loopback unmounts a given loopback device, to simulate the removal of
# an external USB storage device.
umount_loopback () {
    # 1st arg is the loopback device to unmount; default to the first loopback
    # device currently mounted.
    loop_dev=${1:-$(grep loop /etc/mtab | cut -d' ' -f1 | head -1)}

    # If we found a loopback device, it might be (probably is) mounted
    # repeatedly in different places by various buendia cron scripts. Unmount
    # it repeatedly until the unmount attempt fails, at which point we're done.
    if [ -n "$loop_dev" ]; then
        while true; do 
            umount "$loop_dev" 2>/dev/null || break
        done
        losetup -d $loop_dev
    fi

    # Tell Buendia we don't have any more external block devices.
    EXTERNAL_BLOCK_DEVICES=""
    rm -f /usr/share/buendia/site/88-testing-loopback
    . /usr/share/buendia/utils.sh
}

# run_test_suite runs all of the test cases in a given "suite" (i.e. file), in
# lexical order. Buendia integration test cases are bash functions starting
# with the prefix "test_". Test suites are run inside a temporary directory
# which is cleaned up after the test cases are run. run_test_suite writes the
# name of each test case to file descriptor 3 as it passes. If a test case
# returns a non-zero value, the suite is immediately aborted.
run_test_suite () {
    suite=$1
    # Run the entire test suite in a subshell.
    (
        # Make a temporary directory, and clean it (and any loopback devices) up
        # afterwards.
        tmpdir=$(mktemp -d -t buendia_run_test_suite.XXXX)
        trap 'umount_loopback; rm -rf $tmpdir' EXIT

        # Load the functions in the given suite.
        . $suite

        # Find the functions beginning with "test_" and run them in sorted
        # order.
        for test_func in $(compgen -A function | grep "^test_" | sort); do
            # Run the test case function in the working directory, capturing
            # stdin/out.
            cd $tmpdir
            echo -ne "$begin_test $test_func"
            $test_func >${tmpdir}/output 2>&1

            # If the case passed, report success, and write the name of the
            # test case to FD 3 (if run_all_test_suites is listening).
            # Otherwise, dump the output of the failed test, and return with
            # failure.
            result=$?
            if [ $result -eq 0 ]; then
                echo -e "\r$ok $test_func"
                [ -w /dev/fd/3 ] && echo $test_func >&3
            else
                echo
                echo
                cat ${tmpdir}/output
                echo
                echo -e "$bad $test_func"
                return $result
            fi
        done
    )
}

# run_all_test_suites applies run_test_suite to every test suite in a given
# directory, and reports the result. Test suite filenames are assumed to have a
# prefix 'NN-' which specifies the relative order of execution of the suite. A
# test suite must be chmod +x or it will be skipped. If any individual test
# case fails, the current test suite, and any remaining suites, are immediately
# aborted.
run_all_test_suites () {
    if [ "$USER" != "root" ]; then
        echo -e "${warning}: You almost certainly want to run this script using sudo."
    fi
    suite_path=${1:-$BUENDIA_TEST_SUITE_PATH}

    # Create a temp dir for capturing test suite results.
    test_results=$(mktemp -d -t buendia_run_all_test_suites.XXXX)
    trap "rm -rf $test_results" EXIT

    # Generate and count the list of test suites.
    suite_list=${suite_path}/??-*
    suite_count=$(echo $suite_list | wc -w)
    fail=0

    for suite in $suite_list; do
        # If a given suite isn't executable, then let's skip it.
        if [ ! -x $suite ]; then
            echo -e "$skip_suite $suite"
            continue
        fi
        # Record the attempt to run the suite.
        echo $suite >> ${test_results}/suites
        echo -e "$begin_suite $suite"
        # Run the test suite, capturing a list of tests run to file descriptor
        # #3.
        3>>$test_results/tests run_test_suite $suite
        # If the test suite failed, record the failure and abort immediately.
        if [ $? -ne 0 ]; then
            echo -e "$fail_suite $suite"
            fail=1
            break
        fi
    done
    # Count the number of test suites and cases run.
    suites_run=$(wc -l < ${test_results}/suites)
    tests_run=$(wc -l < ${test_results}/tests)
    echo "$suites_run of $suite_count suites run, $tests_run tests passed, $fail test(s) failed."
}
