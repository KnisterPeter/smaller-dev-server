package de.matrixweb.smaller.dev.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.matrixweb.smaller.config.ConfigFile;
import de.matrixweb.smaller.config.Environment;

/**
 * @author markusw
 */
public class Servlet extends WebSocketServlet {

  private static final long serialVersionUID = -7006026708380905881L;

  private static final Logger LOGGER = LoggerFactory.getLogger(Servlet.class);

  private final HttpClient client;

  private final ConfigFile configFile;

  private final Map<Environment, SmallerResourceHandler> resourceHandlers;

  private String liveReloadClient = null;

  /**
   * @param configFile
   * @param resourceHandlers
   */
  public Servlet(final ConfigFile configFile,
      final Map<Environment, SmallerResourceHandler> resourceHandlers) {
    this.configFile = configFile;
    this.resourceHandlers = resourceHandlers;
    this.client = new HttpClient(new HttpHost(configFile.getDevServer()
        .getProxyhost(), configFile.getDevServer().getProxyport()), configFile
        .getDevServer().getPort());
    LiveReloadSocket.start();
  }

  /**
   * @see org.eclipse.jetty.websocket.WebSocketFactory.Acceptor#doWebSocketConnect(javax.servlet.http.HttpServletRequest,
   *      java.lang.String)
   */
  @Override
  public WebSocket doWebSocketConnect(final HttpServletRequest request,
      final String protocol) {
    if ("live-reload".equals(protocol)) {
      return LiveReloadSocket.create();
    }
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(final HttpServletRequest request,
      final HttpServletResponse response) throws ServletException, IOException {
    handleHttpRequest(request, response);
  }

  /**
   * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doPost(final HttpServletRequest request,
      final HttpServletResponse response) throws ServletException, IOException {
    handleHttpRequest(request, response);
  }

  private void handleHttpRequest(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      final String uri = request.getRequestURI();
      LOGGER.debug("Requested uri: {}", uri);
      final Environment env = findEnvironmentByUri(uri);
      if (env != null) {
        this.resourceHandlers.get(env).process(baos, response, uri);
      } else {
        try {
          handleProxyRequest(baos, request, response, uri);
        } catch (final ConnectException | PageNotFoundException e) {
          tryTemplateRendering(baos, request, response, uri);
        } catch (final IOException e) {
          LOGGER.warn("Unable to proxy request", e);
          tryTemplateRendering(baos, request, response, uri);
        }
      }
    } catch (final Exception e) {
      LOGGER.error("Failed to handle request", e);
      response.setContentType("text/html");
      final PrintWriter writer = new PrintWriter(baos);
      writer.write("<html><body><pre>");
      e.printStackTrace(writer);
      writer.write("</pre></body></html>");
      writer.flush();
    }
    try (ServletOutputStream out = response.getOutputStream()) {
      out.write(baos.toByteArray());
    }
  }

  private void tryTemplateRendering(final OutputStream out,
      final HttpServletRequest request, final HttpServletResponse response,
      final String uri) throws IOException {
    try {
      final Environment env = findEnvironmentByFile(uri);
      if (env != null) {
        this.resourceHandlers.get(env).renderTemplate(out, request, response,
            uri, getLiveReloadClient());
      }
    } catch (final IOException e) {
      LOGGER.error("Failed to render template", e);
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private void handleProxyRequest(final OutputStream out,
      final HttpServletRequest request, final HttpServletResponse response,
      final String uri) throws IOException {
    final String path = uri
        + (request.getQueryString() != null ? "?" + request.getQueryString()
            : "");
    this.client.request(createClientRequest(request, path),
        new HttpClient.ResponseHandler() {
          @Override
          public void handle(final StatusLine statusLine,
              final Header[] headers, final HttpEntity entity,
              final ContentType contentType, final Charset charset)
              throws IOException {
            handleResponse(out, statusLine, response, headers, entity,
                contentType, request, uri);
          }
        });
  }

  private HttpRequest createClientRequest(final HttpServletRequest request,
      final String path) throws IOException {
    HttpRequest clientRequest = null;

    if ("POST".equalsIgnoreCase(request.getMethod())) {
      final BasicHttpEntityEnclosingRequest entityRequest = new BasicHttpEntityEnclosingRequest(
          "POST", path);
      entityRequest.setEntity(new InputStreamEntity(request.getInputStream(),
          request.getContentLength(), ContentType.parse(request
              .getContentType())));
      clientRequest = entityRequest;
    } else {
      clientRequest = new BasicHttpRequest(request.getMethod(), path);
    }

    final List<String> skipHeaders = Arrays.asList("Host", "Connection",
        "Accept-Encoding");
    final Enumeration<String> names = request.getHeaderNames();
    while (names.hasMoreElements()) {
      final String name = names.nextElement();
      if (!skipHeaders.contains(name)) {
        final Enumeration<String> values = request.getHeaders(name);
        while (values.hasMoreElements()) {
          final String value = values.nextElement();
          LOGGER.debug("Add request header: " + name + ":" + value);
          clientRequest.addHeader(name, value);
        }
      }
    }

    return clientRequest;
  }

  private void handleResponse(final OutputStream out,
      final StatusLine statusLine, final HttpServletResponse response,
      final Header[] headers, final HttpEntity entity,
      final ContentType contentType, final HttpServletRequest request,
      final String uri) throws IOException {
    // TODO: Make this configurable?
    if (statusLine.getStatusCode() == 404) {
      throw new PageNotFoundException();
    }
    response.setStatus(statusLine.getStatusCode());

    final List<String> skipHeaders = Arrays.asList("Connection",
        "Content-Length");
    for (final Header header : headers) {
      if (!skipHeaders.contains(header.getName())) {
        LOGGER.debug("Add response header: " + header.getName() + ":"
            + header.getValue());
        response.addHeader(header.getName(), header.getValue());
      }
    }

    if (entity != null) {
      try (InputStream in = entity.getContent()) {
        if (this.configFile.getDevServer().isInjectPartials()
            && contentType != null
            && "text/html".equals(contentType.getMimeType())) {
          final Document doc = Jsoup.parse(IOUtils.toString(in));
          injectPartials(doc, uri, request);
          out.write(doc.toString().getBytes("UTF-8"));
        } else {
          LOGGER.debug("Send proxy response");
          IOUtils.copy(in, out);
        }
        if (this.configFile.getDevServer().isLiveReload()
            && contentType != null
            && "text/html".equals(contentType.getMimeType())) {
          LOGGER.debug("Injecting live-reload snippet");
          out.write(getLiveReloadClient().getBytes("UTF-8"));
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void injectPartials(final Document doc, final String uri,
      final HttpServletRequest request) throws IOException,
      UnsupportedEncodingException {
    LOGGER.info("Injecting partials for uri: {}", uri);

    final Environment env = findEnvironmentByFile(uri);
    if (env != null) {
      final SmallerResourceHandler handler = this.resourceHandlers.get(env);

      final Map<String, Object> data = handler.loadRequestConfiguration(uri,
          request);
      if (data.containsKey("partials")) {
        final Map<String, Object> partials = (Map<String, Object>) data
            .get("partials");
        for (final Entry<String, Object> partial : partials.entrySet()) {
          final Map<String, String> partialConfig = (Map<String, String>) partial
              .getValue();
          for (final Element el : doc.select(partial.getKey())) {
            final ByteArrayOutputStream capture = new ByteArrayOutputStream();
            handler.renderTemplate(capture, request, null,
                partialConfig.get("partial"), data, null);
            if ("appendChild".equals(partialConfig.get("mode"))) {
              el.append(capture.toString("UTF-8"));
            } else if ("prependChild".equals(partialConfig.get("mode"))) {
              el.prepend(capture.toString("UTF-8"));
            } else if ("replace".equals(partialConfig.get("mode"))) {
              el.replaceWith(Jsoup.parseBodyFragment(capture.toString("UTF-8"))
                  .select("body *").first());
            }
          }
        }
      }
    }
  }

  private Environment findEnvironmentByUri(final String uri) {
    for (final Environment env : this.configFile.getEnvironments().values()) {
      if (ArrayUtils.contains(env.getProcess(), uri)) {
        return env;
      }
    }
    return null;
  }

  private Environment findEnvironmentByFile(final String file) {
    for (final Entry<Environment, SmallerResourceHandler> entry : this.resourceHandlers
        .entrySet()) {
      final SmallerResourceHandler resourceHandler = entry.getValue();
      if (resourceHandler.hasFile(resourceHandler.getTemplateEngine()
          .getTemplateUri(file)) || resourceHandler.hasFile(file + ".cfg.json")) {
        return entry.getKey();
      }
    }
    return null;
  }

  /**
   * @return Returns the live-reload client code
   * @throws IOException
   */
  public String getLiveReloadClient() throws IOException {
    if (this.liveReloadClient == null) {
      final StringBuilder sb = new StringBuilder();
      sb.append("<script text=\"javascript\">");
      final InputStream in = getClass().getResourceAsStream("/live-reload.js");
      try {
        sb.append(IOUtils.toString(in));
      } finally {
        in.close();
      }
      sb.append("</script>");
      this.liveReloadClient = sb.toString();
    }
    return this.liveReloadClient;
  }

  /**
   * @see javax.servlet.Servlet#destroy()
   */
  @Override
  public void destroy() {
    if (this.client != null) {
      this.client.dispose();
    }
    LiveReloadSocket.stop();
    super.destroy();
  }

  static class PageNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -6875816786862210687L;

  }

}
