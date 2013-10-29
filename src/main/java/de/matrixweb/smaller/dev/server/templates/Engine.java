package de.matrixweb.smaller.dev.server.templates;

import de.matrixweb.smaller.common.SmallerException;

/**
 * @author markusw
 */
public enum Engine {

  /** */
  NULL(NullTemplates.class),
  /** */
  RAW(RawTemplates.class);

  private Class<? extends TemplateEngine> clazz;

  /**
   * 
   */
  private Engine(final Class<? extends TemplateEngine> clazz) {
    this.clazz = clazz;
  }

  /**
   * @return
   */
  public TemplateEngine create() {
    try {
      return this.clazz.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new SmallerException("Failed to create template engine", e);
    }
  }

  /**
   * @param name
   * @return
   */
  public static Engine get(String name) {
    if (name == null) {
      name = "NULL";
    }
    return valueOf(name.toUpperCase());
  }

}
