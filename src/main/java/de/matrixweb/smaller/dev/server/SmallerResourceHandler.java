package de.matrixweb.smaller.dev.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import de.matrixweb.smaller.common.Task;
import de.matrixweb.smaller.common.Version;
import de.matrixweb.smaller.pipeline.Pipeline;
import de.matrixweb.smaller.resource.ProcessorFactory;
import de.matrixweb.smaller.resource.ResourceResolver;
import de.matrixweb.smaller.resource.impl.JavaEEProcessorFactory;
import de.matrixweb.smaller.resource.vfs.VFS;
import de.matrixweb.smaller.resource.vfs.VFSResourceResolver;
import de.matrixweb.smaller.resource.vfs.VFSUtils;
import de.matrixweb.smaller.resource.vfs.wrapped.JavaFile;
import de.matrixweb.smaller.resource.vfs.wrapped.MergingVFS;
import de.matrixweb.smaller.resource.vfs.wrapped.WrappedSystem;

/**
 * @author markusw
 */
public class SmallerResourceHandler {

  private final Config config;

  private final ProcessorFactory processorFactory;

  private final VFS vfs;

  private final ResourceResolver resolver;

  private final Task task;

  private final Pipeline pipeline;

  private WatchService watchService;

  private Map<WatchKey, Path> watches;

  private boolean runWatchdog = true;

  private final Runnable watchdog = new Runnable() {

    @SuppressWarnings("unchecked")
    private <T> WatchEvent<T> cast(final WatchEvent<?> event) {
      return (WatchEvent<T>) event;
    }

    @Override
    public void run() {
      while (SmallerResourceHandler.this.runWatchdog) {
        WatchKey key;
        try {
          key = SmallerResourceHandler.this.watchService.take();
        } catch (final InterruptedException e) {
          continue;
        }
        boolean requireProcessResources = false;
        final Path path = SmallerResourceHandler.this.watches.get(key);
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
          SmallerResourceHandler.this.watches.remove(key);
        }
        if (requireProcessResources) {
          smallerResources();
        }
      }
    }
  };

  /**
   * @param config
   * @throws IOException
   */
  public SmallerResourceHandler(final Config config) throws IOException {
    this.config = config;
    this.processorFactory = new JavaEEProcessorFactory();
    this.vfs = new VFS();
    this.resolver = new VFSResourceResolver(this.vfs);
    this.pipeline = new Pipeline(this.processorFactory);

    prepareFileWatches();
    prepareVfs();
    this.task = new Task(this.config.getProcessors(), StringUtils.join(
        this.config.getIn(), ','), StringUtils.join(this.config.getProcess(),
        ','));
    smallerResources();
  }

  private final void prepareFileWatches() throws IOException {
    this.watchService = FileSystems.getDefault().newWatchService();
    this.watches = new HashMap<WatchKey, Path>();
    for (final File root : this.config.getDocumentRoots()) {
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
            SmallerResourceHandler.this.watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE);
        SmallerResourceHandler.this.watches.put(watchKey, dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private final void prepareVfs() throws IOException {
    final List<WrappedSystem> mergedRoot = new ArrayList<>();
    for (final File root : this.config.getDocumentRoots()) {
      mergedRoot.add(new JavaFile(root));
    }
    this.vfs.mount(this.vfs.find("/"), new MergingVFS(mergedRoot));
  }

  private void smallerResources() {
    this.pipeline.execute(Version.getCurrentVersion(), this.vfs, this.resolver,
        this.task);
  }

  /**
   * @param response
   * @param uri
   * @throws IOException
   */
  public void process(final HttpServletResponse response, final String uri)
      throws IOException {
    final PrintWriter writer = response.getWriter();
    writer.write(VFSUtils.readToString(this.vfs.find(uri)));
    writer.close();
  }

  /**
   * @throws IOException
   */
  public void dispose() throws IOException {
    this.runWatchdog = false;
    this.watchService.close();
    if (this.processorFactory != null) {
      this.processorFactory.dispose();
    }
    if (this.vfs != null) {
      this.vfs.dispose();
    }
  }

}
