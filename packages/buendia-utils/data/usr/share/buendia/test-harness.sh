BUENDIA_TEST_SUITE_PATH=/usr/share/buendia/tests

. /usr/share/buendia/utils.sh

test_begin="[\033[7m RUNNING  \033[0m]"
test_pass="[   \033[32mPASS\033[0m   ]"
test_fail="[   \033[31mFAIL\033[0m   ]"
suite_begin="----"
suite_fail="\033[91mFAILURE:\033[0m"
suite_skip="\033[33mSKIPPED:\033[0m"
warning="\033[33mWARNING\033[0m"

# execute_cron_right_now extracts the shell commands from a given buendia
# crontab, and executes them all right now, in the order found. This gives us
# the ability to perform with some verisimilitude what the crontab will wind up
# doing eventually, without having to wait for it.
execute_cron_right_now () {
    original_crontab=/etc/cron.d/buendia-$1
    cron_file=$(pwd)/buendia-$1.cron

    # From man 5 crontab:
    #   Percent-signs (%) in the command, unless escaped with backslash (\),
    #   will be changed into newline characters, and all data after the first %
    #   will be sent to the command as standard input.
    if grep "%" $original_crontab; then
        echo -e "$warning: cron file contains a %; this script probably won't do what you want!"
        echo "See 'man 5 crontab' for details."
        return 1
    fi

    # Run the contents of the crontab in a subshell, so that we can trap any
    # exit and clean up properly.
    (
        script=cron.sh
        # Move this crontab out of the way so that cron doesn't try to run it while
        # we're using it.
        trap 'mv $cron_file $original_crontab; service cron reload' EXIT
        mv $original_crontab $cron_file
        service cron reload
        # Keep the environment settings at the top of the file.
        grep "^[A-Za-z0-9_]*=" $cron_file > $script
        # Strip away any comments and all cron timings and just run the entirety of
        # the script now.
        grep "^[^#]" $cron_file | cut -d' ' -f7- -s >> $script
        . $script
    )
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

# openmrs_auth generates an authentication header for the Buendia API
openmrs_auth () {
    AUTH=$(echo -n "$SERVER_OPENMRS_USER:$SERVER_OPENMRS_PASSWORD" | base64)
    echo "Authorization: Basic $AUTH"
}

# openmrs_post sends a POST request to a local API endpoint. The POST content
# is taken from stdin.
openmrs_post () {
    curl -H "$(openmrs_auth)" -H "Content-Type: application/json" \
        -s -d @- "http://localhost:9000/openmrs/ws/rest/v1/projectbuendia/$1"
}

# openmrs_get sends a GET request to a local API endpoint.
openmrs_get () {
    curl -s -H "$(openmrs_auth)" "http://localhost:9000/openmrs/ws/rest/v1/projectbuendia/$1"
}

execute_openmrs_sql () {
    mysql -u$OPENMRS_MYSQL_USER -p$OPENMRS_MYSQL_PASSWORD openmrs
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
            cd $tmpdir
            echo -ne "$test_begin $test_func"

            # Run the test case function in the working directory, capturing
            # stdin/out and enabling function tracing and exit-on-error.
            ( set -ex; $test_func ) >${tmpdir}/output 2>&1

            # Capture the return value
            result=$?

            # If the case passed, report success, and write the name of the
            # test case to FD 3 (if run_all_test_suites is listening).
            # Otherwise, dump the output of the failed test, and return with
            # failure.
            if [ $result -eq 0 ]; then
                echo -e "\r$test_pass $test_func"
                [ -w /dev/fd/3 ] && echo $test_func >&3
            else
                echo
                echo
                cat ${tmpdir}/output
                echo
                echo -e "$test_fail $test_func"
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
    suite_path=${1:-$BUENDIA_TEST_SUITE_PATH}

    # Ensure that we are running as root or many things are unlikely to work
    # correctly.
    if [ "$USER" != "root" ]; then
        echo -e "$warning: This script must be run as root."
        return 1
    fi

    # Lock a file to prevent simultaneous execution of the test suites.
    exec 9>>/var/run/lock/buendia-run-tests
    if ! flock -n 9; then
        echo -e "$warning: Integration tests already running! Aborting..."
        return 1
    fi

    # Create a temp dir for capturing test suite results.
    test_results=$(mktemp -d -t buendia_run_all_test_suites.XXXX)
    trap "rm -rf $test_results" EXIT

    # Generate and count the list of test suites.
    suite_list=${suite_path}/??-*
    suite_count=$(echo $suite_list | wc -w)
    tests_failed=0

    for suite in $suite_list; do
        # If a given suite isn't executable, then let's skip it.
        if [ ! -x $suite ]; then
            echo -e "$suite_skip $suite"
            continue
        fi
        # Record the attempt to run the suite.
        echo $suite >> ${test_results}/suites
        echo -e "$suite_begin $suite"
        # Run the test suite, capturing a list of tests run to file descriptor
        # #3.
        3>>$test_results/tests run_test_suite $suite
        # If the test suite failed, record the failure and abort immediately.
        if [ $? -ne 0 ]; then
            echo
            echo -e "$suite_fail $suite"
            tests_failed=1
            break
        fi
    done
    # Count the number of test suites and cases run.
    suites_run=$(wc -l < ${test_results}/suites)
    tests_passed=$(wc -l < ${test_results}/tests)
    echo "$suites_run of $suite_count suites run, $tests_passed tests passed, $tests_failed test(s) failed."
}
