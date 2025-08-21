package com.logflow.notification.providers;

import com.logflow.notification.*;
import com.logflow.core.WorkflowContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * 上下文通知提供者
 * 将通知消息保存到工作流上下文中
 */
public class ContextNotificationProvider extends AbstractNotificationProvider {

    private static final String PROVIDER_TYPE = "context";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String contextKey;
    private String dataFormat;
    private boolean overwrite;
    private WorkflowContext workflowContext;

    @Override
    public String getProviderType() {
        return PROVIDER_TYPE;
    }

    @Override
    public String getProviderName() {
        return "上下文通知";
    }

    @Override
    public String getProviderDescription() {
        return "将通知消息保存到工作流上下文中，支持多种数据格式";
    }

    @Override
    public String[] getSupportedMessageTypes() {
        return new String[] { "TEXT", "JSON", "TEMPLATE" };
    }

    /**
     * 设置工作流上下文
     * 这个方法需要在初始化后调用
     */
    public void setWorkflowContext(WorkflowContext context) {
        this.workflowContext = context;
    }

    @Override
    protected void doInitialize() throws NotificationException {
        // 必需配置
        this.contextKey = getConfigValue("contextKey", String.class);
        if (contextKey == null || contextKey.trim().isEmpty()) {
            throw new NotificationException(PROVIDER_TYPE, "CONFIG_MISSING", "上下文键(contextKey)是必需的配置", null);
        }

        // 可选配置
        this.dataFormat = getConfigValue("dataFormat", String.class, "content"); // content, message, custom
        this.overwrite = getConfigValue("overwrite", Boolean.class, true);

        logger.info("上下文通知提供者初始化完成 - 键: {}, 格式: {}, 覆盖: {}",
                contextKey, dataFormat, overwrite);
    }

    @Override
    protected NotificationResult doSendNotification(NotificationMessage message) throws NotificationException {
        if (workflowContext == null) {
            throw new NotificationException(PROVIDER_TYPE, "CONTEXT_NOT_SET", "工作流上下文未设置", null);
        }

        try {
            Object dataToStore = formatDataForContext(message);

            // 检查是否已存在数据
            if (!overwrite && workflowContext.hasData(contextKey)) {
                logger.warn("上下文键 {} 已存在数据，跳过写入（overwrite=false）", contextKey);
                return new NotificationResult.Builder()
                        .messageId(message.getMessageId())
                        .providerType(PROVIDER_TYPE)
                        .status(NotificationResult.Status.SUCCESS)
                        .message("数据已存在，跳过写入")
                        .build();
            }

            // 保存到上下文
            workflowContext.setData(contextKey, dataToStore);

            logger.info("数据已保存到上下文, 键: {}, 格式: {}", contextKey, dataFormat);

            return new NotificationResult.Builder()
                    .messageId(message.getMessageId())
                    .providerType(PROVIDER_TYPE)
                    .status(NotificationResult.Status.SUCCESS)
                    .message("数据已保存到上下文: " + contextKey)
                    .build();

        } catch (Exception e) {
            throw new NotificationException(PROVIDER_TYPE, "CONTEXT_SAVE_FAILED", "保存到上下文失败", e);
        }
    }

    @Override
    public NotificationValidationResult validateConfiguration(Map<String, Object> config) {
        List<NotificationValidationResult.ValidationError> errors = new java.util.ArrayList<>();
        List<NotificationValidationResult.ValidationWarning> warnings = new java.util.ArrayList<>();

        // 验证上下文键
        String key = getConfigValue("contextKey", String.class);
        if (key == null || key.trim().isEmpty()) {
            errors.add(new NotificationValidationResult.ValidationError("contextKey", "上下文键是必需的"));
        }

        // 验证数据格式
        String format = getConfigValue("dataFormat", String.class, "content");
        if (!java.util.List.of("content", "message", "custom").contains(format.toLowerCase())) {
            warnings.add(new NotificationValidationResult.ValidationWarning("dataFormat",
                    "不支持的数据格式，支持: content, message, custom"));
        }

        return new NotificationValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    protected NotificationTestResult doTestConnection() {
        long startTime = System.currentTimeMillis();

        if (workflowContext == null) {
            return new NotificationTestResult.Builder()
                    .status(NotificationTestResult.Status.FAILED)
                    .message("工作流上下文未设置")
                    .providerType(PROVIDER_TYPE)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        try {
            // 测试写入和读取
            String testKey = contextKey + "_test";
            String testValue = "LogFlow上下文通知测试";

            workflowContext.setData(testKey, testValue);
            String retrieved = workflowContext.getData(testKey);

            // 清理测试数据
            workflowContext.removeData(testKey);

            if (testValue.equals(retrieved)) {
                return new NotificationTestResult.Builder()
                        .status(NotificationTestResult.Status.SUCCESS)
                        .message("上下文读写测试成功")
                        .providerType(PROVIDER_TYPE)
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            } else {
                return new NotificationTestResult.Builder()
                        .status(NotificationTestResult.Status.FAILED)
                        .message("上下文读写测试失败：数据不匹配")
                        .providerType(PROVIDER_TYPE)
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

        } catch (Exception e) {
            return new NotificationTestResult.Builder()
                    .status(NotificationTestResult.Status.FAILED)
                    .message("上下文测试失败: " + e.getMessage())
                    .providerType(PROVIDER_TYPE)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    protected void doDestroy() {
        this.workflowContext = null;
        logger.info("上下文通知提供者已销毁");
    }

    /**
     * 根据配置格式化数据
     */
    private Object formatDataForContext(NotificationMessage message) {
        switch (dataFormat.toLowerCase()) {
            case "content":
                // 只保存消息内容
                return message.getContent();

            case "message":
                // 保存完整的消息对象信息
                return Map.of(
                        "messageId", message.getMessageId(),
                        "title", message.getTitle() != null ? message.getTitle() : "",
                        "content", message.getContent() != null ? message.getContent() : "",
                        "priority", message.getPriority().name(),
                        "messageType", message.getMessageType().name(),
                        "recipients", message.getRecipients(),
                        "createdAt", java.time.LocalDateTime.now(),
                        "metadata", message.getMetadata());

            case "custom":
            default:
                // 自定义格式，可以通过配置指定具体字段
                Map<String, Object> customData = new java.util.HashMap<>();

                // 检查配置中是否指定了要保存的字段
                @SuppressWarnings("unchecked")
                java.util.List<String> fields = getConfigValue("includeFields", java.util.List.class);

                if (fields == null || fields.isEmpty()) {
                    // 默认包含主要字段
                    customData.put("messageId", message.getMessageId());
                    customData.put("content", message.getContent());
                    customData.put("priority", message.getPriority().name());
                } else {
                    // 根据配置包含指定字段
                    for (String field : fields) {
                        switch (field.toLowerCase()) {
                            case "messageid":
                                customData.put("messageId", message.getMessageId());
                                break;
                            case "title":
                                customData.put("title", message.getTitle());
                                break;
                            case "content":
                                customData.put("content", message.getContent());
                                break;
                            case "priority":
                                customData.put("priority", message.getPriority().name());
                                break;
                            case "messagetype":
                                customData.put("messageType", message.getMessageType().name());
                                break;
                            case "recipients":
                                customData.put("recipients", message.getRecipients());
                                break;
                            case "createdat":
                                customData.put("createdAt", java.time.LocalDateTime.now());
                                break;
                            case "metadata":
                                customData.put("metadata", message.getMetadata());
                                break;
                        }
                    }
                }

                return customData;
        }
    }
}
