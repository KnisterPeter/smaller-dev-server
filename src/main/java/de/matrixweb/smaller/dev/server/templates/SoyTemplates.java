package de.matrixweb.smaller.dev.server.templates;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.google.template.soy.SoyFileSet;
import com.google.template.soy.tofu.SoyTofu;

import de.matrixweb.vfs.VFS;

/**
 * @author markusw
 */
public class SoyTemplates implements TemplateEngine {

  private VFS vfs;

  private final Map<String, SoyTofu> cache = new HashMap<>();

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
  public boolean compile(final String path) throws IOException {
    final boolean handled = path.endsWith("soy");
    if (handled) {
      internalCompile(path);
    }
    return handled;
  }

  private void internalCompile(final String path) throws IOException {
    final String key = FilenameUtils.removeExtension(path);
    this.cache.put(key,
        new SoyFileSet.Builder().add(this.vfs.find(key + ".soy").getURL())
            .build().compileToTofu());
  }

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#render(java.lang.String,
   *      java.util.Map, java.util.Map)
   */
  @Override
  public String render(final String path, final Map<String, Object> config,
      final Map<String, Object> data) throws IOException {
    final String key = FilenameUtils.removeExtension(path);
    if (!this.cache.containsKey(key)) {
      internalCompile(path);
    }

    String templateName = (String) config.get("templateName");
    if (templateName == null) {
      templateName = path.substring(path.lastIndexOf('/') + 1);
    }
    templateName = templateName.replace("-", "");

    return this.cache.get(key).newRenderer(templateName).setData(data).render();
  }

}
