package de.matrixweb.smaller.dev.server;

import java.io.File;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author markusw
 */
public class Config {

  private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

  @Option(name = "-h", aliases = { "--help" }, usage = "This screen")
  private boolean help = false;

  @Option(name = "--ip", usage = "The ip to bind to - defaults to 0.0.0.0")
  private String host = "0.0.0.0";

  @Option(name = "--port", usage = "The port to bind to - defaults to 12345")
  private int port = 12345;

  @Option(name = "--proxyhost", required = true, usage = "The host to proxy")
  private String proxyhost;

  @Option(name = "--proxyport", usage = "The port to proxy - defaults to 80")
  private int proxyport = 80;

  @Option(name = "-v", aliases = { "--verbose" }, usage = "To log debug info")
  private boolean debug = false;

  @Option(name = "-p", aliases = { "--processors" }, usage = "To processors to apply to intercepted requests")
  private String processors;

  @Option(name = "-i", aliases = { "--in" }, multiValued = true, usage = "The main input files if any")
  private List<String> in;

  @Option(name = "-P", aliases = { "--process" }, multiValued = true, usage = "The requests to intercept")
  private List<String> process;

  @Option(name = "-d", aliases = { "--document-root" }, multiValued = true, usage = "The folders to scan for resources")
  private List<File> documentRoots;

  @Option(name = "-t", aliases = { "--template-engine" }, usage = "The template engine to use. Could be one of:\n"
      + "  raw        - Just deliveres raw html files\n"
      + "  soy        - Google Closure templates\n"
      + "  velocity   - Apache Velocity templates\n"
      + "  handlebars - Handlebars templates")
  private String templateEngine;

  @Option(name = "-l", aliases = { "--live-reload" }, usage = "Flag to enable live-reload feature")
  private boolean liveReload;

  @Option(name = "--force-full-reload", usage = "Flag to force always full reload on resource changes; Defaults to true for compatibility")
  private boolean forceFullReload = true;

  @Option(name = "--test-framework", usage = "The test framework to use. Could be one of:\n"
      + "  jasmine - Runs jasmine specs (all files matching '*_spec.js'")
  private String testFramework;

  @Option(name = "--test-directory", usage = "The directory the tests are located")
  private File testFolder;

  @Option(name = "--inject-partials", usage = "This enables the injection of partials in proxied pages; Could slowdown the proxy process")
  private boolean injectPartials = false;

  /**
   * @param parser
   * @throws CmdLineException
   *           Thrown if option dependencies are not valid
   */
  public void checkValid(final CmdLineParser parser) throws CmdLineException {
    if ((this.process != null || this.templateEngine != null)
        && this.documentRoots == null) {
      throw new CmdLineException(parser,
          "--document-root is required if --process or --template-engine is given");
    }
    if (this.testFramework == null ^ this.testFolder == null) {
      throw new CmdLineException(parser,
          "--test-framework and --test-directory are required; only one is given");
    }
    if (this.processors == null ^ this.in == null) {
      LOGGER
          .warn("\n\n*** Defined processors or input files but not both. This will result in strange behaviour! ***\n");
    }
  }

  /**
   * @return the help
   */
  public boolean isHelp() {
    return this.help;
  }

  /**
   * @param help
   *          the help to set
   */
  public void setHelp(final boolean help) {
    this.help = help;
  }

  /**
   * @return the host
   */
  public String getHost() {
    return this.host;
  }

  /**
   * @param host
   *          the host to set
   */
  public void setHost(final String host) {
    this.host = host;
  }

  /**
   * @return the port
   */
  public int getPort() {
    return this.port;
  }

  /**
   * @param port
   *          the port to set
   */
  public void setPort(final int port) {
    this.port = port;
  }

  /**
   * @return the proxyhost
   */
  public String getProxyhost() {
    return this.proxyhost;
  }

  /**
   * @param proxyhost
   *          the proxyhost to set
   */
  public void setProxyhost(final String proxyhost) {
    this.proxyhost = proxyhost;
  }

  /**
   * @return the proxyport
   */
  public int getProxyport() {
    return this.proxyport;
  }

  /**
   * @param proxyport
   *          the proxyport to set
   */
  public void setProxyport(final int proxyport) {
    this.proxyport = proxyport;
  }

  /**
   * @return the debug
   */
  public boolean isDebug() {
    return this.debug;
  }

  /**
   * @param debug
   *          the debug to set
   */
  public void setDebug(final boolean debug) {
    this.debug = debug;
  }

  /**
   * @return the processors
   */
  public String getProcessors() {
    return this.processors;
  }

  /**
   * @param processors
   *          the processors to set
   */
  public void setProcessors(final String processors) {
    this.processors = processors;
  }

  /**
   * @return the in
   */
  public List<String> getIn() {
    return this.in;
  }

  /**
   * @param in
   *          the in to set
   */
  public void setIn(final List<String> in) {
    this.in = in;
  }

  /**
   * @return the process
   */
  public List<String> getProcess() {
    return this.process;
  }

  /**
   * @param process
   *          the process to set
   */
  public void setProcess(final List<String> process) {
    this.process = process;
  }

  /**
   * @return the documentRoots
   */
  public List<File> getDocumentRoots() {
    return this.documentRoots;
  }

  /**
   * @param documentRoots
   *          the documentRoots to set
   */
  public void setDocumentRoots(final List<File> documentRoots) {
    this.documentRoots = documentRoots;
  }

  /**
   * @return the templateEngine
   */
  public String getTemplateEngine() {
    return this.templateEngine;
  }

  /**
   * @param templateEngine
   *          the templateEngine to set
   */
  public void setTemplateEngine(final String templateEngine) {
    this.templateEngine = templateEngine;
  }

  /**
   * @return the liveReload
   */
  public boolean isLiveReload() {
    return this.liveReload;
  }

  /**
   * @param liveReload
   *          the liveReload to set
   */
  public void setLiveReload(final boolean liveReload) {
    this.liveReload = liveReload;
  }

  /**
   * @return the forceFullReload
   */
  public boolean isForceFullReload() {
    return this.forceFullReload;
  }

  /**
   * @param forceFullReload
   *          the forceFullReload to set
   */
  public void setForceFullReload(final boolean forceFullReload) {
    this.forceFullReload = forceFullReload;
  }

  /**
   * @return the testFramework
   */
  public String getTestFramework() {
    return this.testFramework;
  }

  /**
   * @param testFramework
   *          the testFramework to set
   */
  public void setTestFramework(final String testFramework) {
    this.testFramework = testFramework;
  }

  /**
   * @return the testFolder
   */
  public File getTestFolder() {
    return this.testFolder;
  }

  /**
   * @param testFolder
   *          the testFolder to set
   */
  public void setTestFolder(final File testFolder) {
    this.testFolder = testFolder;
  }

  /**
   * @return the injectPartials
   */
  public boolean isInjectPartials() {
    return this.injectPartials;
  }

  /**
   * @param injectPartials
   *          the injectPartials to set
   */
  public void setInjectPartials(final boolean injectPartials) {
    this.injectPartials = injectPartials;
  }

}
