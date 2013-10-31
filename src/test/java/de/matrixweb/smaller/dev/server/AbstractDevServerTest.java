package de.matrixweb.smaller.dev.server;

import java.io.IOException;
import java.net.Socket;

import org.junit.AfterClass;
import org.junit.Before;

/**
 * @author markusw
 */
public abstract class AbstractDevServerTest {

  private static DevServer devServer;

  /**
   * 
   */
  @Before
  public void startDevServer() {
    if (devServer == null) {
      devServer = new DevServer(getServerArgs().split(" "));
      new Thread(devServer).start();
      boolean retry = true;
      while (retry) {
        try {
          Thread.sleep(1000);
          new Socket("localhost", 12345);
          retry = false;
        } catch (final IOException | InterruptedException e) {
          // Wait some more
        }
      }
    }
  }

  protected abstract String getServerArgs();

  /**
   * 
   */
  @AfterClass
  public static void stopDevServer() {
    devServer.stop();
    devServer = null;
  }

  private static class DevServer implements Runnable {

    private final String[] args;

    private final Main server = new Main();

    public DevServer(final String... args) {
      this.args = args;
    }

    @Override
    public void run() {
      try {
        this.server.start(this.args);
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    public void stop() {
      this.server.stop();
    }

  }

}
