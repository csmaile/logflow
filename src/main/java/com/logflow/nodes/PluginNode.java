package com.logflow.nodes;

import com.logflow.core.*;
import com.logflow.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 插件节点
 * 基于插件化架构执行各种可扩展的功能，包括数据获取、处理、集成等
 * 
 * 这是 DataSourceNode 的升级版本，提供了更强大的插件化能力：
 * - 支持任何通过插件实现的自定义逻辑
 * - 动态插件加载和JAR包上传
 * - 完整的资源管理和依赖隔离
 * - 配置验证和连接测试
 * 
 * @since 2.0.0
 */
public class PluginNode extends AbstractWorkflowNode {

    private static final Logger logger = LoggerFactory.getLogger(PluginNode.class);
    private final PluginManager pluginManager;
    private DataSourceConnection connection;

    public PluginNode(String id, String name) {
        super(id, name, NodeType.PLUGIN);
        this.pluginManager = PluginManager.getInstance();

        // 确保插件管理器已初始化
        if (pluginManager.getPluginIds().isEmpty()) {
            try {
                pluginManager.initialize();
            } catch (Exception e) {
                logger.warn("插件管理器初始化失败", e);
            }
        }
    }

    @Override
    protected NodeExecutionResult doExecute(WorkflowContext context) throws WorkflowException {
        String pluginType = getConfigValue("pluginType", String.class);

        // 向后兼容：支持旧的 sourceType 配置
        if (pluginType == null) {
            pluginType = getConfigValue("sourceType", String.class);
            if (pluginType != null) {
                logger.warn("配置项 'sourceType' 已废弃，请使用 'pluginType'。节点: {}", id);
            }
        }

        if (pluginType == null || pluginType.trim().isEmpty()) {
            throw new WorkflowException(id, "插件类型(pluginType)不能为空");
        }

        try {
            logger.info("节点 {} 开始执行，插件类型: {}", id, pluginType);

            // 检查插件是否存在
            if (!pluginManager.hasPlugin(pluginType)) {
                throw new WorkflowException(id, "不支持的插件类型: " + pluginType +
                        "，可用插件: " + pluginManager.getPluginIds());
            }

            // 准备插件输入数据（支持多输入）
            Object inputData = preparePluginInputData(context);

            // 创建插件连接/会话
            Map<String, Object> pluginConfig = extractPluginConfig();
            connection = pluginManager.createConnection(pluginType, pluginConfig, context);

            // 如果有输入数据，将其添加到上下文中供插件使用
            if (inputData != null) {
                String inputContextKey = getConfigValue("inputContextKey", String.class, "plugin_input");
                context.setData(inputContextKey, inputData);
                logger.debug("插件节点 {} 设置输入数据到上下文，键: {}", id, inputContextKey);
            }

            // 执行插件逻辑
            Object result = connection.readData(context);

            // 设置输出数据
            setOutputData(context, result);

            // 记录执行统计
            recordExecutionStatistics(context, result);

            String outputKey = getConfigValue("outputKey", String.class, "data");
            logger.info("节点 {} 执行成功，输出键: {}", id, outputKey);
            return NodeExecutionResult.success(id, result);

        } catch (PluginException e) {
            logger.error("节点 {} 插件执行失败", id, e);
            throw new WorkflowException(id, "插件执行失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("节点 {} 执行失败", id, e);
            throw new WorkflowException(id, "插件节点执行失败: " + e.getMessage(), e);
        } finally {
            // 关闭连接
            closeConnection();
        }
    }

    /**
     * 准备插件输入数据（多输入配置）
     */
    private Object preparePluginInputData(WorkflowContext context) throws WorkflowException {
        InputDataProcessor.InputDataResult inputResult = processInputData(context);
        if (!inputResult.isSuccess()) {
            logger.warn("插件节点多输入处理失败: {}, 插件将使用空输入", inputResult.getErrorMessage());
            return null;
        }

        Object inputData = inputResult.getData();
        logger.info("插件节点使用多输入模式, 输入模式: {}", inputResult.getMetadata().get("inputMode"));
        return inputData;
    }

    @Override
    public ValidationResult validate() {
        ValidationResult.Builder builder = ValidationResult.builder();

        // 验证插件类型配置
        String pluginType = getConfigValue("pluginType", String.class);
        if (pluginType == null) {
            pluginType = getConfigValue("sourceType", String.class);
            if (pluginType != null) {
                builder.warning("配置项 'sourceType' 已废弃，请使用 'pluginType'");
            }
        }

        if (pluginType == null || pluginType.trim().isEmpty()) {
            builder.error("插件类型不能为空");
            return builder.build();
        }

        // 检查插件是否存在
        if (!pluginManager.hasPlugin(pluginType)) {
            builder.error("不支持的插件类型: " + pluginType +
                    "，可用插件: " + pluginManager.getPluginIds());
            return builder.build();
        }

        // 验证多输入配置
        MultiInputConfig inputConfig = InputDataProcessor.extractInputConfig(configuration);
        ValidationResult inputValidation = InputDataProcessor.validateInputConfig(inputConfig, id);
        if (!inputValidation.isValid()) {
            builder.errors(inputValidation.getErrors());
            builder.warnings(inputValidation.getWarnings());
        }

        // 使用插件验证配置
        try {
            DataSourcePlugin plugin = pluginManager.getPlugin(pluginType);
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
     * 测试插件连接
     */
    public PluginTestResult testConnection() {
        String pluginType = getConfigValue("pluginType", String.class);
        if (pluginType == null) {
            pluginType = getConfigValue("sourceType", String.class);
        }

        if (pluginType == null || !pluginManager.hasPlugin(pluginType)) {
            return PluginTestResult.failure("插件类型未配置或不存在: " + pluginType);
        }

        try {
            DataSourcePlugin plugin = pluginManager.getPlugin(pluginType);
            Map<String, Object> pluginConfig = extractPluginConfig();
            return plugin.testConnection(pluginConfig);
        } catch (Exception e) {
            logger.error("插件连接测试失败: {}", pluginType, e);
            return PluginTestResult.failure("连接测试失败: " + e.getMessage());
        }
    }

    /**
     * 获取插件信息
     */
    public PluginManager.PluginInfo getPluginInfo() {
        String pluginType = getConfigValue("pluginType", String.class);
        if (pluginType == null) {
            pluginType = getConfigValue("sourceType", String.class);
        }

        if (pluginType == null || !pluginManager.hasPlugin(pluginType)) {
            return null;
        }

        return pluginManager.getPluginInfo(pluginType);
    }

    /**
     * 获取所有可用的插件
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
        String pluginType = getConfigValue("pluginType", String.class);
        if (pluginType == null) {
            pluginType = getConfigValue("sourceType", String.class);
        }

        if (pluginType == null || !pluginManager.hasPlugin(pluginType)) {
            return Collections.emptyList();
        }

        DataSourcePlugin plugin = pluginManager.getPlugin(pluginType);
        return plugin.getSupportedParameters();
    }

    /**
     * 关闭连接
     */
    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                logger.debug("插件连接已关闭: {}", id);
            } catch (Exception e) {
                logger.warn("关闭插件连接时出错: {}", id, e);
            } finally {
                connection = null;
            }
        }
    }

    /**
     * 提取插件配置
     * 从节点配置中提取插件特定的配置参数
     */
    private Map<String, Object> extractPluginConfig() {
        Map<String, Object> pluginConfig = new HashMap<>();

        // 复制所有配置，除了节点特定的配置
        Set<String> nodeSpecificKeys = Set.of("pluginType", "sourceType", "outputKey", "enabled");

        for (Map.Entry<String, Object> entry : configuration.entrySet()) {
            if (!nodeSpecificKeys.contains(entry.getKey())) {
                pluginConfig.put(entry.getKey(), entry.getValue());
            }
        }

        return pluginConfig;
    }

    /**
     * 记录执行统计信息
     */
    private void recordExecutionStatistics(WorkflowContext context, Object result) {
        try {
            // 记录基本统计信息
            context.setMetadata("plugin_node_" + id + "_output_size",
                    result != null ? result.toString().length() : 0);
            context.setMetadata("plugin_node_" + id + "_execution_time",
                    System.currentTimeMillis());

            // 如果结果是集合，记录数量
            if (result instanceof Collection) {
                context.setMetadata("plugin_node_" + id + "_result_count",
                        ((Collection<?>) result).size());
            } else if (result instanceof Map) {
                context.setMetadata("plugin_node_" + id + "_result_keys",
                        ((Map<?, ?>) result).size());
            }

        } catch (Exception e) {
            logger.debug("记录执行统计信息时出错: {}", id, e);
        }
    }
}
