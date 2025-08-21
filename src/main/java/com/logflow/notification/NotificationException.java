package com.logflow.notification;

/**
 * 通知异常类
 */
public class NotificationException extends Exception {

    private final String providerType;
    private final String errorCode;

    public NotificationException(String message) {
        super(message);
        this.providerType = null;
        this.errorCode = null;
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
        this.providerType = null;
        this.errorCode = null;
    }

    public NotificationException(String providerType, String message) {
        super(message);
        this.providerType = providerType;
        this.errorCode = null;
    }

    public NotificationException(String providerType, String errorCode, String message) {
        super(message);
        this.providerType = providerType;
        this.errorCode = errorCode;
    }

    public NotificationException(String providerType, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.providerType = providerType;
        this.errorCode = errorCode;
    }

    public String getProviderType() {
        return providerType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("NotificationException");
        if (providerType != null) {
            sb.append("[").append(providerType).append("]");
        }
        if (errorCode != null) {
            sb.append("(").append(errorCode).append(")");
        }
        sb.append(": ").append(getMessage());
        return sb.toString();
    }
}
