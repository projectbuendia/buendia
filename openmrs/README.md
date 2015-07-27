# Buendia server (OpenMRS module)

Follow these instructions to get your system set up to do Buendia server development.
See the [Buendia wiki](https://github.com/projectbuendia/buendia/wiki) for more details about the app.

## Prerequisites

##### JDK 7
  * If `java -version` does not report a version >= 1.7, install JDK 7:
      * Linux: `sudo apt-get install openjdk-7-jdk`
      * Mac OS: Download from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)

##### Apache Maven

  * Linux: `sudo apt-get install maven`
  * Mac OS:
      * Visit https://maven.apache.org/download.cgi and download the **Binary zip archive**.
      * Unzip the archive in your home directory.
      * Add the `bin` subdirectory of the unpacked archive (e.g. `$HOME/apache-maven-3.3.3/bin`) to your shell's `PATH`.
      * Confirm that the `mvn` command works by running `mvn -v`.

##### MySQL Server 5.6
  * Linux: `sudo apt-get install mysql-server`
  * Mac OS:
      * Go to http://dev.mysql.com/downloads/mysql/ and download the **DMG Archive** for your Mac OS version.
      * Open the downloaded file and then open the .pkg file within to install it.
      * Open System Preferences > MySQL and click **Start MySQL Server** to bring the server up.

##### IntelliJ IDEA
  * Download the Community Edition at https://www.jetbrains.com/idea/download/ and follow the [setup instructions](https://www.jetbrains.com/idea/help/basics-and-installation.html#d1847332e131).


## Building and running the server

1.  Get the Buendia server source code:

        git clone https://github.com/projectbuendia/buendia
        cd buendia

2.  Set up a OpenMRS server configured to use MySQL and initialize the MySQL database with the "dev" site configuration:

        tools/openmrs_setup dev

3.  Build the Buendia module and set up an OpenMRS server with the module installed:

        tools/openmrs_build

4.  Start the server:

        tools/openmrs_run

5.  When you see the line:

        [INFO] Started Jetty Server

    the server is ready, and you can log in at [[http://localhost:9000/openmrs/]] as "buendia" with password "buendia".

After `tools/openmrs_build` is done, your freshly built module will be an `.omod` file in `openmrs-project/server/openmrs/RELEASE/modules`.  If you need to install it into an OpenMRS server running elsewhere, you can upload this file using the Administration > Manage Modules page.

## IntelliJ IDEA project setup

1.  In the "Welcome" dialog, click **Import Project** and select the `openmrs/pom.xml` file inside your `buendia` repo.  As you proceed through the import wizard:
      * Turn on **Search for projects recursively** and **Import Maven projects automatically**.
      * On the "Select Maven projects to import" page, you only need the project named `org.projectbuendia:projectbuendia.openmrs`.  You can deselect the others to reduce clutter.

2.  After you click **Finish**, wait a few moments for IntelliJ IDEA to import the project.  The projectbuendia.openmrs module should appear in the Project pane on the left.

You're all set!

Most of the interesting code resides in [openmrs/omod/src/main/java/org/openmrs/projectbuendia/webservices.rest](http://github.com/projectbuendia/buendia/openmrs/omod/src/main/java/org/openmrs/projectbuendia/webservices.rest).  You can make changes in IntelliJ IDEA and use the **Build** command to confirm that the module builds correctly.

When you want to test your module, use `tools/openmrs_build` and `tools/openmrs_run` from a Terminal within IntelliJ IDEA or any command shell.  You can also do `tools/openmrs_build -DskipTests` to build without running tests (use with care).


## Debugging the server

If you start the OpenMRS server from the shell with `tools/openmrs_run`, it will run with remote debugging enabled so that you can debug the running server from within IntelliJ IDEA.  To set this up:

1. Click Run > Edit Configurations...

2. Click the little plus button (+) in the top-left corner and select **Remote**.

3. Change "Unnamed" to something recognizable, then click **OK**.

Now when you click Run > Debug and use this configuration, IntelliJ IDEA will connect to the currently running OpenMRS server.
