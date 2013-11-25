package de.matrixweb.smaller.dev.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.matrixweb.smaller.dev.server.watch.FileSystemWatch;
import de.matrixweb.smaller.dev.server.watch.FileSystemWatch.FileSystemWatchKey;
import de.matrixweb.smaller.dev.server.watch.FileSystemWatch.FileSytemWatchEvent;
import de.matrixweb.smaller.dev.server.watch.FileSystemWatch.FileSystemClosedWatchServiceException;

/**
 * @author markusw
 */
public class ResourceWatchdog {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ResourceWatchdog.class);

  private final SmallerResourceHandler resourceHandler;

  private final Config config;

  private final FileSystemWatch watcher;

  private final Map<FileSystemWatchKey, Path> watches;

  private boolean runWatchdog = true;

  private final Runnable watchdog = new Runnable() {

    @Override
    public void run() {
      ResourceWatchdog.this.run();
    }
  };
  
  private Executor executor = Executors.newFixedThreadPool(1);

  /**
   * @param resourceHandler
   * @param config
   * @throws IOException
   */
  public ResourceWatchdog(final SmallerResourceHandler resourceHandler,
      final Config config) throws IOException {
    this.resourceHandler = resourceHandler;
    this.config = config;
    this.watcher = FileSystemWatch.Factory.create(config);
    this.watches = new HashMap<FileSystemWatchKey, Path>();
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

  private void watchRecursive(final Path dir) throws IOException {
    Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(final Path dir,
          final BasicFileAttributes attrs) throws IOException {
        FileSystemWatchKey key = ResourceWatchdog.this.watcher.register(dir);
        ResourceWatchdog.this.watches.put(key, dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  @SuppressWarnings("unchecked")
  private <T> FileSytemWatchEvent<T> cast(final FileSytemWatchEvent<?> event) {
    return (FileSytemWatchEvent<T>) event;
  }

  private void run() {
    while (this.runWatchdog) {
      FileSystemWatchKey key;
      try {
        key = this.watcher.take();
        LOGGER.debug("Got watch-key: {}", key);
      } catch (final FileSystemClosedWatchServiceException e) {
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
        executor.execute(new Runnable() {
          @Override
          public void run() {
            resourceHandler.smallerResources(changedResources);
          }
        });
      }
    }
  }

  private List<String> loopEvents(final FileSystemWatchKey key, final Path path) {
    final List<String> changedResources = new ArrayList<>();

    List<Path> checked = new ArrayList<Path>();
    for (final FileSytemWatchEvent<?> event : key.pollEvents()) {
      LOGGER.debug("Polled event: {}", event);
      final FileSytemWatchEvent.Kind<?> kind = event.kind();
      if (kind.isOverflow()) {
        continue;
      }
      final FileSytemWatchEvent<Path> ev = cast(event);
      final Path child = path.resolve(ev.context());
      if (!checked.contains(child)) {
        checked.add(child);
        LOGGER.debug("WatchEvent for {}", child);
        findResourceRoot(changedResources, child);
        watchNewDirectories(kind, child);
      }
    }

    return changedResources;
  }

  private void findResourceRoot(final List<String> changedResources,
      final Path child) {
    for (File root : this.config.getDocumentRoots()) {
      try {
        root = root.getAbsoluteFile().getCanonicalFile();
      } catch (IOException e) {
        LOGGER.warn("Unable to create absolute canonical path for {}: {}",
            root, e.getMessage());
      }
      LOGGER.debug("Check root path {} and change {}", root, child);
      if (child.startsWith(root.getPath())) {
        changedResources.add(child.toFile().getPath()
            .substring(root.getPath().length()));
      }
    }
    File test = config.getTestFolder();
    if (test != null) {
      try {
        test = test.getAbsoluteFile().getCanonicalFile();
      } catch (IOException e) {
        LOGGER.warn("Unable to create absolute canonical path for {}: {}",
            test, e.getMessage());
      }
      if (child.startsWith(test.getPath())) {
        changedResources.add(child.toFile().getPath()
            .substring(test.getPath().length()));
      }
    }
  }

  private void watchNewDirectories(final FileSytemWatchEvent.Kind<?> kind,
      final Path child) {
    if (kind.isEntryCreate()) {
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
    this.watcher.close();
  }

}
