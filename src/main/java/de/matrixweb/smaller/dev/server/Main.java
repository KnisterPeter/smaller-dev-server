package de.matrixweb.smaller.dev.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import de.matrixweb.smaller.clients.common.Util;
import de.matrixweb.smaller.common.Manifest;
import de.matrixweb.smaller.common.SmallerException;
import de.matrixweb.smaller.config.ConfigFile;
import de.matrixweb.smaller.config.DevServer;
import de.matrixweb.smaller.config.Environment;

/**
 * @author markusw
 */
public class Main {

  private Logger logger;

  private Map<Environment, SmallerResourceHandler> resourceHandlers;

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

    final Cmdline config = parseArgs(args);
    if (config == null) {
      return;
    }
    final ConfigFile configFile = parseConfigFile(args);
    updateConfigFileFromCmdLine(configFile, config);

    System.setProperty("logback.configurationFile", "logback-dev-server.xml");
    final LoggerContext loggerContext = (LoggerContext) LoggerFactory
        .getILoggerFactory();
    this.logger = loggerContext.getLogger("de.matrixweb");
    if (configFile.getDevServer().isDebug()) {
      this.logger.setLevel(Level.DEBUG);
      this.logger.debug("Enabled verbose logging");
    } else {
      loggerContext.getLogger("de.matrixweb.smaller").setLevel(Level.INFO);
    }

    final Manifest manifest = new Util(null)
        .convertConfigFileToManifest(configFile);

    this.resourceHandlers = new HashMap<>();
    for (final Environment env : configFile.getEnvironments().values()) {
      this.resourceHandlers.put(env,
          new SmallerResourceHandler(configFile.getDevServer(), env, manifest));
    }

    final Servlet servlet = new Servlet(configFile, this.resourceHandlers);
    this.server = new Server(InetSocketAddress.createUnresolved(configFile
        .getDevServer().getIp(), configFile.getDevServer().getPort()));
    final ServletContextHandler handler = new ServletContextHandler();
    handler.addServlet(new ServletHolder(servlet), "/");
    this.server.setHandler(handler);
    try {
      this.server.start();
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          Main.this.stop();
        }
      });

      this.server.join();
    } catch (final BindException e) {
      stop();
    }
  }

  /**
   * 
   */
  public void stop() {
    logInfo("Stopping server");
    if (this.resourceHandlers != null) {
      for (final SmallerResourceHandler handler : this.resourceHandlers
          .values()) {
        try {
          handler.dispose();
        } catch (final IOException e) {
          logError("Failed to shutdown resource handler", e);
        }
      }
    }
    if (this.server != null) {
      try {
        this.server.stop();
      } catch (final Exception e) {
        logError("Failed to shutdown jetty", e);
      }
    }
  }

  private ConfigFile parseConfigFile(final String... args) throws IOException {
    ConfigFile configFile = null;

    final Iterator<String> it = Arrays.asList(args).iterator();
    while (it.hasNext()) {
      final String arg = it.next();
      if (arg.startsWith("@")) {
        if (configFile != null) {
          throw new SmallerException("Found multiple config files");
        }
        configFile = ConfigFile.read(new File(arg.substring(1)));
      }
    }

    if (configFile == null) {
      configFile = new ConfigFile();
    }
    return configFile;
  }

  @Deprecated
  private Cmdline parseArgs(final String... args) {
    final Cmdline config = new Cmdline();
    final CmdLineParser parser = new CmdLineParser(config);
    parser.setUsageWidth(80);
    try {
      final List<String> params = new ArrayList<>();
      for (final String arg : args) {
        if (!arg.startsWith("@")) {
          params.add(arg);
        }
      }
      parser.parseArgument(params);
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

  private void updateConfigFileFromCmdLine(final ConfigFile configFile,
      final Cmdline cmdline) {
    final DevServer dev = configFile.getDevServer();
    if (cmdline.getHost() != null) {
      dev.setIp(cmdline.getHost());
    }
    if (cmdline.getPort() != null) {
      dev.setPort(cmdline.getPort());
    }
    if (cmdline.getProxyhost() != null) {
      dev.setProxyhost(cmdline.getProxyhost());
    }
    if (cmdline.getProxyport() != null) {
      dev.setProxyport(cmdline.getProxyport());
    }
    if (cmdline.getDebug() != null) {
      dev.setDebug(cmdline.getDebug());
    }
    if (cmdline.getForceFullReload() != null) {
      dev.setForceFullReload(cmdline.getForceFullReload());
    }
    if (cmdline.getLiveReload() != null) {
      dev.setLiveReload(cmdline.getLiveReload());
    }
  }

  private void logInfo(final String msg) {
    if (this.logger != null) {
      this.logger.info(msg);
    } else {
      System.out.println(msg);
    }
  }

  private void logError(final String msg, final Exception e) {
    if (this.logger != null) {
      this.logger.error(msg, e);
    } else {
      System.err.println(msg);
      e.printStackTrace(System.err);
    }
  }

}
