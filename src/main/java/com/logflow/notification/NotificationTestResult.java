package com.logflow.notification;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 通知连接测试结果类
 */
public class NotificationTestResult {

    public enum Status {
        SUCCESS, // 测试成功
        FAILED, // 测试失败
        TIMEOUT, // 测试超时
        UNAUTHORIZED, // 认证失败
        UNAVAILABLE // 服务不可用
    }

    private final Status status;
    private final String message;
    private final String errorCode;
    private final LocalDateTime testTime;
    private final long responseTimeMs;
    private final Map<String, Object> details;
    private final String providerType;

    private NotificationTestResult(Builder builder) {
        this.status = builder.status;
        this.message = builder.message;
        this.errorCode = builder.errorCode;
        this.testTime = builder.testTime;
        this.responseTimeMs = builder.responseTimeMs;
        this.details = new HashMap<>(builder.details);
        this.providerType = builder.providerType;
    }

    // 静态工厂方法

    public static NotificationTestResult success(String providerType) {
        return new Builder()
                .status(Status.SUCCESS)
                .message("连接测试成功")
                .providerType(providerType)
                .testTime(LocalDateTime.now())
                .build();
    }

    public static NotificationTestResult success(String providerType, String message) {
        return new Builder()
                .status(Status.SUCCESS)
                .message(message)
                .providerType(providerType)
                .testTime(LocalDateTime.now())
                .build();
    }

    public static NotificationTestResult failed(String providerType, String message) {
        return new Builder()
                .status(Status.FAILED)
                .message(message)
                .providerType(providerType)
                .testTime(LocalDateTime.now())
                .build();
    }

    public static NotificationTestResult failed(String providerType, String errorCode, String message) {
        return new Builder()
                .status(Status.FAILED)
                .message(message)
                .errorCode(errorCode)
                .providerType(providerType)
                .testTime(LocalDateTime.now())
                .build();
    }

    public static NotificationTestResult timeout(String providerType) {
        return new Builder()
                .status(Status.TIMEOUT)
                .message("连接测试超时")
                .providerType(providerType)
                .testTime(LocalDateTime.now())
                .build();
    }

    public static NotificationTestResult unauthorized(String providerType) {
        return new Builder()
                .status(Status.UNAUTHORIZED)
                .message("认证失败")
                .errorCode("AUTH_FAILED")
                .providerType(providerType)
                .testTime(LocalDateTime.now())
                .build();
    }

    public static NotificationTestResult unavailable(String providerType) {
        return new Builder()
                .status(Status.UNAVAILABLE)
                .message("服务不可用")
                .providerType(providerType)
                .testTime(LocalDateTime.now())
                .build();
    }

    // Getters

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public LocalDateTime getTestTime() {
        return testTime;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }

    public String getProviderType() {
        return providerType;
    }

    // 便捷方法

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }

    public boolean isTimeout() {
        return status == Status.TIMEOUT;
    }

    public boolean isUnauthorized() {
        return status == Status.UNAUTHORIZED;
    }

    public boolean isUnavailable() {
        return status == Status.UNAVAILABLE;
    }

    public Object getDetail(String key) {
        return details.get(key);
    }

    public Object getDetail(String key, Object defaultValue) {
        return details.getOrDefault(key, defaultValue);
    }

    // Builder类

    public static class Builder {
        private Status status;
        private String message;
        private String errorCode;
        private LocalDateTime testTime;
        private long responseTimeMs;
        private Map<String, Object> details = new HashMap<>();
        private String providerType;

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder testTime(LocalDateTime testTime) {
            this.testTime = testTime;
            return this;
        }

        public Builder responseTimeMs(long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
            return this;
        }

        public Builder detail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details.putAll(details);
            return this;
        }

        public Builder providerType(String providerType) {
            this.providerType = providerType;
            return this;
        }

        public NotificationTestResult build() {
            if (testTime == null) {
                testTime = LocalDateTime.now();
            }
            return new NotificationTestResult(this);
        }
    }

    @Override
    public String toString() {
        return String.format("NotificationTestResult{status=%s, provider='%s', responseTime=%dms, message='%s'}",
                status, providerType, responseTimeMs, message);
    }
}
