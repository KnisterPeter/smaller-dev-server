package de.matrixweb.smaller.dev.server;

import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

/**
 * @author markusw
 */
public class AjaxStubTest extends AbstractDevServerTest {

  /**
   * @see de.matrixweb.smaller.dev.server.AbstractDevServerTest#getServerArgs()
   */
  @Override
  protected String getServerArgs() {
    return "@src/test/resources/test1/ajax.yml";
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRequestJson() throws Exception {
    assertThat(
        IOUtils.toString(new URL("http://localhost:12345/sub/test.json")),
        is("{\"a\":\"b\"}"));
  }

}
