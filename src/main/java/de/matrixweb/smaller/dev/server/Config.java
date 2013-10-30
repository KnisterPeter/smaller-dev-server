package de.matrixweb.smaller.dev.server;

import java.io.File;
import java.util.List;

import org.kohsuke.args4j.Option;

/**
 * @author markusw
 */
public class Config {

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
      + "  raw      - Just deliveres raw html files\n"
      + "  soy      - Uses google closure templates to produce the output\n"
      + "  velocity - Uses apache velocity to produce the output")
  private String templateEngine;

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

}
