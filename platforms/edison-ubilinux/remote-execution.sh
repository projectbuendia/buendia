# Utility functions for executing shell commands and scripts on an Edison.

export TARGET_IPADDR
if [ ! -n "$TARGET_IPADDR" ]; then
  TARGET_IPADDR=192.168.2.15
fi

target="root@$TARGET_IPADDR"
key_file=$HOME/.ssh/edison
ssh_opts="-i $key_file -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no"
ssh="ssh $ssh_opts"
scp="scp $ssh_opts"

function connect_linux_ethernet() {
  if [[ "$OSTYPE" == linux* ]]; then
    ifconfig usb0 up 192.168.2.1 2>/dev/null
  fi
}

function connect_mac_ethernet() {
  if [[ "$OSTYPE" == darwin* ]]; then
    usbif=$(ifconfig | grep -v '^en[01]:' | grep -vw UP | grep -o '^en[0-9]\+')
    if [ -n "$usbif" ]; then
      echo "Enter your Mac login password to connect to the Edison." 1>&2
      sudo ifconfig $usbif up 192.168.2.1 2>/dev/null
    fi
  fi
}

function connect_ethernet() {
  let tries=0
  while true; do
    connect_linux_ethernet
    connect_mac_ethernet
    if ping -c 1 -t 1 $TARGET_IPADDR >/dev/null; then break; fi
    sleep 1
    if [[ "$OSTYPE" == darwin* && $tries == 3 ]]; then
      cat <<EOF 1>&2
If the Edison does not appear within 30 seconds of power-on, open
Network Preferences and look for a new Ethernet device.  Try clicking
the small + at the bottom of the list of network devices and looking
for Multifunction Composite Gadget (enX) in the dropdown list.
Select the new network device.  In the Configure IPv4 dropdown list,
select Manually, set your IP Address to 192.168.2.1, and click Apply.
EOF
    fi
    echo -n '.' 1>&2
  done
}

# Executes the bash shell commands passed into stdin on the Edison.
# Note that whereas cat <<EOF | do_on_target will expand shell variables on
# *this* host before execution, cat <<'EOF' | do_on_target will not.
function do_on_target() {
  echo ">> $target" 1>&2
  $ssh $target bash
}

function copy_target_directory() {
  if [ -n "$TARGET_COPIED" ]; then return; fi
  echo 'rm -rf target' | do_on_target
  $scp -pr target $target:
  export TARGET_COPIED=1
}

# Invokes the specified script on the Edison.  The script runs in a copy of
# the target/ subdirectory and is connected to the local stdin and stdout.
function run_on_target() {
  script=$1
  name=$(basename $script)
  shift

  copy_target_directory
  if [[ $script != target/* ]]; then
    chmod 755 $script
    echo "$script -> $target:$name" 1>&2
    # Prevent scp from consuming stdin so that stdin can be piped to ssh below.
    $scp -p $script $target: </dev/null
  fi

  echo ">> $target: ./$name" "$@" 1>&2
  $ssh $target ./$name "$@"
}
