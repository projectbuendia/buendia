#!/usr/bin/env python

# This script generates a SQL file (buendia_concept_dictionary.sql) to add ProjectBuendia concepts 
# into the standard CIEL concept dictionary.
# This will probably not need to be run again, but has been added to the git repository for 
# historical information on how the buendia_concept_dictionary.sql file was generated.
# 
# Rather than doing a diff between the manually edited dictionary state and the official CIEL dictionary
# this script uses various heuristics to spot where we have made manual edits.
# The heuristics are:
# - Standard CIEL concepts have UUIDs which are the id then end with repeated 'A' characters. 
#   We look for not having 'AAAAA' as that is low enough chance probability to be sure it is our added concept
# - Any special string we added for the purpose of a better android UI is stored in
#   the locale %_client (actually 'en_GB_client')
# - Any original concept_answer in the original CIEL dictionary had sort weight matching the concept_answer_id,
#   had a UUID ending in repeated 'C" characters, and a creator id of 1. If ANY of these are not true 
#   the script assumes it has been manually edited 
# - Any concept which had answers edited, we assume it may have had a datatype edited as well 
#   (in case it wasn't coded before)

# This script also renumbers all manually edited concepts to start at 777,000,000 to avoid collisions when the CIEL 
# dictionary adds new concepts.

# It also takes care to get UTF-8 encoding right for the concept names with special characters.


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

def renumber(id):
    if id in concept_renumber:
        return concept_renumber[id]
    elif id in ciel_contributions:
        return ciel_contributions[id]
    else:
        return id

# Known CIEL concepts
# Some CIEL concepts we have added manually, then contributed back to CIEL
# These are treated differently to ones we have added and not contributed back
# map is original Buendia Number: CIEL Number
ciel_contributions = {
    162730: 162826, # Np PCR threshold value
    162729: 162827, # L gene PCR threshold value
    162695: 162750, # Able to walk independently
    162696: 162751, # Able to walk with assistance
    162694: 162752, # Bedridden
}

conn = pymysql.connect(host='localhost', port=3316, user='openmrs', passwd='dxAeApzr97hT', db='openmrs-sdk', charset='utf8')

old_concept_ids = []
new_concept_ids = []

# Concept renumber: We don't want newly added CIEL concepts to clash with ours. So we will prefix ours with 777,000,000.
# This will hopefully make them easily recognisable, significantly less than MAX Int 32, and have plenty of space from
# the CIEL dictionary.
concept_base_id = 777000000
concept_next_id = concept_base_id
concept_renumber = {}

# Make sure that we start higher than any Buendia concept already adjusted above concept_base_id
cur = conn.cursor()
cur.execute("SELECT max(concept_id) FROM concept")
for row in cur:  # No need, but no harm in loop
    concept_next_id = max(row[0], concept_next_id)
cur.close()

print('\n#\n# Extra dictionary changes for the Google/MSF Ebola effort (Project Buendia)\n#')

# Read all new concepts (by making assumptions about the UUID), and create INSERT statements for them.
# If we renumber we will need to renumber all forms as well.
# We will also need to renumber data if we keep old data.
print('\n#\n# Buendia specific concepts, with class etc\n#')
cur = conn.cursor()
concept_columns = ["concept_id","datatype_id","class_id","creator","date_created","uuid"] 
cur.execute("SELECT " +  ",".join(concept_columns) + " FROM concept WHERE concept.uuid NOT LIKE '%AAAA' and concept.retired = 0")
for row in cur:
    id = row[0]
    if id in ciel_contributions:
        continue
    newid = id
    if id<concept_base_id:
        newid = concept_next_id
        concept_next_id = concept_next_id + 1
        concept_renumber[id] = newid
        print('-- renumbered ' + str(id) + " as " + str(newid))

    values = escape(row, conn)
    old_concept_ids.append(id)
    new_concept_ids.append(newid)
    values[0] = str(newid)
    print(insert("concept", concept_columns, values).encode('utf-8'))
cur.close()

# insert all the non-voided strings for the given concepts
print('\n#\n# Names for Buendia specific concepts\n#')
cur = conn.cursor()
concept_name_columns = ["concept_id","name","locale","creator","date_created","uuid","concept_name_type","locale_preferred"]
cur.execute("SELECT " + ",".join(concept_name_columns) + " FROM concept_name WHERE concept_id IN (" + ",".join([str(i) for i in old_concept_ids]) + ") AND voided=0;")
for row in cur:
    values = escape(row, conn)
    values[0] = str(renumber(row[0]));
    print(insert("concept_name", concept_name_columns, values).encode('utf-8'))
cur.close()

print('\n#\n# Client specific name changes for all concepts\n#')
# Copy all client specific name changes, these can be found from our custom locale.
cur = conn.cursor()
# Use REPLACE (MySQL specific) in case any were also done in above step
cur.execute("SELECT " + ",".join(concept_name_columns) + " FROM concept_name where locale LIKE '%_client' and voided=0 ORDER BY concept_id")
for row in cur:
    values = escape(row, conn)
    values[0] = str(renumber(row[0]));
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

# update the answers
print('# Answer updates')
answers = set()
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
        if columns[i] == 'concept_id':
            values[i] = str(renumber(row[i]))
        if columns[i] == 'answer_concept':
            values[i] = str(renumber(row[i]))
            answers.add(row[i])

    # Renumber the concept_answers. O(n) lookup, but that's fine
    for i in range(len(columns)):
        if columns[i] in ['concept_answer_id']:
            values[i] = str(id)
    print(insert('concept_answer', columns, values).encode('utf-8'))
cur.close()

# Make sure we update all concept class/datatypes for these questions and answer concepts too
print('# Data type updates')
cur = conn.cursor()
cur.execute("SELECT concept_id,datatype_id FROM concept WHERE concept_id IN (" + ",".join([str(i) for i in questions.union(answers)])+ ");")
for row in cur:
    concept_id = row[0]
    datatype_id = row[1]
    if concept_id not in concept_renumber: # coded
        print("UPDATE concept SET datatype_id=" + str(datatype_id) + " WHERE concept_id=" + str(concept_id) + ";")

print('\n#\n# Update forms to renumbered concepts\n#')
# As we have done renumbering, renumber all the forms to use the new concept numbers.
for old_id in old_concept_ids + ciel_contributions.keys():
    print("UPDATE field SET concept_id=" + str(renumber(old_id)) + " WHERE concept_id=" + str(old_id) + ";")

# Hopefully data has been wiped too, but in case it hasn't
print('\n#\n# Update observations to renumbered concepts\n#')
for old_id in old_concept_ids + ciel_contributions.keys():
    print("UPDATE obs SET concept_id=" + str(renumber(old_id)) + " WHERE concept_id=" + str(old_id) + ";")
    print("UPDATE obs SET value_coded=" + str(renumber(old_id)) + " WHERE value_coded=" + str(old_id) + ";")


conn.close()