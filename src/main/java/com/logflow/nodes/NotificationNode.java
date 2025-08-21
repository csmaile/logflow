package com.logflow.nodes;

import com.logflow.core.*;
import com.logflow.notification.*;
import com.logflow.notification.providers.ConsoleNotificationProvider;
import com.logflow.notification.providers.DingTalkNotificationProvider;
import com.logflow.notification.providers.EmailNotificationProvider;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知节点
 * 用于发送各种类型的通知消息，替代原来的OutputNode
 */
public class NotificationNode extends AbstractWorkflowNode {

    private static final Map<String, Class<? extends NotificationProvider>> PROVIDER_REGISTRY = new ConcurrentHashMap<>();

    static {
        // 注册内置的通知提供者
        registerProvider("console", ConsoleNotificationProvider.class);
        registerProvider("email", EmailNotificationProvider.class);
        registerProvider("dingtalk", DingTalkNotificationProvider.class);
    }

    private NotificationProvider notificationProvider;

    public NotificationNode(String id, String name) {
        super(id, name, NodeType.NOTIFICATION);
    }

    /**
     * 注册通知提供者
     */
    public static void registerProvider(String type, Class<? extends NotificationProvider> providerClass) {
        PROVIDER_REGISTRY.put(type.toLowerCase(), providerClass);
    }

    /**
     * 获取已注册的提供者类型
     */
    public static String[] getRegisteredProviderTypes() {
        return PROVIDER_REGISTRY.keySet().toArray(new String[0]);
    }

    @Override
    protected NodeExecutionResult doExecute(WorkflowContext context) throws WorkflowException {
        try {
            // 初始化通知提供者（如果还未初始化）
            if (notificationProvider == null || !notificationProvider.isInitialized()) {
                initializeProvider();
            }

            // 构建通知消息
            NotificationMessage message = buildNotificationMessage(context);

            // 发送通知
            NotificationResult result = notificationProvider.sendNotification(message);

            // 保存结果到上下文
            saveResultToContext(context, result);

            if (result.isSuccess()) {
                logger.info("通知发送成功: {} ({})", message.getMessageId(),
                        notificationProvider.getProviderType());

                return NodeExecutionResult.success("通知发送成功", Map.of(
                        "messageId", message.getMessageId(),
                        "providerType", notificationProvider.getProviderType(),
                        "executionTime", result.getExecutionTimeMs(),
                        "status", result.getStatus().name()));
            } else {
                throw new WorkflowException(id, "通知发送失败: " + result.getMessage());
            }

        } catch (NotificationException e) {
            logger.error("通知节点执行失败: {}", id, e);
            throw new WorkflowException(id, "通知发送异常: " + e.getMessage(), e);
        }
    }

    @Override
    public ValidationResult validate() {
        ValidationResult.Builder builder = ValidationResult.builder();

        try {
            // 验证通知提供者类型
            String providerType = getConfigValue("providerType", String.class);
            if (providerType == null || providerType.trim().isEmpty()) {
                builder.error("通知提供者类型不能为空，支持的类型: " +
                        Arrays.toString(getRegisteredProviderTypes()));
                return builder.build();
            }

            if (!PROVIDER_REGISTRY.containsKey(providerType.toLowerCase())) {
                builder.error("不支持的通知提供者类型: " + providerType +
                        "，支持的类型: " + Arrays.toString(getRegisteredProviderTypes()));
                return builder.build();
            }

            // 创建并验证提供者配置
            NotificationProvider provider = createProvider(providerType);
            Map<String, Object> providerConfig = getProviderConfig();

            NotificationValidationResult providerValidation = provider.validateConfiguration(providerConfig);

            if (!providerValidation.isValid()) {
                for (NotificationValidationResult.ValidationError error : providerValidation.getErrors()) {
                    builder.error("provider." + error.getParameter() + ": " + error.getMessage());
                }
            }

            for (NotificationValidationResult.ValidationWarning warning : providerValidation.getWarnings()) {
                builder.warning("provider." + warning.getParameter() + ": " + warning.getMessage());
            }

            // 验证消息配置
            validateMessageConfig(builder);

        } catch (Exception e) {
            builder.error("通知节点验证失败: " + e.getMessage());
        }

        return builder.build();
    }

    /**
     * 初始化通知提供者
     */
    private void initializeProvider() throws NotificationException {
        String providerType = getConfigValue("providerType", String.class);

        if (providerType == null) {
            throw new NotificationException("通知提供者类型不能为空");
        }

        this.notificationProvider = createProvider(providerType);

        Map<String, Object> providerConfig = getProviderConfig();
        notificationProvider.initialize(providerConfig);

        logger.info("通知提供者初始化完成: {} ({})",
                notificationProvider.getProviderName(), providerType);
    }

    /**
     * 创建通知提供者实例
     */
    private NotificationProvider createProvider(String providerType) throws NotificationException {
        Class<? extends NotificationProvider> providerClass = PROVIDER_REGISTRY.get(providerType.toLowerCase());

        if (providerClass == null) {
            throw new NotificationException("不支持的通知提供者类型: " + providerType);
        }

        try {
            return providerClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new NotificationException("创建通知提供者失败: " + providerType, e);
        }
    }

    /**
     * 构建通知消息
     */
    private NotificationMessage buildNotificationMessage(WorkflowContext context) throws WorkflowException {
        // 获取数据
        String inputKey = getConfigValue("inputKey", String.class, "notification_data");
        Object data = context.getData(inputKey);

        // 构建消息
        NotificationMessage.Builder messageBuilder = new NotificationMessage.Builder();

        // 设置标题
        String title = getConfigValue("title", String.class);
        if (title != null) {
            title = replaceVariables(title, context);
        } else {
            title = "LogFlow工作流通知";
        }
        messageBuilder.title(title);

        // 设置内容
        String content = buildMessageContent(data, context);
        messageBuilder.content(content);

        // 设置消息类型
        String messageTypeStr = getConfigValue("messageType", String.class, "TEXT");
        try {
            NotificationMessage.MessageType messageType = NotificationMessage.MessageType
                    .valueOf(messageTypeStr.toUpperCase());
            messageBuilder.messageType(messageType);
        } catch (IllegalArgumentException e) {
            throw new WorkflowException(id, "无效的消息类型: " + messageTypeStr);
        }

        // 设置优先级
        String priorityStr = getConfigValue("priority", String.class, "NORMAL");
        try {
            NotificationMessage.Priority priority = NotificationMessage.Priority.valueOf(priorityStr.toUpperCase());
            messageBuilder.priority(priority);
        } catch (IllegalArgumentException e) {
            throw new WorkflowException(id, "无效的优先级: " + priorityStr);
        }

        // 设置收件人
        @SuppressWarnings("unchecked")
        List<String> recipients = (List<String>) getConfigValue("recipients", List.class);
        if (recipients != null && !recipients.isEmpty()) {
            messageBuilder.recipients(recipients);
        }

        // 设置抄送
        @SuppressWarnings("unchecked")
        List<String> ccRecipients = (List<String>) getConfigValue("ccRecipients", List.class);
        if (ccRecipients != null && !ccRecipients.isEmpty()) {
            messageBuilder.ccRecipients(ccRecipients);
        }

        // 设置变量
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) getConfigValue("variables", Map.class);
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                messageBuilder.variable(entry.getKey(), entry.getValue());
            }
        }

        // 添加上下文变量
        addContextVariables(messageBuilder, context);

        // 设置模板ID
        String templateId = getConfigValue("templateId", String.class);
        if (templateId != null) {
            messageBuilder.templateId(templateId);
        }

        // 设置定时发送
        String scheduleTimeStr = getConfigValue("scheduleTime", String.class);
        if (scheduleTimeStr != null) {
            try {
                LocalDateTime scheduleTime = LocalDateTime.parse(scheduleTimeStr);
                messageBuilder.scheduleTime(scheduleTime);
            } catch (Exception e) {
                logger.warn("无效的定时发送时间格式: {}", scheduleTimeStr);
            }
        }

        return messageBuilder.build();
    }

    /**
     * 构建消息内容
     */
    private String buildMessageContent(Object data, WorkflowContext context) {
        String contentTemplate = getConfigValue("contentTemplate", String.class);

        if (contentTemplate != null) {
            // 使用内容模板
            return replaceVariables(contentTemplate, context);
        } else {
            // 使用默认格式
            StringBuilder content = new StringBuilder();
            content.append("工作流执行通知\n");
            content.append("工作流ID: ").append(context.getWorkflowId()).append("\n");
            content.append("执行ID: ").append(context.getExecutionId()).append("\n");
            content.append("执行时间: ").append(context.getStartTime()).append("\n");

            if (data != null) {
                content.append("数据内容: ").append(data.toString());
            }

            return content.toString();
        }
    }

    /**
     * 添加上下文变量
     */
    private void addContextVariables(NotificationMessage.Builder messageBuilder, WorkflowContext context) {
        messageBuilder.variable("workflowId", context.getWorkflowId());
        messageBuilder.variable("executionId", context.getExecutionId());
        messageBuilder.variable("startTime", context.getStartTime().toString());
        messageBuilder.variable("nodeId", id);
        messageBuilder.variable("nodeName", name);

        // 添加所有上下文数据作为变量
        Map<String, Object> contextData = context.getAllData();
        for (Map.Entry<String, Object> entry : contextData.entrySet()) {
            messageBuilder.variable("ctx." + entry.getKey(), entry.getValue());
        }
    }

    /**
     * 替换变量
     */
    private String replaceVariables(String template, WorkflowContext context) {
        String result = template;

        // 替换基本变量
        result = result.replace("${workflowId}", context.getWorkflowId());
        result = result.replace("${executionId}", context.getExecutionId());
        result = result.replace("${nodeId}", id);
        result = result.replace("${nodeName}", name);
        result = result.replace("${startTime}", context.getStartTime().toString());

        // 替换上下文数据变量
        Map<String, Object> contextData = context.getAllData();
        for (Map.Entry<String, Object> entry : contextData.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * 保存结果到上下文
     */
    private void saveResultToContext(WorkflowContext context, NotificationResult result) {
        String outputKey = getConfigValue("outputKey", String.class, "notification_result");

        Map<String, Object> resultData = Map.of(
                "messageId", result.getMessageId(),
                "status", result.getStatus().name(),
                "success", result.isSuccess(),
                "message", result.getMessage(),
                "providerType", result.getProviderType(),
                "executionTime", result.getExecutionTimeMs(),
                "sendTime", result.getSendTime().toString());

        context.setData(outputKey, resultData);
    }

    /**
     * 获取提供者配置
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getProviderConfig() {
        Map<String, Object> providerConfig = (Map<String, Object>) getConfigValue("providerConfig", Map.class);

        return providerConfig != null ? providerConfig : Map.of();
    }

    /**
     * 验证消息配置
     */
    private void validateMessageConfig(ValidationResult.Builder builder) {
        // 验证消息类型
        String messageType = getConfigValue("messageType", String.class, "TEXT");
        try {
            NotificationMessage.MessageType.valueOf(messageType.toUpperCase());
        } catch (IllegalArgumentException e) {
            builder.error("无效的消息类型: " + messageType +
                    "，支持的类型: TEXT, HTML, MARKDOWN, JSON, TEMPLATE");
        }

        // 验证优先级
        String priority = getConfigValue("priority", String.class, "NORMAL");
        try {
            NotificationMessage.Priority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            builder.error("无效的优先级: " + priority +
                    "，支持的优先级: LOW, NORMAL, HIGH, URGENT");
        }

        // 验证收件人（如果需要）
        String providerType = getConfigValue("providerType", String.class);
        if ("email".equals(providerType)) {
            @SuppressWarnings("unchecked")
            List<String> recipients = (List<String>) getConfigValue("recipients", List.class);
            if (recipients == null || recipients.isEmpty()) {
                builder.warning("邮件通知建议配置收件人列表");
            }
        }
    }

    /**
     * 测试通知连接
     */
    public NotificationTestResult testNotification() {
        try {
            if (notificationProvider == null || !notificationProvider.isInitialized()) {
                initializeProvider();
            }

            return notificationProvider.testConnection();

        } catch (Exception e) {
            return NotificationTestResult.failed("unknown", "测试失败: " + e.getMessage());
        }
    }

    /**
     * 获取提供者指标
     */
    public Map<String, Object> getProviderMetrics() {
        if (notificationProvider instanceof AbstractNotificationProvider) {
            return ((AbstractNotificationProvider) notificationProvider).getMetrics();
        }
        return Map.of();
    }

    /**
     * 销毁通知提供者
     */
    public void destroy() {
        if (notificationProvider != null) {
            notificationProvider.destroy();
            notificationProvider = null;
        }
    }
}
