package com.logflow.plugin;

import com.logflow.core.WorkflowContext;

import java.util.List;
import java.util.Map;

/**
 * 数据源插件接口
 * 
 * 所有数据源插件必须实现此接口，通过SPI机制注册
 */
public interface DataSourcePlugin {

    /**
     * 获取插件唯一标识符
     * 
     * @return 插件ID，如 "mysql", "kafka", "elasticsearch" 等
     */
    String getPluginId();

    /**
     * 获取插件名称
     * 
     * @return 插件显示名称
     */
    String getPluginName();

    /**
     * 获取插件版本
     * 
     * @return 版本号，如 "1.0.0"
     */
    String getVersion();

    /**
     * 获取插件描述
     * 
     * @return 插件功能描述
     */
    String getDescription();

    /**
     * 获取插件作者信息
     * 
     * @return 作者信息
     */
    String getAuthor();

    /**
     * 获取支持的配置参数
     * 
     * @return 配置参数定义列表
     */
    List<PluginParameter> getSupportedParameters();

    /**
     * 初始化插件
     * 调用在插件加载后、首次使用前
     * 
     * @param globalConfig 全局配置
     * @throws PluginException 初始化失败时抛出
     */
    void initialize(Map<String, Object> globalConfig) throws PluginException;

    /**
     * 验证配置参数
     * 
     * @param config 节点配置参数
     * @return 验证结果
     */
    PluginValidationResult validateConfiguration(Map<String, Object> config);

    /**
     * 创建数据源连接/会话
     * 
     * @param config  节点配置参数
     * @param context 工作流上下文
     * @return 数据源连接对象
     * @throws PluginException 连接失败时抛出
     */
    DataSourceConnection createConnection(Map<String, Object> config, WorkflowContext context) throws PluginException;

    /**
     * 测试连接
     * 
     * @param config 节点配置参数
     * @return 测试结果
     */
    PluginTestResult testConnection(Map<String, Object> config);

    /**
     * 获取数据模式/结构信息（可选）
     * 
     * @param config 节点配置参数
     * @return 数据模式信息，如果不支持则返回null
     */
    default DataSourceSchema getSchema(Map<String, Object> config) {
        return null;
    }

    /**
     * 获取插件依赖的JAR包列表（可选）
     * 
     * @return 依赖的JAR包信息
     */
    default List<String> getDependencies() {
        return List.of();
    }

    /**
     * 插件销毁时调用
     * 用于清理资源
     */
    default void destroy() {
        // 默认实现为空
    }

    /**
     * 判断插件是否支持指定的操作
     * 
     * @param operation 操作类型，如 "read", "write", "stream", "batch"
     * @return 是否支持
     */
    default boolean supportsOperation(String operation) {
        return "read".equals(operation);
    }
}
