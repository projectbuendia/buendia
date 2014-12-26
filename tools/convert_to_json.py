#!/usr/bin/python

import json
import sys

input = open(sys.argv[1])
output = open(sys.argv[2], 'w')
xml = input.read()
output.write(json.dumps({'xml':xml, 'patient_id':5, 'enterer_id':1, 'date_entered':'2014-11-13T15:00:00'}))
