#!/usr/bin/env python

"""Taskboard app server.

Requires flask.  Install with 'pip install flask'.
Start the server with 'python app.py'.
To use the taskboard, go to http://<hostname>:5000/.
"""

from flask import Flask, Response, g, request
import flask
import json
import os
import sqlite3

app = Flask(__name__, static_url_path='')
os.chdir(os.path.dirname(__file__) or '.')

def get_db():
    if not hasattr(g, 'db'):
        g.db = sqlite3.connect('data.db')
    return g.db

@app.route('/')
def root():
    return flask.send_file('main.html')

@app.route('/static/<path:path>')
def static_file(path):
    return flask.send_file('static/' + path)

@app.route('/board', methods=['GET'])
def get_board():
    """Gets the JSON for the board layout and labels."""
    db = get_db()
    # An alternate design is to have the client send a version number or
    # an "If-None-Match" header with an ETag; but since the data is almost
    # certainly less than 1000 bytes this saves us nothing -- the response
    # is just one TCP packet either way
    result = '{}'
    for row in db.execute('select value from items where id = "board"'):
        result = row[0]
    return Response(result, mimetype='application/json')

@app.route('/state', methods=['GET'])
def get_state():
    """Gets the JSON representing the state of the task cells."""
    db = get_db()
    result = '{}'
    for row in db.execute('select value from items where id = "state"'):
        result = row[0]
    return Response(result, mimetype='application/json')

@app.route('/state', methods=['POST'])
def post_state():
    """Sets the state of some task cells on a particular tab."""
    db = get_db()
    result = '{}'
    for row in db.execute('select value from items where id = "state"'):
        result = row[0]
    state_data = json.loads(result)
    tid = request.form['tid']
    state_data.setdefault(tid, {})
    for key in request.form.keys():
        if key != 'tid':
            state_data[tid][key] = json.loads(request.form[key])
    db.execute("update items set value = '%s' where id = 'state'" %
               json.dumps(state_data));
    db.commit()
    return Response('OK')

if __name__ == '__main__':
    app.run(host='0.0.0.0')
