package de.matrixweb.smaller.dev.server.watch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import com.barbarysoftware.watchservice.ClosedWatchServiceException;
import com.barbarysoftware.watchservice.StandardWatchEventKind;
import com.barbarysoftware.watchservice.WatchEvent;
import com.barbarysoftware.watchservice.WatchKey;
import com.barbarysoftware.watchservice.WatchService;
import com.barbarysoftware.watchservice.WatchableFile;

import de.matrixweb.smaller.dev.server.Config;

/**
 * @author marwol
 */
public class MacOsFileSystemWatch implements FileSystemWatch {

  private Config config;
  
  private final WatchService watchService;

  private Map<Path, Long> lru = new LinkedHashMap<Path, Long>(5, .75F, true) {
    private static final long serialVersionUID = 7304218013110419912L;
    protected boolean removeEldestEntry(Map.Entry<Path, Long> eldest) {
      return size() > 5;
    }
  };

  private Map<FileSystemWatchKey, Path> watches;

  /**
   * 
   */
  public MacOsFileSystemWatch(Config config, final Map<FileSystemWatchKey, Path> watches) {
    this.config = config;
    this.watches = watches;
    watchService = WatchService.newWatchService();
  }

  /**
   * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch#register(java.nio.file.Path)
   */
  @Override
  public void register(final Path path) throws IOException {
    watches.put(new MacOsWatchKey(new WatchableFile(path.toFile()).register(
        this.watchService, StandardWatchEventKind.OVERFLOW,
        StandardWatchEventKind.ENTRY_CREATE,
        StandardWatchEventKind.ENTRY_MODIFY,
        StandardWatchEventKind.ENTRY_DELETE)), path);
  }

  /**
   * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch#take()
   */
  @Override
  public FileSystemWatchKey take() throws InterruptedException {
    try {
      MacOsWatchKey key = new MacOsWatchKey(this.watchService.take());
      Path path = watches.get(key);
      if (path != null && lru.containsKey(path)) {
        long lastUpdate = lru.get(key);
        if (lastUpdate + config.getWatchThreshold() > System.currentTimeMillis()) {
          key.reset();
          return take();
        }
      }
      lru.put(path, System.currentTimeMillis());
      return key;
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
  public static class MacOsWatchKey implements FileSystemWatchKey {

    private final WatchKey watchKey;

    /**
     * @param watchKey
     */
    public MacOsWatchKey(final WatchKey watchKey) {
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
              return new MacOsFileSytemWatchEvent<>((WatchEvent<?>) input);
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
      MacOsWatchKey other = (MacOsWatchKey) obj;
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
   * @param <T>
   */
  public static class MacOsFileSytemWatchEvent<T> implements
      FileSytemWatchEvent<T> {

    private final WatchEvent<T> watchEvent;

    /**
     * @param watchEvent
     */
    public MacOsFileSytemWatchEvent(final WatchEvent<T> watchEvent) {
      this.watchEvent = watchEvent;
    }

    /**
     * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch.FileSytemWatchEvent#kind()
     */
    @Override
    public FileSytemWatchEvent.Kind<T> kind() {
      return new MacOsKind<T>(this.watchEvent.kind());
    }

    /**
     * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch.FileSytemWatchEvent#context()
     */
    @Override
    @SuppressWarnings("unchecked")
    public T context() {
      Path path = ((File) this.watchEvent.context()).toPath();
      return (T) path;
    }

    /**
     * @param <T>
     */
    public static class MacOsKind<T> implements FileSytemWatchEvent.Kind<T> {

      private final WatchEvent.Kind<T> kind;

      /**
       * @param kind
       */
      public MacOsKind(
          final com.barbarysoftware.watchservice.WatchEvent.Kind<T> kind) {
        this.kind = kind;
      }

      /**
       * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch.FileSytemWatchEvent.Kind#isOverflow()
       */
      @Override
      public boolean isOverflow() {
        return this.kind == StandardWatchEventKind.OVERFLOW;
      }

      /**
       * @see de.matrixweb.smaller.dev.server.watch.FileSystemWatch.FileSytemWatchEvent.Kind#isEntryCreate()
       */
      @Override
      public boolean isEntryCreate() {
        return this.kind == StandardWatchEventKind.ENTRY_CREATE;
      }

    }

  }

}
