package com.logflow.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 抽象通知提供者基类
 * 提供通用功能和模板方法
 */
public abstract class AbstractNotificationProvider implements NotificationProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Map<String, Object> config;
    protected volatile boolean initialized = false;
    private final Map<String, Object> metrics = new ConcurrentHashMap<>();

    public AbstractNotificationProvider() {
        this.config = new ConcurrentHashMap<>();
    }

    @Override
    public void initialize(Map<String, Object> config) throws NotificationException {
        logger.info("初始化通知提供者: {}", getProviderType());

        try {
            // 验证配置
            NotificationValidationResult validation = validateConfiguration(config);
            if (!validation.isValid()) {
                StringBuilder errorMsg = new StringBuilder("配置验证失败: ");
                validation.getErrors().forEach(error -> errorMsg.append(error.getParameter()).append(": ")
                        .append(error.getMessage()).append("; "));
                throw new NotificationException(getProviderType(), errorMsg.toString());
            }

            // 记录警告
            if (validation.hasWarnings()) {
                validation.getWarnings()
                        .forEach(warning -> logger.warn("配置警告 [{}]: {}", warning.getParameter(), warning.getMessage()));
            }

            // 保存配置
            this.config = new ConcurrentHashMap<>(config);

            // 执行具体的初始化逻辑
            doInitialize();

            this.initialized = true;

            // 初始化指标
            resetMetrics();

            logger.info("通知提供者初始化完成: {}", getProviderType());

        } catch (Exception e) {
            logger.error("通知提供者初始化失败: {}", getProviderType(), e);
            this.initialized = false;
            if (e instanceof NotificationException) {
                throw e;
            } else {
                throw new NotificationException(getProviderType(), "INIT_FAILED", "初始化失败", e);
            }
        }
    }

    @Override
    public NotificationResult sendNotification(NotificationMessage notification) throws NotificationException {
        if (!initialized) {
            throw new NotificationException(getProviderType(), "提供者未初始化");
        }

        logger.debug("发送通知: {} ({})", notification.getTitle(), notification.getMessageId());

        long startTime = System.currentTimeMillis();

        try {
            // 增加发送计数
            incrementMetric("total_attempts");

            // 验证消息
            validateMessage(notification);

            // 执行具体的发送逻辑
            NotificationResult result = doSendNotification(notification);

            long executionTime = System.currentTimeMillis() - startTime;

            // 更新指标
            if (result.isSuccess()) {
                incrementMetric("successful_sends");
                updateMetric("avg_execution_time", executionTime);
            } else {
                incrementMetric("failed_sends");
            }

            logger.debug("通知发送完成: {} - {} ({}ms)",
                    notification.getMessageId(), result.getStatus(), executionTime);

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            incrementMetric("failed_sends");

            logger.error("通知发送失败: {} ({}ms)", notification.getMessageId(), executionTime, e);

            if (e instanceof NotificationException) {
                throw e;
            } else {
                throw new NotificationException(getProviderType(), "SEND_FAILED", "发送失败", e);
            }
        }
    }

    @Override
    public NotificationTestResult testConnection() {
        logger.debug("测试连接: {}", getProviderType());

        if (!initialized) {
            return NotificationTestResult.failed(getProviderType(), "提供者未初始化");
        }

        long startTime = System.currentTimeMillis();

        try {
            NotificationTestResult result = doTestConnection();
            long responseTime = System.currentTimeMillis() - startTime;

            logger.debug("连接测试完成: {} - {} ({}ms)",
                    getProviderType(), result.getStatus(), responseTime);

            return result;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("连接测试失败: {} ({}ms)", getProviderType(), responseTime, e);

            return NotificationTestResult.failed(getProviderType(), "测试异常: " + e.getMessage());
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void destroy() {
        logger.info("销毁通知提供者: {}", getProviderType());

        try {
            doDestroy();
            this.initialized = false;
            this.config.clear();
            this.metrics.clear();

            logger.info("通知提供者已销毁: {}", getProviderType());

        } catch (Exception e) {
            logger.error("销毁通知提供者时发生异常: {}", getProviderType(), e);
        }
    }

    // 抽象方法 - 子类需要实现

    /**
     * 执行具体的初始化逻辑
     */
    protected abstract void doInitialize() throws NotificationException;

    /**
     * 执行具体的发送逻辑
     */
    protected abstract NotificationResult doSendNotification(NotificationMessage notification)
            throws NotificationException;

    /**
     * 执行具体的连接测试逻辑
     */
    protected abstract NotificationTestResult doTestConnection();

    /**
     * 执行具体的销毁逻辑
     */
    protected abstract void doDestroy();

    // 辅助方法

    /**
     * 验证消息
     */
    protected void validateMessage(NotificationMessage notification) throws NotificationException {
        if (notification == null) {
            throw new NotificationException(getProviderType(), "通知消息不能为空");
        }

        if (notification.getTitle() == null || notification.getTitle().trim().isEmpty()) {
            throw new NotificationException(getProviderType(), "通知标题不能为空");
        }

        if (notification.getContent() == null || notification.getContent().trim().isEmpty()) {
            throw new NotificationException(getProviderType(), "通知内容不能为空");
        }

        // 检查支持的消息类型
        String[] supportedTypes = getSupportedMessageTypes();
        if (supportedTypes != null && supportedTypes.length > 0) {
            boolean typeSupported = false;
            String messageType = notification.getMessageType().name();

            for (String supportedType : supportedTypes) {
                if (supportedType.equalsIgnoreCase(messageType)) {
                    typeSupported = true;
                    break;
                }
            }

            if (!typeSupported) {
                throw new NotificationException(getProviderType(),
                        "不支持的消息类型: " + messageType);
            }
        }
    }

    /**
     * 获取配置值
     */
    protected <T> T getConfigValue(String key, Class<T> type) {
        Object value = config.get(key);
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return type.cast(value);
        }

        // 尝试字符串转换
        if (type == String.class) {
            return type.cast(value.toString());
        }

        // 尝试数值转换
        if (value instanceof String) {
            String strValue = (String) value;
            if (type == Integer.class || type == int.class) {
                return type.cast(Integer.parseInt(strValue));
            } else if (type == Long.class || type == long.class) {
                return type.cast(Long.parseLong(strValue));
            } else if (type == Boolean.class || type == boolean.class) {
                return type.cast(Boolean.parseBoolean(strValue));
            } else if (type == Double.class || type == double.class) {
                return type.cast(Double.parseDouble(strValue));
            }
        }

        return null;
    }

    /**
     * 获取配置值（带默认值）
     */
    protected <T> T getConfigValue(String key, Class<T> type, T defaultValue) {
        T value = getConfigValue(key, type);
        return value != null ? value : defaultValue;
    }

    /**
     * 增加指标计数
     */
    protected void incrementMetric(String metricName) {
        metrics.merge(metricName, 1L, (oldValue, newValue) -> (Long) oldValue + (Long) newValue);
    }

    /**
     * 更新指标值
     */
    protected void updateMetric(String metricName, Object value) {
        if ("avg_execution_time".equals(metricName)) {
            // 计算平均值
            Long totalTime = (Long) metrics.getOrDefault("total_execution_time", 0L);
            Long count = (Long) metrics.getOrDefault("execution_count", 0L);

            totalTime += (Long) value;
            count++;

            metrics.put("total_execution_time", totalTime);
            metrics.put("execution_count", count);
            metrics.put("avg_execution_time", totalTime / count);
        } else {
            metrics.put(metricName, value);
        }
    }

    /**
     * 获取指标值
     */
    protected Object getMetric(String metricName) {
        return metrics.get(metricName);
    }

    /**
     * 获取所有指标
     */
    public Map<String, Object> getMetrics() {
        return new ConcurrentHashMap<>(metrics);
    }

    /**
     * 重置指标
     */
    protected void resetMetrics() {
        metrics.clear();
        metrics.put("total_attempts", 0L);
        metrics.put("successful_sends", 0L);
        metrics.put("failed_sends", 0L);
        metrics.put("total_execution_time", 0L);
        metrics.put("execution_count", 0L);
        metrics.put("avg_execution_time", 0L);
    }

    @Override
    public String toString() {
        return String.format("%s{type='%s', initialized=%s}",
                getClass().getSimpleName(), getProviderType(), initialized);
    }
}
