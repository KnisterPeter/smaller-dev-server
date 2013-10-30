package de.matrixweb.smaller.dev.server.templates;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.URLTemplateLoader;

import de.matrixweb.smaller.resource.vfs.VFS;

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
    return this.handlebars.compile(FilenameUtils.removeExtension(path)).apply(
        null);
  }

}
