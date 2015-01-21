#!/usr/bin/env python

from __future__ import print_function

import pymysql
import datetime

def escape(value, conn):
	if isinstance(value, tuple):
		return [escape(v, conn) for v in value]
	elif isinstance(value, unicode):
		result = (u'"' + unicode(conn.escape_string(value)) + u'"')
		return result
	elif isinstance(value, str):
		return '"' + conn.escape_string(value) + '"'
	elif isinstance(value, datetime.datetime):
		return u'"' + str(value) + u'"'
	elif value is None:
		return 'NULL'
	else:
		return str(value)

def insert(table, columns, values, replace=False):
	if replace:
		verb=u"REPLACE"
	else:
		verb=u"INSERT"
	return verb + u" INTO " + table + u" (" + u",".join(columns) + u") VALUES (" + u",".join(values) + u");"
		
def renumber(id, concept_renumber):
	if id in concept_renumber:
		return concept_renumber[id]
	else:
		return id
	
conn = pymysql.connect(host='localhost', port=3316, user='openmrs', passwd='dxAeApzr97hT', db='openmrs-sdk', charset='utf8')

old_concept_ids = []
new_concept_ids = []

# Concept renumber: We don't want newly added CIEL concepts to clash with ours. So we will prefix ours with 777,000,000.
# This will hopefully make them easily recognisable, significantly less than MAX Int 32, and have plenty of space from
# the CIEL dictionary.
# We need to make sure
concept_base_id = 777000000 
concept_next_id = concept_base_id
concept_renumber = {}

print('\n#\n# Extra dictionary changes for the Google/MSF Ebola effort (Project Buendia)\n#')


# TODO: renumber concepts to avoid potential number conflicts with future dictionary
# Read all new concepts (by making assumptions about the UUID), and create INSERT statements for them.
# If we renumber we will need to renumber all forms as well.
# We will also need to renumber data if we keep old data.
print('\n#\n# Buendia specific concepts, with class etc\n#')
cur = conn.cursor()
concept_columns = ["concept_id","datatype_id","class_id","creator","date_created","uuid"] 
cur.execute("SELECT " +  ",".join(concept_columns) + " FROM concept WHERE concept.uuid NOT LIKE '%AAAA' and concept.retired = 0")
for row in cur:
	id = row[0]
	newid = id
	if id<concept_base_id:
		newid = concept_next_id
		concept_next_id = concept_next_id + 1
		concept_renumber[id] = newid
	
	values = escape(row, conn)
	old_concept_ids.append(id)
	new_concept_ids.append(newid)
	values[0] = str(newid)
	print(insert("concept", concept_columns, values).encode('utf-8'))
cur.close()

# TODO: renumber concept_names to avoid potential number conflicts with future dictionary
# insert all the non-voided strings for the given concepts
print('\n#\n# Names for Buendia specific concepts\n#')
cur = conn.cursor()
concept_name_columns = ["concept_id","name","locale","creator","date_created","uuid","concept_name_type","locale_preferred"]
cur.execute("SELECT " + ",".join(concept_name_columns) + " FROM concept_name WHERE concept_id IN (" + ",".join([str(i) for i in old_concept_ids]) + ") AND voided=0;")
for row in cur:
	values = escape(row, conn)
	values[0] = str(renumber(row[0], concept_renumber));
	print(insert("concept_name", concept_name_columns, values).encode('utf-8'))
cur.close()

print('\n#\n# Client specific name changes for all concepts\n#')
# Copy all client specific name changes, these can be found from our custom locale.
cur = conn.cursor()
# Use REPLACE (MySQL specific) in case any were also done in above step
cur.execute("SELECT " + ",".join(concept_name_columns) + " FROM concept_name where locale LIKE '%_client' and voided=0 ORDER BY concept_id")
for row in cur:
	values = escape(row, conn)
	values[0] = str(renumber(row[0], concept_renumber));
	print(insert("concept_name", concept_name_columns, values, replace=True).encode('utf-8'))
cur.close()

# We need to find any changes we have made to concept answers. Tricky. We have almost certainly done this via the UI.
# We will use the fact that having too many is not bad, but having too few is, and 3 heuristics:
# 1) The original concept_answer_uuid from the original database is filled with 'CCCCCC'
# 2) The original sort_weight is the concept_answer_id
# 3) The original creator was 1
# We can't just get our new answers, we also need to find any changes we made to answer order ids.
# The way we will do this is use the 3 heuristics aboce to find any potentially changed questions (concept_id)
# then dump all answers for those questions, to get the sort weights right.
# We will also dump all previous answers for those concepts

print('\n#\n# Buendia specific associations between questions and answers, or changes in ordering of answers\n#')
questions = set()
cur = conn.cursor()
cur.execute("SELECT concept_id FROM concept_answer WHERE sort_weight != concept_answer_id OR creator != 1 OR uuid NOT LIKE '%CCCCC';")
for row in cur:
	questions.add(row[0])
cur.close()
print('DELETE FROM concept_answer WHERE concept_id IN (' + ",".join([str(i) for i in questions]) + ');')

# Make sure we update all concept class/datatypes for these concepts too, as we may have modified the datatype
print('# Data type updates')
cur = conn.cursor()
cur.execute("SELECT concept_id,datatype_id FROM concept WHERE concept_id IN (" + ",".join([str(i) for i in questions])+ ");")
for row in cur:
	concept_id = row[0]
	datatype_id = row[1]
	if concept_id not in concept_renumber and datatype_id == 2: # coded
		print("UPDATE concept SET datatype_id=2 WHERE concept_id=" + str(concept_id) + ";")
		
# update the answers
print('# Answer updates')
cur = conn.cursor()
cur.execute("SELECT * FROM concept_answer WHERE concept_id IN (" + ",".join([str(i) for i in questions]) + ") ORDER BY concept_id,concept_answer_id;")
columns = [col[0] for col in cur.description]
concept_answer_next_id = concept_base_id
for row in cur:
	id = concept_answer_next_id
	concept_answer_next_id = concept_answer_next_id + 1
	values = escape(row, conn)
	
	# Renumber the concepts from our concept renumber map
	for i in range(len(columns)):
		if columns[i] in ['concept_id', 'answer_concept']:
			values[i] = str(renumber(row[i], concept_renumber))
	# Renumber the concept_answers. O(n) lookup, but that's fine
	for i in range(len(columns)):
		if columns[i] in ['concept_answer_id']:
			values[i] = str(id)	
	print(insert('concept_answer', columns, values).encode('utf-8'))
cur.close()

print('\n#\n# Update forms to renumbered concepts\n#')
# As we have done renumbering, renumber all the forms to use the new concept numbers.
for old_id in old_concept_ids:
	print("UPDATE field SET concept_id=" + str(renumber(old_id, concept_renumber)) + " WHERE concept_id=" + str(old_id) + ";")

# Hopefully data has been wiped too, but in case it hasn't
print('\n#\n# Update observations to renumbered concepts\n#')
for old_id in old_concept_ids:
	print("UPDATE obs SET concept_id=" + str(renumber(old_id, concept_renumber)) + " WHERE concept_id=" + str(old_id) + ";")
	print("UPDATE obs SET value_coded=" + str(renumber(old_id, concept_renumber)) + " WHERE value_coded=" + str(old_id) + ";")

	
conn.close()