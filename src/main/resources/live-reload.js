(function() {
  var __socket = new WebSocket(location.href.replace(/https?:\/\/([^/]+)\/.*/, 'ws://$1/live-reload'), 'live-reload');
  __socket.onmessage = function(e) {
    if (e.data === 'reload') {
      console.log('reloading window', e);
      location.reload();
    }
    if (e.data === 'ping') __socket.send('pong');
  }
})();
