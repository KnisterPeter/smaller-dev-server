package de.matrixweb.smaller.dev.server;

import org.kohsuke.args4j.Option;

/**
 * @author markusw
 */
public class Cmdline {

  @Option(name = "-h", aliases = { "--help" }, usage = "This screen")
  private boolean help = false;

  @Option(name = "--ip", usage = "The ip to bind to - defaults to 0.0.0.0")
  private String host;

  @Option(name = "--port", usage = "The port to bind to - defaults to 12345")
  private Integer port;

  @Option(name = "--proxyhost", usage = "The host to proxy")
  private String proxyhost;

  @Option(name = "--proxyport", usage = "The port to proxy - defaults to 80")
  private Integer proxyport;

  @Option(name = "-v", aliases = { "--verbose" }, usage = "To log debug info")
  private Boolean debug;

  @Option(name = "-l", aliases = { "--live-reload" }, usage = "Flag to enable live-reload feature")
  private Boolean liveReload;

  @Option(name = "--force-full-reload", usage = "Flag to force always full reload on resource changes; Defaults to true for compatibility")
  private Boolean forceFullReload = true;

  /**
   * @return the help
   */
  public Boolean isHelp() {
    return this.help;
  }

  /**
   * @param help
   *          the help to set
   */
  public void setHelp(final Boolean help) {
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
  public Integer getPort() {
    return this.port;
  }

  /**
   * @param port
   *          the port to set
   */
  public void setPort(final Integer port) {
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
  public Integer getProxyport() {
    return this.proxyport;
  }

  /**
   * @param proxyport
   *          the proxyport to set
   */
  public void setProxyport(final Integer proxyport) {
    this.proxyport = proxyport;
  }

  /**
   * @return the debug
   */
  public Boolean getDebug() {
    return this.debug;
  }

  /**
   * @param debug
   *          the debug to set
   */
  public void setDebug(final Boolean debug) {
    this.debug = debug;
  }

  /**
   * @return the liveReload
   */
  public Boolean getLiveReload() {
    return this.liveReload;
  }

  /**
   * @param liveReload
   *          the liveReload to set
   */
  public void setLiveReload(final Boolean liveReload) {
    this.liveReload = liveReload;
  }

  /**
   * @return the forceFullReload
   */
  public Boolean getForceFullReload() {
    return this.forceFullReload;
  }

  /**
   * @param forceFullReload
   *          the forceFullReload to set
   */
  public void setForceFullReload(final Boolean forceFullReload) {
    this.forceFullReload = forceFullReload;
  }

}
