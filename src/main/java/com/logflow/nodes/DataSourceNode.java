package com.logflow.nodes;

import com.logflow.core.*;
import com.logflow.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 数据源节点
 * 基于插件化架构从各种外部数据源获取数据
 */
public class DataSourceNode extends AbstractWorkflowNode {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceNode.class);
    private final PluginManager pluginManager;
    private DataSourceConnection connection;

    public DataSourceNode(String id, String name) {
        super(id, name, NodeType.DATASOURCE);
        this.pluginManager = PluginManager.getInstance();

        // 确保插件管理器已初始化
        if (pluginManager.getPluginIds().isEmpty()) {
            // 插件管理器未初始化，进行初始化
            try {
                pluginManager.initialize();
            } catch (Exception e) {
                logger.warn("插件管理器初始化失败", e);
            }
        }
    }

    @Override
    protected NodeExecutionResult doExecute(WorkflowContext context) throws WorkflowException {
        String sourceType = getConfigValue("sourceType", String.class);

        if (sourceType == null || sourceType.trim().isEmpty()) {
            throw new WorkflowException(id, "数据源类型(sourceType)不能为空");
        }

        try {
            logger.info("节点 {} 开始执行，数据源类型: {}", id, sourceType);

            // 检查插件是否存在
            if (!pluginManager.hasPlugin(sourceType)) {
                throw new WorkflowException(id, "不支持的数据源类型: " + sourceType +
                        "，可用插件: " + pluginManager.getPluginIds());
            }

            // 创建数据源连接
            Map<String, Object> pluginConfig = extractPluginConfig();
            connection = pluginManager.createConnection(sourceType, pluginConfig, context);

            // 读取数据
            Object data = connection.readData(context);

            // 设置输出数据
            String outputKey = getConfigValue("outputKey", String.class, "data");
            context.setData(outputKey, data);

            // 记录统计信息
            recordDataStatistics(context, data);

            logger.info("节点 {} 执行成功，输出键: {}", id, outputKey);
            return NodeExecutionResult.success("数据源读取成功", data);

        } catch (PluginException e) {
            logger.error("节点 {} 插件执行失败", id, e);
            throw new WorkflowException(id, "数据源插件执行失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("节点 {} 执行失败", id, e);
            throw new WorkflowException(id, "数据源节点执行失败: " + e.getMessage(), e);
        } finally {
            // 关闭连接
            closeConnection();
        }
    }

    @Override
    public ValidationResult validate() {
        ValidationResult.Builder builder = ValidationResult.builder();

        // 验证基本配置
        String sourceType = getConfigValue("sourceType", String.class);
        if (sourceType == null || sourceType.trim().isEmpty()) {
            builder.error("数据源类型不能为空");
            return builder.build();
        }

        // 检查插件是否存在
        if (!pluginManager.hasPlugin(sourceType)) {
            builder.error("不支持的数据源类型: " + sourceType +
                    "，可用插件: " + pluginManager.getPluginIds());
            return builder.build();
        }

        // 使用插件验证配置
        try {
            DataSourcePlugin plugin = pluginManager.getPlugin(sourceType);
            Map<String, Object> pluginConfig = extractPluginConfig();
            PluginValidationResult pluginResult = plugin.validateConfiguration(pluginConfig);

            if (!pluginResult.isValid()) {
                for (PluginValidationResult.ValidationError error : pluginResult.getErrors()) {
                    builder.error(error.getParameter() + ": " + error.getMessage());
                }
            }

            // 添加警告
            for (PluginValidationResult.ValidationWarning warning : pluginResult.getWarnings()) {
                builder.warning(warning.getParameter() + ": " + warning.getMessage());
            }

        } catch (Exception e) {
            builder.error("插件验证失败: " + e.getMessage());
        }

        return builder.build();
    }

    /**
     * 测试数据源连接
     */
    public PluginTestResult testConnection() {
        String sourceType = getConfigValue("sourceType", String.class);

        if (sourceType == null || !pluginManager.hasPlugin(sourceType)) {
            return PluginTestResult.failure("插件不存在: " + sourceType);
        }

        try {
            Map<String, Object> pluginConfig = extractPluginConfig();
            return pluginManager.testConnection(sourceType, pluginConfig);
        } catch (Exception e) {
            return PluginTestResult.failure("测试连接失败", e);
        }
    }

    /**
     * 获取数据源模式信息
     */
    public DataSourceSchema getDataSourceSchema() {
        String sourceType = getConfigValue("sourceType", String.class);

        if (sourceType == null || !pluginManager.hasPlugin(sourceType)) {
            return null;
        }

        try {
            DataSourcePlugin plugin = pluginManager.getPlugin(sourceType);
            Map<String, Object> pluginConfig = extractPluginConfig();
            return plugin.getSchema(pluginConfig);
        } catch (Exception e) {
            logger.warn("获取数据源模式失败", e);
            return null;
        }
    }

    /**
     * 获取插件信息
     */
    public PluginManager.PluginInfo getPluginInfo() {
        String sourceType = getConfigValue("sourceType", String.class);

        if (sourceType == null || !pluginManager.hasPlugin(sourceType)) {
            return null;
        }

        return pluginManager.getPluginInfo(sourceType);
    }

    /**
     * 获取所有可用的数据源插件
     */
    public static Collection<PluginManager.PluginInfo> getAvailablePlugins() {
        PluginManager pluginManager = PluginManager.getInstance();

        // 确保插件管理器已初始化
        if (pluginManager.getPluginIds().isEmpty()) {
            try {
                pluginManager.initialize();
            } catch (Exception e) {
                logger.warn("插件管理器初始化失败", e);
            }
        }

        return pluginManager.getPluginInfos();
    }

    /**
     * 获取插件支持的参数
     */
    public List<PluginParameter> getPluginParameters() {
        String sourceType = getConfigValue("sourceType", String.class);

        if (sourceType == null || !pluginManager.hasPlugin(sourceType)) {
            return Collections.emptyList();
        }

        DataSourcePlugin plugin = pluginManager.getPlugin(sourceType);
        return plugin.getSupportedParameters();
    }

    /**
     * 提取插件配置
     * 从节点配置中提取插件特定的配置参数
     */
    private Map<String, Object> extractPluginConfig() {
        Map<String, Object> pluginConfig = new HashMap<>();

        // 复制所有配置，除了节点特定的配置
        Set<String> nodeSpecificKeys = Set.of("sourceType", "outputKey", "enabled");

        for (Map.Entry<String, Object> entry : configuration.entrySet()) {
            if (!nodeSpecificKeys.contains(entry.getKey())) {
                pluginConfig.put(entry.getKey(), entry.getValue());
            }
        }

        return pluginConfig;
    }

    /**
     * 记录数据统计信息
     */
    private void recordDataStatistics(WorkflowContext context, Object data) {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("nodeId", id);
            stats.put("sourceType", getConfigValue("sourceType", String.class));
            stats.put("timestamp", System.currentTimeMillis());

            // 计算数据大小
            if (data instanceof Collection) {
                stats.put("recordCount", ((Collection<?>) data).size());
            } else if (data != null) {
                stats.put("recordCount", 1);
            } else {
                stats.put("recordCount", 0);
            }

            // 获取连接统计信息
            if (connection != null) {
                Map<String, Object> connectionStats = connection.getDataStatistics();
                stats.putAll(connectionStats);
            }

            // 存储统计信息到上下文
            context.setData(id + "_statistics", stats);

        } catch (Exception e) {
            logger.debug("记录数据统计信息失败", e);
        }
    }

    /**
     * 关闭数据源连接
     */
    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                logger.warn("关闭数据源连接失败", e);
            } finally {
                connection = null;
            }
        }
    }

    @Override
    public String toString() {
        String sourceType = getConfigValue("sourceType", String.class, "unknown");
        return String.format("DataSourceNode{id='%s', name='%s', sourceType='%s'}",
                id, name, sourceType);
    }
}