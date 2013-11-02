package de.matrixweb.smaller.dev.server;

import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

/**
 * @author markusw
 */
public class VelocityTemplatesTest extends AbstractDevServerTest {

  /**
   * @see de.matrixweb.smaller.dev.server.AbstractDevServerTest#getServerArgs()
   */
  @Override
  protected String getServerArgs() {
    return "--proxyhost localhost --proxyport 3000 -d src/test/resources/test1 -t velocity";
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRenderIndex() throws Exception {
    assertThat(
        IOUtils.toString(new URL("http://localhost:12345/sub/test.txt")),
        is("VM"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRenderWithData() throws Exception {
    assertThat(
        IOUtils.toString(new URL("http://localhost:12345/sub/test-data.txt")),
        is("    a\n    b\n    c\n"));
  }

}
