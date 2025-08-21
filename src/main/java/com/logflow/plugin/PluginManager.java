package com.logflow.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 插件管理器
 * 负责插件的发现、加载、管理和生命周期控制
 */
public class PluginManager {

    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);
    private static final String DEFAULT_PLUGIN_DIR = "plugins";
    private static final String PLUGIN_SERVICE_FILE = "META-INF/services/" + DataSourcePlugin.class.getName();

    private final Map<String, PluginInfo> registeredPlugins = new ConcurrentHashMap<>();
    private final Map<String, DataSourcePlugin> loadedPlugins = new ConcurrentHashMap<>();
    private final Map<String, URLClassLoader> pluginClassLoaders = new ConcurrentHashMap<>();
    private final Set<String> pluginDirectories = new HashSet<>();
    private final PluginSecurityValidator securityValidator = new PluginSecurityValidator();

    private Map<String, Object> globalConfig = new HashMap<>();
    private volatile boolean initialized = false;
    private boolean securityEnabled = true;

    private static volatile PluginManager instance;

    private PluginManager() {
        // 添加默认插件目录
        pluginDirectories.add(DEFAULT_PLUGIN_DIR);
    }

    /**
     * 获取插件管理器单例实例
     */
    public static PluginManager getInstance() {
        if (instance == null) {
            synchronized (PluginManager.class) {
                if (instance == null) {
                    instance = new PluginManager();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化插件管理器
     */
    public synchronized void initialize() {
        initialize(Collections.emptyMap());
    }

    /**
     * 初始化插件管理器
     * 
     * @param globalConfig 全局配置
     */
    public synchronized void initialize(Map<String, Object> globalConfig) {
        if (initialized) {
            logger.warn("插件管理器已经初始化");
            return;
        }

        logger.info("正在初始化插件管理器...");
        this.globalConfig = new HashMap<>(globalConfig);

        // 加载内置插件
        loadBuiltinPlugins();

        // 扫描插件目录
        scanPluginDirectories();

        initialized = true;
        logger.info("插件管理器初始化完成，共加载 {} 个插件", loadedPlugins.size());
    }

    /**
     * 添加插件搜索目录
     */
    public void addPluginDirectory(String directory) {
        pluginDirectories.add(directory);
        if (initialized) {
            // 如果已经初始化，则立即扫描新目录
            scanPluginDirectory(directory);
        }
    }

    /**
     * 从JAR文件加载插件
     */
    public void loadPluginFromJar(String jarFilePath) throws PluginException {
        File jarFile = new File(jarFilePath);
        if (!jarFile.exists() || !jarFile.isFile()) {
            throw new PluginException("JAR文件不存在: " + jarFilePath);
        }

        try {
            logger.info("正在加载插件JAR: {}", jarFilePath);

            // 创建类加载器
            URL jarUrl = jarFile.toURI().toURL();
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[] { jarUrl },
                    Thread.currentThread().getContextClassLoader());

            // 发现并加载插件
            List<DataSourcePlugin> plugins = discoverPluginsInJar(jarFile, classLoader);

            for (DataSourcePlugin plugin : plugins) {
                registerPlugin(plugin, classLoader, jarFilePath);
            }

            logger.info("成功从JAR加载 {} 个插件: {}", plugins.size(), jarFilePath);

        } catch (Exception e) {
            throw new PluginException("加载插件JAR失败: " + jarFilePath, e);
        }
    }

    /**
     * 注册插件
     */
    public void registerPlugin(DataSourcePlugin plugin) throws PluginException {
        registerPlugin(plugin, null, null);
    }

    /**
     * 注册插件（内部方法）
     */
    private void registerPlugin(DataSourcePlugin plugin, URLClassLoader classLoader, String jarPath)
            throws PluginException {
        String pluginId = plugin.getPluginId();

        if (pluginId == null || pluginId.trim().isEmpty()) {
            throw new PluginException("插件ID不能为空");
        }

        // 安全验证
        if (securityEnabled) {
            PluginSecurityValidator.PluginSecurityResult securityResult = securityValidator.validatePlugin(plugin,
                    jarPath);

            if (securityResult.getSecurityLevel() == PluginSecurityValidator.SecurityLevel.CRITICAL) {
                throw new PluginException(pluginId, "插件安全验证失败，存在严重安全风险",
                        new SecurityException(securityResult.toString()));
            }

            if (securityResult.hasRisk()) {
                logger.warn("插件 {} 存在安全风险: {}", pluginId, securityResult);
            }
        }

        if (registeredPlugins.containsKey(pluginId)) {
            logger.warn("插件 {} 已经注册，将覆盖现有插件", pluginId);
            unregisterPlugin(pluginId);
        }

        try {
            // 初始化插件
            plugin.initialize(globalConfig);

            // 创建插件信息
            PluginInfo pluginInfo = new PluginInfo(
                    plugin.getPluginId(),
                    plugin.getPluginName(),
                    plugin.getVersion(),
                    plugin.getDescription(),
                    plugin.getAuthor(),
                    jarPath,
                    System.currentTimeMillis());

            // 注册插件
            registeredPlugins.put(pluginId, pluginInfo);
            loadedPlugins.put(pluginId, plugin);

            if (classLoader != null) {
                pluginClassLoaders.put(pluginId, classLoader);
            }

            logger.info("成功注册插件: {} v{} by {}",
                    plugin.getPluginName(), plugin.getVersion(), plugin.getAuthor());

        } catch (Exception e) {
            throw new PluginException(pluginId, "插件初始化失败", e);
        }
    }

    /**
     * 注销插件
     */
    public void unregisterPlugin(String pluginId) {
        DataSourcePlugin plugin = loadedPlugins.remove(pluginId);
        if (plugin != null) {
            try {
                plugin.destroy();
                logger.info("插件 {} 已注销", pluginId);
            } catch (Exception e) {
                logger.warn("插件 {} 注销时出现错误", pluginId, e);
            }
        }

        registeredPlugins.remove(pluginId);

        URLClassLoader classLoader = pluginClassLoaders.remove(pluginId);
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                logger.warn("关闭插件类加载器失败: {}", pluginId, e);
            }
        }
    }

    /**
     * 获取插件
     */
    public DataSourcePlugin getPlugin(String pluginId) {
        return loadedPlugins.get(pluginId);
    }

    /**
     * 获取所有已注册的插件ID
     */
    public Set<String> getPluginIds() {
        return new HashSet<>(registeredPlugins.keySet());
    }

    /**
     * 获取所有插件信息
     */
    public Collection<PluginInfo> getPluginInfos() {
        return new ArrayList<>(registeredPlugins.values());
    }

    /**
     * 获取插件信息
     */
    public PluginInfo getPluginInfo(String pluginId) {
        return registeredPlugins.get(pluginId);
    }

    /**
     * 检查插件是否存在
     */
    public boolean hasPlugin(String pluginId) {
        return loadedPlugins.containsKey(pluginId);
    }

    /**
     * 创建数据源连接
     */
    public DataSourceConnection createConnection(String pluginId, Map<String, Object> config,
            com.logflow.core.WorkflowContext context) throws PluginException {
        DataSourcePlugin plugin = getPlugin(pluginId);
        if (plugin == null) {
            throw PluginException.pluginNotFound(pluginId);
        }

        // 验证配置
        PluginValidationResult validation = plugin.validateConfiguration(config);
        if (!validation.isValid()) {
            throw PluginException.invalidConfig(pluginId, validation.getErrorSummary());
        }

        return plugin.createConnection(config, context);
    }

    /**
     * 测试插件连接
     */
    public PluginTestResult testConnection(String pluginId, Map<String, Object> config) {
        try {
            DataSourcePlugin plugin = getPlugin(pluginId);
            if (plugin == null) {
                return PluginTestResult.failure("插件不存在: " + pluginId);
            }

            return plugin.testConnection(config);

        } catch (Exception e) {
            return PluginTestResult.failure("测试连接失败", e);
        }
    }

    /**
     * 重新加载所有插件
     */
    public void reloadAllPlugins() {
        logger.info("正在重新加载所有插件...");

        // 注销所有插件
        Set<String> pluginIds = new HashSet<>(loadedPlugins.keySet());
        for (String pluginId : pluginIds) {
            unregisterPlugin(pluginId);
        }

        // 重新初始化
        initialized = false;
        initialize(globalConfig);
    }

    /**
     * 销毁插件管理器
     */
    public synchronized void destroy() {
        if (!initialized) {
            return;
        }

        logger.info("正在销毁插件管理器...");

        // 注销所有插件
        Set<String> pluginIds = new HashSet<>(loadedPlugins.keySet());
        for (String pluginId : pluginIds) {
            unregisterPlugin(pluginId);
        }

        initialized = false;
        logger.info("插件管理器已销毁");
    }

    // 私有方法

    /**
     * 加载内置插件
     */
    private void loadBuiltinPlugins() {
        logger.info("正在加载内置插件...");

        ServiceLoader<DataSourcePlugin> serviceLoader = ServiceLoader.load(DataSourcePlugin.class);
        int count = 0;

        for (DataSourcePlugin plugin : serviceLoader) {
            try {
                registerPlugin(plugin);
                count++;
            } catch (Exception e) {
                logger.error("加载内置插件失败: {}", plugin.getClass().getName(), e);
            }
        }

        logger.info("成功加载 {} 个内置插件", count);
    }

    /**
     * 扫描插件目录
     */
    private void scanPluginDirectories() {
        for (String directory : pluginDirectories) {
            scanPluginDirectory(directory);
        }
    }

    /**
     * 扫描单个插件目录
     */
    private void scanPluginDirectory(String directory) {
        Path pluginDir = Paths.get(directory);
        if (!Files.exists(pluginDir) || !Files.isDirectory(pluginDir)) {
            logger.debug("插件目录不存在，跳过: {}", directory);
            return;
        }

        logger.info("正在扫描插件目录: {}", directory);

        try {
            Files.list(pluginDir)
                    .filter(path -> path.toString().toLowerCase().endsWith(".jar"))
                    .forEach(jarPath -> {
                        try {
                            loadPluginFromJar(jarPath.toString());
                        } catch (Exception e) {
                            logger.error("加载插件JAR失败: {}", jarPath, e);
                        }
                    });
        } catch (IOException e) {
            logger.error("扫描插件目录失败: {}", directory, e);
        }
    }

    /**
     * 在JAR文件中发现插件
     */
    private List<DataSourcePlugin> discoverPluginsInJar(File jarFile, URLClassLoader classLoader) throws Exception {
        List<DataSourcePlugin> plugins = new ArrayList<>();

        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry serviceEntry = jar.getJarEntry(PLUGIN_SERVICE_FILE);
            if (serviceEntry == null) {
                logger.debug("JAR文件中没有找到插件服务文件: {}", jarFile.getName());
                return plugins;
            }

            // 读取服务文件
            List<String> pluginClassNames = Files.readAllLines(
                    Paths.get(jarFile.toURI()).resolve(PLUGIN_SERVICE_FILE));

            for (String className : pluginClassNames) {
                className = className.trim();
                if (className.isEmpty() || className.startsWith("#")) {
                    continue;
                }

                try {
                    Class<?> pluginClass = classLoader.loadClass(className);
                    if (DataSourcePlugin.class.isAssignableFrom(pluginClass)) {
                        DataSourcePlugin plugin = (DataSourcePlugin) pluginClass.getDeclaredConstructor().newInstance();
                        plugins.add(plugin);
                        logger.debug("发现插件: {} 在 {}", className, jarFile.getName());
                    }
                } catch (Exception e) {
                    logger.error("加载插件类失败: {} 在 {}", className, jarFile.getName(), e);
                }
            }
        }

        return plugins;
    }

    // 安全管理方法

    /**
     * 启用或禁用安全验证
     */
    public void setSecurityEnabled(boolean enabled) {
        this.securityEnabled = enabled;
        logger.info("插件安全验证已{}启用", enabled ? "" : "禁");
    }

    /**
     * 检查安全验证是否启用
     */
    public boolean isSecurityEnabled() {
        return securityEnabled;
    }

    /**
     * 启用严格安全模式
     */
    public void enableStrictSecurityMode() {
        securityValidator.enableStrictMode();
        logger.info("已启用严格安全模式");
    }

    /**
     * 添加受信任的插件作者
     */
    public void addTrustedAuthor(String author) {
        securityValidator.addTrustedAuthor(author);
    }

    /**
     * 验证JAR文件安全性
     */
    public PluginSecurityValidator.PluginSecurityResult validateJarSecurity(String jarPath) {
        return securityValidator.validateJarFile(jarPath);
    }

    /**
     * 验证已加载插件的安全性
     */
    public PluginSecurityValidator.PluginSecurityResult validatePluginSecurity(String pluginId) {
        DataSourcePlugin plugin = loadedPlugins.get(pluginId);
        if (plugin == null) {
            PluginSecurityValidator.PluginSecurityResult result = new PluginSecurityValidator.PluginSecurityResult(
                    pluginId);
            result.addCriticalIssue("插件不存在");
            result.determineSecurityLevel();
            return result;
        }

        PluginInfo pluginInfo = registeredPlugins.get(pluginId);
        String jarPath = pluginInfo != null ? pluginInfo.getJarPath() : null;

        return securityValidator.validatePlugin(plugin, jarPath);
    }

    /**
     * 获取所有插件的安全状态报告
     */
    public Map<String, PluginSecurityValidator.PluginSecurityResult> getSecurityReport() {
        Map<String, PluginSecurityValidator.PluginSecurityResult> report = new HashMap<>();

        for (String pluginId : loadedPlugins.keySet()) {
            report.put(pluginId, validatePluginSecurity(pluginId));
        }

        return report;
    }

    /**
     * 插件信息类
     */
    public static class PluginInfo {
        private final String id;
        private final String name;
        private final String version;
        private final String description;
        private final String author;
        private final String jarPath;
        private final long loadTime;

        public PluginInfo(String id, String name, String version, String description,
                String author, String jarPath, long loadTime) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.description = description;
            this.author = author;
            this.jarPath = jarPath;
            this.loadTime = loadTime;
        }

        // Getters
        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }

        public String getAuthor() {
            return author;
        }

        public String getJarPath() {
            return jarPath;
        }

        public long getLoadTime() {
            return loadTime;
        }

        @Override
        public String toString() {
            return String.format("PluginInfo{id='%s', name='%s', version='%s'}", id, name, version);
        }
    }
}
