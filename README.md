OpenMRS-module
==============

Preparation
-----------

- Make sure you have JDK 7 installed
- Install the OpenMRS SDK installed:
  https://wiki.openmrs.org/display/docs/OpenMRS+SDK
- Check with "omrs-version"

Building and running
--------------------

Enter the following commands, replacing
/opt/omrssdk-1.0.7/apache-maven/bin with your installation of Maven.
(If you have Maven installed already, type `which mvn` to find the
relevant path.)

```
cd modules
./install-modules /opt/omrssdk-1.0.7/apache-maven/bin
cd projectbuendia.openmrs
omrs-run -Pinstall-wizard
```

This will:
- Install the xforms jar files (omod and api) into the local Maven repo
- Build the module
- Run the tests
- Start OpenMRS

Visit http://localhost:8080/openmrs to view the server; on the first
run, it wlil ask you to go through the installation wizard. If this is
the only version of OpenMRS you want on your system, you can use the
"simple" option (and add demo data). Otherwise, use the "advanced"
option and create a new MySQL database (e.g. openmrs-sdk).

For future runs, just use `omrs-run` (no `-Pinstall-wizard`).

Note: you need to make sure Maven is running with Java 7. The exact
way of doing this will depend on your platform, but
http://stackoverflow.com/questions/18813828 gives a good starting point.
If you run `which omrs-run` you'll find out where OpenMRS SDK was
installed; under that directory, there's an `apache-maven` directory
with a `bin` directory under that - if you run

    /opt/omrssdk-1.0.7/apache-maven/bin/mvn -v
    
(or the equivalent, based on your SDK location) you should see
something like:

```
Apache Maven 3.1.0 (893ca28a1da9d5f51ac03827af98bb730128f9f2;
2013-06-28 03:15:32+0100)
Maven home: /opt/omrssdk-1.0.7/apache-maven
Java version: 1.7.0_65, vendor: Oracle Corporation
Java home: /usr/lib/jvm/java-7-openjdk-amd64/jre
Default locale: en_GB, platform encoding: UTF-8
OS name: "linux", version: "3.13.0-39-generic", arch: "amd64", family: "unix"
```

The bit with "Java version" is important - it needs to be Java 1.7.
Not Java 1.6 (or we won't be able to use Java 7 language constructs,
which really help) and not Java 1.8 (which OpenMRS currently thinks
isn't as recent as Java 1.6 - see https://issues.openmrs.org/browse/TRUNK-4514).

REST collections exposed
----

This module exposes the following collections, under the
`projectbuendia` namespace:

xform
=====

(e.g. http://localhost:8080/openmrs/ws/rest/v1/projectbuendia/xform)

Template xforms from the OpenMRS forms database.

Supports:

- List forms, e.g. .../xform
- Fetch an individual form, e.g. .../xform/{form-uuid}
- Default representation (just metadata) and full representation
  (includes an `xml` property with the full XML)
  
For the full representation, use `?v=full` at the end of the URL.
