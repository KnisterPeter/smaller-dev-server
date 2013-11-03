package de.matrixweb.smaller.dev.server.templates;

import de.matrixweb.smaller.common.SmallerException;
import de.matrixweb.vfs.VFS;

/**
 * @author markusw
 */
public enum Engine {

  /** */
  NULL(NullTemplates.class),
  /** */
  RAW(RawTemplates.class),
  /** */
  SOY(SoyTemplates.class),
  /** */
  VELOCITY(VelocityTemplates.class),
  /** */
  HANDLEBARS(HandlebarsTemplates.class);

  private Class<? extends TemplateEngine> clazz;

  /**
   * 
   */
  private Engine(final Class<? extends TemplateEngine> clazz) {
    this.clazz = clazz;
  }

  /**
   * @param vfs
   * @return Returns a new crated instance of the selected
   *         {@link TemplateEngine}
   */
  public TemplateEngine create(final VFS vfs) {
    try {
      final TemplateEngine templateEngine = this.clazz.newInstance();
      templateEngine.setVfs(vfs);
      return templateEngine;
    } catch (InstantiationException | IllegalAccessException e) {
      throw new SmallerException("Failed to create template engine", e);
    }
  }

  /**
   * @param name
   * @return Returns the selected {@link Engine} factory
   */
  public static Engine get(String name) {
    if (name == null) {
      name = "NULL";
    }
    return valueOf(name.toUpperCase());
  }

}
