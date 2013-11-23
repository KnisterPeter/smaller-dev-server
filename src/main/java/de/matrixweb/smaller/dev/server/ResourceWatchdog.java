package de.matrixweb.smaller.dev.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author markusw
 */
public class ResourceWatchdog {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ResourceWatchdog.class);

  private final SmallerResourceHandler resourceHandler;

  private final Config config;

  private final WatchService watchService;

  private final Map<WatchKey, Path> watches;

  private boolean runWatchdog = true;

  private final Runnable watchdog = new Runnable() {

    @Override
    public void run() {
      ResourceWatchdog.this.run();
    }
  };

  /**
   * @param resourceHandler
   * @param config
   * @throws IOException
   */
  public ResourceWatchdog(final SmallerResourceHandler resourceHandler,
      final Config config) throws IOException {
    this.resourceHandler = resourceHandler;
    this.config = config;
    this.watchService = createWatchService();
    this.watches = new HashMap<WatchKey, Path>();
    for (final File root : config.getDocumentRoots()) {
      LOGGER.debug("Watching {}", root);
      watchRecursive(Paths.get(root.getPath()));
    }
    if (config.getTestFolder() != null) {
      LOGGER.debug("Watching {}", config.getTestFolder());
      watchRecursive(Paths.get(config.getTestFolder().getPath()));
    }
    final Thread thread = new Thread(this.watchdog, "Smaller Resource Watchdog");
    thread.setDaemon(true);
    thread.start();
  }

  private WatchService createWatchService() throws IOException {
    final String osName = System.getProperty("os.name");
    if (osName.startsWith("Mac OS X") || osName.startsWith("Darwin")) {
      return new MacWatchService();
    }
    return FileSystems.getDefault().newWatchService();
  }

  private void watchRecursive(final Path dir) throws IOException {
    Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(final Path dir,
          final BasicFileAttributes attrs) throws IOException {
        final WatchKey watchKey = dir.register(
            ResourceWatchdog.this.watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE);
        ResourceWatchdog.this.watches.put(watchKey, dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  @SuppressWarnings("unchecked")
  private <T> WatchEvent<T> cast(final WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  private void run() {
    while (this.runWatchdog) {
      WatchKey key;
      try {
        key = this.watchService.take();
      } catch (final ClosedWatchServiceException e) {
        this.runWatchdog = false;
        continue;
      } catch (final InterruptedException e) {
        continue;
      }
      final List<String> changedResources = loopEvents(key,
          this.watches.get(key));
      final boolean valid = key.reset();
      if (!valid) {
        this.watches.remove(key);
      }
      if (changedResources.size() > 0) {
        this.resourceHandler.smallerResources(changedResources);
      }
    }
  }

  private List<String> loopEvents(final WatchKey key, final Path path) {
    final List<String> changedResources = new ArrayList<>();

    for (final WatchEvent<?> event : key.pollEvents()) {
      final WatchEvent.Kind<?> kind = event.kind();
      if (kind == StandardWatchEventKinds.OVERFLOW) {
        continue;
      }
      final WatchEvent<Path> ev = cast(event);
      final Path child = path.resolve(ev.context());
      LOGGER.debug("WatchEvent for {}", child);
      findResourceRoot(changedResources, child);
      watchNewDirectories(kind, child);
    }

    return changedResources;
  }

  private void findResourceRoot(final List<String> changedResources,
      final Path child) {
    for (final File root : this.config.getDocumentRoots()) {
      if (child.startsWith(root.getPath())) {
        changedResources.add(child.toFile().getPath()
            .substring(root.getPath().length()));
      }
    }
    if (this.config.getTestFolder() != null) {
      if (child.startsWith(this.config.getTestFolder().getPath())) {
        changedResources.add(child.toFile().getPath()
            .substring(this.config.getTestFolder().getPath().length()));
      }
    }
  }

  private void watchNewDirectories(final WatchEvent.Kind<?> kind,
      final Path child) {
    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
      try {
        if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
          watchRecursive(child);
        }
      } catch (final IOException x) {
        // Ignore this one
      }
    }
  }

  void stop() throws IOException {
    this.runWatchdog = false;
    this.watchService.close();
  }

  private static class MacWatchService implements WatchService {

    private final com.barbarysoftware.watchservice.WatchService ws = com.barbarysoftware.watchservice.WatchService
        .newWatchService();

    /**
     * @see java.nio.file.WatchService#poll()
     */
    @Override
    public WatchKey poll() {
      return new MacWatchKey(this.ws.poll());
    }

    /**
     * @see java.nio.file.WatchService#poll(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public WatchKey poll(final long timeout, final TimeUnit unit)
        throws InterruptedException {
      return new MacWatchKey(this.ws.poll(timeout, unit));
    }

    /**
     * @see java.nio.file.WatchService#take()
     */
    @Override
    public WatchKey take() throws InterruptedException {
      return new MacWatchKey(this.ws.take());
    }

    /**
     * @see java.nio.file.WatchService#close()
     */
    @Override
    public void close() throws IOException {
      this.ws.close();
    }

    private class MacWatchKey implements WatchKey {

      private final com.barbarysoftware.watchservice.WatchKey key;

      /**
       * @param key
       */
      public MacWatchKey(final com.barbarysoftware.watchservice.WatchKey key) {
        this.key = key;
      }

      /**
       * @see java.nio.file.WatchKey#watchable()
       */
      @Override
      public Watchable watchable() {
        throw new UnsupportedOperationException();
      }

      @SuppressWarnings("unchecked")
      @Override
      public List<WatchEvent<?>> pollEvents() {
        final List<com.barbarysoftware.watchservice.WatchEvent<?>> events = this.key
            .pollEvents();
        return ListUtils.transformedList(events, new Transformer() {
          @Override
          public Object transform(final Object input) {
            return new MacWatchEvent<>(
                (com.barbarysoftware.watchservice.WatchEvent<?>) input);
          }
        });
      }

      /**
       * @see java.nio.file.WatchKey#isValid()
       */
      @Override
      public boolean isValid() {
        return this.key.isValid();
      }

      /**
       * @see java.nio.file.WatchKey#cancel()
       */
      @Override
      public void cancel() {
        this.key.cancel();
      }

      /**
       * @see java.nio.file.WatchKey#reset()
       */
      @Override
      public boolean reset() {
        return this.key.reset();
      }

    }

    private class MacWatchEvent<TE> implements WatchEvent<TE> {

      private final com.barbarysoftware.watchservice.WatchEvent<TE> event;

      /**
       * @param event
       */
      public MacWatchEvent(
          final com.barbarysoftware.watchservice.WatchEvent<TE> event) {
        this.event = event;
      }

      /**
       * @see java.nio.file.WatchEvent#kind()
       */
      @Override
      public Kind<TE> kind() {
        return new MacKind<>(this.event.kind());
      }

      /**
       * @see java.nio.file.WatchEvent#count()
       */
      @Override
      public int count() {
        return this.event.count();
      }

      /**
       * @see java.nio.file.WatchEvent#context()
       */
      @Override
      public TE context() {
        return this.event.context();
      }

      private class MacKind<TK> implements Kind<TK> {

        private final com.barbarysoftware.watchservice.WatchEvent.Kind<TK> kind;

        /**
         * @param kind
         */
        public MacKind(
            final com.barbarysoftware.watchservice.WatchEvent.Kind<TK> kind) {
          this.kind = kind;
        }

        /**
         * @see java.nio.file.WatchEvent.Kind#name()
         */
        @Override
        public String name() {
          return this.kind.name();
        }

        /**
         * @see java.nio.file.WatchEvent.Kind#type()
         */
        @Override
        public Class<TK> type() {
          return this.kind.type();
        }

      }

    }

  }

}
