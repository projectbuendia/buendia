NOTE: this is a snapshot of openmrs-module-sync imported at
fa7c22feab927092fd05c72768bf2948ef7c78a6. See
https://github.com/openmrs/openmrs-module-sync/commit/fa7c22feab927092fd05c72768bf2948ef7c78a6

We do this instead of using a git submodule so that it works better with our build tools and because
we're building a snapshot version anyway.

TODO: Switch to using a submodule instead, or a proper sync module release.

openmrs-module-sync
===================

**Overview**
-------------------
Synchronization is needed when you have multiple sites using OpenMRS with separate databases and you want them to copy data to each other. In this situation, one site has to act as the "Parent", and one acts as the "Child". The Parent is always the computer or site that is most central. Multiple child sites send their data up to the parent site for dissemination. Children/parent sites can be set up in a multi hierarchy way.

The Sync Module works by keeping track of all changes going through the API into the database. These changes are then replayed on the other server(s). The changes are stripped of all primary key references. Instead, UUIDs are used to compare and reference other data. Because of this, the parent/child servers must be exact duplicates when sync is first started.

**Requirements**
-------------------
This module uses code from the [Data Synchronization Project](https://wiki.openmrs.org/display/projects/Data+Synchronization+Project) and should be installed on OpenMRS v1.8.3 or greater.

**Download and Installation**
-------------------
1. [Download module](https://modules.openmrs.org/#/show/sync/)
2. Install the omod with the Manage Modules admin page
3. See [parent server setup](https://wiki.openmrs.org/display/docs/Sync+Module+Parent+Server+Setup) and [child server setup](https://wiki.openmrs.org/display/docs/Sync+Module+Child+Server+Setup) pages for more on how to configure the parent and child servers.

**Updating The Sync Module to a New Version**
-------------------
The sync module is a [Mandatory Module](https://wiki.openmrs.org/display/docs/Mandatory+Module) and cannot be stopped or unloaded without stopping OpenMRS. This prevents any data loss for connected parent/child servers from occurring. However, it means that there is no way to remove the module to install a new one in the traditional fashion.
  1. [Download](http://modules.openmrs.org/modules/view.jsp?module=sync) the latest version of the omod
  2. Option #1:
        a. Visit the Manage Modules admin page and upload the module with the "Upgrade An Existing Module" form on the            right side of the page
  3. Option #2:
        a. Visit the Module Properties admin page (/openmrs/admin/modules/moduleProperties.form) to find where your              modules are stored on the server.
        b. Delete the old sync-0.1.omod file
        c. Copy in the new sync-0.2.omod file
        d. Restart openmrs and/or tomcat

**Uninstalling the Sync Module**
-------------------
1. Set the **sync.mandatory** module to **false**.
2. Visit the Module Properties admin page (/openmrs/admin/modules/moduleProperties.form) to find where your modules      are stored on the server filesystem.
3. Delete the sync-*.omod file
4. Restart openmrs and/or tomcat

To clean up the database, these statements need to be executed. Be careful as you are also deleting all the sync history and can't resume your work with the Sync Module later on.
'''
drop table sync_server_record;
drop table sync_server_class;
drop table sync_server;
drop table sync_class;
drop table sync_record;
drop table sync_import;
delete from global_property where property like 'sync.%';
'''
Additionally you should stop and remove the Scheduled Tasks "Cleanup Old Sync Records" and "master Data Synchronization" (if the automatic sync via web is configured).

**Upgrading OpenMRS when using the Sync Module**
-------------------
When upgrading OpenMRS, occasionally data is modified that is key to sync (a uuid column). This is done outside the API, so care must be taken to update the child sites correctly.

This is the general process you should follow whenever upgrading openmrs
  1. Make sure all sites are sync'd to their parents and no data entry is happening on any parent/child server
  2. Go through the upgrade process on the parent server. (Download+install the new war file, view the webapp and run      all database updates)
  3. Immediately visit "Sync --> Upgrade Scripts" on the administration screen
  4. On that form, if upgrading from 1.5, choose 1.5.* and click the Download button (if your old version is not           there, no updates are needed)
  5. Run the sql script on each child database (mysql -u -p -e"source upgrade.sql" openmrs)
  6. Go through the upgrade process on each child server (similar to the second step)

**WARNINGS!**
-------------------
  1. **Direct Database Updates**
     Once the sync module is installed, all database updates should be done through the openmrs api. Direct sql on the      database should not be used because those changes will not cascade to the parent/child servers.

     Instead use the [groovy module](https://wiki.openmrs.org/display/docs/Groovy+Module) to make calls on the OpenMRS      API to change the database.
  2. **Conflict Handling/Resolution**
     The initial version of the sync module provides no means for conflict handling or resolution: it simply operates      on a last-in-wins model. The assumption is that patient-level conflicts are rare in practical use, and therefore      conflict handling is not a "must-have" feature for a version 1.0 of the system. It is recommended that the Sync       module, as currently designed, NOT be used in situations where frequent conflicts are expected (aka concurrent        patient editing).
  3. **Using Sync with Infopath**
     It is strongly recommended that you don't depend on sync to propagate changes to your infopath forms.

     Infopath XSNs are .cab files that contain a whole bunch of files inside of them.  Some of these files have the        hard-coded paths of the server that one was working on when modifying the form.  The problem is that these            hard-coded strings then get propagated to the other servers through sync (when it's working).   In the past,          we've seen forms from one site being submitted to another site, which caused all kinds of problems.

     It is recommended to block synchronization of org.openmrs.module.formentry, and do all XSN manipulation by hand       on each server.
  4. **Alert and AlertRecipient**
     These objects are supported in Sync Module Beta 2 + and with OpenMRS v1.5.3+, v.1.6.2+, and 1.7+. Any other           version and you will have to "ignore" the class "org.openmrs.notification.AlertRecipient" from both sending and       receiving in order to avoid errors.
