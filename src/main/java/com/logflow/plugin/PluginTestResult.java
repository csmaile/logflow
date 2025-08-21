package com.logflow.plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * 插件连接测试结果
 */
public class PluginTestResult {

    private boolean success;
    private String message;
    private long responseTime;
    private Map<String, Object> details;
    private Exception exception;

    public PluginTestResult() {
        this.details = new HashMap<>();
    }

    public static PluginTestResult success(String message) {
        PluginTestResult result = new PluginTestResult();
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

    public static PluginTestResult success(String message, long responseTime) {
        PluginTestResult result = success(message);
        result.setResponseTime(responseTime);
        return result;
    }

    public static PluginTestResult failure(String message) {
        PluginTestResult result = new PluginTestResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    public static PluginTestResult failure(String message, Exception exception) {
        PluginTestResult result = failure(message);
        result.setException(exception);
        return result;
    }

    public static PluginTestResult failure(Exception exception) {
        return failure(exception.getMessage(), exception);
    }

    public PluginTestResult withDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }

    public PluginTestResult withResponseTime(long responseTime) {
        this.responseTime = responseTime;
        return this;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PluginTestResult{");
        sb.append("success=").append(success);
        sb.append(", message='").append(message).append('\'');
        if (responseTime > 0) {
            sb.append(", responseTime=").append(responseTime).append("ms");
        }
        if (!details.isEmpty()) {
            sb.append(", details=").append(details);
        }
        sb.append('}');
        return sb.toString();
    }
}
