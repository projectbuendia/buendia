var ROOT_URL = '';

var current_tid = '1';
var board_data = {};
var state_data = {};

// Adds a tab to the tab list on the left.
function add_tab(tid, label) {
  $('#tablist').append(
      $('<div class="tab" id="tab-' + tid + '"></div>')
          .text(label)
          .click(function() { select_tab(tid); })
  );
}

// Iterates over all tabs.
function for_each_tab(callback) {
  var tabs = board_data.tabs || {};
  var order = tabs.order || [];
  var labels = tabs.labels || {};
  for (var i = 0; i < order.length; i++) {
    var tid = order[i] + '';
    callback(tid, labels[tid] || '');
  }
}

// Iterates over all cells.
function for_each_cell(callback, tid) {
  var cells = board_data.cells || {};
  var layout = cells.layout || [];
  var labels = cells.labels || {};
  var states = state_data[tid || current_tid] || {};
  for (var r = 0; r < layout.length; r++) {
    for (var c = 0; c < layout[r].length; c++) {
      var cid = layout[r][c] + '';
      callback(cid, labels[cid] || '', states[cid], r, c);
    }
  }
}

// Updates the display of the board labels and layout based on the board data.
function update_board() {
  // Default to 'ALL' if current tab is removed.
  var new_current_tid = 'ALL';
  $('#tablist').empty();
  for_each_tab(function(tid, label) {
    add_tab(tid, label);
    if (tid === current_tid) {
      new_current_tid = tid; // current tab still exists, stay on it
    }
  });
  add_tab('ALL', 'ALL');

  // To visually connect the selected tab and the board, the right edge of
  // the selected tab flares out; this is achieved with a rounded bottom-right
  // corner on the tab above, and a rounded top-right corner on the tab below.
  // We add an extra tab below so that this also works for the last tab.
  $('#tablist').append(
      $('<div class="tab" id="tab-EXTRA"></div>')
  );

  var layout = (board_data.cells || {}).layout || [];
  $('#board').empty();
  for (var r = 0; r < layout.length; r++) {
    $('#board').append($('<div class="row"></div>'));
  }
  var rows = $('.row').css('height', 100.0/layout.length + '%');
  for_each_cell(function(cid, label, state, r, c) {
    var cell = $('<div class="cell" id="cell-' + cid + '"></div>')
        .text(label)
        .css('width', (100.0/layout[r].length) + '%');
    if (cid.match(/^c/)) {
      cell.append(
          $('<div class="counter"></div>').append(
              $('<span class="dec">\u2212</span>')
                  .click(function() { adjust_cell(cid, -1); })
          ).append(
              $('<span class="value" id="value-' + cid + '">0</span>')
          ).append(
              $('<span class="inc">+</span>')
                  .click(function() { adjust_cell(cid, 1); })
          )
      );
    } else {
      cell.click(function() { toggle_cell(cid); });
    }
    $(rows[r]).append(cell);
  });

  select_tab(new_current_tid);
}

// Switch to a particular tab (by tab ID).  The special tab with ID 'ALL'
// is the list showing all tasks.
function select_tab(tid) {
  current_tid = tid;
  update_cells();
  update_list();
  if (tid === 'ALL') {
    $('#board').hide();
    $('#list').show();
  } else {
    $('#board').show();
    $('#list').hide();
  }
}

// Updates the display of the tabs and cells based on the button state data.
function update_cells() {
  var order = ((board_data.tabs || {}).order || []).concat(['ALL', 'EXTRA']);
  for (var i = 0; i < order.length; i++) {
    var tab = $('#tab-' + order[i]);
    tab.toggleClass('above-selected', (order[i + 1] + '') === current_tid);
    tab.toggleClass('selected', (order[i] + '') === current_tid);
    tab.toggleClass('below-selected', (order[i - 1] + '') === current_tid);
  }
  for_each_tab(function(tid) {
    var cells_on = 0;
    for_each_cell(function(cid, label, state) {
      cells_on += (cid.match(/^[rc]/) ? 0 : (state ? 1 : 0));
    }, tid);
    $('#tab-' + tid).toggleClass('on', cells_on > 0);
  });
  for_each_cell(function(cid, label, state) {
    $('#cell-' + cid).toggleClass('on', !!state);
    $('#value-' + cid).text(state || 0);
  });
}

// Updates the display of the list of tasks based on the cell state data.
function update_list() {
  $('#list').empty();
  var sums = {}, summed_cells = {};
  for_each_tab(function(tid, tab_label) {
    for_each_cell(function(cid, cell_label, state) {
      if (state && !cid.match(/^[rc]/)) {
        var text = tab_label + ': ' + cell_label;
        $('#list').append($('<div class="item"></div>').text(text));
      }
      if (cid.match(/^c/)) {
        sums[cid] = (sums[cid] || 0) + (state || 0);
        if (state) {
          (summed_cells[cid] = (summed_cells[cid] || [])).push(tab_label);
        }
      }
    }, tid);
  });
  for_each_cell(function(cid, cell_label) {
    if (cid.match(/^c/)) {
      var text = cell_label + ': ' + sums[cid] + ' total';
      if (summed_cells[cid]) text += ' (' + summed_cells[cid].join(', ') + ')';
      $('#list').append($('<div class="item"></div>').text(text));
    }
  });
};

// Changes the state of one of the cells.
function toggle_cell(toggle_cid) {
  var states = state_data[current_tid] || {};
  for_each_cell(function(cid, label, state) {
    if (cid === toggle_cid) {
      states[cid] = state = !state;
      send_cell(cid, state);
    }
  });
  update_cells();
};

// Adjusts a counter by an increment.
function adjust_cell(adjust_cid, delta) {
  var states = state_data[current_tid] || {};
  for_each_cell(function(cid, label, state) {
    if (cid === adjust_cid) {
      states[cid] = state = Math.max(0, (state || 0) + delta);
      send_cell(cid, state);
    }
  });
  update_cells();
}

// Sends the state of one of the cells to the server.
function send_cell(cid, state) {
  $.ajax({
    url: ROOT_URL + '/state',
    type: 'POST',
    data: 'tid=' + current_tid + '&' + cid + '=' + state
  });
}

// Changes the label on one of the cells.  (Currently unused.)
function set_cell_label(cid, label) {
  (board_data.cells || {}).labels[cid] = label;
  send_cell_labels();
}

// Changes the label on one of the tabs.  (Currently unused.)
function set_tab_label(tid, label) {
  (board_data.tabs || {}).labels[tid] = label;
  send_tab_labels();
}

// Polls the server for an update to the board layout and labels.
function poll_board() {
  $.ajax({
    url: ROOT_URL + '/board',
    type: 'GET',
    dataType: 'JSON',
    success: function(data) { 
      board_data = data;
      update_board();
    }
  });
}

// Polls the server for an update to the cell states.
function poll_state() {
  $.ajax({
    url: ROOT_URL + '/state',
    type: 'GET',
    dataType: 'JSON',
    success: function(data) { 
      state_data = data;
      update_cells();
      update_list();
    }
  });
}

// Updates the font size to suit the window size.
function update_font_size() {
  var size1 = window.innerWidth / 30;
  var size2 = window.innerHeight / 24;
  $('#window').css('font-size', Math.min(size1, size2));
};

// Starts the polling loop.
$(document).on('ready', function() {
  update_font_size();
  poll_board();
  poll_state();
  setInterval(poll_state, 2000);
  setInterval(poll_board, 10000);
});
$(window).on('resize', update_font_size);
