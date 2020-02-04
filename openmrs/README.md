# Buendia server (OpenMRS module)

This repository contains the Buendia OpenMRS module,
which works with OpenMRS Platform 1.10.x.
See the [Buendia wiki](https://github.com/projectbuendia/buendia/wiki) for more details about the project and about the Buendia app that accesses the API provided by this module.

## Developer setup

Follow the instructions below to get your system set up to do Buendia server development.

You can also refer to the following notes on [setting up Buendia on a fresh Debian stretch install](README-debian.md).

### Prerequisites

##### JDK 8
  * If `java -version` does not report version 1.8.x, install JDK 8.
      * Debian Linux: `sudo apt-get install openjdk-8-jdk`
      * Mac OS: Download from [Oracle](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

##### Apache Maven

  * AMI Linux: `sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo; sudo sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo; sudo yum install -y apache-maven`
  * Debian Linux: `sudo apt-get install maven`
  * Mac OS: `brew install maven`
  * Mac OS (alternate):
      * Visit https://maven.apache.org/download.cgi and download the **Binary zip archive**.
      * Unzip the archive in your home directory.
      * Add the `bin` subdirectory of the unpacked archive (e.g. `$HOME/apache-maven-3.3.3/bin`) to your shell's `PATH`.
      * Confirm that the `mvn` command works by running `mvn -v`.

##### MySQL Server 5.6 or higher

  * AMI Linux: `sudo yum install -y mysql56-server; sudo mysql_install_db; sudo chown -R mysql.mysql /var/lib/mysql; service mysqld start`
  * Debian Linux: `sudo apt-get install mysql-server`
  * Mac OS:
      * Go to http://dev.mysql.com/downloads/mysql/ and download the **DMG Archive** for your Mac OS version.
      * Open the downloaded file and then open the .pkg file within to install it.
      * Open System Preferences > MySQL and click **Start MySQL Server** to bring the server up.

##### Python 2.6 or 2.7 and the MySQLdb Python module

Most likely, you already have Python 2 installed with your operating system, so you only need to add the MySQLdb module.

  * AMI Linux: `sudo yum install -y mysql-devel python-devel MySQL-python gcc; sudo easy_install MySQL-python`
  * Debian Linux: `sudo apt-get install python python-mysqldb`
  * Mac OS: `sudo pip install mysqlclient`

##### IntelliJ IDEA
  * Download the Community Edition at https://www.jetbrains.com/idea/download/ and follow the [setup instructions](https://www.jetbrains.com/idea/help/basics-and-installation.html#d1847332e131).


### Building and running the server

1.  Get the Buendia server source code:

        git clone https://github.com/projectbuendia/buendia
        cd buendia

2.  Set up a OpenMRS database and server:

        tools/dev_setup

3.  Build the Buendia module:

        tools/dev_build

4.  Start the server, and wait about a minute for it to start up:

        tools/dev_run

5.  When you see the line:

        [INFO] Starting ProtocolHandler ["http-bio-8080"]

    the server is ready, and you can log in at [[http://localhost:9000/openmrs/]] as "buendia" with password "buendia".

6.  Apply a buendia profile. See [Setting Up A Buendia Profile](https://github.com/projectbuendia/buendia/wiki/Setting-Up-a-Buendia-profile) for more information.

After `tools/dev_build` is done, your freshly built module will be an `.omod` file in `openmrs-project/server/openmrs/RELEASE/modules`.  If you need to install it into an OpenMRS server running elsewhere, you can upload this file using the Administration > Manage Modules page.

### IntelliJ IDEA project setup

1.  In the "Welcome" dialog, click **Import Project** and select the `openmrs/pom.xml` file inside your `buendia` repo.  As you proceed through the import wizard:
      * Turn on **Search for projects recursively** and **Import Maven projects automatically**.
      * On the "Select Maven projects to import" page, you only need the project named `org.projectbuendia:projectbuendia.openmrs`.  You can deselect the others to reduce clutter.

2.  After you click **Finish**, wait a few moments for IntelliJ IDEA to import the project.  The projectbuendia.openmrs module should appear in the Project pane on the left.

You're all set!

Most of the interesting code resides in [openmrs/omod/src/main/java/org/openmrs/projectbuendia/webservices.rest](http://github.com/projectbuendia/buendia/openmrs/omod/src/main/java/org/openmrs/projectbuendia/webservices.rest).  You can make changes in IntelliJ IDEA and use the **Build** command to confirm that the module builds correctly.

When you want to test your module, use `tools/dev_build` and `tools/dev_run` from a Terminal within IntelliJ IDEA or any command shell.  You can also do `tools/dev_build -f` to build without running tests (use with care).


### Debugging the server

If you start the OpenMRS server from the shell with `tools/dev_run`, it will run with remote debugging enabled so that you can debug the running server from within IntelliJ IDEA.  To set this up:

1. Click Run > Edit Configurations...

2. Click the little plus button (+) in the top-left corner and select **Remote**.

3. Change "Unnamed" to something recognizable, then click **OK**.

Now when you click Run > Debug and use this configuration, IntelliJ IDEA will connect to the currently running OpenMRS server.

### Debugging the Unit Tests

You can debug the unit tests with

```shell
cd openmrs
mvn -Dmaven.surefire.debug test
```

The tests will pause until a debugger has attached. See the [Surefire Documentation](http://maven.apache.org/surefire/maven-surefire-plugin/examples/debugging.html) for more information.
