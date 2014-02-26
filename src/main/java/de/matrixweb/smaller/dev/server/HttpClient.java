package de.matrixweb.smaller.dev.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.SocketFactory;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.matrixweb.smaller.dev.server.Servlet.PageNotFoundException;

/**
 * @author markusw
 */
public class HttpClient {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(HttpClient.class);

  private final Pattern rewritePattern;

  private final HttpHost targetHost;

  private final int localport;

  private final SocketFactory socketFactory;

  private final BasicHttpProcessor httpProcessor;

  private final HttpRequestExecutor executor;

  private final ObjectPool<DefaultHttpClientConnection> connPoll;

  private static final PoolableObjectFactory<DefaultHttpClientConnection> POOL_FACTORY = new BasePoolableObjectFactory<DefaultHttpClientConnection>() {
    @Override
    public DefaultHttpClientConnection makeObject() {
      return new DefaultHttpClientConnection();
    }

    @Override
    public void destroyObject(final DefaultHttpClientConnection conn)
        throws IOException {
      conn.close();
    }
  };

  /**
   * @param targetHost
   * @param localport
   */
  public HttpClient(final HttpHost targetHost, final int localport) {
    this.targetHost = targetHost;
    this.localport = localport;
    this.socketFactory = SocketFactory.getDefault();

    this.rewritePattern = Pattern.compile("^(http.?://)"
        + targetHost.getHostName() + "(?::" + targetHost.getPort() + ")?(.*)$");

    this.httpProcessor = new BasicHttpProcessor();
    // Required request interceptors
    this.httpProcessor.addInterceptor(new RequestContent(true));
    this.httpProcessor.addInterceptor(new RequestTargetHost() {
      @Override
      public void process(final HttpRequest request, final HttpContext context)
          throws HttpException, IOException {
        if (!request.containsHeader(HTTP.TARGET_HOST)) {
          final HttpHost targetHost = (HttpHost) context
              .getAttribute(ExecutionContext.HTTP_TARGET_HOST);
          // Skip port in header if default
          if (targetHost.getPort() == 80) {
            request.addHeader(HTTP.TARGET_HOST, targetHost.getHostName());
          } else {
            super.process(request, context);
          }
          LOGGER.debug("Send Host header: {}",
              request.getHeaders(HTTP.TARGET_HOST));
        }
      }
    });
    // Recommended request interceptors
    this.httpProcessor.addInterceptor(new RequestConnControl());
    this.httpProcessor.addInterceptor(new RequestUserAgent());
    this.httpProcessor.addInterceptor(new RequestExpectContinue());

    this.executor = new HttpRequestExecutor();

    this.connPoll = new GenericObjectPool<>(POOL_FACTORY, 8,
        GenericObjectPool.WHEN_EXHAUSTED_BLOCK,
        GenericObjectPool.DEFAULT_MAX_WAIT);
  }

  /**
   * @param request
   * @param responseHandler
   * @throws IOException
   */
  public void request(final HttpRequest request,
      final ResponseHandler responseHandler) throws IOException {
    try {
      DefaultHttpClientConnection conn = this.connPoll.borrowObject();
      try {
        openConnection(this.targetHost, conn, request.getParams());
        handleResponse(responseHandler,
            executeRequest(request, conn, createContext(request, conn)));
      } catch (final IOException e) {
        this.connPoll.invalidateObject(conn);
        conn = null;
        throw e;
      } finally {
        if (conn != null) {
          this.connPoll.returnObject(conn);
        }
      }
    } catch (final PageNotFoundException e) {
      throw e;
    } catch (final Exception e) {
      if (e instanceof IOException) {
        throw (IOException) e;
      }
      throw new IOException(
          "Unhandled Exception; Probably connection cooling problem", e);
    }
  }

  private void openConnection(final HttpHost targetHost,
      final DefaultHttpClientConnection conn, final HttpParams params)
      throws IOException {
    if (!conn.isOpen()) {
      final Socket socket = this.socketFactory.createSocket();
      final int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
      final int soTimeout = HttpConnectionParams.getSoTimeout(params);
      socket.setSoTimeout(soTimeout);
      socket
          .connect(
              new InetSocketAddress(targetHost.getHostName(), targetHost
                  .getPort()), connTimeout);
      conn.bind(socket, params);
    }
  }

  private BasicHttpContext createContext(final HttpRequest request,
      final DefaultHttpClientConnection conn) {
    final BasicHttpContext context = new BasicHttpContext(null);
    context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
    context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, this.targetHost);
    context.setAttribute(ExecutionContext.HTTP_REQUEST, request);
    return context;
  }

  private HttpResponse executeRequest(final HttpRequest request,
      final DefaultHttpClientConnection conn, final BasicHttpContext context)
      throws IOException {
    return executeRequest(request, conn, context, true);
  }

  private HttpResponse executeRequest(final HttpRequest request,
      final DefaultHttpClientConnection conn, final BasicHttpContext context,
      final boolean withRetry) throws IOException {
    HttpResponse response = null;
    try {
      this.executor.preProcess(request, this.httpProcessor, context);
      response = this.executor.execute(request, conn, context);
      this.executor.postProcess(response, this.httpProcessor, context);
    } catch (SocketException | NoHttpResponseException e) {
      if (!withRetry) {
        throw e;
      }
      // Retry with reopened connection
      openConnection(this.targetHost, conn, request.getParams());
      return executeRequest(request, conn, context, false);
    } catch (final HttpException e) {
      if (!withRetry) {
        throw new IOException("Failed to execute http request", e);
      }
      // Retry with reopened connection
      openConnection(this.targetHost, conn, request.getParams());
      return executeRequest(request, conn, context, false);
    }
    return response;
  }

  private void handleResponse(final ResponseHandler responseHandler,
      final HttpResponse response) throws IOException {
    rewriteRedirect(response);
    final HttpEntity entity = response.getEntity();
    ContentType ct = null;
    Charset charset = HTTP.DEF_CONTENT_CHARSET;
    if (entity != null) {
      ct = ContentType.get(entity);
      if (ct != null) {
        charset = ct.getCharset();
      }
      if (charset == null) {
        charset = HTTP.DEF_CONTENT_CHARSET;
      }
    }
    responseHandler.handle(response.getStatusLine(), response.getAllHeaders(),
        entity, ct, charset);
  }

  private void rewriteRedirect(final HttpResponse response) {
    final Header location = response.getFirstHeader("Location");
    if (location != null) {
      final Matcher matcher = this.rewritePattern.matcher(location.getValue());
      if (matcher.matches()) {
        response.setHeader("Location", matcher.group(1) + "localhost:"
            + this.localport + matcher.group(2));
      }
    }
  }

  /**
   * 
   */
  public void dispose() {
    try {
      this.connPoll.close();
    } catch (final Exception e) {
      LOGGER.error("Failed to close connection pool", e);
    }
  }

  /**
   * 
   */
  public static interface ResponseHandler {

    /**
     * @param statusLine
     * @param headers
     * @param entity
     * @param contentType
     * @param charset
     * @throws IOException
     */
    void handle(StatusLine statusLine, Header[] headers, HttpEntity entity,
        ContentType contentType, Charset charset) throws IOException;

  }

}
