package de.matrixweb.smaller.dev.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author markusw
 */
public class LiveReloadSocket implements WebSocket.OnTextMessage {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(LiveReloadSocket.class);

  private static List<LiveReloadSocket> sockets = new ArrayList<>();

  private Connection connection;

  /**
   * @return Returns a new web socket
   */
  public static LiveReloadSocket create() {
    final LiveReloadSocket socket = new LiveReloadSocket();
    sockets.add(socket);
    return socket;
  }

  /**
   * 
   */
  public static void broadcastReload() {
    for (final LiveReloadSocket socket : sockets) {
      try {
        socket.connection.sendMessage("reload");
      } catch (final IOException e) {
        LOGGER.error("Failed to send message to client", e);
        socket.connection.disconnect();
        sockets.remove(socket);
      }
    }
  }

  /**
   * @see org.eclipse.jetty.websocket.WebSocket#onOpen(org.eclipse.jetty.websocket.WebSocket.Connection)
   */
  @Override
  public void onOpen(final Connection connection) {
    LOGGER.info("Opened new LiveReloadSocket connection");
    this.connection = connection;
  }

  /**
   * @see org.eclipse.jetty.websocket.WebSocket.OnTextMessage#onMessage(java.lang.String)
   */
  @Override
  public void onMessage(final String data) {
    LOGGER.info("Received live-reload message: {}", data);
  }

  /**
   * @see org.eclipse.jetty.websocket.WebSocket#onClose(int, java.lang.String)
   */
  @Override
  public void onClose(final int closeCode, final String message) {
    LOGGER.info("WebSocket closed: {} [code={}]", message, closeCode);
    sockets.remove(this);
  }

}
