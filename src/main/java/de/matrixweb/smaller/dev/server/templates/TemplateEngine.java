package de.matrixweb.smaller.dev.server.templates;

import java.io.IOException;

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
   * @return Returns the rendered template as {@link String}
   * @throws IOException
   */
  String render(String path) throws IOException;

}
