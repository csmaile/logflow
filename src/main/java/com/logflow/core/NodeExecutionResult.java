package com.logflow.core;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 节点执行结果
 */
public class NodeExecutionResult {
    
    private final String nodeId;
    private final boolean success;
    private final Object data;
    private final String message;
    private final LocalDateTime executionTime;
    private final long executionDurationMs;
    private final Map<String, Object> metadata;
    
    private NodeExecutionResult(Builder builder) {
        this.nodeId = builder.nodeId;
        this.success = builder.success;
        this.data = builder.data;
        this.message = builder.message;
        this.executionTime = builder.executionTime;
        this.executionDurationMs = builder.executionDurationMs;
        this.metadata = new HashMap<>(builder.metadata);
    }
    
    /**
     * 创建成功结果
     */
    public static NodeExecutionResult success(String nodeId, Object data) {
        return new Builder(nodeId)
                .success(true)
                .data(data)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static NodeExecutionResult failure(String nodeId, String message) {
        return new Builder(nodeId)
                .success(false)
                .message(message)
                .build();
    }
    
    /**
     * 创建Builder
     */
    public static Builder builder(String nodeId) {
        return new Builder(nodeId);
    }
    
    // Getters
    public String getNodeId() { return nodeId; }
    public boolean isSuccess() { return success; }
    public Object getData() { return data; }
    public String getMessage() { return message; }
    public LocalDateTime getExecutionTime() { return executionTime; }
    public long getExecutionDurationMs() { return executionDurationMs; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    
    /**
     * Builder模式
     */
    public static class Builder {
        private final String nodeId;
        private boolean success = false;
        private Object data;
        private String message;
        private LocalDateTime executionTime = LocalDateTime.now();
        private long executionDurationMs = 0;
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder(String nodeId) {
            this.nodeId = nodeId;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder data(Object data) {
            this.data = data;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder executionTime(LocalDateTime executionTime) {
            this.executionTime = executionTime;
            return this;
        }
        
        public Builder executionDuration(long durationMs) {
            this.executionDurationMs = durationMs;
            return this;
        }
        
        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }
        
        public NodeExecutionResult build() {
            return new NodeExecutionResult(this);
        }
    }
}
