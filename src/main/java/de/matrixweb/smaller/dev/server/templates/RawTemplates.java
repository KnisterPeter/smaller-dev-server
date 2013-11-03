package de.matrixweb.smaller.dev.server.templates;

import java.io.IOException;
import java.util.Map;

import de.matrixweb.vfs.VFS;
import de.matrixweb.vfs.VFSUtils;

/**
 * @author markusw
 */
public class RawTemplates implements TemplateEngine {

  private VFS vfs;

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#setVfs(de.matrixweb.vfs.VFS)
   */
  @Override
  public void setVfs(final VFS vfs) {
    this.vfs = vfs;
  }

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#compile(java.lang.String)
   */
  @Override
  public boolean compile(final String path) {
    // We could not decice here if this is a smaller-resource or a template
    // resource
    return false;
  }

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#render(java.lang.String,
   *      java.util.Map, java.util.Map)
   */
  @Override
  public String render(final String path, final Map<String, Object> config,
      final Map<String, Object> data) throws IOException {
    return VFSUtils.readToString(this.vfs.find(path));
  }

}
