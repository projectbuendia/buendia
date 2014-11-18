#!/usr/bin/python

import json
import sys

input = open(sys.argv[1])
output = open(sys.argv[2], 'w')
output.write(json.load(input)['xml'])
