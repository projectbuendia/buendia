/*
 * This script takes a database cleared by clear_server.sql and sets up
 * initial data for the MSF EMC in Magburaka, Sierra Leone.  It will
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
-- DC: Unsure if this will work setting location_id and not setting uuid.
SET @emc_id = 'msf.loc.chad';

INSERT INTO location (location_id, name, creator, date_created) VALUES
    (@emc_id, 'Chad', @admin_id, NOW())
    ON DUPLICATE KEY UPDATE uuid = uuid;

SET @itfc_id = 'msf.loc.chad.bokoro.itfc';

INSERT INTO location (location_id, name, creator, date_created, parent_location) VALUES
    (@itfc_id, 'Bokoro ITFC', @admin_id, NOW(), @emc_id),
    ('msf.loc.chad.bokoro.bokoro-atfc', 'Bokoro ATFC', @admin_id, NOW(), @emc_id),
    ('msf.loc.chad.bokoro.ngama-atfc', 'Ngama ATFC', @admin_id, NOW(), @emc_id),
    ('msf.loc.chad.bokoro.moyto-atfc', 'Moyto ATFC', @admin_id, NOW(), @emc_id)
    ON DUPLICATE KEY UPDATE uuid = uuid;

-- Add the tents.
INSERT INTO location (location_id, name, creator, date_created, parent_location) VALUES
    ('ITFC ICU', @admin_id, NOW(), @itfc_id),
    ('ITFC Phase I', @admin_id, NOW(), @itfc_id),
    ('ITFC Phase I-T', @admin_id, NOW(), @itfc_id),
    ('ITFC Phase II', @admin_id, NOW(), @itfc_id),
    ('Bokoro ATFC', @admin_id, NOW(), 'msf.loc.chad.bokoro.bokoro-atfc'),
    ('Ngama ATFC', @admin_id, NOW(), 'msf.loc.chad.bokoro.ngama-atfc'),
    ('Moyto ATFC', @admin_id, NOW(), 'msf.loc.chad.bokoro.moyto-atfc')
    ON DUPLICATE KEY UPDATE uuid = uuid;
