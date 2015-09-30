/*
 * This script takes a database cleared by clear_server.sql and sets up
 * initial data for the MSF Nutrition Programme in Bokoro, Chad.  It will
 * also double checks some data that should already be present and adds
 * it if missing.
 *
 * Bear in mind this assumes you are starting from a snapshot of our
 * development database and so doesn't add the whole concept dictionary.
 */

-- The OpenMRS user account used by the server will be set up by the
-- buendia-server package to have a username and password determined by
-- the SERVER_OPENMRS_USER and SERVER_OPENMRS_PASSWORD settings.

-- The Guest User account for end users will be created by the server.

SELECT @admin_id := user_id FROM users WHERE system_id = 'admin';

-- Make sure the set of allowed locales is correct.
UPDATE global_property SET property_value = 'en, en_GB_client'
    WHERE property = 'locale.allowed.list';

-- Make sure camp and zone locations are present.
-- ON DUPLICATE IGNORE is safe as there is a unique index on uuid.
INSERT INTO location (name, creator, date_created, uuid) VALUES
    ('Chad', @admin_id, NOW(), 'c149ee48-bcda-4661-a3bf-d98847dfd18c')
    ON DUPLICATE KEY UPDATE uuid = uuid;

SELECT @emc_id := location_id FROM location
    WHERE uuid = 'c149ee48-bcda-4661-a3bf-d98847dfd18c';

INSERT INTO location (name, creator, date_created, uuid, parent_location) VALUES
    ('Bokoro ITFC', @admin_id, NOW(), '69890fb4-49bb-4c14-a834-b1dead6a34df', @emc_id),
    ('Bokoro ATFC', @admin_id, NOW(), 'e8f112b2-e121-43b3-b1cc-d5f16a88e567', @emc_id),
    ('Ngama ATFC', @admin_id, NOW(), '0c96ad58-f580-4f0a-88ba-cafb88b406a8', @emc_id),
    ('Moyto ATFC', @admin_id, NOW(), 'e8c01a9c-1cb1-43dc-9478-4bcada286bde', @emc_id)
    ON DUPLICATE KEY UPDATE uuid = uuid;

-- Add the tents.
SELECT @bokoro_itfc_id := location_id FROM location
    WHERE uuid='69890fb4-49bb-4c14-a834-b1dead6a34df';
SELECT @bokoro_atfc_id := location_id FROM location
    WHERE uuid='e8f112b2-e121-43b3-b1cc-d5f16a88e567';
SELECT @ngama_atfc_id := location_id FROM location
    WHERE uuid = '0c96ad58-f580-4f0a-88ba-cafb88b406a8';
SELECT @moyto_atfc_id := location_id FROM location
    WHERE uuid = 'e8c01a9c-1cb1-43dc-9478-4bcada286bde';

INSERT INTO location (name, creator, date_created, uuid, parent_location) VALUES
    ('ITFC ICU', @admin_id, NOW(), '03213d10-550f-4253-bf66-d7c6ec8d1eb5', @bokoro_itfc_id),
    ('ITFC Phase I', @admin_id, NOW(), '3a15c457-ef0d-4129-8f61-893b084977fa', @bokoro_itfc_id),
    ('ITFC Phase I-T', @admin_id, NOW(), '59964e20-bbb9-4c5e-8a0b-a2d7b656362d', @bokoro_itfc_id),
    ('ITFC Phase II', @admin_id, NOW(), '0c3740e1-a204-4f2b-bceb-97a1334bd653', @bokoro_itfc_id),
    ('Bokoro ATFC', @admin_id, NOW(), '3d3da080-2493-4733-93f2-5fcc1faa3403', @bokoro_atfc_id),
    ('Ngama ATFC', @admin_id, NOW(), '3086db2b-50ad-4bfb-b7eb-19f904660ca1', @ngama_atfc_id),
    ('Moyto ATFC', @admin_id, NOW(), '0a01b40c-bc74-4910-8d56-b7a408af4aff', @moyto_atfc_id)
    ON DUPLICATE KEY UPDATE uuid = uuid;
