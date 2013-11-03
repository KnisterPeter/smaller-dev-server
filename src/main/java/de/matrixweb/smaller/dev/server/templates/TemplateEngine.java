package de.matrixweb.smaller.dev.server.templates;

import java.io.IOException;
import java.util.Map;

import de.matrixweb.vfs.VFS;

/**
 * @author markusw
 */
public interface TemplateEngine {

  /**
   * @param vfs
   */
  void setVfs(final VFS vfs);

  /**
   * @param path
   * @return Returns true if this {@link TemplateEngine} compiled the given
   *         resource, false otherwise
   * @throws IOException
   *           Throw if the compiler fails
   */
  boolean compile(String path) throws IOException;

  /**
   * @param path
   *          The path to the template
   * @param config
   *          The template configuration
   * @param data
   *          The data to render into the template
   * @return Returns the rendered template as {@link String}
   * @throws IOException
   */
  String render(String path, Map<String, Object> config,
      Map<String, Object> data) throws IOException;

}
