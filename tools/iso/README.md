# Buendia Network Installer

This directory contains the components needed to construct a *mostly automated*
installer image for the Buendia server, suitable for burning to USB drive or
CD-ROM.

## Quick Start

1. Build the installer image as described below, or download it from (somewhere).
2. Write the installer image to bootable media, using e.g.
   [balenaEtcher](https://www.balena.io/etcher/) if you're on MacOS.
3. Boot an Intel NUC or other system from the USB media. Unless the machine is
   connected to Ethernet with a DHCP server on the network, you will probably
   be prompted for networking details. This is a network installer, after all.
3. Follow the prompts related to disk partitioning and boot loader. You can hit
   enter at any prompt to use the defaults. On the NUC, this is intended to
   result in a functioning system.
4. The installer will take ~5 minutes to run, maybe longer, depending on your
   network speed, and download 300-400 MB of additional software.
5. You will be prompted to remove the installer media. When the system reboots,
   it will take about a minute to finish configuring the system. A login prompt
   may be shown before the first configuration is complete.
6. After a couple minutes, the OpenMRS console will be available at
   `http://localhost:9000/openmrs/`. If you've installed to a NUC and have a
   `buendia` Wi-Fi network, the server will be available at
   `http://10.18.0.50:9000/openmrs/`.
7. You can log into the console as either `buendia` or `root`. The password is
   the same.

## Building the Installer

Building the installer image requires a Debian(-ish) Linux system. From the
current directory, run the following:

```
  $ build-buendia-iso
```

This produces a file called `buendia-install.iso` which is a bootable ISO
image. This image can be uploaded to a different machine, or installed directly
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
the netinst prompts. This preseed configuration adds our
projectbuendia.github.io _unstable_ repository to the list of apt sources, and
then appends the following packages to the default server installation:

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
