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

# Executes the (ash, not bash) shell commands passed into stdin on the Edison.
# Note that whereas cat <<EOF | do_in_yocto will expand shell variables on
# this host before execution, cat <<'EOF' | do_in_yocto will not.
function do_in_yocto() {
  echo ">> $target" 1>&2
  $ssh $target ash
}

# Executes the specified script on the Edison.  The remotely running script
# is connected to the local stdin and stdout.
function run_script_in_yocto() {
  script=$1
  name=$(basename $script)
  shift

  $ssh $target mkdir -p /usr/local/bin
  chmod 755 $script
  echo "$script -> $target:/usr/local/bin/$name" 1>&2
  # Prevent scp from consuming stdin so that stdin can be piped to ssh below.
  $scp -p $script $target:/usr/local/bin </dev/null

  echo ">> $target: $name" "$@" 1>&2
  $ssh $target /usr/local/bin/$name "$@"
}

# Executes the bash shell commands passed into stdin on the Edison within the
# Debian chroot.  Whereas cat <<EOF | do_in_debian will expand shell variables
# on this host before execution, cat <<'EOF' | do_in_debian will not.
function do_in_debian() {
  echo ">> $target(debian)" 1>&2
  $ssh $target /usr/local/bin/enter-debian /bin/bash
}

# Executes the specified script on the Edison within the Debian chroot.  The
# remotely running script is connected to the local stdin and stdout.
function run_script_in_debian() {
  script=$1
  name=$(basename $script)
  shift

  $ssh $target mkdir -p /debian/usr/local/bin
  chmod 755 $script
  echo "$script -> $target(debian):/usr/local/bin/$name" 1>&2
  # Prevent scp from consuming stdin so that stdin can be piped to ssh below.
  $scp -p $script $target:/debian/usr/local/bin </dev/null

  echo ">> $target(debian): $name" "$@" 1>&2
  $ssh $target /usr/local/bin/enter-debian $name "$@"
}

function connect_linux_ethernet() {
  if [[ "$OSTYPE" == linux* ]]; then
    if [ $(id -u) != 0 ]; then
      echo "Username: $USER (on $(hostname))" 1>&2
      sudo ifconfig usb0 up 192.168.2.1 2>/dev/null
    else
      ifconfig usb0 up 192.168.2.1 2>/dev/null
    fi
  fi
}

function connect_mac_ethernet() {
  if [[ "$OSTYPE" == darwin* ]]; then
    usbif=$(ifconfig | grep -v '^en[01]:' | grep -vw UP | grep -o '^en[0-9]\+')
    if [ -n "$usbif" ]; then
      if [ $(id -u) != 0 ]; then
        echo "Username: $USER (on $(hostname))" 1>&2
        sudo ifconfig $usbif up 192.168.2.1 2>/dev/null
      else
        ifconfig $usbif up 192.168.2.1 2>/dev/null
      fi
    fi
  fi
}

function connect_ethernet() {
  retry_count=0
  while true; do
    connect_linux_ethernet || true
    connect_mac_ethernet || true
    if ping -c 1 -t 1 $TARGET_IPADDR >/dev/null; then break; fi
    if [[ $retry_count == 0 ]]; then
      echo "Waiting for Edison to come up at $TARGET_IPADDR.  Connect"
      echo "a USB cable from this computer to the Edison's USB OTG port."
    fi
    sleep 1

    echo -n '.' 1>&2
    let retry_count=retry_count+1
    if [[ "$OSTYPE" == darwin* && $retry_count == 3 ]]; then
      cat <<EOF 1>&2

If the Edison does not appear within 30 seconds of power-on, open
Network Preferences and look for a new Ethernet device.  Try clicking
the small + at the bottom of the list of network devices and looking
for Multifunction Composite Gadget (enX) in the dropdown list.
Select the new network device.  In the Configure IPv4 dropdown list,
select Manually, set your IP Address to 192.168.2.1, and click Apply.
EOF
    fi
  done
}