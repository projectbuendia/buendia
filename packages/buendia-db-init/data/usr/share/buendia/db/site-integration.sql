-- This SQL script takes a database populated from a db-snapshot and
-- adds initial data for running integration tests.


-- System Users: make sure Android is present
-- This is surprisingly hard as there is not a unique constraint on username
-- or system_id, so the normal tricks for INSERT IF NOT EXISTS don't work.
-- Ignoring for now.

-- Buendia users - make sure user, provider and person information present
-- for Guest User

-- Program. Make sure Ebola program is present.


-- Location make sure Camp, and zones are present. Create tents.
-- ON DUPLICATE IGNORE is safe as there is a unique index on uuid
SELECT @android := user_id FROM users WHERE username='android' LIMIT 1;
INSERT INTO location (name, creator, date_created, uuid) VALUES
    ('Facility', @android, NOW(), 'e20f2bf8-8650-11e4-9aee-040ccecfdba4')
    ON DUPLICATE KEY UPDATE uuid=uuid;
SELECT @emc_id := location_id FROM location WHERE uuid='e20f2bf8-8650-11e4-9aee-040ccecfdba4' LIMIT 1;
-- insert the zones
INSERT INTO location (name, creator, date_created, uuid, parent_location) VALUES
    ('Triage', @android, NOW(), '3f75ca61-ec1a-4739-af09-25a84e3dd237', @emc_id),
    ('Suspected Zone', @android, NOW(), '2f1e2418-ede6-481a-ad80-b9939a7fde8e', @emc_id),
    ('Probable Zone', @android, NOW(), '3b11e7c8-a68a-4a5f-afb3-a4a053592d0e', @emc_id),
    ('Confirmed Zone', @android, NOW(), 'b9038895-9c9d-4908-9e0d-51fd535ddd3c', @emc_id),
    ('Morgue', @android, NOW(), '4ef642b9-9843-4d0d-9b2b-84fe1984801f', @emc_id),
    ('Discharged', @android, NOW(), 'd7ca63c3-6ea0-4357-82fd-0910cc17a2cb', @emc_id)
    ON DUPLICATE KEY UPDATE uuid=uuid;

-- Make sure the allowed locales is correct
UPDATE global_property SET property_value="en, en_GB_client" WHERE property = "locale.allowed.list";

-- Insert the tents (more than enough in each zone to work at most EMCs).
SELECT @confirmed_id := location_id FROM location WHERE uuid='b9038895-9c9d-4908-9e0d-51fd535ddd3c' LIMIT 1;
SELECT @suspect_id := location_id FROM location WHERE uuid='2f1e2418-ede6-481a-ad80-b9939a7fde8e' LIMIT 1;
SELECT @probable_id := location_id FROM location WHERE uuid='3b11e7c8-a68a-4a5f-afb3-a4a053592d0e' LIMIT 1;
INSERT INTO location (name, creator, date_created, uuid, parent_location) VALUES
    ('S1', @android, NOW(), 'a72f944b-cb50-4bc5-9ac0-f93c44d71b10', @suspect_id),
    ('S2', @android, NOW(), 'd81a33d9-2711-47e2-9d47-77e32e0281b9', @suspect_id),
    ('S3', @android, NOW(), 'c7a69e19-8650-11e4-9cc2-040ccecfdba4', @suspect_id),
    ('S4', @android, NOW(), 'c87ad5ab-8650-11e4-afd6-040ccecfdba4', @suspect_id),
    ('P1', @android, NOW(), '0d36bdce-7f0a-11e4-88ec-42010af084c0', @probable_id),
    ('P2', @android, NOW(), '0d36beb7-7f0a-11e4-88ec-42010af084c0', @probable_id),
    ('P3', @android, NOW(), 'c89d00a1-8650-11e4-a82d-040ccecfdba4', @probable_id),
    ('P4', @android, NOW(), 'c8ba4385-8650-11e4-8641-040ccecfdba4', @probable_id),
    ('C1', @android, NOW(), '46a8cb21-d9eb-416d-86ee-90a018122859', @confirmed_id),
    ('C2', @android, NOW(), '0a49d383-7019-4f1f-bf4b-875f2cd58964', @confirmed_id),
    ('C3', @android, NOW(), '4443985e-adbc-4c90-aaac-b27635cb73ac', @confirmed_id),
    ('C4', @android, NOW(), '3ca154be-afd1-4074-893d-596bcb423a54', @confirmed_id),
    ('C5', @android, NOW(), '6b993dab-7f0a-11e4-88ec-42010af084c0', @confirmed_id),
    ('C6', @android, NOW(), '0cce735e-a0c8-4b21-a05e-539b6bb93441', @confirmed_id),
    ('C7', @android, NOW(), '5542080a-45db-435e-8505-8e65309ae9d5', @confirmed_id),
    ('C8', @android, NOW(), '87233c64-125a-4e8e-b292-f866a8ecb2b4', @confirmed_id),
    ('C9', @android, NOW(), 'c8d6607a-8650-11e4-9dfb-040ccecfdba4', @confirmed_id),
    ('C10', @android, NOW(), 'c8f5a8d7-8650-11e4-81c0-040ccecfdba4', @confirmed_id)
    ON DUPLICATE KEY UPDATE uuid=uuid;
