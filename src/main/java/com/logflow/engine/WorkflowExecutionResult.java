package com.logflow.engine;

import com.logflow.core.NodeExecutionResult;
import com.logflow.core.WorkflowContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流执行结果
 */
public class WorkflowExecutionResult {
    
    private final String executionId;
    private final boolean success;
    private final String message;
    private final Map<String, NodeExecutionResult> nodeResults;
    private final WorkflowContext context;
    private final LocalDateTime executionTime;
    private final long executionDurationMs;
    private final Map<String, Object> metadata;
    
    private WorkflowExecutionResult(Builder builder) {
        this.executionId = builder.executionId;
        this.success = builder.success;
        this.message = builder.message;
        this.nodeResults = new HashMap<>(builder.nodeResults);
        this.context = builder.context;
        this.executionTime = builder.executionTime;
        this.executionDurationMs = builder.executionDurationMs;
        this.metadata = new HashMap<>(builder.metadata);
    }
    
    /**
     * 创建成功结果
     */
    public static WorkflowExecutionResult success(String executionId, WorkflowContext context) {
        return new Builder(executionId)
                .success(true)
                .context(context)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static WorkflowExecutionResult failure(String executionId, String message) {
        return new Builder(executionId)
                .success(false)
                .message(message)
                .build();
    }
    
    /**
     * 创建Builder
     */
    public static Builder builder(String executionId) {
        return new Builder(executionId);
    }
    
    // Getters
    public String getExecutionId() { return executionId; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Map<String, NodeExecutionResult> getNodeResults() { return new HashMap<>(nodeResults); }
    public WorkflowContext getContext() { return context; }
    public LocalDateTime getExecutionTime() { return executionTime; }
    public long getExecutionDurationMs() { return executionDurationMs; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    
    /**
     * 获取执行统计信息
     */
    public ExecutionStatistics getStatistics() {
        int totalNodes = nodeResults.size();
        int successfulNodes = (int) nodeResults.values().stream()
                .filter(NodeExecutionResult::isSuccess)
                .count();
        int failedNodes = totalNodes - successfulNodes;
        
        long totalExecutionTime = nodeResults.values().stream()
                .mapToLong(NodeExecutionResult::getExecutionDurationMs)
                .sum();
        
        return new ExecutionStatistics(totalNodes, successfulNodes, failedNodes, 
                totalExecutionTime, executionDurationMs);
    }
    
    /**
     * 获取失败的节点结果
     */
    public Map<String, NodeExecutionResult> getFailedNodeResults() {
        Map<String, NodeExecutionResult> failedResults = new HashMap<>();
        for (Map.Entry<String, NodeExecutionResult> entry : nodeResults.entrySet()) {
            if (!entry.getValue().isSuccess()) {
                failedResults.put(entry.getKey(), entry.getValue());
            }
        }
        return failedResults;
    }
    
    /**
     * Builder模式
     */
    public static class Builder {
        private final String executionId;
        private boolean success = false;
        private String message;
        private Map<String, NodeExecutionResult> nodeResults = new HashMap<>();
        private WorkflowContext context;
        private LocalDateTime executionTime = LocalDateTime.now();
        private long executionDurationMs = 0;
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder(String executionId) {
            this.executionId = executionId;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder nodeResults(Map<String, NodeExecutionResult> nodeResults) {
            this.nodeResults = new HashMap<>(nodeResults);
            return this;
        }
        
        public Builder nodeResult(String nodeId, NodeExecutionResult result) {
            this.nodeResults.put(nodeId, result);
            return this;
        }
        
        public Builder context(WorkflowContext context) {
            this.context = context;
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
        
        public WorkflowExecutionResult build() {
            return new WorkflowExecutionResult(this);
        }
    }
    
    /**
     * 执行统计信息
     */
    public static class ExecutionStatistics {
        private final int totalNodes;
        private final int successfulNodes;
        private final int failedNodes;
        private final long totalNodeExecutionTime;
        private final long workflowExecutionTime;
        
        public ExecutionStatistics(int totalNodes, int successfulNodes, int failedNodes,
                                 long totalNodeExecutionTime, long workflowExecutionTime) {
            this.totalNodes = totalNodes;
            this.successfulNodes = successfulNodes;
            this.failedNodes = failedNodes;
            this.totalNodeExecutionTime = totalNodeExecutionTime;
            this.workflowExecutionTime = workflowExecutionTime;
        }
        
        public int getTotalNodes() { return totalNodes; }
        public int getSuccessfulNodes() { return successfulNodes; }
        public int getFailedNodes() { return failedNodes; }
        public long getTotalNodeExecutionTime() { return totalNodeExecutionTime; }
        public long getWorkflowExecutionTime() { return workflowExecutionTime; }
        
        public double getSuccessRate() {
            return totalNodes > 0 ? (double) successfulNodes / totalNodes * 100 : 0;
        }
        
        public double getAverageNodeExecutionTime() {
            return totalNodes > 0 ? (double) totalNodeExecutionTime / totalNodes : 0;
        }
    }
}
