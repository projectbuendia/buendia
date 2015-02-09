/*
 * WARNING
 * 
 * This SQL script resets an OpenMRS database to an empty state.
 * The aim of it is to still be usable, so have key critical users and 
 * locations and forms. However, it will have no demo data and may be missing
 * other important set-up information (like tents).
 * 
 * Before running, make sure you have a backup.
 *
 * To get data for a demo or a clean start in a new location, see the
 * buendia-db-init package.
 */


-- HL7 forms are used for xform submission
DELETE FROM hl7_in_archive WHERE 1;
DELETE FROM hl7_in_error WHERE 1;
DELETE FROM hl7_in_queue WHERE 1;

-- Remove all orders
DELETE FROM drug_order WHERE 1; -- references orders
DELETE FROM order_type WHERE parent IS NOT NULL; -- references orders
DELETE FROM test_order WHERE 1; -- references orders
DELETE FROM orders WHERE 1; -- references encounter

-- Remove all encounters/observations
DELETE FROM active_list WHERE 1; -- references obs
DELETE FROM concept_proposal_tag_map WHERE 1; -- references concept_proposal
DELETE FROM concept_proposal WHERE 1; -- references obs, encounter
DELETE FROM note WHERE 1; -- references obs, encounter
DELETE FROM obs WHERE 1; -- references encounter
DELETE FROM encounter_provider WHERE 1; -- references encounter
DELETE FROM encounter WHERE 1;

-- At the moment we aren't using location tags, clear them out.
DELETE FROM location_tag_map WHERE 1;
DELETE FROM location_tag WHERE 1;

-- Start with no providers at all
DELETE FROM provider_attribute WHERE 1; -- references provider
DELETE FROM provider WHERE 1;

-- No patients in any cohorts
DELETE FROM cohort_member WHERE 1;

-- No visits
DELETE FROM visit_attribute WHERE 1; -- references visit
DELETE FROM visit WHERE 1;

-- Create a temporary table to keep a list of forms which are not being deleted.
CREATE TEMPORARY TABLE keep_forms SELECT * FROM form WHERE uuid IN (
    "c47d4d3d-f9a3-4f97-9623-d7acee81d401", -- new patient
    "736b90ee-fda6-4438-a6ed-71acd36381f3", -- new observation
    "ea43f213-66fb-4af6-8a49-70fd6b9ce5d4", -- chart observations
    "975afbce-d4e3-4060-a25f-afcd0e5564ef", -- chart constants
    "34d727a6-e515-4f27-ae91-703ba2c164ae"  -- test results
);

-- Remove all other forms, first clearing out foreign keys that refer to them
UPDATE form_field SET parent_form_field = NULL WHERE
    form_id NOT IN (SELECT form_id FROM keep_forms);
DELETE FROM xforms_xform WHERE form_id NOT IN (SELECT form_id FROM keep_forms);
DELETE FROM form_resource WHERE form_id NOT IN (SELECT form_id FROM keep_forms);
DELETE FROM form_field WHERE form_id NOT IN (SELECT form_id FROM keep_forms);
DELETE FROM form WHERE form_id NOT IN (SELECT form_id FROM keep_forms);

-- Delete patients.
DELETE FROM patient_identifier WHERE 1; -- refers to patient
DELETE FROM patient_state WHERE 1; -- refers to patient_program
DELETE FROM patient_program WHERE 1; -- refers to patient
DELETE FROM patient WHERE 1;

-- Keep the MSF type and the OpenMRS identifier type; delete all others.
DELETE FROM patient_identifier_type WHERE
    name != 'OpenMRS Identification Number' AND name != 'MSF';

-- Create a temporary table to hold the users that are not to be deleted.
-- For information about the daemon user see https://wiki.openmrs.org/display/docs/Daemon+User
CREATE TEMPORARY TABLE keep_users SELECT * FROM users WHERE system_id IN
    ("admin", "daemon");

-- Create a "buendia_admin" user, and assign to it all the local changes
-- that we want to keep that were made by users other than "admin".
SET @admin_id := (SELECT user_id FROM users WHERE system_id = 'admin');
SET @admin_person_id := (SELECT person_id FROM users WHERE system_id = 'admin');
SET @buendia_admin_id := (SELECT MAX(user_id) FROM keep_users) + 1;

-- Ensure that a row exists with user_id = @buendia_admin_id.
INSERT IGNORE INTO users (user_id, creator, person_id)
    VALUES (@buendia_admin_id, @admin_id, @admin_person_id);

-- Populate or overwrite all the other fields in the row.
UPDATE users SET user_id = @buendia_admin_id,
                 system_id = "buendia_admin",
                 username = "buendia_admin",
                 password = NULL,
                 salt = NULL,
                 secret_question = NULL,
                 secret_answer = NULL,
                 creator = @admin_id,
                 date_created = NOW(),
                 changed_by = @admin_id,
                 date_changed = NOW(),
                 retired = 0,
                 retired_by = NULL,
                 date_retired = NULL,
                 retire_reason = NULL,
                 uuid = "09f979d7-b091-11e4-bc78-040ccecfdba4" WHERE
    user_id = @buendia_admin_id;

-- Add "buendia_admin" to the list of users we want to keep.
INSERT INTO keep_users (SELECT * FROM users WHERE user_id = @buendia_admin_id);

-- Delete all other users, first clearing out foreign keys that refer to them.
DELETE FROM notification_alert_recipient WHERE
    user_id IS NOT NULL AND user_id != @admin_id;
DELETE FROM user_property WHERE
    user_id IS NOT NULL AND user_id != @admin_id;
DELETE FROM user_role WHERE
    user_id IS NOT NULL AND user_id != @admin_id;

UPDATE active_list SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE active_list SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE active_list_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE active_list_type SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE care_setting SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE care_setting SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE care_setting SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE cohort SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE cohort SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE cohort SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE concept SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE concept SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE concept SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE concept_answer SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE concept_class SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE concept_class SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE concept_datatype SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE concept_datatype SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE concept_description SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE concept_description SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE concept_map_type SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE concept_map_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE concept_map_type SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE concept_name SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE concept_name SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE concept_proposal SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE concept_proposal SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE concept_reference_map SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE concept_reference_map SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE concept_reference_source SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE concept_reference_source SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE concept_reference_term SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE concept_reference_term SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE concept_reference_term SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE concept_reference_term_map SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE concept_reference_term_map SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE concept_set SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE drug SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE drug SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE drug SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE drug_reference_map SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE drug_reference_map SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE drug_reference_map SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE encounter SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE encounter SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE encounter SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE encounter_provider SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE encounter_provider SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE encounter_provider SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE encounter_role SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE encounter_role SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE encounter_role SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE encounter_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE encounter_type SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE field SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE field SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE field SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE field_answer SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE field_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE form SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE form SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE form SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE form_field SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE form_field SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;

UPDATE hl7_source SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE location SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE location SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE location SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE location_attribute SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE location_attribute SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE location_attribute SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE location_attribute_type SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE location_attribute_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE location_attribute_type SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE location_tag SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE location_tag SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE location_tag SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE note SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE note SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE notification_alert SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE notification_alert SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;

UPDATE obs SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE obs SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE order_frequency SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE order_frequency SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE order_frequency SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE order_type SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE order_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE order_type SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE orders SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE orders SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE patient SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE patient SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE patient SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE patient_identifier SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE patient_identifier SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;
UPDATE patient_identifier SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;

UPDATE patient_identifier_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE patient_identifier_type SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE patient_program SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE patient_program SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE patient_program SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE patient_state SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE patient_state SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE patient_state SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE person SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE person SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE person SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE person_address SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE person_address SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;
UPDATE person_address SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;

UPDATE person_attribute SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE person_attribute SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE person_attribute SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE person_attribute_type SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE person_attribute_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE person_attribute_type SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE person_merge_log SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE person_merge_log SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE person_merge_log SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE person_name SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE person_name SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE program SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE program SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;

UPDATE program_workflow SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE program_workflow SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE program_workflow_state SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE program_workflow_state SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE provider SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE provider SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE provider SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE provider_attribute SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE provider_attribute SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE provider_attribute SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE provider_attribute_type SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE provider_attribute_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE provider_attribute_type SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE relationship SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE relationship SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE relationship SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE relationship_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE relationship_type SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE report_object SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE report_object SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE report_object SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE reporting_report_design SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE reporting_report_design SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE reporting_report_design SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE reporting_report_design_resource SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;
UPDATE reporting_report_design_resource SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE reporting_report_design_resource SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE reporting_report_processor SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE reporting_report_processor SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE reporting_report_processor SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE reporting_report_request SET requested_by = @buendia_admin_id WHERE
    requested_by IS NOT NULL AND requested_by != @admin_id;

UPDATE scheduler_task_config SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE scheduler_task_config SET created_by = @buendia_admin_id WHERE
    created_by IS NOT NULL AND created_by != @admin_id;

UPDATE serialized_object SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE serialized_object SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE serialized_object SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE users SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE users SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE users SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE visit SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE visit SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE visit SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE visit_attribute SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE visit_attribute SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE visit_attribute SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE visit_attribute_type SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE visit_attribute_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE visit_attribute_type SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE visit_type SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE visit_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE visit_type SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

UPDATE xforms_person_repeat_attribute SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;
UPDATE xforms_person_repeat_attribute SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE xforms_person_repeat_attribute SET voided_by = @buendia_admin_id WHERE
    voided_by IS NOT NULL AND voided_by != @admin_id;

UPDATE xforms_xform SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE xforms_xform SET changed_by = @buendia_admin_id WHERE
    changed_by IS NOT NULL AND changed_by != @admin_id;

DELETE FROM users WHERE user_id NOT IN (SELECT user_id FROM keep_users);

-- Delete all other persons, first clearing out foreign keys that refer to them.
DELETE FROM person_address WHERE
    person_id NOT IN (SELECT person_id FROM keep_users);
DELETE FROM person_attribute WHERE
    person_id NOT IN (SELECT person_id FROM keep_users);
DELETE FROM person_merge_log WHERE
    winner_person_id NOT IN (SELECT person_id FROM keep_users);
DELETE FROM person_merge_log WHERE
    loser_person_id NOT IN (SELECT person_id FROM keep_users);
DELETE FROM person_name WHERE
    person_id NOT IN (SELECT person_id FROM keep_users);
DELETE FROM relationship WHERE
    person_a NOT IN (SELECT person_id FROM keep_users);
DELETE FROM relationship WHERE
    person_b NOT IN (SELECT person_id FROM keep_users);
DELETE FROM xforms_person_repeat_attribute WHERE
    person_id NOT IN (SELECT person_id FROM keep_users);

DELETE FROM person WHERE person_id NOT IN (SELECT person_id FROM keep_users);

-- Create a temporary table for the locations to keep.
CREATE TEMPORARY TABLE keep_locations SELECT * FROM location WHERE uuid IN (
    "3449f5fe-8e6b-4250-bcaa-fca5df28ddbf", -- EMC
    "3f75ca61-ec1a-4739-af09-25a84e3dd237", -- Triage Zone
    "2f1e2418-ede6-481a-ad80-b9939a7fde8e", -- Suspected Zone
    "3b11e7c8-a68a-4a5f-afb3-a4a053592d0e", -- Probable Zone
    "b9038895-9c9d-4908-9e0d-51fd535ddd3c", -- Confirmed Zone
    "4ef642b9-9843-4d0d-9b2b-84fe1984801f", -- Morgue
    "d7ca63c3-6ea0-4357-82fd-0910cc17a2cb" -- Discharged
);

-- Remove other locations, first clearing out foreign keys that refer to them.
UPDATE location SET parent_location = NULL WHERE
    location_id NOT IN (SELECT location_id FROM keep_locations);
DELETE FROM location_attribute WHERE
    location_id NOT IN (SELECT location_id FROM keep_locations);
DELETE FROM location WHERE
    location_id NOT IN (SELECT location_id FROM keep_locations);
