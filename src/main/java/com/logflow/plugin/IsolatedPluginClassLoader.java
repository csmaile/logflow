package com.logflow.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 隔离插件类加载器
 * 为每个插件提供独立的类加载环境，解决依赖冲突问题
 */
public class IsolatedPluginClassLoader extends URLClassLoader {

    private static final Logger logger = LoggerFactory.getLogger(IsolatedPluginClassLoader.class);

    private final String pluginId;
    private final Set<String> sharedPackages;
    private final Set<String> pluginPackages;
    private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();
    private final ClassLoader parentLoader;

    // 默认共享的包前缀（系统核心包）
    private static final Set<String> DEFAULT_SHARED_PACKAGES = Set.of(
            "java.",
            "javax.",
            "sun.",
            "com.sun.",
            "org.slf4j.",
            "org.apache.commons.logging.",
            "com.logflow.core.",
            "com.logflow.plugin.");

    // 默认插件独占的包前缀
    private static final Set<String> DEFAULT_PLUGIN_PACKAGES = Set.of(
            "com.mysql.",
            "org.postgresql.",
            "redis.clients.",
            "org.apache.kafka.",
            "com.mongodb.",
            "org.elasticsearch.",
            "com.rabbitmq.",
            "org.apache.http.",
            "com.fasterxml.jackson.",
            "com.google.gson.",
            "org.json.");

    /**
     * 构造器
     * 
     * @param pluginId 插件ID
     * @param urls     插件JAR文件URL数组
     * @param parent   父类加载器
     */
    public IsolatedPluginClassLoader(String pluginId, URL[] urls, ClassLoader parent) {
        super(urls, null); // 不使用父类加载器的默认委派
        this.pluginId = pluginId;
        this.parentLoader = parent;
        this.sharedPackages = new HashSet<>(DEFAULT_SHARED_PACKAGES);
        this.pluginPackages = new HashSet<>(DEFAULT_PLUGIN_PACKAGES);

        logger.debug("创建插件类加载器: {} with {} URLs", pluginId, urls.length);
    }

    /**
     * 添加共享包前缀
     * 共享包将从父类加载器加载，避免重复加载
     */
    public void addSharedPackage(String packagePrefix) {
        sharedPackages.add(packagePrefix);
        logger.debug("插件 {} 添加共享包: {}", pluginId, packagePrefix);
    }

    /**
     * 添加插件独占包前缀
     * 独占包将优先从插件JAR加载，实现依赖隔离
     */
    public void addPluginPackage(String packagePrefix) {
        pluginPackages.add(packagePrefix);
        logger.debug("插件 {} 添加独占包: {}", pluginId, packagePrefix);
    }

    /**
     * 获取插件ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * 自定义类加载逻辑
     * 实现依赖隔离的核心逻辑
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // 1. 检查是否已经加载
            Class<?> clazz = loadedClasses.get(name);
            if (clazz != null) {
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }

            // 2. 检查是否为共享包 - 委派给父类加载器
            if (isSharedPackage(name)) {
                try {
                    clazz = parentLoader.loadClass(name);
                    loadedClasses.put(name, clazz);
                    logger.trace("从父类加载器加载共享类: {}", name);

                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                } catch (ClassNotFoundException e) {
                    // 父类加载器找不到，继续尝试自己加载
                    logger.trace("父类加载器无法加载: {}, 尝试插件加载器", name);
                }
            }

            // 3. 尝试从插件JAR加载（插件独占包或父类加载器找不到的类）
            try {
                clazz = findClass(name);
                loadedClasses.put(name, clazz);
                logger.trace("从插件JAR加载类: {} (插件: {})", name, pluginId);

                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            } catch (ClassNotFoundException e) {
                // 插件JAR中也找不到
                logger.trace("插件JAR中未找到类: {}", name);
            }

            // 4. 最后尝试父类加载器（非共享包也可能需要父类加载器）
            if (!isSharedPackage(name)) {
                try {
                    clazz = parentLoader.loadClass(name);
                    loadedClasses.put(name, clazz);
                    logger.trace("从父类加载器加载非共享类: {}", name);

                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                } catch (ClassNotFoundException ignored) {
                    // 最终失败
                }
            }

            throw new ClassNotFoundException("无法找到类: " + name + " (插件: " + pluginId + ")");
        }
    }

    /**
     * 检查是否为共享包
     */
    private boolean isSharedPackage(String className) {
        return sharedPackages.stream().anyMatch(className::startsWith);
    }

    /**
     * 检查是否为插件独占包
     */
    private boolean isPluginPackage(String className) {
        return pluginPackages.stream().anyMatch(className::startsWith);
    }

    /**
     * 获取资源
     * 优先从插件JAR获取资源
     */
    @Override
    public URL getResource(String name) {
        // 1. 先从插件JAR查找
        URL url = findResource(name);
        if (url != null) {
            logger.trace("从插件JAR获取资源: {} (插件: {})", name, pluginId);
            return url;
        }

        // 2. 再从父类加载器查找
        url = parentLoader.getResource(name);
        if (url != null) {
            logger.trace("从父类加载器获取资源: {}", name);
        }

        return url;
    }

    /**
     * 获取所有资源
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> resources = new ArrayList<>();

        // 1. 从插件JAR获取
        Enumeration<URL> pluginResources = findResources(name);
        while (pluginResources.hasMoreElements()) {
            resources.add(pluginResources.nextElement());
        }

        // 2. 从父类加载器获取
        Enumeration<URL> parentResources = parentLoader.getResources(name);
        while (parentResources.hasMoreElements()) {
            URL url = parentResources.nextElement();
            if (!resources.contains(url)) {
                resources.add(url);
            }
        }

        return Collections.enumeration(resources);
    }

    /**
     * 获取加载统计信息
     */
    public ClassLoaderStatistics getStatistics() {
        int totalClasses = loadedClasses.size();
        long totalMemory = estimateMemoryUsage();

        Map<String, Integer> packageStats = new HashMap<>();
        for (String className : loadedClasses.keySet()) {
            String packageName = getPackageName(className);
            packageStats.merge(packageName, 1, Integer::sum);
        }

        return new ClassLoaderStatistics(
                pluginId,
                totalClasses,
                totalMemory,
                packageStats,
                getURLs().length);
    }

    /**
     * 估算内存使用量
     */
    private long estimateMemoryUsage() {
        // 简单估算：每个类大约占用 1KB 内存
        return loadedClasses.size() * 1024L;
    }

    /**
     * 获取包名
     */
    private String getPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        logger.debug("清理插件类加载器资源: {}", pluginId);

        try {
            // 清理加载的类缓存
            loadedClasses.clear();

            // 关闭URLClassLoader
            close();

            logger.info("插件类加载器资源清理完成: {}", pluginId);

        } catch (IOException e) {
            logger.warn("清理插件类加载器资源时发生异常: {}", pluginId, e);
        }
    }

    @Override
    public String toString() {
        return String.format("IsolatedPluginClassLoader{pluginId='%s', loadedClasses=%d}",
                pluginId, loadedClasses.size());
    }

    /**
     * 类加载器统计信息
     */
    public static class ClassLoaderStatistics {
        private final String pluginId;
        private final int loadedClassCount;
        private final long memoryUsage;
        private final Map<String, Integer> packageStatistics;
        private final int jarCount;

        public ClassLoaderStatistics(String pluginId, int loadedClassCount, long memoryUsage,
                Map<String, Integer> packageStatistics, int jarCount) {
            this.pluginId = pluginId;
            this.loadedClassCount = loadedClassCount;
            this.memoryUsage = memoryUsage;
            this.packageStatistics = new HashMap<>(packageStatistics);
            this.jarCount = jarCount;
        }

        // Getters
        public String getPluginId() {
            return pluginId;
        }

        public int getLoadedClassCount() {
            return loadedClassCount;
        }

        public long getMemoryUsage() {
            return memoryUsage;
        }

        public Map<String, Integer> getPackageStatistics() {
            return new HashMap<>(packageStatistics);
        }

        public int getJarCount() {
            return jarCount;
        }

        @Override
        public String toString() {
            return String.format("ClassLoaderStats{plugin='%s', classes=%d, memory=%dKB, jars=%d}",
                    pluginId, loadedClassCount, memoryUsage / 1024, jarCount);
        }
    }
}
