package test.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DebugHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;

/**
 * @author markusw
 */
public class TestServer {

  /**
   * @param args
   * @throws Exception
   */
  public static void main(final String[] args) throws Exception {
    final Server server = new Server(3000);
    final DebugHandler debug = new DebugHandler();
    debug.setOutputStream(System.out);
    debug.setHandler(new DefaultHandler() {
      @Override
      public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
          final HttpServletResponse response) throws IOException, ServletException {

        System.err.println("target: " + target);
        if ("/GET/reply".equals(target)) {
          if ("GET".equalsIgnoreCase(request.getMethod())) {
            response.setContentType("text/html");
            final PrintWriter writer = response.getWriter();
            writer
                .write("<html><body><h1>Hello World</h1>"
                    + "<form action=\"/POST/reply\" method=\"POST\"><input type=\"hidden\" value=\"some-value\" /><input type=\"submit\" value=\"POST reply\" /></form>"
                    + "<form action=\"/POST/redirect\" method=\"POST\"><input type=\"hidden\" value=\"some-value\" /><input type=\"submit\" value=\"POST redirect\" /></form>"
                    + "</body></html>");
            writer.close();
          } else {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
          }
        } else if ("/POST/reply".equals(target)) {
          if ("POST".equalsIgnoreCase(request.getMethod())) {
            response.setContentType("text/html");
            final PrintWriter writer = response.getWriter();
            writer.write("<html><body><h1>Hello World</h1><a href=\"/GET/reply\">GET</a></body></html>");
            writer.close();
          } else {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
          }
        } else if ("/POST/redirect".equals(target)) {
          if ("POST".equalsIgnoreCase(request.getMethod())) {
            response.sendRedirect("/GET/reply");
          } else {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
          }
        } else {
          response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
      }
    });
    server.setHandler(debug);
    server.start();
    server.join();
  }
}
