package com.logflow.plugin;

/**
 * 插件异常类
 * 用于表示插件相关的错误
 */
public class PluginException extends Exception {

    private String pluginId;
    private String errorCode;

    public PluginException(String message) {
        super(message);
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginException(String pluginId, String message) {
        super(message);
        this.pluginId = pluginId;
    }

    public PluginException(String pluginId, String message, Throwable cause) {
        super(message, cause);
        this.pluginId = pluginId;
    }

    public PluginException(String pluginId, String errorCode, String message) {
        super(message);
        this.pluginId = pluginId;
        this.errorCode = errorCode;
    }

    public PluginException(String pluginId, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.pluginId = pluginId;
        this.errorCode = errorCode;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PluginException");

        if (pluginId != null) {
            sb.append(" [").append(pluginId).append("]");
        }

        if (errorCode != null) {
            sb.append(" (").append(errorCode).append(")");
        }

        sb.append(": ").append(getMessage());

        return sb.toString();
    }

    // 常用错误代码常量
    public static final String ERROR_PLUGIN_NOT_FOUND = "PLUGIN_NOT_FOUND";
    public static final String ERROR_PLUGIN_INIT_FAILED = "PLUGIN_INIT_FAILED";
    public static final String ERROR_CONNECTION_FAILED = "CONNECTION_FAILED";
    public static final String ERROR_INVALID_CONFIG = "INVALID_CONFIG";
    public static final String ERROR_READ_FAILED = "READ_FAILED";
    public static final String ERROR_WRITE_FAILED = "WRITE_FAILED";
    public static final String ERROR_PLUGIN_LOAD_FAILED = "PLUGIN_LOAD_FAILED";
    public static final String ERROR_OPERATION_NOT_SUPPORTED = "OPERATION_NOT_SUPPORTED";

    // 便捷创建方法
    public static PluginException pluginNotFound(String pluginId) {
        return new PluginException(pluginId, ERROR_PLUGIN_NOT_FOUND,
                "Plugin not found: " + pluginId);
    }

    public static PluginException connectionFailed(String pluginId, String message) {
        return new PluginException(pluginId, ERROR_CONNECTION_FAILED,
                "Connection failed: " + message);
    }

    public static PluginException connectionFailed(String pluginId, String message, Throwable cause) {
        return new PluginException(pluginId, ERROR_CONNECTION_FAILED,
                "Connection failed: " + message, cause);
    }

    public static PluginException invalidConfig(String pluginId, String message) {
        return new PluginException(pluginId, ERROR_INVALID_CONFIG,
                "Invalid configuration: " + message);
    }

    public static PluginException readFailed(String pluginId, String message) {
        return new PluginException(pluginId, ERROR_READ_FAILED,
                "Read operation failed: " + message);
    }

    public static PluginException readFailed(String pluginId, String message, Throwable cause) {
        return new PluginException(pluginId, ERROR_READ_FAILED,
                "Read operation failed: " + message, cause);
    }

    public static PluginException operationNotSupported(String pluginId, String operation) {
        return new PluginException(pluginId, ERROR_OPERATION_NOT_SUPPORTED,
                "Operation not supported: " + operation);
    }
}
