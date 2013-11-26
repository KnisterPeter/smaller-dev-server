package de.matrixweb.smaller.dev.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import de.matrixweb.smaller.config.ConfigFile;

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

    final Config config = parseArgs(setupFromConfigFile(args));
    if (config == null) {
      return;
    }

    System.setProperty("logback.configurationFile", "logback-dev-server.xml");
    final LoggerContext loggerContext = (LoggerContext) LoggerFactory
        .getILoggerFactory();
    this.logger = loggerContext.getLogger("de.matrixweb");
    if (config.isDebug()) {
      this.logger.setLevel(Level.DEBUG);
      this.logger.debug("Enabled verbose logging");
    } else {
      loggerContext.getLogger("de.matrixweb.smaller").setLevel(Level.INFO);
    }

    this.resourceHandler = new SmallerResourceHandler(config);
    final Servlet servlet = new Servlet(config, this.resourceHandler);
    this.server = new Server(InetSocketAddress.createUnresolved(
        config.getHost(), config.getPort()));
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
    this.logger.info("Stopping server");
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

  private String[] setupFromConfigFile(final String... args) throws IOException {
    final List<String> result = new ArrayList<>();
    final Iterator<String> it = Arrays.asList(args).iterator();
    while (it.hasNext()) {
      final String arg = it.next();
      if (arg.startsWith("@")) {
        final ConfigFile config = ConfigFile.read(new File(arg.substring(1)));
        for (final String folder : config.getFiles().getFolder()) {
          result.add("--document-root");
          result.add(folder);
        }
        if (config.getDevServer().isDebug()) {
          result.add("--verbose");
        }
        if (config.getDevServer().isLiveReload()) {
          result.add("--live-reload");
          if (config.getDevServer().isForceFullReload()) {
            result.add("--force-full-reload");
          }
        }
        if (config.getDevServer().getIp() != null) {
          result.add("--ip");
          result.add(config.getDevServer().getIp());
        }
        if (config.getDevServer().getPort() > 0) {
          result.add("--port");
          result.add(String.valueOf(config.getDevServer().getPort()));
        }
        if (config.getDevServer().getProxyhost() != null) {
          result.add("--proxyhost");
          result.add(config.getDevServer().getProxyhost());
        }
        if (config.getDevServer().getProxyport() > 0) {
          result.add("--proxyport");
          result.add(String.valueOf(config.getDevServer().getProxyport()));
        }
        if (config.getDevServer().getTemplateEngine() != null) {
          result.add("--template-engine");
          result.add(config.getDevServer().getTemplateEngine());
        }
        if (config.getDevServer().getTests() != null) {
          result.add("--test-framework");
          result.add(config.getDevServer().getTests().getFramework());
          result.add("--test-directory");
          result.add(config.getDevServer().getTests().getFolder());
        }
        for (final String process : config.getDevServer().getProcess()) {
          result.add("--process");
          result.add(process);
        }
        if (config.getProcessors() != null) {
          if (config.getProcessors().get("js") != null) {
            result.add("--js-processors");
            result.add(StringUtils.join(config.getProcessors().get("js"), ','));
          }
          if (config.getProcessors().get("css") != null) {
            result.add("--css-processors");
            result.add(StringUtils.join(config.getProcessors().get("css"), ','));
          }
        }

        final List<String> inFiles = new ArrayList<>(2);
        for (final String[] values : config.getProcessors().values()) {
          inFiles.add(config.getTasks().get(values[0]).getSrc()[0]);
        }
        for (final String in : inFiles) {
          result.add("--in");
          result.add(in);
        }
        if (config.getDevServer().isInjectPartials()) {
          result.add("--inject-partials");
        }

        // TODO: Configuration
        // config.getTasks();
      } else {
        result.add(arg);
      }
    }
    return result.toArray(new String[result.size()]);
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
