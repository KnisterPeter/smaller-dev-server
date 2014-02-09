package de.matrixweb.smaller.dev.server.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.matrixweb.nodejs.NodeJsExecutor;
import de.matrixweb.smaller.common.SmallerException;
import de.matrixweb.vfs.VFS;
import de.matrixweb.vfs.VFSUtils;

/**
 * @author markusw
 */
public class JasmineTestRunner implements TestRunner {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(JasmineTestRunner.class);

  private NodeJsExecutor node;

  /**
   * @see de.matrixweb.smaller.dev.server.tests.TestRunner#run(de.matrixweb.vfs.VFS)
   */
  @Override
  public void run(final VFS vfs) throws SmallerException {
    LOGGER.info("Executing test");
    try {
      if (this.node == null) {
        this.node = new NodeJsExecutor();
        this.node.setModule(getClass(), "minijasminenode-0.2.4",
            "tests/jasmine.js");
      }
      final long start = System.currentTimeMillis();

      // vfs.compact();
      FileUtils.writeStringToFile(new File("/tmp/mod1.coffee"),
          VFSUtils.readToString(vfs.find("/mod1.coffee")));
      FileUtils.writeStringToFile(new File("/tmp/mod1.js"),
          VFSUtils.readToString(vfs.find("/mod1.js")));

      final Map<String, Object> options = new HashMap<>();
      final String resultFile = this.node.run(vfs, null, options);
      final long end = System.currentTimeMillis();
      handleResponse(VFSUtils.readToString(vfs.find('/' + resultFile)), start,
          end);
    } catch (final IOException e) {
      throw new SmallerException("Failed to execute test runner", e);
    }
  }

  @SuppressWarnings("unchecked")
  private void handleResponse(final String response, final long start,
      final long end) throws IOException {
    System.err.println(response);
    final Map<String, Object> result = new ObjectMapper().readValue(response,
        Map.class);
    System.out.println("Success: " + result.get("result"));
    System.out.println("Duration: " + (end - start) / 1000f + "s");
    System.out.println("Test-Duration: "
        + Long.parseLong(result.get("duration").toString()) / 1000f + "s");
  }

  /**
   * @see de.matrixweb.smaller.dev.server.tests.TestRunner#dispose()
   */
  @Override
  public void dispose() {
    if (this.node != null) {
      this.node.dispose();
    }
  }

}
