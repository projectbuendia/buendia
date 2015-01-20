Run ./setup to erase an Edison and bring it to a basic state enabling
all further installation and configuration to be done by .deb packages:

  - Flashed with Edison firmware image (Yocto Linux)
  - Debian GNU/Linux 7 (within a /home/root/debian chroot inside Yocto)
  - Yocto configured to execute Debian startup scripts
  - root password set

The host machine is expected to have dfu-util.
