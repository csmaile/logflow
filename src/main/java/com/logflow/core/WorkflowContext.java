package com.logflow.core;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流执行上下文
 * 存储工作流执行过程中的数据和状态信息
 */
public class WorkflowContext {
    
    private final String workflowId;
    private final String executionId;
    private final LocalDateTime startTime;
    private final Map<String, Object> data;
    private final Map<String, Object> metadata;
    
    public WorkflowContext(String workflowId, String executionId) {
        this.workflowId = workflowId;
        this.executionId = executionId;
        this.startTime = LocalDateTime.now();
        this.data = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
    }
    
    /**
     * 获取工作流ID
     */
    public String getWorkflowId() {
        return workflowId;
    }
    
    /**
     * 获取执行ID
     */
    public String getExecutionId() {
        return executionId;
    }
    
    /**
     * 获取开始时间
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    /**
     * 设置数据
     */
    public void setData(String key, Object value) {
        data.put(key, value);
    }
    
    /**
     * 获取数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) data.get(key);
    }
    
    /**
     * 获取数据（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, T defaultValue) {
        return (T) data.getOrDefault(key, defaultValue);
    }
    
    /**
     * 获取所有数据
     */
    public Map<String, Object> getAllData() {
        return new HashMap<>(data);
    }
    
    /**
     * 设置元数据
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * 获取元数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key) {
        return (T) metadata.get(key);
    }
    
    /**
     * 获取所有元数据
     */
    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * 检查是否包含指定键的数据
     */
    public boolean hasData(String key) {
        return data.containsKey(key);
    }
    
    /**
     * 移除数据
     */
    public Object removeData(String key) {
        return data.remove(key);
    }
    
    /**
     * 清空所有数据
     */
    public void clearData() {
        data.clear();
    }
}
