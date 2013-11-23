package de.matrixweb.smaller.dev.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author markusw
 */
public class Servlet extends WebSocketServlet {

  private static final long serialVersionUID = -7006026708380905881L;

  private static final Logger LOGGER = LoggerFactory.getLogger(Servlet.class);

  private final HttpClient client;

  private final Config config;

  private final SmallerResourceHandler resourceHandler;

  /**
   * @param config
   * @param resourceHandler
   */
  public Servlet(final Config config,
      final SmallerResourceHandler resourceHandler) {
    this.config = config;
    this.resourceHandler = resourceHandler;
    this.client = new HttpClient(new HttpHost(config.getProxyhost(),
        config.getProxyport()), config.getPort());
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
      if (this.config.getProcess() != null
          && this.config.getProcess().contains(uri)) {
        // TODO: Allow wildcard uris
        this.resourceHandler.process(baos, response, uri);
      } else {
        try {
          handleProxyRequest(baos, request, response, uri);
        } catch (final IOException e) {
          LOGGER.warn("Unable to proxy request", e);
          // TODO: IOException is not the right one => only render template if
          // 404 is given from proxy or proxy is not available
          try {
            this.resourceHandler.renderTemplate(baos, request, response, uri);
          } catch (final IOException e2) {
            LOGGER.error("Failed to render template", e2);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
          }
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
                contentType);
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
      final ContentType contentType) throws IOException {
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
      LOGGER.debug("Send proxy response");
      try (InputStream in = entity.getContent()) {
        IOUtils.copy(in, out);
        if (this.config.isLiveReload()
            && "text/html".equals(contentType.getMimeType())) {
          LOGGER.debug("Injecting live-reload snippet");
          out.write(this.resourceHandler.getLiveReloadClient()
              .getBytes("UTF-8"));
        }
      }
    }
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

}
