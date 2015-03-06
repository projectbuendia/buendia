Taskboard
---------

Provides a board with a configurable number of rows, a configurable number
of buttons in each row, and configurable labels on the buttons.  Each button
can be toggled on and off, and its state is reflected on all other clients
that are viewing the board.

There is a configurable number of tabs, and each tab has its own copy of the
board with its own button states.

There is also a tab that lists all the active task buttons across all the tabs.

Typical use: tabs are locations, buttons are tasks to be done (e.g. cleaning
needed in Tent 2).  Or buttons are resources in use or available (e.g. Bed 5
in use in Tent 3).

Setup
-----

Requires Python 2.7 and Flask.  Install flask with 'pip install flask'.

Before first use, initialize the database with 'python reset.py'.

Start the server with 'python app.py'.  Then open http://[hostname]:5000/
to use the taskboard.

To do
-----

  - Transmit changes instead of entire board state and entire cell state.
  - Add simple editing functions (add/remove buttons, edit button labels,
    add/remove tabs, edit tab labels).
