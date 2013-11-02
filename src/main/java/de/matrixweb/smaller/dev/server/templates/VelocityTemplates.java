package de.matrixweb.smaller.dev.server.templates;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.URLResourceLoader;

import de.matrixweb.smaller.resource.vfs.VFS;

/**
 * @author markusw
 */
public class VelocityTemplates implements TemplateEngine {

  private VFS vfs;

  private VelocityEngine engine;

  private final Map<String, Template> cache = new HashMap<>();

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#setVfs(de.matrixweb.smaller.resource.vfs.VFS)
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
    final boolean handled = path.endsWith("vm");
    if (handled) {
      internalCompile(path);
    }
    return handled;
  }

  private void internalCompile(final String path) throws IOException {
    if (this.engine == null) {
      setupVelocityEngine();
    }

    final String key = FilenameUtils.removeExtension(path);
    this.cache.put(key,
        this.engine.getTemplate(this.vfs.find(key + ".vm").getPath()));
  }

  private void setupVelocityEngine() throws IOException {
    final Properties props = new Properties();
    props.setProperty("resource.loader", "url");
    props.setProperty("url.resource.loader.class",
        URLResourceLoader.class.getName());
    props.setProperty("url.resource.loader.root", this.vfs.find("/").getURL()
        .toExternalForm());
    props.setProperty("url.resource.loader.cache", "false");

    this.engine = new VelocityEngine();
    this.engine.init(props);
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

    final StringWriter writer = new StringWriter();
    final VelocityContext context = new VelocityContext();
    if (data != null) {
      for (final Entry<String, Object> entry : data.entrySet()) {
        context.put(entry.getKey(), entry.getValue());
      }
    }
    this.cache.get(key).merge(context, writer);
    return writer.toString();
  }

}
