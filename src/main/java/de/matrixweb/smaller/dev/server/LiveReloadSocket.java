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

  private static KeepAliveSocket keepAlive = new KeepAliveSocket();

  private Connection connection;

  /**
   * 
   */
  public static void start() {
    final Thread thread = new Thread(keepAlive);
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * 
   */
  public static void stop() {
    keepAlive.stop();
  }

  /**
   * @return Returns a new web socket
   */
  public static LiveReloadSocket create() {
    final LiveReloadSocket socket = new LiveReloadSocket();
    sockets.add(socket);
    return socket;
  }

  static void sendPing() {
    for (final LiveReloadSocket socket : sockets) {
      try {
        socket.connection.sendMessage("ping");
      } catch (final IOException e) {
        LOGGER.error("Failed to send message to client", e);
        socket.connection.disconnect();
        sockets.remove(socket);
      }
    }
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

  /** */
  static class KeepAliveSocket implements Runnable {

    private boolean running = true;

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      while (this.running) {
        try {
          Thread.sleep(1000 * 60);
        } catch (final InterruptedException e) {
          // Ignore this
        }
        LiveReloadSocket.sendPing();
      }
    }

    /**
     * 
     */
    public void stop() {
      this.running = false;
    }

  }

}
