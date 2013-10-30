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

  /**
   * @param args
   * @throws Exception
   */
  public static void main(final String[] args) throws Exception {
    Locale.setDefault(Locale.ENGLISH);
    new Main().run(args);
  }

  private void run(final String... args) throws Exception {
    final Config config = parseArgs(args);
    if (config == null) {
      return;
    }

    final Logger logger = (Logger) LoggerFactory.getLogger(Main.class);
    if (config.isDebug()) {
      logger.setLevel(Level.DEBUG);
    }

    final SmallerResourceHandler resourceHandler = new SmallerResourceHandler(
        config);
    final Servlet servlet = new Servlet(config, resourceHandler);
    final Server server = new Server(InetSocketAddress.createUnresolved(
        config.getHost(), config.getPort()));
    final ServletHandler handler = new ServletHandler();
    handler.addServletWithMapping(new ServletHolder(servlet), "/");
    server.setHandler(handler);
    server.start();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (resourceHandler != null) {
          try {
            resourceHandler.dispose();
          } catch (final IOException e) {
            logger.error("Failed to shutdown watchdog", e);
          }
        }
        if (server != null) {
          try {
            server.stop();
          } catch (final Exception e) {
            logger.error("Failed to shutdown jetty", e);
          }
        }
      }
    });

    server.join();
  }

  private Config parseArgs(final String... args) {
    final Config config = new Config();
    final CmdLineParser parser = new CmdLineParser(config);
    parser.setUsageWidth(80);
    try {
      parser.parseArgument(args);
      if (config.isHelp()) {
        parser.printUsage(new PrintWriter(System.err), null);
        return null;
      }
    } catch (final CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(new PrintWriter(System.err), null);
      return null;
    }
    return config;
  }

}
