/*
 *
 * WARNING
 * 
 * This SQL script resets an OpenMRS database to an empty state.
 * The aim of it is to still be usable, so have key critical users and 
 * locations and forms. However, it will have no demo data and may be missing
 * other important set-up information (like tents).
 * 
 * Before running, make sure you have a backup.
 *
 * To get data for a clean start in a new location run "add_fresh_start_data.sql".
 * To get data for a realistic demo run "add_demo_data.sql".
 *
 */


-- HL7 forms are used for xform submission
DELETE FROM hl7_in_archive WHERE 1;
DELETE FROM hl7_in_error WHERE 1;
DELETE FROM hl7_in_queue WHERE 1;

-- Remove all encounters/observations
DELETE FROM note WHERE 1;
DELETE FROM obs WHERE 1;
DELETE FROM encounter_provider WHERE 1;
DELETE FROM encounter WHERE 1;

-- At the moment we aren't using location tags, clear them out.
DELETE FROM location_tag_map WHERE 1;
DELETE FROM location_tag WHERE 1;

-- Start with no providers at all
DELETE FROM provider WHERE 1;

-- No patients in any cohorts
DELETE FROM cohort_member WHERE 1;

-- No visits
DELETE FROM visit WHERE 1;

-- Delete all forms except the known charts and XForms
-- Create a temporary table to keep a list of forms which are not being deleted.
CREATE TEMPORARY TABLE keep_forms SELECT * FROM form WHERE uuid IN (
    "c47d4d3d-f9a3-4f97-9623-d7acee81d401", -- new patient
    "736b90ee-fda6-4438-a6ed-71acd36381f3", -- new observation
    "ea43f213-66fb-4af6-8a49-70fd6b9ce5d4", -- chart observations
    "975afbce-d4e3-4060-a25f-afcd0e5564ef", -- chart constants
    "34d727a6-e515-4f27-ae91-703ba2c164ae"  -- test results
);

-- Clear out the parent_form_field otherwise foreign key constraints stop the delete.
UPDATE form_field SET parent_form_field = NULL WHERE form_id NOT IN (SELECT form_id FROM keep_forms);
DELETE FROM xforms_xform WHERE form_id NOT IN (SELECT form_id FROM keep_forms);
DELETE FROM form_field WHERE form_id NOT IN (SELECT form_id FROM keep_forms);
DELETE FROM form_resource WHERE form_id NOT IN (SELECT form_id FROM keep_forms);
DELETE FROM form WHERE form_id NOT IN (SELECT form_id FROM keep_forms);

-- Delete patients and people.
DELETE FROM patient_identifier WHERE 1;
DELETE FROM patient_state WHERE 1;
DELETE FROM patient_program WHERE 1;
DELETE FROM patient WHERE 1;

-- Create a temporary table to hold the users that are not to be deleted.
-- For information about the daemon user see https://wiki.openmrs.org/display/docs/Daemon+User
CREATE TEMPORARY TABLE keep_users SELECT * FROM users WHERE (system_id IN ("admin", "daemon"));

DELETE FROM person_address WHERE person_id NOT IN (SELECT person_id FROM keep_users);
DELETE FROM person_attribute WHERE person_id NOT IN (SELECT person_id FROM keep_users);
DELETE FROM notification_alert_recipient WHERE user_id NOT IN (SELECT user_id FROM keep_users);
DELETE FROM user_property WHERE user_id NOT IN (SELECT user_id FROM keep_users);
DELETE FROM user_role WHERE user_id NOT IN (SELECT user_id FROM keep_users);

-- Remove the foreign keys for creator etc. before deleting. Pretend all users modified by admin.
SELECT @admin_id := user_id FROM users WHERE system_id = 'admin';
UPDATE person_name SET creator = @admin_id WHERE creator IS NOT NULL;
UPDATE person SET creator = @admin_id WHERE creator IS NOT NULL; 
UPDATE person SET voided_by = @admin_id WHERE voided_by IS NOT NULL; 

DELETE FROM person_name WHERE person_id NOT IN (SELECT person_id FROM keep_users);
DELETE FROM users WHERE user_id NOT IN (SELECT user_id FROM keep_users);
DELETE FROM person WHERE person_id NOT IN (SELECT person_id FROM keep_users);

-- Keep the MSF type and the OpenMRS identifier type, delete all others.
DELETE FROM patient_identifier_type WHERE name != 'OpenMRS Identification Number' AND name != 'MSF';

-- Remove all locations except the facility and the known zones.
CREATE TEMPORARY TABLE keep_locations SELECT * FROM location WHERE uuid IN (
    "3449f5fe-8e6b-4250-bcaa-fca5df28ddbf", -- EMC
    "3f75ca61-ec1a-4739-af09-25a84e3dd237", -- Triage Zone
    "2f1e2418-ede6-481a-ad80-b9939a7fde8e", -- Suspected Zone
    "3b11e7c8-a68a-4a5f-afb3-a4a053592d0e", -- Probable Zone
    "b9038895-9c9d-4908-9e0d-51fd535ddd3c", -- Confirmed Zone
    "4ef642b9-9843-4d0d-9b2b-84fe1984801f", -- Morgue
    "d7ca63c3-6ea0-4357-82fd-0910cc17a2cb" -- Discharged
);

-- Clear out the parent to allow deleting locations without violating foreign key constraints.
UPDATE location SET parent_location = NULL WHERE location_id NOT IN (SELECT location_id FROM keep_locations);
DELETE FROM location WHERE location_id NOT IN (SELECT location_id FROM keep_locations);
