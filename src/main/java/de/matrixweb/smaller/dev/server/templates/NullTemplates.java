package de.matrixweb.smaller.dev.server.templates;

import java.io.IOException;
import java.util.Map;

import de.matrixweb.vfs.VFS;

/**
 * @author markusw
 */
public class NullTemplates implements TemplateEngine {

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#setVfs(de.matrixweb.vfs.VFS)
   */
  @Override
  public void setVfs(final VFS vfs) {
  }

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#getTemplateUri(java.lang.String)
   */
  @Override
  public String getTemplateUri(final String uri) {
    return uri;
  }

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#compile(java.lang.String)
   */
  @Override
  public boolean compile(final String path) {
    return false;
  }

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#render(java.lang.String,
   *      java.util.Map, java.util.Map)
   */
  @Override
  public String render(final String path, final Map<String, Object> config,
      final Map<String, Object> data) throws IOException {
    throw new IOException("NullTemplate at " + path);
  }

}
