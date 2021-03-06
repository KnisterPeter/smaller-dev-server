package de.matrixweb.smaller.dev.server.watch;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * @author marwol
 */
public interface FileSystemWatch {

  /**
   * @param path
   * @throws IOException
   */
  void register(Path path) throws IOException;

  /**
   * @return
   * @throws InterruptedException
   */
  FileSystemWatchKey take() throws InterruptedException;

  /**
   * @throws IOException
   */
  void close() throws IOException;

  /** */
  static interface FileSystemWatchKey {

    boolean reset();

    Collection<FileSytemWatchEvent<?>> pollEvents();

  }

  /**
   * @param <T>
   */
  static interface FileSytemWatchEvent<T> {

    Kind<T> kind();

    T context();

    static interface Kind<T> {

      boolean isOverflow();

      boolean isEntryCreate();

    }

  }

  /** */
  static class FileSystemClosedWatchServiceException extends RuntimeException {

    private static final long serialVersionUID = 7910573924225778405L;

  }

  /** */
  static class Factory {

    public static FileSystemWatch create(
        final Map<FileSystemWatchKey, Path> watches) throws IOException {
      final String osName = System.getProperty("os.name");
      if (osName.startsWith("Mac OS X") || osName.startsWith("Darwin")) {
        return new MacOsFileSystemWatch(watches);
      }
      return new DefaultFileSystemWatch(watches);
    }

  }

}
