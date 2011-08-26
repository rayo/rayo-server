jQuery.periodic = function (options, callback) {

  var settings = jQuery.extend({}, jQuery.periodic.defaults, {
    cancel        : cancel
  }, options);

  settings.cur_period = settings.period;
  settings.tid = false;
  var prev_ajax_response = '';

  run();

  return settings;

  function run() {

    cancel();
    settings.tid = setTimeout(function() {

      callback.call(settings);
      
      if(settings.tid)
        run();
    }, settings.cur_period);
  }

  function cancel() {
    clearTimeout(settings.tid);
    settings.tid = null;
  }
  
  // other functions we might want to implement
  function pause() {}
  function resume() {}
  function log() {}
};

jQuery.periodic.defaults = {
    period       : 1000,      
    max_period   : 1000*60*60*24,   // a day.
    on_max       : undefined  
};