package de.matrixweb.smaller.dev.server.tests;

import de.matrixweb.smaller.common.SmallerException;
import de.matrixweb.smaller.dev.server.templates.Engine;

/**
 * @author markusw
 */
public enum TestFramework {

  /** */
  NONE(NoneTestRunner.class),
  /** */
  JASMINE(JasmineTestRunner.class);

  private Class<? extends TestRunner> clazz;

  /**
   * @param clazz
   */
  private TestFramework(final Class<? extends TestRunner> clazz) {
    this.clazz = clazz;
  }

  /**
   * @return Returns a new created instance of the selected {@link TestRunner}
   */
  public TestRunner create() {
    try {
      final TestRunner testRunner = this.clazz.newInstance();
      return testRunner;
    } catch (InstantiationException | IllegalAccessException e) {
      throw new SmallerException("Failed to create test runner", e);
    }
  }

  /**
   * @param name
   * @return Returns the selected {@link Engine} factory
   */
  public static TestFramework get(String name) {
    if (name == null) {
      name = "NONE";
    }
    return valueOf(name.toUpperCase());
  }

}
