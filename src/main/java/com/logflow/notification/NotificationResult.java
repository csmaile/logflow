package com.logflow.notification;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 通知发送结果类
 */
public class NotificationResult {

    public enum Status {
        SUCCESS, // 成功
        FAILED, // 失败
        PENDING, // 等待中
        RETRYING, // 重试中
        CANCELLED // 已取消
    }

    private final String messageId;
    private final Status status;
    private final String message;
    private final String errorCode;
    private final LocalDateTime sendTime;
    private final long executionTimeMs;
    private final Map<String, Object> details;
    private final String providerType;
    private final int retryCount;

    private NotificationResult(Builder builder) {
        this.messageId = builder.messageId;
        this.status = builder.status;
        this.message = builder.message;
        this.errorCode = builder.errorCode;
        this.sendTime = builder.sendTime;
        this.executionTimeMs = builder.executionTimeMs;
        this.details = new HashMap<>(builder.details);
        this.providerType = builder.providerType;
        this.retryCount = builder.retryCount;
    }

    // 静态工厂方法

    public static NotificationResult success(String messageId, String providerType) {
        return new Builder()
                .messageId(messageId)
                .status(Status.SUCCESS)
                .message("通知发送成功")
                .providerType(providerType)
                .sendTime(LocalDateTime.now())
                .build();
    }

    public static NotificationResult success(String messageId, String providerType, String message) {
        return new Builder()
                .messageId(messageId)
                .status(Status.SUCCESS)
                .message(message)
                .providerType(providerType)
                .sendTime(LocalDateTime.now())
                .build();
    }

    public static NotificationResult failed(String messageId, String providerType, String message) {
        return new Builder()
                .messageId(messageId)
                .status(Status.FAILED)
                .message(message)
                .providerType(providerType)
                .sendTime(LocalDateTime.now())
                .build();
    }

    public static NotificationResult failed(String messageId, String providerType, String errorCode, String message) {
        return new Builder()
                .messageId(messageId)
                .status(Status.FAILED)
                .message(message)
                .errorCode(errorCode)
                .providerType(providerType)
                .sendTime(LocalDateTime.now())
                .build();
    }

    public static NotificationResult pending(String messageId, String providerType) {
        return new Builder()
                .messageId(messageId)
                .status(Status.PENDING)
                .message("通知正在发送中")
                .providerType(providerType)
                .sendTime(LocalDateTime.now())
                .build();
    }

    // Getters

    public String getMessageId() {
        return messageId;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public LocalDateTime getSendTime() {
        return sendTime;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }

    public String getProviderType() {
        return providerType;
    }

    public int getRetryCount() {
        return retryCount;
    }

    // 便捷方法

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public boolean isRetrying() {
        return status == Status.RETRYING;
    }

    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    public Object getDetail(String key) {
        return details.get(key);
    }

    public Object getDetail(String key, Object defaultValue) {
        return details.getOrDefault(key, defaultValue);
    }

    // Builder类

    public static class Builder {
        private String messageId;
        private Status status;
        private String message;
        private String errorCode;
        private LocalDateTime sendTime;
        private long executionTimeMs;
        private Map<String, Object> details = new HashMap<>();
        private String providerType;
        private int retryCount = 0;

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

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

        public Builder sendTime(LocalDateTime sendTime) {
            this.sendTime = sendTime;
            return this;
        }

        public Builder executionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
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

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public NotificationResult build() {
            if (sendTime == null) {
                sendTime = LocalDateTime.now();
            }
            return new NotificationResult(this);
        }
    }

    @Override
    public String toString() {
        return String.format("NotificationResult{messageId='%s', status=%s, provider='%s', message='%s'}",
                messageId, status, providerType, message);
    }
}
