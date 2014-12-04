#!/usr/bin/python

import binascii
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
sql.append("SELECT @android := user_id FROM users WHERE username='android' LIMIT 1;\n")
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
	sql.append("-- %s %s\n" % (patient["given_name"],patient["family_name"]))
	# MySQL date format is YYYY-MM-DD	
	age = patient["age"].strip().upper()
	if age[-1] == 'Y':
		dateSql = "DATE_SUB(CURDATE(), INTERVAL %s MONTH)" % (str(int(age[:-1]) * 12 + 6))
	else:
		dateSql = "DATE_SUB(CURDATE(), INTERVAL %s DAY)" % (str(int(age[:-1]) * 30 + 15))
	sql.append("INSERT INTO person (gender,birthdate,creator,date_created) VALUES (%s,%s,@android,NOW());\n" %
	    (wrap(patient["gender"]),dateSql))
	sql.append("SELECT @person_id := LAST_INSERT_ID();\n")
	sql.append("INSERT INTO person_name (person_id,given_name,family_name,creator,date_created,uuid) ")
	sql.append("VALUES (@person_id,%s,%s,@android,NOW(),UUID());\n" %
	    (wrap(patient["given_name"]),wrap(patient["family_name"])))
	
	
print "trying to write %s.sql" % sitename
with open("%s.sql" % sitename, "w") as out:
	out.write("".join(sql))	
