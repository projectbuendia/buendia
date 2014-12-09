# Utility functions for executing shell commands and scripts on an Edison.

if [ ! -n "$TARGET_IPADDR" ]; then
  export TARGET_IPADDR=192.168.2.15
  echo "Using default target address $TARGET_IPADDR."
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
