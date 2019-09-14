# Buendia Network Installer

This directory contains the components needed to construct a *mostly automated*
installer image for the Buendia server, suitable for burning to USB drive or
CD-ROM.

**Note that the installer image does not itself contain the Buendia software.**
It installs Buendia over the Internet from our Debian package repository, which
is a GitHub Pages site backed by [our builds repo](https://github.com/projectbuendia/builds).
These packages are built from the `dev` branch and automatically committed to
the builds repo by CircleCI.  So, when you use this installer to set up a server,
your new server will get the latest available packages built from `dev`.

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
   default, "Graphical install", which leads to a kernel panic),
   and wait a minute or so for the installer to start up.
5. Enter the wireless network and password when prompted.  (If the
   machine has an Ethernet connection to a network with a DHCP server,
   it should set up networking automatically and skip this step.)
6. For all the prompts related to disk partitioning, you can just press
   Enter to use the default option.
7. After you've completed the partitioning step, you can walk away and
   let the installer run.  It typically takes about 10 minutes, depending
   on your network speed, and downloads 300–400 MB of additional software.
8. When it's done, you will be prompted to reboot your new system.  Upon
   rebooting, the system will take about a minute to finish configuring
   the Buendia software; a login prompt may be shown before this is complete.
9. Within 2–3 minutes of booting, OpenMRS should be up and running.
   If you want to watch for it to come up, you can log into the console
   as `buendia` or `root` with the password `buendia` and run the command
   `tail -f /var/log/tomcat7/catalina.out`.  Once you see `Server startup
   in xxx ms`, OpenMRS and the Buendia server are ready for use.
10. There is a server admin page at `http://localhost:9999/` and OpenMRS
    will be available at `http://localhost:9000/openmrs/`.
    If you've installed to a NUC and have a `buendia` Wi-Fi network,
    the admin page will be at `http://10.18.0.50:9999/` and
    OpenMRS will be available at `http://10.18.0.50:9000/openmrs/`.

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
* `buendia-pkgserver`
* `buendia-update`
* `buendia-backup`
* `buendia-monitoring`

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
