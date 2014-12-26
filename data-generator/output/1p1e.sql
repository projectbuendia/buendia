--
-- Working data
--
SELECT @android := user_id FROM users WHERE username='android' LIMIT 1;
SELECT @assigned_location_id := person_attribute_type_id FROM person_attribute_type WHERE name='assigned_location' LIMIT 1;
SELECT @msf_type := patient_identifier_type_id FROM patient_identifier_type WHERE name='MSF' LIMIT 1;
SELECT @root_location := location_id FROM location WHERE uuid='3449f5fe-8e6b-4250-bcaa-fca5df28ddbf' LIMIT 1;
CREATE TEMPORARY TABLE locale_order (id INT PRIMARY KEY,locale VARCHAR(30));INSERT INTO locale_order (id, locale) VALUES (1, 'en_GB_client'), (2, 'en');--
-- Users
--
-- Guest User
INSERT INTO person (gender,creator,date_created) VALUES ('M',@android,NOW());
SELECT @person_id := LAST_INSERT_ID();
INSERT INTO person_name (person_id,given_name,family_name,creator,date_created,uuid) VALUES (@person_id,"Guest","User",@android,NOW(),UUID());
INSERT INTO users (system_id,username,password,salt,creator,date_created,person_id,uuid)
  VALUES ("20-8","guest","95151f0c72533553ae33030ad85513c18a48a4a39b3caebc5063e2eeda9c545e57c66fba753195cf82db80784b58f0f62cbe4ab7bd993ea41935ed0dd8a78606","29c8440135084b85b7ac8d5f5cf8ff6d1d5906f18a59e8bd6bd32a5d26d3f7bf888f42e0bb5d5a71aff869c9251080a02975a10e556c865b781c1edc8d360038",@android,NOW(),@person_id,UUID());
INSERT INTO provider (person_id,name,creator,date_created,uuid) VALUES (@person_id,"Guest User",@android,NOW(),UUID());
-- Jay Achar
INSERT INTO person (gender,creator,date_created) VALUES ('M',@android,NOW());
SELECT @person_id := LAST_INSERT_ID();
INSERT INTO person_name (person_id,given_name,family_name,creator,date_created,uuid) VALUES (@person_id,"Jay","Achar",@android,NOW(),UUID());
INSERT INTO users (system_id,username,password,salt,creator,date_created,person_id,uuid)
  VALUES ("21-6","jay","aaf2b63a44655ccc4d8309b3cb068992518725c03ae023e77f0c707b9ca0f64b18ca7abce9b3363cfef393cef7d2c0ca3e3e3b7e5eab4a7605a16b7a55c69f78","c1e9fe0ebe2aca93f6d470f470eebb016ff67378d6043be60cb05449a5aa7b2254491606633c544ea0b76e75e1cb7af7d045aed2850b810cbd1df19fdc0dac3b",@android,NOW(),@person_id,UUID());
INSERT INTO provider (person_id,name,creator,date_created,uuid) VALUES (@person_id,"Jay Achar",@android,NOW(),UUID());
-- Simon Collins
INSERT INTO person (gender,creator,date_created) VALUES ('M',@android,NOW());
SELECT @person_id := LAST_INSERT_ID();
INSERT INTO person_name (person_id,given_name,family_name,creator,date_created,uuid) VALUES (@person_id,"Simon","Collins",@android,NOW(),UUID());
INSERT INTO users (system_id,username,password,salt,creator,date_created,person_id,uuid)
  VALUES ("22-4","simon","b8a45310ec22b9ea6ac1e44c6f4e594d7a076c911c83203408ce911f8d34b33aa05fc98c44bb8f06df4307fdcf6a7486e8337aebd20cea2f3cb846213754b85d","05be4cf5b1c6a4f700d633420f7c9c0ee2674e9761c2c32fd93e51325041a24f07acef646859715ac5e1c9ff4f023b45a6f1eeb445978682d36d2548d62f72be",@android,NOW(),@person_id,UUID());
INSERT INTO provider (person_id,name,creator,date_created,uuid) VALUES (@person_id,"Simon Collins",@android,NOW(),UUID());
-- Jane Greig
INSERT INTO person (gender,creator,date_created) VALUES ('M',@android,NOW());
SELECT @person_id := LAST_INSERT_ID();
INSERT INTO person_name (person_id,given_name,family_name,creator,date_created,uuid) VALUES (@person_id,"Jane","Greig",@android,NOW(),UUID());
INSERT INTO users (system_id,username,password,salt,creator,date_created,person_id,uuid)
  VALUES ("23-2","jane","4b4a174eb6fd9a69841478072aa06597d67d56423b6f972e97ce994fa115dc9c30b5760c23247dd9b66ccb1821e6c654abd480ddb75c275cf189ae0abf39036b","05b6b90d0ca79a237c57d8a4b977f31592873a5d82292945a99d1c20764b3bfc668bdc7d8544af60086ec5167469946a1c486bced65f4186e9eb2124d3c46d85",@android,NOW(),@person_id,UUID());
INSERT INTO provider (person_id,name,creator,date_created,uuid) VALUES (@person_id,"Jane Greig",@android,NOW(),UUID());
-- Ivan Gayton
INSERT INTO person (gender,creator,date_created) VALUES ('M',@android,NOW());
SELECT @person_id := LAST_INSERT_ID();
INSERT INTO person_name (person_id,given_name,family_name,creator,date_created,uuid) VALUES (@person_id,"Ivan","Gayton",@android,NOW(),UUID());
INSERT INTO users (system_id,username,password,salt,creator,date_created,person_id,uuid)
  VALUES ("24-0","ivan","063a151b3048b657444cf3cea8cb88c0c7c61fd0e3c1f48939a5addc606f3609b33eb48df08947f6ad102ce2df8be05fbd2dc8606d188e0d44ebfe015eab14cb","aa7b0f9a79b4b6529e1a296c6cb177accdf8c3558369a9ccf57e070bf7fa83485f95dd68dda837e5e5700fde59064b98c2ab9657ab7fe4dac26061f9d1b32db1",@android,NOW(),@person_id,UUID());
INSERT INTO provider (person_id,name,creator,date_created,uuid) VALUES (@person_id,"Ivan Gayton",@android,NOW(),UUID());
--
-- Patients
--
--
-- @firstname@ LastName
INSERT INTO person (gender,birthdate,creator,date_created,uuid) VALUES ("F",DATE_SUB(CURDATE(), INTERVAL 318 MONTH),@android,DATE_SUB(CURDATE(), INTERVAL 8 DAY),UUID());
SELECT @person_id := LAST_INSERT_ID();
INSERT INTO person_name (person_id,given_name,family_name,creator,date_created,uuid) VALUES (@person_id,"@firstname@","LastName",@android,DATE_SUB(CURDATE(), INTERVAL 8 DAY),UUID());
INSERT INTO patient (patient_id,creator,date_created) VALUES (@person_id,@android,DATE_SUB(CURDATE(), INTERVAL 8 DAY));
INSERT INTO patient_identifier (patient_id,identifier,identifier_type,location_id,creator,date_created,uuid) VALUES (@person_id,"KH.0",@msf_type,@root_location,@android,NOW(),UUID());
SELECT @location_id := location_id FROM location WHERE name="Confirmed 1";
INSERT INTO person_attribute (person_id,value,person_attribute_type_id,creator,date_created,uuid) VALUES (@person_id,@location_id,@assigned_location_id,@android,NOW(),UUID());
SELECT @location_id := location_id FROM location WHERE name="Triage";
INSERT INTO encounter (encounter_type,patient_id,location_id,encounter_datetime,creator,date_created,uuid) 
  VALUES (2,@person_id,@location_id,ADDTIME(CAST(DATE_SUB(CURDATE(), INTERVAL 8 DAY) AS DATETIME), "15:00"),@android,NOW(),UUID());
SELECT @encounter_id := LAST_INSERT_ID();
SELECT @encounter_datetime := encounter_datetime FROM encounter WHERE encounter_id=@encounter_id;
SELECT @provider_id := provider_id FROM provider WHERE name="Guest User";
INSERT INTO encounter_provider (encounter_id,provider_id,encounter_role_id,creator,date_created,uuid) 
  VALUES (@encounter_id,@provider_id,3,@android,NOW(),UUID());
SELECT @concept_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Temperature °C" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
INSERT INTO obs (person_id,concept_id,encounter_id,obs_datetime,value_numeric,creator,date_created,uuid) 
  VALUES (@person_id,@concept_id,@encounter_id,@encounter_datetime,37.4,@android,NOW(),UUID());
SELECT @concept_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Pregnant" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
SELECT @value_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Yes" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
INSERT INTO obs (person_id,concept_id,encounter_id,obs_datetime,value_coded,creator,date_created,uuid) 
  VALUES (@person_id,@concept_id,@encounter_id,@encounter_datetime,@value_id,@android,NOW(),UUID());
SELECT @concept_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Condition" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
SELECT @value_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Good" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
INSERT INTO obs (person_id,concept_id,encounter_id,obs_datetime,value_coded,creator,date_created,uuid) 
  VALUES (@person_id,@concept_id,@encounter_id,@encounter_datetime,@value_id,@android,NOW(),UUID());
SELECT @concept_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Best Conscious State" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
SELECT @value_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Alert" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
INSERT INTO obs (person_id,concept_id,encounter_id,obs_datetime,value_coded,creator,date_created,uuid) 
  VALUES (@person_id,@concept_id,@encounter_id,@encounter_datetime,@value_id,@android,NOW(),UUID());
SELECT @concept_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Mobility" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
SELECT @value_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Walking" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
INSERT INTO obs (person_id,concept_id,encounter_id,obs_datetime,value_coded,creator,date_created,uuid) 
  VALUES (@person_id,@concept_id,@encounter_id,@encounter_datetime,@value_id,@android,NOW(),UUID());
SELECT @concept_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Tolerating Diet" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
SELECT @value_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Food" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
INSERT INTO obs (person_id,concept_id,encounter_id,obs_datetime,value_coded,creator,date_created,uuid) 
  VALUES (@person_id,@concept_id,@encounter_id,@encounter_datetime,@value_id,@android,NOW(),UUID());
SELECT @concept_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Hydration" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
SELECT @value_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Well hydrated" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
INSERT INTO obs (person_id,concept_id,encounter_id,obs_datetime,value_coded,creator,date_created,uuid) 
  VALUES (@person_id,@concept_id,@encounter_id,@encounter_datetime,@value_id,@android,NOW(),UUID());
SELECT @concept_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Vomiting" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
SELECT @value_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="None" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
INSERT INTO obs (person_id,concept_id,encounter_id,obs_datetime,value_coded,creator,date_created,uuid) 
  VALUES (@person_id,@concept_id,@encounter_id,@encounter_datetime,@value_id,@android,NOW(),UUID());
SELECT @concept_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Diarrhoea" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
SELECT @value_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="None" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
INSERT INTO obs (person_id,concept_id,encounter_id,obs_datetime,value_coded,creator,date_created,uuid) 
  VALUES (@person_id,@concept_id,@encounter_id,@encounter_datetime,@value_id,@android,NOW(),UUID());
SELECT @concept_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="Pain Assessment" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
SELECT @value_id := concept.concept_id FROM concept_name  JOIN concept ON concept.concept_id=concept_name.concept_id INNER JOIN locale_order ON concept_name.locale=locale_order.locale WHERE name="None" AND voided=0 AND concept.retired=0 ORDER BY locale_order.id ASC LIMIT 1;
INSERT INTO obs (person_id,concept_id,encounter_id,obs_datetime,value_coded,creator,date_created,uuid) 
  VALUES (@person_id,@concept_id,@encounter_id,@encounter_datetime,@value_id,@android,NOW(),UUID());
