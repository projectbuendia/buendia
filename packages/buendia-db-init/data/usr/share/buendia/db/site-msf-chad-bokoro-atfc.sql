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
    ('ROOT LOCATION', @admin_id, NOW(), '3449f5fe-8e6b-4250-bcaa-fca5df28ddbf')
    ON DUPLICATE KEY UPDATE uuid = uuid;

SELECT @emc_id := location_id FROM location
    WHERE uuid = '3449f5fe-8e6b-4250-bcaa-fca5df28ddbf';

INSERT INTO location (name, creator, date_created, uuid, parent_location) VALUES
    ('Bokoro', @admin_id, NOW(), '69890fb4-49bb-4c14-a834-b1dead6a34df', @emc_id)
    ON DUPLICATE KEY UPDATE uuid = uuid;

-- Add the tents.
SELECT @bokoro_id := location_id FROM location
    WHERE uuid='69890fb4-49bb-4c14-a834-b1dead6a34df';

INSERT INTO location (name, creator, date_created, uuid, parent_location) VALUES
    ('UNTI SI', @admin_id, NOW(), '03213d10-550f-4253-bf66-d7c6ec8d1eb5', @bokoro_id),
    ('UNTI Phase I', @admin_id, NOW(), '3a15c457-ef0d-4129-8f61-893b084977fa', @bokoro_id),
    ('UNTI Phase I-T', @admin_id, NOW(), '59964e20-bbb9-4c5e-8a0b-a2d7b656362d', @bokoro_id),
    ('UNTI Phase II', @admin_id, NOW(), '0c3740e1-a204-4f2b-bceb-97a1334bd653', @bokoro_id),
    ('Maigana UNTA', @admin_id, NOW(), '768070fa-72f9-4e82-862d-293508715677', @bokoro_id),
    ('Arbouchatak UNTA', @admin_id, NOW(), '219b09bc-af37-40a8-84f4-a1cbf00238a9', @bokoro_id),
    ('Djokana UNTA', @admin_id, NOW(), 'f2e198e8-0f6b-4297-8850-291f66b001d5', @bokoro_id),
    ('Gama 1 UNTA', @admin_id, NOW(), '3086db2b-50ad-4bfb-b7eb-19f904660ca1', @bokoro_id),
    ('Tersefe UNTA', @admin_id, NOW(), 'ff172222-f39d-49b7-981a-becacff59a5c', @bokoro_id),
    ('Gama 2 UNTA', @admin_id, NOW(), '99d09d16-9247-43f2-98ac-587adb202e43', @bokoro_id),
    ('Dilbini UNTA', @admin_id, NOW(), '34ddf5d6-0254-406f-a323-d44598b1ca57', @bokoro_id),
    ('Ouled-Bili UNTA', @admin_id, NOW(), '38808aa4-9df0-4755-8259-109c49299688', @bokoro_id),
    ('Bisney UNTA', @admin_id, NOW(), '3c6c2634-de50-4084-89e1-8bad27a5f7b8', @bokoro_id),
    ('Abirebi UNTA', @admin_id, NOW(), '06e0c3d9-c496-47cf-9e8a-b829fbad3c15', @bokoro_id),
    ('Tchaway UNTA', @admin_id, NOW(), '6b1512f0-fc74-4bab-87fe-e49d5212138f', @bokoro_id),
    ('Dilema UNTA', @admin_id, NOW(), '69641751-9a2c-476f-bd63-1a4a442ee804', @bokoro_id),
    ('Gambir UNTA', @admin_id, NOW(), 'cf38f962-9030-4ac5-b769-0e902ba0917b', @bokoro_id),
    ('Bokoro 1 UNTA', @admin_id, NOW(), '3d3da080-2493-4733-93f2-5fcc1faa3403', @bokoro_id),
    ('Abgode UNTA', @admin_id, NOW(), 'd48a0b7a-52da-4db7-b133-0d67e1874124', @bokoro_id),
    ('Moito UNTA', @admin_id, NOW(), '0a01b40c-bc74-4910-8d56-b7a408af4aff', @bokoro_id)
    ON DUPLICATE KEY UPDATE uuid = uuid;
