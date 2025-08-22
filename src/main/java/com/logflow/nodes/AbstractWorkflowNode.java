package com.logflow.nodes;

import com.logflow.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流节点抽象基类
 * 提供节点的基础实现
 */
public abstract class AbstractWorkflowNode implements WorkflowNode {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected String id;
    protected String name;
    protected NodeType type;
    protected Map<String, Object> configuration;

    public AbstractWorkflowNode(String id, String name, NodeType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.configuration = new HashMap<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return type;
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return new HashMap<>(configuration);
    }

    @Override
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = new HashMap<>(configuration);
    }

    @Override
    public NodeExecutionResult execute(WorkflowContext context) throws WorkflowException {
        logger.info("开始执行节点: {} ({})", name, id);

        long startTime = System.currentTimeMillis();
        try {
            // 执行前验证
            ValidationResult validation = validate();
            if (!validation.isValid()) {
                throw new WorkflowException(id, "节点配置验证失败: " + String.join(", ", validation.getErrors()));
            }

            // 执行具体逻辑
            NodeExecutionResult result = doExecute(context);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("节点执行完成: {} 耗时: {}ms", id, duration);

            return NodeExecutionResult.builder(id)
                    .success(result.isSuccess())
                    .data(result.getData())
                    .message(result.getMessage())
                    .executionDuration(duration)
                    .metadata(result.getMetadata())
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("节点执行失败: {} 耗时: {}ms", id, duration, e);

            return NodeExecutionResult.builder(id)
                    .success(false)
                    .message("执行失败: " + e.getMessage())
                    .executionDuration(duration)
                    .build();
        }
    }

    /**
     * 子类实现具体的执行逻辑
     */
    protected abstract NodeExecutionResult doExecute(WorkflowContext context) throws WorkflowException;

    /**
     * 获取配置值
     */
    protected <T> T getConfigValue(String key, Class<T> type) {
        Object value = configuration.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    /**
     * 获取配置值（带默认值）
     */
    protected <T> T getConfigValue(String key, Class<T> type, T defaultValue) {
        T value = getConfigValue(key, type);
        return value != null ? value : defaultValue;
    }

    /**
     * 检查必需的配置项
     */
    protected ValidationResult validateRequiredConfigs(String... requiredKeys) {
        ValidationResult.Builder builder = ValidationResult.builder();

        for (String key : requiredKeys) {
            if (!configuration.containsKey(key) || configuration.get(key) == null) {
                builder.error("缺少必需的配置项: " + key);
            }
        }

        return builder.build();
    }

    /**
     * 处理多输入配置并获取输入数据
     * 
     * @param context 工作流上下文
     * @return 输入数据处理结果
     */
    protected InputDataProcessor.InputDataResult processInputData(WorkflowContext context) {
        MultiInputConfig inputConfig = InputDataProcessor.extractInputConfig(configuration);
        return InputDataProcessor.processInputData(context, inputConfig, this.id);
    }

    /**
     * 获取输入数据（多输入模式）
     * 
     * @param context 工作流上下文
     * @return 输入数据
     */
    protected Object getInputData(WorkflowContext context) {
        InputDataProcessor.InputDataResult result = processInputData(context);
        return result.isSuccess() ? result.getData() : null;
    }

    /**
     * 设置输出数据到上下文
     * 
     * @param context 工作流上下文
     * @param data    要输出的数据
     */
    protected void setOutputData(WorkflowContext context, Object data) {
        String outputKey = getConfigValue("outputKey", String.class);
        if (outputKey != null && !outputKey.trim().isEmpty()) {
            context.setData(outputKey, data);
            logger.debug("节点 {} 设置输出数据到键: {}", id, outputKey);
        } else {
            logger.warn("节点 {} 未配置输出键，无法设置输出数据", id);
        }
    }
}
