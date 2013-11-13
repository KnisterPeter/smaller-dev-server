var __socket = new WebSocket(location.href.replace(/https?:\/\/([^/]+)\/.*/, 'ws://$1/live-reload'), 'live-reload');
__socket.onmessage = function(e) {
  console.log('reloading window', e);
  if (e.data === 'reload') location.reload();
  if (e.data === 'ping') __socket.send('pong');
}
