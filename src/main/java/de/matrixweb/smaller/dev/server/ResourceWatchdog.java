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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

/**
 * @author markusw
 */
public class ResourceWatchdog {

  private final SmallerResourceHandler resourceHandler;

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
    this.watchService = FileSystems.getDefault().newWatchService();
    this.watches = new HashMap<WatchKey, Path>();
    for (final File root : config.getDocumentRoots()) {
      watchRecursive(Paths.get(root.getPath()));
    }
    final Thread thread = new Thread(this.watchdog);
    thread.setDaemon(true);
    thread.start();
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
      boolean requireProcessResources = false;
      final Path path = this.watches.get(key);
      for (final WatchEvent<?> event : key.pollEvents()) {
        final WatchEvent.Kind<?> kind = event.kind();
        if (kind == StandardWatchEventKinds.OVERFLOW) {
          continue;
        }
        requireProcessResources = true;
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
          final WatchEvent<Path> ev = cast(event);
          final Path child = path.resolve(ev.context());
          try {
            if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
              watchRecursive(child);
            }
          } catch (final IOException x) {
            // Ignore this one
          }
        }
      }
      final boolean valid = key.reset();
      if (!valid) {
        this.watches.remove(key);
      }
      if (requireProcessResources) {
        this.resourceHandler.smallerResources();
      }
    }
  }

  void stop() throws IOException {
    this.runWatchdog = false;
    this.watchService.close();
  }

}
