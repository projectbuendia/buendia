openmrs-module-xforms
=====================

**Overview**
=====================
This module serves as one of the alternatives to Microsoft InfoPath for OpenMRS data entry.
The module converts an OpenMRS form to an XForm.
Data entry can be done using any browser that supports JavaScript. The browser which has been tested most frequently with this module is Mozilla Firefox and hence is the recommended.

This module also consumes and serves OpenMRS forms as XForms from and to applications that use the XForms standard.
An example of such applications are the XForms Mobile Data Collection tools for OpenMRS.
The communication between the mobile device and this module can take places via HTTP, Bluetooth or SMS.
The Bluetooth and SMS communication is implemented by the OpenMRS Bluetooth and SMS scheduled tasks respectively.
Applications that work in offline mode will normally start by downloading a set of patients to collect data for. So these patient sets are supplied by this module.
To ensure that only authorized users will access these applications, the module also serves the users to be downloaded and used for such purposes.

**Installation**
=====================
1. Download the [latest version](https://dev.openmrs.org/modules/view.jsp?module=xforms) from the OpenMRS module repository and add it to your OpenMRS application using the Administration->Manage Modules page.

**Entering Data**
=====================
1. Search for a patient and select him or her.
2. Click the Form Entry tab.
3. Click Edit to open an existing encounter. If you want to fill a new encounter, select the form you want to fill and Click the add button to display the form.
4. After filling the form, click the Submit button to save it or Cancel button to discard the changes and go back to the patient screen.

**Known Issues**
=====================
1. When using Bluetooth to connect to openmrs, any changes to form definitions are received only after restarting OpenMRS or the Bluetooth service.

**Additional Information**
=====================
Wiki Page - [XForms](https://wiki.openmrs.org/display/docs/XForms+Module)        
JIRA Page - [XForms](https://issues.openmrs.org/browse/XFRM/?selectedTab=com.atlassian.jira.jira-projects-plugin:summary-panel)
