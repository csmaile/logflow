package com.logflow.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 插件资源管理器
 * 负责插件的生命周期管理、资源监控和自动清理
 */
public class PluginResourceManager {

    private static final Logger logger = LoggerFactory.getLogger(PluginResourceManager.class);

    // 配置参数
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 30 * 60 * 1000; // 30分钟
    private static final long DEFAULT_CHECK_INTERVAL_MS = 5 * 60 * 1000; // 5分钟
    private static final double DEFAULT_MEMORY_THRESHOLD = 0.8; // 80%内存阈值
    private static final int DEFAULT_MAX_PLUGINS = 50; // 最大插件数
    private static final long DEFAULT_MIN_ACCESS_INTERVAL = 60 * 1000; // 最小访问间隔1分钟

    private final PluginManager pluginManager;
    private final ScheduledExecutorService scheduler;
    private final Map<String, PluginUsageInfo> pluginUsage;
    private final MemoryMXBean memoryBean;

    // 配置参数
    private volatile long idleTimeoutMs = DEFAULT_IDLE_TIMEOUT_MS;
    private volatile long checkIntervalMs = DEFAULT_CHECK_INTERVAL_MS;
    private volatile double memoryThreshold = DEFAULT_MEMORY_THRESHOLD;
    private volatile int maxPlugins = DEFAULT_MAX_PLUGINS;
    private volatile boolean autoManagementEnabled = true;

    // 统计信息
    private final AtomicLong totalUnloads = new AtomicLong(0);
    private final AtomicLong memoryBasedUnloads = new AtomicLong(0);
    private final AtomicLong idleBasedUnloads = new AtomicLong(0);
    private final AtomicLong capacityBasedUnloads = new AtomicLong(0);

    public PluginResourceManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "PluginResourceManager");
            t.setDaemon(true);
            return t;
        });
        this.pluginUsage = new ConcurrentHashMap<>();
        this.memoryBean = ManagementFactory.getMemoryMXBean();

        // 启动资源监控任务
        startMonitoring();
    }

    /**
     * 启动监控任务
     */
    private void startMonitoring() {
        // 定期清理任务
        scheduler.scheduleAtFixedRate(
                this::performCleanup,
                checkIntervalMs,
                checkIntervalMs,
                TimeUnit.MILLISECONDS);

        // 内存监控任务
        scheduler.scheduleAtFixedRate(
                this::monitorMemoryUsage,
                30000, // 30秒后开始
                30000, // 每30秒检查一次
                TimeUnit.MILLISECONDS);

        logger.info("插件资源管理器已启动，清理间隔: {}ms, 空闲超时: {}ms",
                checkIntervalMs, idleTimeoutMs);
    }

    /**
     * 记录插件使用
     */
    public void recordPluginUsage(String pluginId) {
        long currentTime = System.currentTimeMillis();

        pluginUsage.compute(pluginId, (id, info) -> {
            if (info == null) {
                info = new PluginUsageInfo(pluginId, currentTime);
            }

            // 防止频繁更新（1分钟内的重复访问不更新）
            if (currentTime - info.getLastAccessTime() > DEFAULT_MIN_ACCESS_INTERVAL) {
                info.updateAccess();
                logger.trace("记录插件使用: {} (总访问次数: {})", pluginId, info.getAccessCount());
            }

            return info;
        });
    }

    /**
     * 执行清理操作
     */
    private void performCleanup() {
        if (!autoManagementEnabled) {
            return;
        }

        try {
            logger.debug("开始执行插件资源清理...");

            // 1. 检查内存使用情况
            if (isMemoryPressure()) {
                performMemoryBasedCleanup();
            }

            // 2. 检查空闲插件
            performIdleCleanup();

            // 3. 检查插件数量限制
            performCapacityCleanup();

            // 4. 清理失效的使用信息
            cleanupOrphanUsageInfo();

            logger.debug("插件资源清理完成");

        } catch (Exception e) {
            logger.error("执行插件资源清理时发生异常", e);
        }
    }

    /**
     * 检查是否存在内存压力
     */
    private boolean isMemoryPressure() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double usedRatio = (double) heapUsage.getUsed() / heapUsage.getMax();

        boolean pressure = usedRatio > memoryThreshold;
        if (pressure) {
            logger.debug("检测到内存压力: {:.2f}% > {:.2f}%", usedRatio * 100, memoryThreshold * 100);
        }

        return pressure;
    }

    /**
     * 基于内存压力的清理
     */
    private void performMemoryBasedCleanup() {
        logger.info("执行基于内存压力的插件清理...");

        List<String> candidates = getUnloadCandidates(UnloadReason.MEMORY_PRESSURE);

        // 卸载最少使用的插件，直到内存压力缓解
        int unloadCount = 0;
        for (String pluginId : candidates) {
            if (!isMemoryPressure()) {
                break; // 内存压力已缓解
            }

            if (unloadPlugin(pluginId, UnloadReason.MEMORY_PRESSURE)) {
                unloadCount++;
                memoryBasedUnloads.incrementAndGet();
            }

            if (unloadCount >= 5) {
                break; // 避免一次卸载过多插件
            }
        }

        if (unloadCount > 0) {
            logger.info("基于内存压力卸载了 {} 个插件", unloadCount);
        }
    }

    /**
     * 空闲插件清理
     */
    private void performIdleCleanup() {
        long currentTime = System.currentTimeMillis();
        List<String> idlePlugins = new ArrayList<>();

        for (PluginUsageInfo info : pluginUsage.values()) {
            if (currentTime - info.getLastAccessTime() > idleTimeoutMs) {
                idlePlugins.add(info.getPluginId());
            }
        }

        if (!idlePlugins.isEmpty()) {
            logger.debug("发现 {} 个空闲插件", idlePlugins.size());

            for (String pluginId : idlePlugins) {
                if (unloadPlugin(pluginId, UnloadReason.IDLE_TIMEOUT)) {
                    idleBasedUnloads.incrementAndGet();
                }
            }
        }
    }

    /**
     * 容量限制清理
     */
    private void performCapacityCleanup() {
        Collection<PluginManager.PluginInfo> loadedPlugins = pluginManager.getPluginInfos();

        if (loadedPlugins.size() > maxPlugins) {
            int excessCount = loadedPlugins.size() - maxPlugins;
            logger.info("插件数量 {} 超过限制 {}，需要卸载 {} 个插件",
                    loadedPlugins.size(), maxPlugins, excessCount);

            List<String> candidates = getUnloadCandidates(UnloadReason.CAPACITY_LIMIT);

            for (int i = 0; i < excessCount && i < candidates.size(); i++) {
                String pluginId = candidates.get(i);
                if (unloadPlugin(pluginId, UnloadReason.CAPACITY_LIMIT)) {
                    capacityBasedUnloads.incrementAndGet();
                }
            }
        }
    }

    /**
     * 获取卸载候选插件列表
     * 按优先级排序：访问次数少、最近未使用的优先卸载
     */
    private List<String> getUnloadCandidates(UnloadReason reason) {
        List<PluginUsageInfo> candidates = new ArrayList<>();

        // 获取所有已加载的插件
        Collection<PluginManager.PluginInfo> loadedPlugins = pluginManager.getPluginInfos();

        for (PluginManager.PluginInfo pluginInfo : loadedPlugins) {
            String pluginId = pluginInfo.getId();

            // 跳过系统关键插件
            if (isSystemCriticalPlugin(pluginId)) {
                continue;
            }

            PluginUsageInfo usageInfo = pluginUsage.get(pluginId);
            if (usageInfo != null) {
                candidates.add(usageInfo);
            }
        }

        // 排序：优先卸载访问次数少、最近未使用的插件
        candidates.sort((a, b) -> {
            // 1. 按访问次数排序（升序）
            int accessCompare = Long.compare(a.getAccessCount(), b.getAccessCount());
            if (accessCompare != 0) {
                return accessCompare;
            }

            // 2. 按最后访问时间排序（升序，最久未使用的优先）
            return Long.compare(a.getLastAccessTime(), b.getLastAccessTime());
        });

        return candidates.stream()
                .map(PluginUsageInfo::getPluginId)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 检查是否为系统关键插件
     */
    private boolean isSystemCriticalPlugin(String pluginId) {
        // 定义系统关键插件，不能被自动卸载
        Set<String> criticalPlugins = Set.of("mock", "file");
        return criticalPlugins.contains(pluginId);
    }

    /**
     * 卸载插件
     */
    private boolean unloadPlugin(String pluginId, UnloadReason reason) {
        try {
            logger.info("卸载插件: {} (原因: {})", pluginId, reason);

            // 从插件管理器卸载
            pluginManager.unregisterPlugin(pluginId);

            // 清理使用信息
            pluginUsage.remove(pluginId);

            // 更新统计
            totalUnloads.incrementAndGet();

            logger.info("插件 {} 已卸载", pluginId);
            return true;

        } catch (Exception e) {
            logger.error("卸载插件 {} 失败", pluginId, e);
            return false;
        }
    }

    /**
     * 清理孤立的使用信息
     */
    private void cleanupOrphanUsageInfo() {
        Set<String> loadedPluginIds = pluginManager.getPluginInfos().stream()
                .map(PluginManager.PluginInfo::getId)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        List<String> orphanIds = new ArrayList<>();
        for (String pluginId : pluginUsage.keySet()) {
            if (!loadedPluginIds.contains(pluginId)) {
                orphanIds.add(pluginId);
            }
        }

        if (!orphanIds.isEmpty()) {
            logger.debug("清理 {} 个孤立的插件使用信息", orphanIds.size());
            orphanIds.forEach(pluginUsage::remove);
        }
    }

    /**
     * 监控内存使用情况
     */
    private void monitorMemoryUsage() {
        try {
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            long usedMB = heapUsage.getUsed() / 1024 / 1024;
            long maxMB = heapUsage.getMax() / 1024 / 1024;
            double usedRatio = (double) heapUsage.getUsed() / heapUsage.getMax();

            logger.trace("内存使用情况: {}/{} MB ({:.1f}%)", usedMB, maxMB, usedRatio * 100);

            // 如果内存使用率过高，触发紧急清理
            if (usedRatio > 0.9) {
                logger.warn("内存使用率过高 ({:.1f}%)，触发紧急清理", usedRatio * 100);
                performMemoryBasedCleanup();
            }

        } catch (Exception e) {
            logger.error("监控内存使用情况时发生异常", e);
        }
    }

    /**
     * 获取资源管理统计信息
     */
    public ResourceManagementStatistics getStatistics() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        Map<String, PluginUsageInfo> currentUsage = new HashMap<>(pluginUsage);

        return new ResourceManagementStatistics(
                pluginManager.getPluginInfos().size(),
                currentUsage.size(),
                heapUsage.getUsed(),
                heapUsage.getMax(),
                totalUnloads.get(),
                memoryBasedUnloads.get(),
                idleBasedUnloads.get(),
                capacityBasedUnloads.get(),
                autoManagementEnabled,
                currentUsage);
    }

    /**
     * 强制清理指定插件
     */
    public boolean forceUnloadPlugin(String pluginId) {
        logger.info("强制卸载插件: {}", pluginId);
        return unloadPlugin(pluginId, UnloadReason.MANUAL);
    }

    /**
     * 启用/禁用自动管理
     */
    public void setAutoManagementEnabled(boolean enabled) {
        this.autoManagementEnabled = enabled;
        logger.info("插件自动管理已{}", enabled ? "启用" : "禁用");
    }

    /**
     * 设置配置参数
     */
    public void setIdleTimeout(long timeoutMs) {
        this.idleTimeoutMs = timeoutMs;
        logger.info("设置插件空闲超时: {}ms", timeoutMs);
    }

    public void setMemoryThreshold(double threshold) {
        this.memoryThreshold = threshold;
        logger.info("设置内存阈值: {:.1f}%", threshold * 100);
    }

    public void setMaxPlugins(int maxPlugins) {
        this.maxPlugins = maxPlugins;
        logger.info("设置最大插件数: {}", maxPlugins);
    }

    /**
     * 关闭资源管理器
     */
    public void shutdown() {
        logger.info("关闭插件资源管理器...");

        autoManagementEnabled = false;
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        pluginUsage.clear();
        logger.info("插件资源管理器已关闭");
    }

    /**
     * 插件使用信息
     */
    public static class PluginUsageInfo {
        private final String pluginId;
        private final long createTime;
        private volatile long lastAccessTime;
        private volatile long accessCount;

        public PluginUsageInfo(String pluginId, long createTime) {
            this.pluginId = pluginId;
            this.createTime = createTime;
            this.lastAccessTime = createTime;
            this.accessCount = 1;
        }

        public void updateAccess() {
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount++;
        }

        // Getters
        public String getPluginId() {
            return pluginId;
        }

        public long getCreateTime() {
            return createTime;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public long getAccessCount() {
            return accessCount;
        }

        public long getIdleDuration() {
            return System.currentTimeMillis() - lastAccessTime;
        }
    }

    /**
     * 卸载原因枚举
     */
    public enum UnloadReason {
        IDLE_TIMEOUT("空闲超时"),
        MEMORY_PRESSURE("内存压力"),
        CAPACITY_LIMIT("容量限制"),
        MANUAL("手动卸载");

        private final String description;

        UnloadReason(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * 资源管理统计信息
     */
    public static class ResourceManagementStatistics {
        private final int loadedPluginCount;
        private final int trackedPluginCount;
        private final long memoryUsed;
        private final long memoryMax;
        private final long totalUnloads;
        private final long memoryBasedUnloads;
        private final long idleBasedUnloads;
        private final long capacityBasedUnloads;
        private final boolean autoManagementEnabled;
        private final Map<String, PluginUsageInfo> pluginUsage;

        public ResourceManagementStatistics(int loadedPluginCount, int trackedPluginCount,
                long memoryUsed, long memoryMax,
                long totalUnloads, long memoryBasedUnloads,
                long idleBasedUnloads, long capacityBasedUnloads,
                boolean autoManagementEnabled,
                Map<String, PluginUsageInfo> pluginUsage) {
            this.loadedPluginCount = loadedPluginCount;
            this.trackedPluginCount = trackedPluginCount;
            this.memoryUsed = memoryUsed;
            this.memoryMax = memoryMax;
            this.totalUnloads = totalUnloads;
            this.memoryBasedUnloads = memoryBasedUnloads;
            this.idleBasedUnloads = idleBasedUnloads;
            this.capacityBasedUnloads = capacityBasedUnloads;
            this.autoManagementEnabled = autoManagementEnabled;
            this.pluginUsage = new HashMap<>(pluginUsage);
        }

        // Getters
        public int getLoadedPluginCount() {
            return loadedPluginCount;
        }

        public int getTrackedPluginCount() {
            return trackedPluginCount;
        }

        public long getMemoryUsed() {
            return memoryUsed;
        }

        public long getMemoryMax() {
            return memoryMax;
        }

        public double getMemoryUsageRatio() {
            return (double) memoryUsed / memoryMax;
        }

        public long getTotalUnloads() {
            return totalUnloads;
        }

        public long getMemoryBasedUnloads() {
            return memoryBasedUnloads;
        }

        public long getIdleBasedUnloads() {
            return idleBasedUnloads;
        }

        public long getCapacityBasedUnloads() {
            return capacityBasedUnloads;
        }

        public boolean isAutoManagementEnabled() {
            return autoManagementEnabled;
        }

        public Map<String, PluginUsageInfo> getPluginUsage() {
            return new HashMap<>(pluginUsage);
        }
    }
}
