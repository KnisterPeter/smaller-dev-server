package de.matrixweb.smaller.dev.server.templates;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.URLTemplateLoader;

import de.matrixweb.vfs.VFS;

/**
 * @author markusw
 */
public class HandlebarsTemplates implements TemplateEngine {

  private final Handlebars handlebars = new Handlebars()
      .with(new URLTemplateLoader() {
        @Override
        protected URL getResource(final String location) throws IOException {
          return HandlebarsTemplates.this.vfs.find(location).getURL();
        }
      });

  private VFS vfs;

  private final Map<String, Template> cache = new HashMap<>();

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#setVfs(de.matrixweb.vfs.VFS)
   */
  @Override
  public void setVfs(final VFS vfs) {
    this.vfs = vfs;
  }

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#getTemplateUri(java.lang.String)
   */
  @Override
  public String getTemplateUri(final String uri) {
    return FilenameUtils.removeExtension(uri) + ".hbs";
  }

  /**
   * @see de.matrixweb.smaller.dev.server.templates.TemplateEngine#compile(java.lang.String)
   */
  @Override
  public boolean compile(final String path) throws IOException {
    final boolean handled = path.endsWith("hbs");
    if (handled) {
      internalCompile(path);
    }
    return handled;
  }

  private void internalCompile(final String path) throws IOException {
    final String key = FilenameUtils.removeExtension(path);
    this.cache.put(key, this.handlebars.compile(key));
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
    return this.cache.get(key).apply(data);
  }

}
