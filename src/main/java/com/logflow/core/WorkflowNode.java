package com.logflow.core;

import java.util.Map;

/**
 * 工作流节点基础接口
 * 所有节点类型都需要实现此接口
 */
public interface WorkflowNode {
    
    /**
     * 获取节点ID
     * @return 节点唯一标识
     */
    String getId();
    
    /**
     * 获取节点名称
     * @return 节点名称
     */
    String getName();
    
    /**
     * 获取节点类型
     * @return 节点类型
     */
    NodeType getType();
    
    /**
     * 获取节点配置
     * @return 节点配置信息
     */
    Map<String, Object> getConfiguration();
    
    /**
     * 设置节点配置
     * @param configuration 配置信息
     */
    void setConfiguration(Map<String, Object> configuration);
    
    /**
     * 执行节点逻辑
     * @param context 工作流执行上下文
     * @return 执行结果
     * @throws WorkflowException 执行异常
     */
    NodeExecutionResult execute(WorkflowContext context) throws WorkflowException;
    
    /**
     * 验证节点配置是否有效
     * @return 验证结果
     */
    ValidationResult validate();
}
