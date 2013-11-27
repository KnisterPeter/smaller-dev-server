package de.matrixweb.smaller.dev.server;

import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

/**
 * @author markusw
 */
public class SoyTemplatesTest extends AbstractDevServerTest {

  /**
   * @see de.matrixweb.smaller.dev.server.AbstractDevServerTest#getServerArgs()
   */
  @Override
  protected String getServerArgs() {
    return "@src/test/resources/test1/soy.yml";
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRenderIndex() throws Exception {
    assertThat(
        IOUtils.toString(new URL("http://localhost:12345/sub/test.txt")),
        is("SOY"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRenderWithData() throws Exception {
    assertThat(
        IOUtils.toString(new URL("http://localhost:12345/sub/test-data.txt")),
        is("abc"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRenderOtherTemplate() throws Exception {
    assertThat(IOUtils.toString(new URL(
        "http://localhost:12345/sub/test-data.txt?template=other")),
        is("OTHER SOY"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRenderSameTemplate() throws Exception {
    assertThat(IOUtils.toString(new URL(
        "http://localhost:12345/sub/test-data.txt?template=same")),
        is("SAME SOY"));
  }

}
