package de.matrixweb.smaller.dev.server.templates;

import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import com.google.template.soy.SoyFileSet;
import com.google.template.soy.tofu.SoyTofu;

import de.matrixweb.smaller.resource.vfs.VFS;
import de.matrixweb.smaller.resource.vfs.VFSUtils;
import de.matrixweb.smaller.resource.vfs.VFile;

/**
 * @author markusw
 */
public class SoyTemplates implements TemplateEngine {

  private VFS vfs;

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#setVfs(de.matrixweb.smaller.resource.vfs.VFS)
   */
  @Override
  public void setVfs(final VFS vfs) {
    this.vfs = vfs;
  }

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#render(java.lang.String)
   */
  @Override
  public String render(final String path) throws IOException {
    final VFile file = this.vfs.find(FilenameUtils.removeExtension(path)
        + ".soy");
    final SoyFileSet sfs = new SoyFileSet.Builder().add(
        VFSUtils.readToString(file), file.getPath()).build();
    // TODO: Cache this, but change on file change
    final SoyTofu tofu = sfs.compileToTofu();
    // TODO: Make this configurable
    return tofu.newRenderer("namespace.template").render();
  }

}
