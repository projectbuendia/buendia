# Installing a development setup using Vagrant

## Install VirtualBox and Vagrant for your OS

* [VirtualBox](https://www.virtualbox.org/wiki/Downloads) is a virtual machine host.
* [Vagrant](https://www.vagrantup.com/downloads.html) is a command-line tool for managing virtual machines.

## Check out the Buendia server code and start your Vagrant VM

    git clone https://github.com/projectbuendia/buendia
    cd buendia/tools/vagrant
    vagrant up

This will set up and provision your virtual machine, which will download and
set up 1-2 GB of software. Be patient; it takes a few minutes the first time.

## Log into your VM

    vagrant ssh
    cd /mnt/buendia

Your local cloned copy of the Buendia project has been shared to your VM as the
`/mnt/buendia` directory. Changes made locally will appear in your VM and vice
versa.

## Set up and build Buendia in the VM

Once you're logged into your VM, follow the [remaining
instructions](../../openmrs/README-debian.md#set-up-the-server-database-as-a-dev-site)
for setting up the server database, building Buendia, and running the server.

## Connect to Buendia from your web browser

Open [http://localhost:9000/openmrs/](http://localhost:9000/openmrs/) in your
web browswer and log in as buendia/buendia. The web page will take a few
seconds to load the first time.

## Shut down (and restart) your VM

You may want to shut down your VM when you're not working on it.

    vagrant halt

You can bring it back with:

    vagrant up

Finally you can get rid of it permanently with:

    vagrant destroy

## [ADVANCED] Set up an Alpine Linux dev environment

    vagrant up alpine
    vagrant ssh alpine
