package de.matrixweb.smaller.dev.server.templates;

import java.io.IOException;
import java.util.Map;

import de.matrixweb.smaller.resource.vfs.VFS;

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
   *          The path to the template
   * @param data
   *          The data to render into the template
   * @return Returns the rendered template as {@link String}
   * @throws IOException
   */
  String render(String path, Map<String, Object> data) throws IOException;

}
