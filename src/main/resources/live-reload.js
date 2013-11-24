(function() {
  var __socket = new WebSocket(location.href.replace(/https?:\/\/([^/]+)\/.*/, 'ws://$1/live-reload'), 'live-reload');
  __socket.onmessage = function(e) {
    var message = JSON.parse(e.data);
    if (message.kind === 'reload') {
      console.log('reloading window', e);
      location.reload();
    } else if (message.kind === 'change') {
      var full = message.data['full'];
      var js = message.data['js'];
      var css = message.data['css'];
      if (full) {
        location.reload();
      } else {
        if (js) {
          var node = document.querySelector('script[src^="' + js + '"]');
          node.parentNode.removeChild(node);
          var script = document.createElement("script");
          script.setAttribute('src', js);
          document.body.appendChild(script);
        }
        if (css) {
          var node = document.querySelector('link[href^="' + css + '"]');
          node.setAttribute('href', node.getAttribute('href'));
        }
      }
    } else if (message.kind === 'ping') {
      __socket.send('pong');
    }
  }
})();
