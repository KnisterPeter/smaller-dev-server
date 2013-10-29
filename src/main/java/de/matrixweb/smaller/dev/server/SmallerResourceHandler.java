package de.matrixweb.smaller.dev.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import de.matrixweb.smaller.common.Task;
import de.matrixweb.smaller.common.Version;
import de.matrixweb.smaller.dev.server.templates.Engine;
import de.matrixweb.smaller.dev.server.templates.TemplateEngine;
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

  private final ResourceWatchdog resourceWatchdog;

  private final TemplateEngine templateEngine;

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
    this.resourceWatchdog = new ResourceWatchdog(this, config);
    prepareVfs();
    this.task = new Task(this.config.getProcessors(), StringUtils.join(
        this.config.getIn(), ','), StringUtils.join(this.config.getProcess(),
        ','));
    this.templateEngine = Engine.get(this.config.getTemplateEngine()).create();
    this.templateEngine.setVfs(this.vfs);

    smallerResources();
  }

  private final void prepareVfs() throws IOException {
    final List<WrappedSystem> mergedRoot = new ArrayList<>();
    for (final File root : this.config.getDocumentRoots()) {
      mergedRoot.add(new JavaFile(root));
    }
    this.vfs.mount(this.vfs.find("/"), new MergingVFS(mergedRoot));
  }

  void smallerResources() {
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
   * @param response
   * @param uri
   * @throws IOException
   */
  public void renderTemplate(final HttpServletResponse response,
      final String uri) throws IOException {
    String path = uri;
    if ("/".equals(path)) {
      path += "index.html";
    }
    final PrintWriter writer = response.getWriter();
    writer.write(this.templateEngine.render(path));
    writer.close();
  }

  /**
   * @throws IOException
   */
  public void dispose() throws IOException {
    this.resourceWatchdog.stop();
    if (this.processorFactory != null) {
      this.processorFactory.dispose();
    }
    if (this.vfs != null) {
      this.vfs.dispose();
    }
  }

}
