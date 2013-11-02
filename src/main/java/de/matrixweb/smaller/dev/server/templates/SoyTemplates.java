package de.matrixweb.smaller.dev.server.templates;

import java.io.IOException;
import java.util.Map;

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
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#render(java.lang.String,
   *      java.util.Map, java.util.Map)
   */
  @Override
  public String render(final String path, final Map<String, Object> config,
      final Map<String, Object> data) throws IOException {
    final VFile file = this.vfs.find(FilenameUtils.removeExtension(path)
        + ".soy");

    final SoyFileSet sfs = new SoyFileSet.Builder().add(
        VFSUtils.readToString(file), file.getPath()).build();
    final SoyTofu tofu = sfs.compileToTofu();

    String templateName = (String) config.get("templateName");
    if (templateName == null) {
      templateName = path.substring(path.lastIndexOf('/') + 1);
    }
    templateName = templateName.replace("-", "");

    return tofu.newRenderer(templateName).setData(data).render();
  }

}
