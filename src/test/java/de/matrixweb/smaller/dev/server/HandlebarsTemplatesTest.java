package de.matrixweb.smaller.dev.server;

import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

/**
 * @author markusw
 */
public class HandlebarsTemplatesTest extends AbstractDevServerTest {

  /**
   * @see de.matrixweb.smaller.dev.server.AbstractDevServerTest#getServerArgs()
   */
  @Override
  protected String getServerArgs() {
    return "--proxyhost localhost --proxyport 3000 -d src/test/resources/tests -t handlebars";
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRenderIndex() throws Exception {
    assertThat(
        IOUtils.toString(new URL("http://localhost:12345/sub/test.txt")),
        is("\nHBS\n"));
  }

}
