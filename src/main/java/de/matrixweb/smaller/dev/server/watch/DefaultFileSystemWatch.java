package de.matrixweb.smaller.dev.server.watch;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

/**
 * @author marwol
 */
public class DefaultFileSystemWatch implements FileSystemWatch {

  private final WatchService watchService;

  /**
   * @throws IOException
   */
  public DefaultFileSystemWatch() throws IOException {
    this.watchService = FileSystems.getDefault().newWatchService();
  }

  /**
   * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch#register(java.nio.file.Path)
   */
  @Override
  public FileSystemWatchKey register(final Path path) throws IOException {
    return new DefaultWatchKey(path.register(this.watchService,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_DELETE));
  }

  /**
   * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch#take()
   */
  @Override
  public FileSystemWatchKey take() throws InterruptedException {
    try {
      return new DefaultWatchKey(this.watchService.take());
    } catch (ClosedWatchServiceException e) {
      throw new FileSystemClosedWatchServiceException();
    }
  }

  /**
   * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch#close()
   */
  @Override
  public void close() throws IOException {
    this.watchService.close();
  }

  /** */
  public static class DefaultWatchKey implements FileSystemWatchKey {

    private final WatchKey watchKey;

    /**
     * @param watchKey
     */
    public DefaultWatchKey(final WatchKey watchKey) {
      this.watchKey = watchKey;
    }

    /**
     * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch.FileSystemWatchKey#pollEvents()
     */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<FileSytemWatchEvent<?>> pollEvents() {
      return CollectionUtils.collect(this.watchKey.pollEvents(),
          new Transformer() {
            @Override
            public Object transform(final Object input) {
              return new DefaultFileSytemWatchEvent<>((WatchEvent<?>) input);
            }
          });
    }

    /**
     * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch.FileSystemWatchKey#reset()
     */
    @Override
    public boolean reset() {
      return this.watchKey.reset();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
          + (this.watchKey == null ? 0 : this.watchKey.hashCode());
      return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      DefaultWatchKey other = (DefaultWatchKey) obj;
      if (this.watchKey == null) {
        if (other.watchKey != null) {
          return false;
        }
      } else if (!this.watchKey.equals(other.watchKey)) {
        return false;
      }
      return true;
    }

  }

  /**
   * @author marwol
   * @param <T>
   */
  public static class DefaultFileSytemWatchEvent<T> implements
      FileSytemWatchEvent<T> {

    private final WatchEvent<T> watchEvent;

    /**
     * @param watchEvent
     */
    public DefaultFileSytemWatchEvent(final WatchEvent<T> watchEvent) {
      this.watchEvent = watchEvent;
    }

    /**
     * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch.FileSytemWatchEvent#kind()
     */
    @Override
    public FileSytemWatchEvent.Kind<T> kind() {
      return new DefaultKind<T>(this.watchEvent.kind());
    }

    /**
     * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch.FileSytemWatchEvent#context()
     */
    @Override
    public T context() {
      return this.watchEvent.context();
    }

    /**
     * @author marwol
     * @param <T>
     */
    public static class DefaultKind<T> implements FileSytemWatchEvent.Kind<T> {

      private final WatchEvent.Kind<T> kind;

      /**
       * @param kind
       */
      public DefaultKind(final java.nio.file.WatchEvent.Kind<T> kind) {
        this.kind = kind;
      }

      /**
       * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch.FileSytemWatchEvent.Kind#isOverflow()
       */
      @Override
      public boolean isOverflow() {
        return this.kind == StandardWatchEventKinds.OVERFLOW;
      }

      /**
       * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch.FileSytemWatchEvent.Kind#isEntryCreate()
       */
      @Override
      public boolean isEntryCreate() {
        return this.kind == StandardWatchEventKinds.ENTRY_CREATE;
      }

    }

  }

}
