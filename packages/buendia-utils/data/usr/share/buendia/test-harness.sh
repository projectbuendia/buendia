UTILS_TEST_SUITE_PATH=/usr/share/buendia/tests

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
        # Effectively disable any sleep commands in the script.
        sed -i -e 's/sleep [0-9][0-9]*/sleep 0.1/g' $script
        . $script
    )
}

# mount_storage creates an ext2-formatted loopback device, and mounts it
# inside the current working directory, to simulate the insertion of an
# external USB storage device.
mount_storage () {
    # 1st arg sets the size of the loopback device; default is 250M.
    size=${1:-250M}

    # Choose a mount point inside the current working directory
    # (which is assumed to be a temp dir created by the suite harness).
    # 
    # If this is already mounted, then return success early (and
    # assume that the existing loopback device is big enough).
    mnt_point=$(pwd)/mnt
    grep $mnt_point /etc/mtab && return 0

    # Do we want to test physical devices specifically?
    if bool "$UTILS_TEST_PHYSICAL_BACKUP"; then
        partitions=$(external_file_systems)
        mkdir -p $mnt_point
        for device in $partitions; do
            mount $device $mnt_point && return 0
        done
        echo "UTILS_TEST_PHYSICAL_BACKUP is set, but no external file system could be found."
        echo "The following partitions were checked: ${partitions}"
    fi

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

# umount_storage unmounts a given loopback device, to simulate the removal of
# an external USB storage device.
umount_storage () {
    mnt_point=$(pwd)/mnt

    # 1st arg is the loopback device to unmount; default to the first loopback
    # device currently mounted.
    loop_dev=${1:-$(grep "$mnt_point" /etc/mtab | cut -d' ' -f1 | head -1)}

    # If we were asked to test physical backups then just umount the local
    # mount point.
    if bool "$UTILS_TEST_PHYSICAL_BACKUP"; then
        grep "$mnt_point" /etc/mtab && umount "$mnt_point"
        return 0
    fi

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

# execute_openmrs_sql sends SQL commands directly to the OpenMRS database
execute_openmrs_sql () {
    mysql -u$OPENMRS_MYSQL_USER -p$OPENMRS_MYSQL_PASSWORD openmrs
}

# build_dummy_package creates a new Debian package on the fly, with a version
# based on the UNIX epoch time, consisting of a single site configuration test
# file.
build_dummy_package () {
    # using epoch secs in the version string ensures monotonic increase
    version="0.0.$(date +%s)"
    # Make the control file
    cat >control <<EOF
Package: buendia-dummy
Version: ${version}
Architecture: all
Description: Mock package for integration testing
Maintainer: projectbuendia.org
EOF
    # Make a dummy site file
    echo "BUENDIA_DUMMY_VERSION=${version}" | create "data/usr/share/buendia/site/85-dummy"
    # Make sure we know it's a debian package
    echo "2.0" > debian-binary
    # Package it all up into a deb
    deb="buendia-dummy_${version}_all.deb"
    tar cfz control.tar.gz control
    (cd data; tar cfz ../data.tar.gz .)
    # https://ubuntuforums.org/archive/index.php/t-1481153.html
    # "So it appears that "debian-binary" has to be listed before the gzipped
    # files or else the resulting deb will not be valid." Just... wow.
    ar rc $deb debian-binary control.tar.gz data.tar.gz
    # Clean up
    rm -rf data data.tar.gz control control.tar.gz debian-binary
    # Print the filename of the package
    echo $deb
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
        trap 'umount_storage; rm -rf $tmpdir' EXIT

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

            # If the case passed, report success, and write the name of the
            # test case to FD 3 (if run_all_test_suites is listening).
            # Otherwise, dump the output of the failed test, and return with
            # failure.
            result=$?
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
    if [ "$1" = "--unsafe" ]; then
        # Run *all* tests, including those that might disrupt services or
        # modify the database.
        UTILS_RUN_UNSAFE_TESTS=1
        shift
    fi

    suite_path=${1:-$UTILS_TEST_SUITE_PATH}

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

    # Notify the user if we're not planning to run all the tests.
    if ! bool "$UTILS_RUN_UNSAFE_TESTS"; then
        echo -e "$warning: Running tests marked safe. Use --unsafe if you want ALL tests run."
    fi

    # Create a temp dir for capturing test suite results.
    test_results=$(mktemp -d -t buendia_run_all_test_suites.XXXX)
    trap "rm -rf $test_results" EXIT

    # Generate and count the list of test suites.
    suite_list=${suite_path}/??-*
    suite_count=$(echo $suite_list | wc -w)
    tests_failed=0

    # Open file descriptor #3 so that run_test_suite can record a list of
    # passing test cases.
    exec 3>$test_results/tests

    for suite in $suite_list; do
        # If a given suite isn't executable, then let's skip it.
        if [ ! -x $suite ]; then
            echo -e "$suite_skip $suite"
            continue
        fi
        # If we're not running unsafe tests, and the test doesn't have "safe"
        # in the name, skip it.
        if ( echo $suite | grep -vwq safe ) && ! bool "$UTILS_RUN_UNSAFE_TESTS"; then
            echo -e "$suite_skip $suite"
            continue
        fi
        # Run the test suite
        echo -e "$suite_begin $suite"
        run_test_suite $suite
        result=$?
        # Record the attempt to run the suite.
        echo $suite >> ${test_results}/suites
        # If the test suite failed, record the failure and abort immediately.
        if [ $result -ne 0 ]; then
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
