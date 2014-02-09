package de.matrixweb.smaller.dev.server;

import java.io.File;
import java.io.IOException;
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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;

import de.matrixweb.smaller.common.Manifest;
import de.matrixweb.smaller.common.ProcessDescription;
import de.matrixweb.smaller.common.SmallerException;
import de.matrixweb.smaller.common.Version;
import de.matrixweb.smaller.config.DevServer;
import de.matrixweb.smaller.config.Environment;
import de.matrixweb.smaller.dev.server.templates.Engine;
import de.matrixweb.smaller.dev.server.templates.TemplateEngine;
import de.matrixweb.smaller.dev.server.tests.TestFramework;
import de.matrixweb.smaller.dev.server.tests.TestRunner;
import de.matrixweb.smaller.pipeline.Pipeline;
import de.matrixweb.smaller.resource.ProcessorFactory;
import de.matrixweb.smaller.resource.ResourceResolver;
import de.matrixweb.smaller.resource.VFSResourceResolver;
import de.matrixweb.smaller.resource.impl.JavaEEProcessorFactory;
import de.matrixweb.vfs.VFS;
import de.matrixweb.vfs.VFSUtils;
import de.matrixweb.vfs.VFile;
import de.matrixweb.vfs.scanner.ResourceScanner;
import de.matrixweb.vfs.scanner.VFSResourceLister;
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

  private DevServer devServer;

  private final Environment env;

  private ProcessorFactory processorFactory;

  private final VFS vfs;

  private ResourceResolver resolver;

  private final Manifest manifest;

  private ProcessDescription processDescription;

  private Pipeline pipeline;

  private final ResourceWatchdog resourceWatchdog;

  private final TemplateEngine templateEngine;

  private TestRunner testRunner;

  private final Map<String, Map<String, Object>> configCache = new HashMap<>();

  private ResourceScanner resourceScanner;

  /**
   * @param devServer
   * @param env
   * @param manifest
   * @throws IOException
   */
  public SmallerResourceHandler(final DevServer devServer,
      final Environment env, final Manifest manifest) throws IOException {
    try {
      this.devServer = devServer;
      this.env = env;
      this.manifest = manifest;
      this.vfs = new VFS();
      this.resourceWatchdog = new ResourceWatchdog(this, env);
      prepareVfs();
      if (env.getProcess() != null) {
        this.processorFactory = new JavaEEProcessorFactory();
        this.resolver = new VFSResourceResolver(this.vfs);
        this.pipeline = new Pipeline(this.processorFactory);
        setupTasks(manifest);
      }
      this.templateEngine = Engine.get(env.getTemplateEngine())
          .create(this.vfs);
      this.testRunner = TestFramework.get(env.getTestFramework()).create();

      smallerResources(null);
    } catch (IOException | RuntimeException e) {
      dispose();
      throw e;
    }
  }

  private void setupTasks(final Manifest manifest) {
    for (final ProcessDescription processDescription : manifest
        .getProcessDescriptions()) {
      if (processDescription.getOutputFile() != null
          && processDescription.getOutputFile()
              .equals(this.env.getProcess()[0])) {
        this.processDescription = processDescription;
      }
    }
  }

  private final void prepareVfs() throws IOException {
    final List<WrappedSystem> mergedRoot = new ArrayList<>();
    for (final String root : this.env.getFiles().getFolder()) {
      LOGGER.debug("Added document-root: {}", root);
      mergedRoot.add(new JavaFile(new File(root)));
    }
    this.vfs.mount(this.vfs.find("/"), new MergingVFS(mergedRoot));

    this.resourceScanner = new ResourceScanner(new VFSResourceLister(this.vfs),
        this.env.getFiles().getIncludes(), this.env.getFiles().getExcludes());
  }

  void smallerResources(final List<String> changedResources) {
    if (changedResources != null) {
      changedResources.retainAll(this.resourceScanner.getResources());
    }
    if (changedResources == null || !changedResources.isEmpty()) {
      LOGGER.info("Changed resources: {}", changedResources);

      final PushInfo pushInfo = new PushInfo();
      @SuppressWarnings("unchecked")
      List<String> remaining = changedResources != null ? new ArrayList<>(
          changedResources) : Collections.EMPTY_LIST;
      if (remaining.size() > 0) {
        remaining = cleanupConfigCache(changedResources, pushInfo);
      }
      if (remaining.size() > 0) {
        remaining = recompileTemplates(remaining, pushInfo);
      }
      if (changedResources == null || remaining.size() > 0) {
        recompileTask(changedResources, pushInfo);
      }
      LOGGER.debug(
          "PushInfo: {}, {}, {}",
          new Object[] { pushInfo.isFullReload(), pushInfo.getJs(),
              pushInfo.getCss() });
      LiveReloadSocket.broadcastReload(pushInfo);
    }
  }

  private List<String> cleanupConfigCache(final List<String> changedResources,
      final PushInfo pushInfo) {
    final List<String> remaining = new ArrayList<>(changedResources);
    final Iterator<String> it = remaining.iterator();
    while (it.hasNext()) {
      final String change = it.next();
      final String key = change.substring(0,
          change.length() - ".cfg.json".length());
      if (this.configCache.containsKey(key)) {
        pushInfo.setFullReload(true);
        this.configCache.remove(key);
        it.remove();
      }
    }
    return remaining;
  }

  private final List<String> recompileTemplates(
      final List<String> changedResources, final PushInfo pushInfo) {
    final List<String> remaining = new ArrayList<>(changedResources);
    final Iterator<String> it = remaining.iterator();
    while (it.hasNext()) {
      final String path = it.next();
      try {
        if (this.templateEngine.compile(path)) {
          pushInfo.setFullReload(true);
          it.remove();
        }
      } catch (final IOException e) {
        pushInfo.addMessage("Failed to compile template '" + path + "'");
        LOGGER.warn("Failed to compile template: " + path, e);
      }
    }
    return remaining;
  }

  private void recompileTask(final List<String> changedResources,
      final PushInfo pushInfo) {
    if (this.processDescription != null) {
      try {
        final long start = System.currentTimeMillis();

        executeSmaller(changedResources, this.processDescription);

        final String out = this.processDescription.getOutputFile();
        if (start < this.vfs.find(out).getLastModified()) {
          if (out.endsWith(".js")) {
            pushInfo.setJs(out);
          } else if (out.endsWith(".css")) {
            pushInfo.setCss(out);
          }
        }
      } catch (IOException | SmallerException e) {
        final StringBuilder sb = new StringBuilder();
        for (final Throwable t : ExceptionUtils.getThrowables(e)) {
          sb.append(": ").append(t.getMessage());
        }
        pushInfo.addMessage("Failed to compile resources: " + sb.substring(2));
        LOGGER.error("Failed to process resources", e);
      }
    }
  }

  private void executeSmaller(final List<String> changedResources,
      final ProcessDescription processDescription) throws IOException {
    if (this.pipeline != null) {
      this.pipeline.execute(Version.getCurrentVersion(), this.vfs,
          this.resolver, this.manifest, processDescription);

      // TODO: Add test run
    }
  }

  @Deprecated
  private void executeSmallerTask(final List<String> changedResources) {
    if (this.env.getTestFiles() != null) {
      final VFS testVfs = new VFS();
      try {
        final List<WrappedSystem> mounts = new ArrayList<>();
        mounts.add(new WrappedVFS(this.vfs.find("/")));
        for (final String folder : this.env.getTestFiles().getFolder()) {
          mounts.add(new JavaFile(new File(folder)));
        }
        testVfs.mount(testVfs.find("/"),
            new MergingVFS(mounts.toArray(new WrappedSystem[mounts.size()])));

        this.testRunner.run(testVfs);
      } catch (final IOException e) {
        LOGGER.error("Failed to execute tests", e);
      } finally {
        testVfs.dispose();
      }
    }
  }

  /**
   * @param out
   * @param response
   * @param uri
   * @throws IOException
   */
  public void process(final OutputStream out,
      final HttpServletResponse response, final String uri) throws IOException {
    LOGGER.debug("Reply with smaller-resource at '{}'", uri);
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
   * @param file
   * @return Returns true if the given file path is in this resource handlers
   *         file set
   */
  public boolean hasFile(final String file) {
    return this.resourceScanner.getResources().contains(file);
  }

  /**
   * @return the templateEngine
   */
  TemplateEngine getTemplateEngine() {
    return this.templateEngine;
  }

  /**
   * @param out
   * @param request
   * @param response
   * @param uri
   * @param liveReloadCode
   * @throws IOException
   */
  public void renderTemplate(final OutputStream out,
      final HttpServletRequest request, final HttpServletResponse response,
      final String uri, final String liveReloadCode) throws IOException {
    String path = uri;
    if ("/".equals(path)) {
      path += "index.html";
    }
    final Map<String, Object> data = loadRequestConfiguration(path, request);
    renderTemplate(out, response, path, data, liveReloadCode);
  }

  /**
   * @param out
   * @param response
   * @param uri
   * @param data
   * @param liveReloadCode
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public void renderTemplate(final OutputStream out,
      final HttpServletResponse response, final String uri,
      final Map<String, Object> data, final String liveReloadCode)
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
      if (liveReloadCode != null && this.devServer.isLiveReload()) {
        writer.write(liveReloadCode);
      }
    }
    writer.flush();
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

  /** */
  public class PushInfo {

    private List<String> messages = Collections.emptyList();

    private boolean fullReload = false;

    private String js = null;

    private String css = null;

    /**
     * @param message
     */
    public void addMessage(final String message) {
      if (this.messages.isEmpty()) {
        this.messages = new ArrayList<>();
      }
      this.messages.add(message);
    }

    /**
     * @return the messages
     */
    public List<String> getMessages() {
      return this.messages;
    }

    /**
     * @param fullReload
     *          the fullReload to set
     */
    public void setFullReload(final boolean fullReload) {
      this.fullReload = fullReload;
    }

    /**
     * @return True if a full page reload is required
     */
    public boolean isFullReload() {
      return this.messages.isEmpty()
          && (this.fullReload || this.js != null && this.css != null || this.js != null
              && SmallerResourceHandler.this.devServer.isForceFullReload());
    }

    /**
     * @return the js
     */
    public String getJs() {
      return this.js;
    }

    /**
     * @param js
     *          the js to set
     */
    public void setJs(final String js) {
      this.js = js;
    }

    /**
     * @return the css
     */
    public String getCss() {
      return this.css;
    }

    /**
     * @param css
     *          the css to set
     */
    public void setCss(final String css) {
      this.css = css;
    }

  }

}
