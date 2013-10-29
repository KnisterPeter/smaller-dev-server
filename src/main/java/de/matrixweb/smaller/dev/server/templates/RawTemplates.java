package de.matrixweb.smaller.dev.server.templates;

import java.io.IOException;

import de.matrixweb.smaller.resource.vfs.VFS;
import de.matrixweb.smaller.resource.vfs.VFSUtils;

/**
 * @author markusw
 */
public class RawTemplates implements TemplateEngine {

  private VFS vfs;

  /**
   * @param vfs
   *          the vfs to set
   */
  public void setVfs(final VFS vfs) {
    this.vfs = vfs;
  }

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#render(java.lang.String)
   */
  @Override
  public String render(final String path) throws IOException {
    return VFSUtils.readToString(this.vfs.find(path));
  }

}
