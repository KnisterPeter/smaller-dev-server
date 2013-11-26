package de.matrixweb.smaller.dev.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import de.matrixweb.smaller.common.SmallerException;
import de.matrixweb.smaller.common.Task;
import de.matrixweb.smaller.common.Version;
import de.matrixweb.smaller.dev.server.templates.Engine;
import de.matrixweb.smaller.dev.server.templates.TemplateEngine;
import de.matrixweb.smaller.dev.server.tests.TestFramework;
import de.matrixweb.smaller.dev.server.tests.TestRunner;
import de.matrixweb.smaller.pipeline.Pipeline;
import de.matrixweb.smaller.resource.ProcessorFactory;
import de.matrixweb.smaller.resource.ResourceResolver;
import de.matrixweb.smaller.resource.VFSResourceResolver;
import de.matrixweb.smaller.resource.impl.JavaEEProcessorFactory;
import de.matrixweb.vfs.ResourceScanner.VFSResourceLister;
import de.matrixweb.vfs.VFS;
import de.matrixweb.vfs.VFSUtils;
import de.matrixweb.vfs.VFile;
import de.matrixweb.vfs.wrapped.JavaFile;
import de.matrixweb.vfs.wrapped.MergingVFS;
import de.matrixweb.vfs.wrapped.WrappedSystem;
import de.matrixweb.vfs.wrapped.WrappedVFS;

/**
 * @author markusw
 */
public class SmallerResourceHandler {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SmallerResourceHandler.class);

  private final Config config;

  private ProcessorFactory processorFactory;

  private final VFS vfs;

  private ResourceResolver resolver;

  private Task task;

  private Task jsTask;

  private Task cssTask;

  private Pipeline pipeline;

  private final ResourceWatchdog resourceWatchdog;

  private final TemplateEngine templateEngine;

  private String liveReloadClient = null;

  private TestRunner testRunner;

  private final Map<String, Map<String, Object>> configCache = new HashMap<>();

  /**
   * @param config
   * @throws IOException
   */
  public SmallerResourceHandler(final Config config) throws IOException {
    try {
      this.config = config;
      this.vfs = new VFS();
      this.resourceWatchdog = new ResourceWatchdog(this, config);
      prepareVfs();
      if (config.getProcess() != null) {
        this.processorFactory = new JavaEEProcessorFactory();
        this.resolver = new VFSResourceResolver(this.vfs);
        this.pipeline = new Pipeline(this.processorFactory);
        setupTasks(config);
      }
      this.templateEngine = Engine.get(this.config.getTemplateEngine()).create(
          this.vfs);
      this.testRunner = TestFramework.get(config.getTestFramework()).create();

      smallerResources(null);
    } catch (IOException | RuntimeException e) {
      dispose();
      throw e;
    }
  }

  private void setupTasks(final Config config) {
    if (this.config.getProcessors() != null) {
      this.task = new Task(this.config.getProcessors(), StringUtils.join(
          this.config.getIn(), ','), StringUtils.join(this.config.getProcess(),
          ','));
      this.task
          .setOptionsDefinition("global:source-maps=true;coffeeScript:bare=true");
    }
    if (config.getJsProcessors() != null) {
      this.jsTask = new Task(this.config.getJsProcessors(), StringUtils.join(
          this.config.getIn(), ','), StringUtils.join(this.config.getProcess(),
          ','));
      this.jsTask
          .setOptionsDefinition("global:source-maps=true;coffeeScript:bare=true");
    }
    if (config.getCssProcessors() != null) {
      this.cssTask = new Task(this.config.getCssProcessors(), StringUtils.join(
          this.config.getIn(), ','), StringUtils.join(this.config.getProcess(),
          ','));
      this.cssTask
          .setOptionsDefinition("global:source-maps=true;coffeeScript:bare=true");
    }
  }

  private final void prepareVfs() throws IOException {
    final List<WrappedSystem> mergedRoot = new ArrayList<>();
    for (final File root : this.config.getDocumentRoots()) {
      mergedRoot.add(new JavaFile(root));
    }
    this.vfs.mount(this.vfs.find("/"), new MergingVFS(mergedRoot));
  }

  void smallerResources(final List<String> changedResources) {
    SmallerResourceHandler.LOGGER.debug("Changed resources: {}",
        changedResources);

    final MutableBoolean fullReload = new MutableBoolean(
        this.config.isForceFullReload());
    String jsReload = null;
    String cssReload = null;
    @SuppressWarnings("unchecked")
    List<String> remaining = changedResources != null ? new ArrayList<>(
        changedResources) : Collections.EMPTY_LIST;
    if (remaining.size() > 0) {
      remaining = cleanupConfigCache(changedResources, fullReload);
    }
    if (remaining.size() > 0) {
      remaining = recompileTemplates(remaining, fullReload);
    }
    if (changedResources == null || remaining.size() > 0) {
      if (this.task != null || this.jsTask != null || this.cssTask != null) {
        try {
          if (this.task != null) {
            executeSmallerTask(changedResources, this.task, true);
            fullReload.setValue(true);
          } else {
            // TODO: Check if changed resources are within the task to execute
            if (this.jsTask != null) {
              executeSmallerTask(changedResources, this.jsTask, true);
              jsReload = getProcessByExtension(".js");
            }
            if (this.cssTask != null) {
              executeSmallerTask(changedResources, this.cssTask, false);
              cssReload = getProcessByExtension(".css");
            }
          }
        } catch (final SmallerException e) {
          SmallerResourceHandler.LOGGER.error("Failed to process resources", e);
        }
      }
    }
    LiveReloadSocket.broadcastReload(fullReload.booleanValue(), jsReload,
        cssReload);
  }

  private void executeSmallerTask(final List<String> changedResources,
      final Task task, final boolean runTests) {
    VFSResourceLister lister = new VFSResourceLister(this.vfs);
    // config.getJsProcessors();
    // config.getCssProcessors();

    // TODO: Check if test-resources was changed
    // => rebuild test resources
    // => rerun tests
    // otherwise:
    // => rerun whole stack
    this.pipeline.execute(Version.getCurrentVersion(), this.vfs, this.resolver,
        task);

    if (runTests && this.config.getTestFolder() != null) {
      final VFS testVfs = new VFS();
      try {
        testVfs.mount(testVfs.find("/"),
            new MergingVFS(new WrappedVFS(this.vfs.find("/")), new JavaFile(
                this.config.getTestFolder())));
        this.testRunner.run(testVfs);
      } catch (final IOException e) {
        SmallerResourceHandler.LOGGER.error("Failed to execute tests", e);
      } finally {
        testVfs.dispose();
      }
    }
  }

  private String getProcessByExtension(final String extension) {
    for (String process : this.config.getProcess()) {
      if (process.endsWith(extension)) {
        return process;
      }
    }
    return null;
  }

  private List<String> cleanupConfigCache(final List<String> changedResources,
      final MutableBoolean reload) {
    final List<String> remaining = new ArrayList<>(changedResources);
    final Iterator<String> it = remaining.iterator();
    while (it.hasNext()) {
      final String change = it.next();
      final String key = change.substring(0,
          change.length() - ".cfg.json".length());
      if (this.configCache.containsKey(key)) {
        reload.setValue(true);
        this.configCache.remove(key);
        it.remove();
      }
    }
    return remaining;
  }

  private final List<String> recompileTemplates(
      final List<String> changedResources, final MutableBoolean reload) {
    final List<String> remaining = new ArrayList<>(changedResources);
    final Iterator<String> it = remaining.iterator();
    while (it.hasNext()) {
      final String path = it.next();
      try {
        if (this.templateEngine.compile(path)) {
          reload.setValue(true);
          it.remove();
        }
      } catch (final IOException e) {
        SmallerResourceHandler.LOGGER.warn("Failed to compile template: "
            + path, e);
      }
    }
    return remaining;
  }

  /**
   * @param out
   * @param response
   * @param uri
   * @throws IOException
   */
  public void process(final OutputStream out,
      final HttpServletResponse response, final String uri) throws IOException {
    if (uri.endsWith("js")) {
      response.setContentType("application/javascript");
    } else if (uri.endsWith("css")) {
      response.setContentType("text/css");
    }
    response.addHeader("Pragma", "no-cache");
    final PrintWriter writer = new PrintWriter(out);
    writer.write(VFSUtils.readToString(this.vfs.find(uri)));
    writer.flush();
  }

  /**
   * @param out
   * @param request
   * @param response
   * @param uri
   * @throws IOException
   */
  public void renderTemplate(final OutputStream out,
      final HttpServletRequest request, final HttpServletResponse response,
      final String uri) throws IOException {
    String path = uri;
    if ("/".equals(path)) {
      path += "index.html";
    }
    final Map<String, Object> data = loadRequestConfiguration(path, request);
    renderTemplate(out, request, response, path, data, false);
  }

  /**
   * @param out
   * @param request
   * @param response
   * @param uri
   * @param data
   * @param partial
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public void renderTemplate(final OutputStream out,
      final HttpServletRequest request, final HttpServletResponse response,
      final String uri, final Map<String, Object> data, final boolean partial)
      throws IOException {
    String path = uri;
    final PrintWriter writer = new PrintWriter(out);
    if (data.containsKey("jsonResponse")) {
      if (response != null) {
        response.setContentType("application/json");
      }
      writer.write(new ObjectMapper().writeValueAsString(data
          .get("jsonResponse")));
    } else {
      String contentType = "text/html";
      if (data.containsKey("contentType")) {
        contentType = data.get("contentType").toString();
      }
      if (response != null) {
        response.addHeader("Content-Type", contentType);
      }
      if (data.containsKey("templatePath")) {
        path = data.get("templatePath").toString();
      }
      writer.write(this.templateEngine.render(path, data,
          (Map<String, Object>) data.get("templateData")));
      if (!partial && this.config.isLiveReload()) {
        writer.write(getLiveReloadClient());
      }
    }
    writer.flush();
  }

  /**
   * @return Returns the live-reload client code
   * @throws IOException
   */
  public String getLiveReloadClient() throws IOException {
    if (this.liveReloadClient == null) {
      final StringBuilder sb = new StringBuilder();
      sb.append("<script text=\"javascript\">");
      final InputStream in = getClass().getResourceAsStream("/live-reload.js");
      try {
        sb.append(IOUtils.toString(in));
      } finally {
        in.close();
      }
      sb.append("</script>");
      this.liveReloadClient = sb.toString();
    }
    return this.liveReloadClient;
  }

  /**
   * @param path
   *          The uri to load a configuration for
   * @param request
   *          The incomming http request
   * @return Returns a configuration map for the given parameters
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> loadRequestConfiguration(final String path,
      final HttpServletRequest request) throws IOException {
    Map<String, Object> o = this.configCache.get(path);
    if (o == null) {
      final VFile file = this.vfs.find(path + ".cfg.json");
      if (file.exists()) {
        o = new ObjectMapper().readValue(file.getURL(), Map.class);
        final Map<String, Object> target = new HashMap<>();
        for (final String params : o.keySet()) {
          target.put(createRequestParamKey(buildRequestParameterMap(params)),
              o.get(params));
        }
        o = target;
        this.configCache.put(path, o);
      }
    }
    if (o != null) {
      o = (Map<String, Object>) o.get(createRequestParamKey(request
          .getParameterMap()));
    }
    if (o == null) {
      o = new HashMap<>();
    }
    return o;
  }

  private Map<String, String[]> buildRequestParameterMap(final String params) {
    final Map<String, List<String>> temp = new HashMap<>();
    for (final String param : params.split("&")) {
      if (!"".equals(param)) {
        final String[] parts = param.split("=", 2);
        if (!temp.containsKey(parts[0])) {
          temp.put(parts[0], new ArrayList<String>());
        }
        temp.get(parts[0]).add(parts[1]);
      }
    }
    final Map<String, String[]> parameters = new HashMap<>();
    for (final Entry<String, List<String>> entry : temp.entrySet()) {
      parameters.put(entry.getKey(),
          entry.getValue().toArray(new String[entry.getValue().size()]));
    }
    return parameters;
  }

  private String createRequestParamKey(final Map<String, String[]> parameters)
      throws IOException {
    final List<String> parts = new ArrayList<>();

    final List<String> names = new ArrayList<>(parameters.keySet());
    Collections.sort(names);
    for (final String name : names) {
      final String[] values = parameters.get(name);
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
    if (this.resourceWatchdog != null) {
      this.resourceWatchdog.stop();
    }
    if (this.processorFactory != null) {
      this.processorFactory.dispose();
    }
    if (this.vfs != null) {
      this.vfs.dispose();
    }
    if (this.testRunner != null) {
      this.testRunner.dispose();
    }
  }

}
