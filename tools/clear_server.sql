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
DELETE FROM hl7_in_archive;
DELETE FROM hl7_in_error;
DELETE FROM hl7_in_queue;

-- Remove all encounters/observations
DELETE FROM active_list_allergy; -- refers to concept
DELETE FROM active_list_problem;
DELETE FROM active_list; -- refers to person, obs, and users
DELETE FROM concept_proposal_tag_map; -- refers to concept_proposal
DELETE FROM concept_proposal; -- refers to obs and encounter
DELETE FROM note; -- refers to obs, encounter, and patient
DELETE FROM obs; -- refers to order and encounter

-- Remove all orders
DELETE FROM drug_order; -- refers to orders and concepts
DELETE FROM order_type WHERE parent IS NOT NULL; -- refers to orders
DELETE FROM test_order; -- refers to orders
DELETE FROM orders; -- refers to encounter and patient

-- Remove all encounters
DELETE FROM encounter_provider; -- refers to encounter
DELETE FROM encounter; -- refers to patient
DELETE FROM visit; -- refers to patient, location, concept

-- Start with no providers at all
DELETE FROM provider_attribute; -- refers to provider
DELETE FROM provider; -- refers to person

-- Delete cohorts
DELETE FROM cohort_member; -- refers to patient

-- Delete visits
DELETE FROM visit_attribute; -- refers to visit
DELETE FROM visit; -- refers to location

-- Delete forms, first clearing out foreign keys that refer to them.
DELETE FROM xforms_xform;
DELETE FROM form_resource;
UPDATE form_field SET parent_form_field = NULL;
DELETE FROM form_field; -- refers to field, form, and form_field
DELETE FROM form;
DELETE FROM field_answer; -- refers to field and concept
DELETE FROM field;

-- Delete patients.
DELETE FROM patient_identifier; -- refers to patient
DELETE FROM patient_state; -- refers to patient_program
DELETE FROM patient_program; -- refers to patient
DELETE FROM patient;

-- Delete programs
DELETE FROM program_workflow_state; -- referes to program_workflow
DELETE FROM program_workflow; -- referes to program
DELETE FROM program;

-- Remove the locations, first clearing out foreign keys that refer to them.
UPDATE location SET parent_location = NULL;
DELETE FROM location_attribute; -- refers to location, location_attribute_type
DELETE FROM location_attribute_type;
DELETE FROM location_tag_map; -- refers to location and location_tag
DELETE FROM location_tag;
DELETE FROM location;

-- Delete metadata sharing information.
DELETE FROM metadatasharing_exported_package;
DELETE FROM metadatasharing_imported_package;
DELETE FROM metadatasharing_imported_item;

-- Delete notification alerts.
DELETE FROM notification_alert_recipient; -- refers to notification_alert
DELETE FROM notification_alert;

-- Delete serialized objects.
DELETE FROM serialized_object;

-- Keep the OpenMRS identifier type; delete all others.
DELETE FROM patient_identifier_type WHERE name NOT LIKE 'OpenMRS%';

-- Create a temporary table to hold the users that are not to be deleted.
-- For information about the daemon user, see https://wiki.openmrs.org/display/docs/Daemon+User
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
                 uuid = "buendia_user_buendia_admin"
WHERE user_id = @buendia_admin_id;

-- Add "buendia_admin" to the list of users to keep.
INSERT INTO keep_users (SELECT * FROM users WHERE user_id = @buendia_admin_id);

-- Delete records that refer to users that will be deleted.
DELETE FROM notification_alert_recipient WHERE
    user_id IS NOT NULL AND user_id != @admin_id;
DELETE FROM user_property WHERE
    user_id IS NOT NULL AND user_id != @admin_id;
DELETE FROM user_role WHERE
    user_id IS NOT NULL AND user_id != @admin_id;

-- Replace all foreign keys that refer to users that will be deleted.
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

UPDATE field_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

UPDATE hl7_source SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;

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

UPDATE patient_identifier_type SET creator = @buendia_admin_id WHERE
    creator IS NOT NULL AND creator != @admin_id;
UPDATE patient_identifier_type SET retired_by = @buendia_admin_id WHERE
    retired_by IS NOT NULL AND retired_by != @admin_id;

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

-- Finally, it is safe to delete the users.
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

-- Clear the huge concept word and concept reference tables; we don't need them.
DELETE FROM concept_word;
DELETE FROM concept_reference_term_map;
DELETE FROM concept_reference_map;
DELETE FROM concept_reference_term;

-- Delete all the custom Buendia concepts.
SET @max_id := 999999;
DELETE FROM concept_answer WHERE concept_id > @max_id;
DELETE FROM concept_answer WHERE answer_concept > @max_id;
DELETE FROM concept_complex WHERE concept_id > @max_id;
DELETE FROM concept_description WHERE concept_id > @max_id;
DELETE FROM concept_name WHERE concept_id > @max_id;
DELETE FROM concept_numeric WHERE concept_id > @max_id;
DELETE FROM concept_proposal WHERE concept_id > @max_id;
DELETE FROM concept_proposal WHERE obs_concept_id > @max_id;
DELETE FROM concept_set WHERE concept_id > @max_id;
DELETE FROM concept_set WHERE concept_set > @max_id;
DELETE FROM concept_state_conversion WHERE concept_id > @max_id;
DELETE FROM concept_word WHERE concept_id > @max_id;
DELETE FROM drug WHERE concept_id > @max_id;
DELETE FROM drug WHERE dosage_form > @max_id;
DELETE FROM drug WHERE route > @max_id;
DELETE FROM drug_ingredient WHERE concept_id > @max_id;
DELETE FROM drug_ingredient WHERE ingredient_id > @max_id;
DELETE FROM order_frequency WHERE concept_id > @max_id;
DELETE FROM orders WHERE concept_id > @max_id;
DELETE FROM concept WHERE concept_id > @max_id;
