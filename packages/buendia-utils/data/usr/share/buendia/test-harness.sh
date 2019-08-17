BUENDIA_TEST_SUITE_PATH=/usr/share/buendia/tests

. /usr/share/buendia/utils.sh

begin_suite="--------"
begin_test="[ ???? ]"
ok="[  \033[32mOK\033[0m  ]"
pass="[ \033[92mPASS\033[0m ]"
bad="[ \033[31mBAD\033[0m  ]"
fail_suite="[ \033[91mFAIL\033[0m ]"
skip_suite="[ \033[33mSKIP\033[0m ]"

execute_cron_right_now () {
    cron_file=/etc/cron.d/buendia-$1
    script=cron.sh
    # Keep the environment settings at the top of the file.
    grep "^[A-Za-z_]*=" $cron_file > $script
    # Strip away the cron timings and just run the entirety of the script now.
    cut -d' ' -f7- -s $cron_file >> $script
    . $script
}

mount_loopback () {
    size=${1:-250M}
    mnt_point=$(pwd)/loop
    grep $mnt_point /etc/mtab && return 0
    loop_img=$(pwd)/loopback.img
    loop_dev=$(losetup -f)

    dd if=/dev/zero of=$loop_img bs=$size count=1
    losetup $loop_dev $loop_img
    mkfs.ext2 $loop_dev
    mkdir -p $mnt_point
    mount $loop_dev $mnt_point

    echo "EXTERNAL_BLOCK_DEVICES=loop" > /usr/share/buendia/site/88-testing-loopback
    . /usr/share/buendia/utils.sh
}

umount_loopback () {
    loop_dev=${1:-$(grep loop /etc/mtab | cut -d' ' -f1 | head -1)}
    if [ -n "$loop_dev" ]; then
        while true; do 
            umount $loop_dev 2>/dev/null || break
        done
        losetup -d $loop_dev
    fi
    EXTERNAL_BLOCK_DEVICES=""
    rm -f /usr/share/buendia/site/88-testing-loopback
    . /usr/share/buendia/utils.sh
}

run_test_suite () {
    suite=$1
    (
        tmpdir=$(mktemp -d -t buendia_run_test_suite.XXXX)
        trap 'umount_loopback; rm -rf $tmpdir' EXIT
        . $suite
        for test_func in $(compgen -A function | grep "^test_" | sort); do
            cd $tmpdir
            echo -ne "$begin_test $test_func"
            $test_func >${tmpdir}/output 2>&1
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

run_all_test_suites () {
    suite_path=${1:-$BUENDIA_TEST_SUITE_PATH}
    test_results=$(mktemp -d -t buendia_run_all_test_suites.XXXX)
    trap "rm -rf $test_results" EXIT
    suite_list=${suite_path}/??-*
    suite_count=$(echo $suite_list | wc -w)
    fail=0
    for suite in $suite_list; do
        if [ ! -x $suite ]; then
            echo -e "$skip_suite $suite"
            continue
        fi
        echo $suite >> ${test_results}/suites
        echo -e "$begin_suite $suite"
        3>>$test_results/tests run_test_suite $suite
        if [ $? -ne 0 ]; then
            echo -e "$fail_suite $suite"
            fail=1
            break
        fi
    done
    suites_run=$(wc -l < ${test_results}/suites)
    tests_run=$(wc -l < ${test_results}/tests)
    echo "$suites_run of $suite_count suites run, $tests_run tests passed, $fail test(s) failed."
}
