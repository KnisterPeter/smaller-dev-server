package de.matrixweb.smaller.dev.server.tests;

import de.matrixweb.smaller.common.SmallerException;
import de.matrixweb.vfs.VFS;

/**
 * @author markusw
 */
public interface TestRunner {

  /**
   * @param vfs
   * @throws SmallerException
   */
  void run(VFS vfs) throws SmallerException;

  /**
   * 
   */
  void dispose();

}
