# Buendia Network Installer

This directory contains the components needed to construct a *mostly automated*
installer image for the Buendia server, suitable for burning to USB drive or
CD-ROM.

## Quick Start

The following instructions will turn a fresh Intel NUC into a functioning
Buendia server.  The installer images are constructed and tested to work
on a NUC; they should work for most other systems as well.

1. Download the installer image, or build it as described below.
2. Write the installer image to bootable media, using e.g.
   [balenaEtcher](https://www.balena.io/etcher/) if you're on MacOS.
3. Boot the Intel NUC or other system from the USB media.  If the
   NUC doesn't automatically boot from USB, you can press F10 during
   startup to select the boot device.
4. The Debian installer menu should appear.  Choose "Install" (not the
   default, "Graphical install", which leads to a kernel panic).
5. Enter the wireless network and password when prompted.  (If the
   machine has an Ethernet connection to a network with a DHCP server,
   it should set up networking automatically and skip this step.)
6. For all the prompts related to disk partitioning and boot loader,
   you can just press Enter to use the default option.
7. The installer typically takes 5 to 10 minutes to run, depending on your
   network speed, and will download 300–400 MB of additional software.
8. You will be prompted to remove the installer media and reboot.  When
   the system reboots, it will take about a minute to finish configuring
   the system; a login prompt may be shown before this is complete.
9. If you want to watch for OpenMRS to come up, you can log into the
   console as `buendia` or `root` with the password `buendia`, and run
   the command `tail -f /var/log/buendia/buendia-warmup.log` (this
   shows you the log of the warmup task, which runs once a minute).
10. After a couple minutes, the OpenMRS web application will be available
    at `http://localhost:9000/openmrs/`.  If you've installed to a NUC
    and have a `buendia` Wi-Fi network, the server will be available at
   `http://10.18.0.50:9000/openmrs/`.

## Building the Installer

Building the installer image requires a Debian(-ish) Linux system. From the
current directory, run the following:

```
  $ build-buendia-iso
```

This produces a file called `buendia-install.iso` which is a bootable ISO
image.  (You may see a bunch of warnings from `libisofs` complaining about
adding symlinks to a Joliet tree; these are safe to ignore.)

This image can be uploaded to a different machine, or installed directly
to USB media, for example:

```
  $ sudo bash -c `cat buendia-install.iso > /dev/<usb drive>`
```

## Details

The [`build-buendia-iso`](build-buendia-iso) constructs the image by starting
with a standard Debian 9.9 'stretch' ISO, which also contains the non-free
firmware needed by the Intel NUC wireless card.

The script adds `apt-transport-https` to the list of packages to add to the
base system, and then adds a [Debian preseed file](preseed.cfg) to seed most of
the netinst prompts.

The preseed configuration adds our projectbuendia.github.io _unstable_
repository to the list of apt sources, and then appends the following packages
to the default server installation:

* `buendia-site-test`
* `buendia-server`
* `buendia-networking`
* `buendia-dashboard`

Also, the preseed configuration triggers the creation of an empty file in
`/etc/buendia-defer-reconfigure` in the new system, which suppresses most of
the Buendia-specific configruation steps until after the system has booted for
the first time and both MySQL and Tomcat are running.

Finally, the installer sets the automated curses installer to be the default,
and then rebuilds the ISO image into a new file.

## Preseeding

The preseed file was created by running a Debian installation to completion on
a NUC, and then installing `debconf-tools` and running `debconf-get-selections
--installer` and `debconf-get-selections`, and saving the result as a starting
point. 

Some options related to disk partitioning and bootloader installation
were removed as these seemed to be causing problems when booting in a virtual
machine, at least. The omission of these options is most of why the installer
is not fully automated. Fixing this might be a worthwhile future task.
