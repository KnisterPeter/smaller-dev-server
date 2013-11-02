package de.matrixweb.smaller.dev.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.base.Joiner;

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
import de.matrixweb.smaller.resource.vfs.VFile;
import de.matrixweb.smaller.resource.vfs.wrapped.JavaFile;
import de.matrixweb.smaller.resource.vfs.wrapped.MergingVFS;
import de.matrixweb.smaller.resource.vfs.wrapped.WrappedSystem;

/**
 * @author markusw
 */
public class SmallerResourceHandler {

  private final Config config;

  private ProcessorFactory processorFactory;

  private final VFS vfs;

  private ResourceResolver resolver;

  private Task task;

  private Pipeline pipeline;

  private final ResourceWatchdog resourceWatchdog;

  private final TemplateEngine templateEngine;

  /**
   * @param config
   * @throws IOException
   */
  public SmallerResourceHandler(final Config config) throws IOException {
    this.config = config;
    this.vfs = new VFS();
    this.resourceWatchdog = new ResourceWatchdog(this, config);
    prepareVfs();
    if (config.getProcess() != null) {
      this.processorFactory = new JavaEEProcessorFactory();
      this.resolver = new VFSResourceResolver(this.vfs);
      this.pipeline = new Pipeline(this.processorFactory);
      this.task = new Task(this.config.getProcessors(), StringUtils.join(
          this.config.getIn(), ','), StringUtils.join(this.config.getProcess(),
          ','));
    }
    this.templateEngine = Engine.get(this.config.getTemplateEngine()).create(
        this.vfs);

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
    if (this.task != null) {
      this.pipeline.execute(Version.getCurrentVersion(), this.vfs,
          this.resolver, this.task);
    }
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
   * @param request
   * @param response
   * @param uri
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public void renderTemplate(final HttpServletRequest request,
      final HttpServletResponse response, final String uri) throws IOException {
    String path = uri;
    if ("/".equals(path)) {
      path += "index.html";
    }
    final PrintWriter writer = response.getWriter();
    final Map<String, Object> data = readJsonData(request);
    if (data.containsKey("jsonResponse")) {
      response.setContentType("application/json");
      writer.write(new ObjectMapper().writeValueAsString(data
          .get("jsonResponse")));
    } else {
      if (data.containsKey("templatePath")) {
        path = data.get("templatePath").toString();
      }
      writer.write(this.templateEngine.render(path, data,
          (Map<String, Object>) data.get("templateData")));
    }
    writer.close();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> readJsonData(final HttpServletRequest request)
      throws IOException {
    Map<String, Object> o = null;
    final String uri = request.getRequestURI();
    final VFile file = this.vfs.find(uri + ".cfg.js");
    if (file.exists()) {
      final Map<String, Object> data = new ObjectMapper().readValue(
          file.getURL(), Map.class);
      o = (Map<String, Object>) data.get(createRequestParamKey(request));
    }
    if (o == null) {
      o = new HashMap<>();
    }
    return o;
  }

  private String createRequestParamKey(final HttpServletRequest request)
      throws IOException {
    final List<String> parts = new ArrayList<>();

    final List<String> names = new ArrayList<>(request.getParameterMap()
        .keySet());
    Collections.sort(names);
    for (final String name : names) {
      final String[] values = request.getParameterValues(name);
      if (values.length > 1) {
        Arrays.sort(values);
        for (final String value : values) {
          parts.add(name + "=" + value);
        }
      } else {
        parts.add(name + "=" + values[0]);
      }
    }

    return Joiner.on('&').join(parts);
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
