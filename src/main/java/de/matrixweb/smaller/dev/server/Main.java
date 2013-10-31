package de.matrixweb.smaller.dev.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Locale;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author markusw
 */
public class Main {

  private Logger logger;

  private SmallerResourceHandler resourceHandler;

  private Server server;

  /**
   * @param args
   * @throws Exception
   */
  public static void main(final String... args) throws Exception {
    new Main().start(args);
  }

  /**
   * @param args
   * @throws Exception
   */
  public void start(final String... args) throws Exception {
    Locale.setDefault(Locale.ENGLISH);

    final Config config = parseArgs(args);
    if (config == null) {
      return;
    }

    this.logger = (Logger) LoggerFactory.getLogger(Main.class);
    if (config.isDebug()) {
      this.logger.setLevel(Level.DEBUG);
    }

    this.resourceHandler = new SmallerResourceHandler(config);
    final Servlet servlet = new Servlet(config, this.resourceHandler);
    this.server = new Server(InetSocketAddress.createUnresolved(
        config.getHost(), config.getPort()));
    final ServletHandler handler = new ServletHandler();
    handler.addServletWithMapping(new ServletHolder(servlet), "/");
    this.server.setHandler(handler);
    this.server.start();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        Main.this.stop();
      }
    });

    this.server.join();
  }

  /**
   * 
   */
  public void stop() {
    if (this.resourceHandler != null) {
      try {
        this.resourceHandler.dispose();
      } catch (final IOException e) {
        this.logger.error("Failed to shutdown watchdog", e);
      }
    }
    if (this.server != null) {
      try {
        this.server.stop();
      } catch (final Exception e) {
        this.logger.error("Failed to shutdown jetty", e);
      }
    }
  }

  private Config parseArgs(final String... args) {
    final Config config = new Config();
    final CmdLineParser parser = new CmdLineParser(config);
    parser.setUsageWidth(80);
    try {
      parser.parseArgument(args);
      config.checkValid(parser);
      if (config.isHelp()) {
        parser.printUsage(new PrintWriter(System.err), null);
        return null;
      }
    } catch (final CmdLineException e) {
      System.err.println(e.getMessage() + "\n");
      parser.printUsage(new PrintWriter(System.err), null);
      return null;
    }
    return config;
  }

}
