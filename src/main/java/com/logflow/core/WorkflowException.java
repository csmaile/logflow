package com.logflow.core;

/**
 * 工作流异常
 */
public class WorkflowException extends Exception {
    
    private final String nodeId;
    private final String errorCode;
    
    public WorkflowException(String message) {
        super(message);
        this.nodeId = null;
        this.errorCode = null;
    }
    
    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
        this.nodeId = null;
        this.errorCode = null;
    }
    
    public WorkflowException(String nodeId, String message) {
        super(message);
        this.nodeId = nodeId;
        this.errorCode = null;
    }
    
    public WorkflowException(String nodeId, String message, Throwable cause) {
        super(message, cause);
        this.nodeId = nodeId;
        this.errorCode = null;
    }
    
    public WorkflowException(String nodeId, String errorCode, String message) {
        super(message);
        this.nodeId = nodeId;
        this.errorCode = errorCode;
    }
    
    public WorkflowException(String nodeId, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.nodeId = nodeId;
        this.errorCode = errorCode;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
