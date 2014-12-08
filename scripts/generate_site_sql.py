#!/usr/bin/python

import binascii
import codecs
import getopt
import hashlib
import json
import random
import sys
import time

def usage():
    print "usage:"
    print "generate_site_sql.py sitename"
    print "reads sitename.json outputs sitename.sql"

def is_number(s):
    try:
        float(s)
        return True
    except ValueError:
        return False

def encode(s):
    m = hashlib.sha512()
    m.update(s)
    return m.hexdigest()

# Generate the salt/password values needed by OpenMRS, see UserServiceImpl.java
def passwordAndSalt(password):
    now = long(time.time() * 1000)
    extra = random.getrandbits(64)
    salt = encode(str(now) + str(extra))
    return encode(password + salt),salt

# Generate the check digit needed by OpenMRS, see LuhnIdentifierValidator.java
def luhnIdentifier(base):
    trimmedUppercaseUndecoratedIdentifier = str(base).strip().upper();
    # this will privatebe a running total
    sum = 0
    # loop through digits from right to left
    i = 0
    for ch in reversed(trimmedUppercaseUndecoratedIdentifier):
        digit = ord(ch) - 48
        # weight will be the current digit's contribution to
        # the running total
        if i % 2 == 0:
            # for alternating digits starting with the rightmost, we
            # use our formula this is the same as multiplying x 2 and
            # adding digits together for values 0 to 9. Using the
            # following formula allows us to gracefully calculate a
            # weight for non-numeric "digits" as well (from their
            # ASCII value - 48).
            weight = (2 * digit) - int((digit / 5) * 9)
        else:
            # even-positioned digits just contribute their ascii
            # value minus 48
            weight = digit
        # keep a running total of weights
        sum = sum + weight
        i=i+1

    # avoid sum less than 10 (if characters below "0" allowed,
    # this could happen)
    sum = abs(sum) + 10
    # check digit is amount needed to reach next number
    # divisible by ten
    return (10 - (sum % 10)) % 10

def wrap(s):
    # TODO(nfortescue): this is terrible, use properly escaped SQL arguments
    return '"' + s + '"'

# Add SQL to add a new system user into the users and person table
# assumes the @android has been defined so we can use it as creator
def appendNewUser(user, sql, next_system_id):
    sql.append("-- %s %s\n" % (user["given_name"],user["family_name"]))
    # insert person first so we have a person id to add to users
    sql.append("INSERT INTO person (gender,creator,date_created) VALUES ('M',@android,NOW());\n")
    sql.append("SELECT @person_id := LAST_INSERT_ID();\n")

    sql.append("INSERT INTO person_name (person_id,given_name,family_name,creator,date_created,uuid) ")
    sql.append("VALUES (@person_id,%s,%s,@android,NOW(),UUID());\n" %
        (wrap(user["given_name"]),wrap(user["family_name"])))

    # insert user (for login)
    sql.append("INSERT INTO users (system_id,username,password,salt,creator,date_created,person_id,uuid)\n")
    system_id = str(next_system_id) + '-' + str(luhnIdentifier(next_system_id))

    password,salt=passwordAndSalt("Password123")
    sql.append("  VALUES (%s,%s,%s,%s,@android,NOW(),@person_id,UUID());\n" %
        (wrap(system_id),wrap(user["username"]),wrap(password),wrap(salt)))

    # Insert provider (for encounters)
    sql.append("INSERT INTO provider (person_id,name,creator,date_created,uuid) ")
    sql.append("VALUES (@person_id,%s,@android,NOW(),UUID());\n" %
        (wrap(user["given_name"]+" "+user["family_name"])))

def getConceptId(varname, name):
    # order by preferred for locale DESC as some strings (like Alert) are duplicate, but
    # but only marked as preferred for the right one.
    sql.append(("SELECT @%s := concept.concept_id FROM concept_name " % (varname)) +
        " JOIN concept ON concept.concept_id=concept_name.concept_id "
        "INNER JOIN locale_order ON concept_name.locale=locale_order.locale " +
        "WHERE name=%s AND voided=0 AND concept.retired=0 "
        "ORDER BY locale_order.id ASC, locale_preferred DESC LIMIT 1;\n" % (wrap(name)))

def getLocationId(location):
    sql.append("SELECT @location_id := location_id FROM location WHERE name=%s;\n" % (wrap(location)))


try:                                
    opts, args = getopt.getopt(sys.argv, "", [])
except getopt.GetoptError:          
    usage()
    sys.exit(2)
if len(args) != 2:
    usage()
    sys.exit(2)

sitename = args[1]

print "trying to load %s.json" % sitename
data = json.load(open("%s.json" % sitename))


sql = []
sql.append("--\n-- Working data\n--\n")
sql.append("SELECT @android := user_id FROM users WHERE username='android' LIMIT 1;\n")
sql.append("SELECT @assigned_location_id := person_attribute_type_id FROM person_attribute_type WHERE name='assigned_location' LIMIT 1;\n")
sql.append("SELECT @msf_type := patient_identifier_type_id FROM patient_identifier_type WHERE name='MSF' LIMIT 1;\n")
sql.append("SELECT @root_location := location_id FROM location WHERE uuid='3449f5fe-8e6b-4250-bcaa-fca5df28ddbf' LIMIT 1;\n")
# Set up a temporary table to order our locales by preference
sql.append("CREATE TEMPORARY TABLE locale_order (id INT PRIMARY KEY,locale VARCHAR(30));")
sql.append("INSERT INTO locale_order (id, locale) VALUES (1, 'en_GB_client'), (2, 'en');")

# Ideally we would get the max id from the database. However legal system ids must have a check digit,
# which we have to add. And we can't do that without SQL fetch, then python/Java, then SQL insert.
# Instead we will make the assumption that users are already mostly deleted, and start from a constant (like 20),
# and work from there.
next_system_id = 20
sql.append("--\n-- Users\n--\n")
for user in data["users"]:
    appendNewUser(user, sql, next_system_id)
    next_system_id = next_system_id + 1

sql.append("--\n-- Patients\n--\n")
for patient in data["patients"]:
    if "given_name" not in patient:
        print "No given name for " + str(patient)
        continue
    family_name = (patient["family_name"] if "family_name" in patient else "")
    sql.append("--\n-- %s %s\n" % (patient["given_name"],family_name))
    # MySQL date format is YYYY-MM-DD
    age = patient["age"].strip().upper()
    if age[-1] == 'Y':
        dateSql = "DATE_SUB(CURDATE(), INTERVAL %s MONTH)" % (str(int(age[:-1]) * 12 + 6))
    elif age[-1] == 'M':
        dateSql = "DATE_SUB(CURDATE(), INTERVAL %s DAY)" % (str(int(age[:-1]) * 30 + 15))
    else:
        raise Exception("Bad age ending, must be M or Y:" + age)
    sql.append("INSERT INTO person (gender,birthdate,creator,date_created,uuid) ")
    sql.append("VALUES (%s,%s,@android,DATE_SUB(CURDATE(), INTERVAL %d DAY),UUID());\n" %
        (wrap(patient["gender"]),dateSql,patient["admitted_days_ago"]))
    sql.append("SELECT @person_id := LAST_INSERT_ID();\n")

    sql.append("INSERT INTO person_name (person_id,given_name,family_name,creator,date_created,uuid) ")
    sql.append("VALUES (@person_id,%s,%s,@android,DATE_SUB(CURDATE(), INTERVAL %d DAY),UUID());\n" %
        (wrap(patient["given_name"]),wrap(family_name),patient["admitted_days_ago"]))

    sql.append("INSERT INTO patient (patient_id,creator,date_created) ")
    sql.append("VALUES (@person_id,@android,DATE_SUB(CURDATE(), INTERVAL %d DAY));\n" % (patient["admitted_days_ago"]))

    # Patient identifier
    sql.append("INSERT INTO patient_identifier (patient_id,identifier,identifier_type,location_id,creator,date_created,uuid) ")
    sql.append("VALUES (@person_id,%s,@msf_type,@root_location,@android,NOW(),UUID());\n" % (wrap(patient["patient_id"])))

    # Person attribute for the assigned location
    if "assigned_location" in patient:
        getLocationId(patient["assigned_location"])
        sql.append("INSERT INTO person_attribute (person_id,value,person_attribute_type_id,creator,date_created,uuid) ")
        sql.append("VALUES (@person_id,@location_id,@assigned_location_id,@android,NOW(),UUID());\n")

    if not "encounters" in patient:
        continue
    for encounter in patient["encounters"]:
        getLocationId(encounter['location'])
        sql.append("INSERT INTO encounter (encounter_type,patient_id,location_id,encounter_datetime,creator,date_created,uuid) \n")
        sql.append("  VALUES (2,@person_id,@location_id,ADDTIME(CAST(DATE_SUB(CURDATE(), INTERVAL %d DAY) AS DATETIME), %s),@android,NOW(),UUID());\n" %
            (encounter["days_ago"], wrap(encounter["time"])))
        sql.append("SELECT @encounter_id := LAST_INSERT_ID();\n")
        sql.append("SELECT @encounter_datetime := encounter_datetime FROM encounter WHERE encounter_id=@encounter_id;\n")

        sql.append("SELECT @provider_id := provider_id FROM provider WHERE name=%s;\n" %
            (wrap(encounter['provider'])))
        sql.append("INSERT INTO encounter_provider (encounter_id,provider_id,encounter_role_id,creator,date_created,uuid) \n")
        sql.append("  VALUES (@encounter_id,@provider_id,3,@android,NOW(),UUID());\n")
        for observation in encounter["observations"]:
            # if a value isn't set just ignore this line, makes the data export script easier to write.
            if not "value" in observation:
                continue
            getConceptId("concept_id", observation["concept"])
            val = observation["value"]
            if is_number(val):
                value = val
                value_column = "value_numeric"
            else:
                getConceptId("value_id", val)
                value = "@value_id"
                value_column = "value_coded"
            sql.append(("INSERT INTO obs (person_id,concept_id,encounter_id,obs_datetime,%s,creator,date_created,uuid) " % (value_column)) +
                ("\n  VALUES (@person_id,@concept_id,@encounter_id,@encounter_datetime,%s,@android,NOW(),UUID());\n" % (value)))



print "trying to write %s.sql" % sitename
with codecs.open("%s.sql" % sitename, "w", "utf-8") as out:
    out.write("".join(sql))
