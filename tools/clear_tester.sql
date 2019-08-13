/*
 * This SQL script deletes everything associated with the "tester" account.
 * To reset a server to a clean state for testing, run this script and then
 * create the "tester" account with:
 *
 * buendia-openmrs-account-setup tester tester
 */

SET @user_id := (SELECT user_id FROM users WHERE username = 'tester');
SET @person_id := (SELECT person_id FROM users WHERE username = 'tester');
SET @buendia_admin_id := (SELECT user_id FROM users WHERE username = 'buendia_admin');
SET @admin_person_id := (SELECT person_id FROM users WHERE system_id = 'admin');

CREATE TEMPORARY TABLE delete_users
    SELECT * FROM users WHERE creator = @user_id or user_id = @user_id;

-- Remove concepts
DELETE FROM concept_word WHERE concept_name_id IN
    (SELECT concept_name_id FROM concept_name WHERE creator IN (SELECT user_id FROM delete_users));
DELETE FROM concept_word WHERE concept_name_id IN
    (SELECT concept_id FROM concept WHERE creator IN (SELECT user_id FROM delete_users));
DELETE FROM concept_name WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM concept WHERE creator IN (SELECT user_id FROM delete_users);

-- Remove observations
DELETE FROM note WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM obs WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM obs WHERE person_id IN (SELECT person_id FROM delete_users);

-- Remove orders
DELETE FROM order_frequency WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM order_type WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM orders WHERE creator IN (SELECT user_id FROM delete_users);

-- Remove encounters
DELETE FROM encounter_role WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM encounter_provider WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM encounter WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM visit WHERE creator IN (SELECT user_id FROM delete_users);

-- Remove providers
DELETE FROM provider_attribute WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM provider WHERE creator IN (SELECT user_id FROM delete_users);

-- Remove forms, first clearing out foreign keys that refer to them.
DELETE FROM xforms_xform WHERE creator IN (SELECT user_id FROM delete_users);
UPDATE form_field SET parent_form_field = NULL WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM form_field WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM form WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM field_answer WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM field WHERE creator IN (SELECT user_id FROM delete_users);

-- Remove patients.
DELETE FROM patient_identifier WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM patient WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM patient WHERE patient_id IN (SELECT person_id FROM delete_users);

-- Replace all other foreign keys that refer to the user.
UPDATE concept SET changed_by = @buendia_admin_id WHERE changed_by IN (SELECT user_id FROM delete_users);
UPDATE concept SET retired_by = @buendia_admin_id WHERE retired_by IN (SELECT user_id FROM delete_users);
UPDATE encounter SET changed_by = @buendia_admin_id WHERE changed_by IN (SELECT user_id FROM delete_users);
UPDATE encounter SET voided_by = @buendia_admin_id WHERE voided_by IN (SELECT user_id FROM delete_users);
UPDATE form SET changed_by = @buendia_admin_id WHERE changed_by IN (SELECT user_id FROM delete_users);
UPDATE form SET retired_by = @buendia_admin_id WHERE retired_by IN (SELECT user_id FROM delete_users);
UPDATE obs SET voided_by = @buendia_admin_id WHERE voided_by IN (SELECT user_id FROM delete_users);
UPDATE orders SET voided_by = @buendia_admin_id WHERE voided_by IN (SELECT user_id FROM delete_users);
UPDATE patient SET changed_by = @buendia_admin_id WHERE changed_by IN (SELECT user_id FROM delete_users);
UPDATE patient SET voided_by = @buendia_admin_id WHERE voided_by IN (SELECT user_id FROM delete_users);
UPDATE patient_identifier SET changed_by = @buendia_admin_id WHERE changed_by IN (SELECT user_id FROM delete_users);
UPDATE patient_identifier SET voided_by = @buendia_admin_id WHERE voided_by IN (SELECT user_id FROM delete_users);
UPDATE patient_identifier_type SET creator = @buendia_admin_id WHERE creator IN (SELECT user_id FROM delete_users);
UPDATE patient_identifier_type SET retired_by = @buendia_admin_id WHERE retired_by IN (SELECT user_id FROM delete_users);
UPDATE person SET changed_by = @buendia_admin_id WHERE changed_by IN (SELECT user_id FROM delete_users);
UPDATE person SET voided_by = @buendia_admin_id WHERE voided_by IN (SELECT user_id FROM delete_users);
UPDATE person_attribute SET changed_by = @buendia_admin_id WHERE changed_by IN (SELECT user_id FROM delete_users);
UPDATE person_attribute SET voided_by = @buendia_admin_id WHERE voided_by IN (SELECT user_id FROM delete_users);
UPDATE provider SET changed_by = @buendia_admin_id WHERE changed_by IN (SELECT user_id FROM delete_users);
UPDATE provider SET retired_by = @buendia_admin_id WHERE retired_by IN (SELECT user_id FROM delete_users);
UPDATE provider_attribute SET changed_by = @buendia_admin_id WHERE changed_by IN (SELECT user_id FROM delete_users);
UPDATE provider_attribute SET voided_by = @buendia_admin_id WHERE voided_by IN (SELECT user_id FROM delete_users);
UPDATE users SET creator = @buendia_admin_id WHERE creator IN (SELECT user_id FROM delete_users);
UPDATE users SET changed_by = @buendia_admin_id WHERE changed_by IN (SELECT user_id FROM delete_users);
UPDATE users SET retired_by = @buendia_admin_id WHERE retired_by IN (SELECT user_id FROM delete_users);

-- Break the person -> user -> person cycle.
UPDATE users SET person_id = @admin_person_id WHERE user_id IN (SELECT user_id FROM delete_users);
UPDATE users SET person_id = @admin_person_id WHERE person_id IN (SELECT person_id FROM delete_users);

-- Remove persons created by the user.
DELETE FROM person_attribute WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM person_attribute WHERE person_id IN
    (SELECT person_id FROM person WHERE creator IN (SELECT user_id FROM delete_users));
DELETE FROM person_name WHERE creator IN (SELECT user_id FROM delete_users);
DELETE FROM person_name WHERE person_id IN
    (SELECT person_id FROM person WHERE creator IN (SELECT user_id FROM delete_users));
DELETE FROM patient_identifier WHERE patient_id IN
    (SELECT person_id FROM person WHERE creator IN (SELECT user_id FROM delete_users));
DELETE FROM patient WHERE patient_id IN
    (SELECT person_id FROM person WHERE creator IN (SELECT user_id FROM delete_users));
DELETE FROM person WHERE creator IN (SELECT user_id FROM delete_users);

-- Remove the user and users created by the user.
DELETE FROM user_property WHERE user_id IN (SELECT user_id FROM delete_users);
DELETE FROM user_role WHERE user_id IN (SELECT user_id FROM delete_users);
DELETE FROM users WHERE user_id IN (SELECT user_id FROM delete_users);
DELETE FROM users WHERE person_id IN (SELECT person_id FROM delete_users);

-- Remove persons associated with the user.
DELETE FROM provider WHERE person_id IN (SELECT person_id FROM delete_users);
DELETE FROM person_attribute WHERE person_id IN (SELECT person_id FROM delete_users);
DELETE FROM person_name WHERE person_id IN (SELECT person_id FROM delete_users);
DELETE FROM person WHERE person_id IN (SELECT person_id FROM delete_users);

