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

Enter the following commands:

```
cd projectbuendia.openmrs
omrs-run
```

This will:
- Build the module
- Run the tests
- Start OpenMRS

Visit http://localhost:8080/openmrs to view the server; on the first
run, it will take a while building the database etc, but after that the
database will remain for future use.
