package com.logflow.notification.providers;

import com.logflow.notification.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 控制台通知提供者
 * 将通知输出到控制台，主要用于测试和演示
 */
public class ConsoleNotificationProvider extends AbstractNotificationProvider {

    private String outputFormat;
    private boolean showTimestamp;
    private boolean showPriority;
    private String timestampFormat;

    @Override
    public String getProviderType() {
        return "console";
    }

    @Override
    public String getProviderName() {
        return "控制台通知";
    }

    @Override
    public String getProviderDescription() {
        return "将通知消息输出到控制台，支持多种格式化选项";
    }

    @Override
    public String[] getSupportedMessageTypes() {
        return new String[] { "TEXT", "MARKDOWN", "JSON" };
    }

    @Override
    protected void doInitialize() throws NotificationException {
        // 加载配置
        this.outputFormat = getConfigValue("outputFormat", String.class, "simple");
        this.showTimestamp = getConfigValue("showTimestamp", Boolean.class, true);
        this.showPriority = getConfigValue("showPriority", Boolean.class, true);
        this.timestampFormat = getConfigValue("timestampFormat", String.class, "yyyy-MM-dd HH:mm:ss");

        logger.info("控制台通知提供者初始化完成 - 格式: {}, 时间戳: {}, 优先级: {}",
                outputFormat, showTimestamp, showPriority);
    }

    @Override
    protected NotificationResult doSendNotification(NotificationMessage notification) throws NotificationException {
        long startTime = System.currentTimeMillis();

        try {
            String formattedMessage = formatMessage(notification);

            // 根据优先级选择输出方式
            if (notification.getPriority() == NotificationMessage.Priority.URGENT ||
                    notification.getPriority() == NotificationMessage.Priority.HIGH) {
                System.err.println(formattedMessage); // 高优先级用错误输出
            } else {
                System.out.println(formattedMessage); // 普通优先级用标准输出
            }

            long executionTime = System.currentTimeMillis() - startTime;

            return new NotificationResult.Builder()
                    .messageId(notification.getMessageId())
                    .status(NotificationResult.Status.SUCCESS)
                    .message("控制台通知发送成功")
                    .providerType(getProviderType())
                    .executionTimeMs(executionTime)
                    .detail("outputFormat", outputFormat)
                    .detail("outputStream", notification.getPriority().ordinal() >= 2 ? "stderr" : "stdout")
                    .build();

        } catch (Exception e) {
            throw new NotificationException(getProviderType(), "CONSOLE_OUTPUT_FAILED", "控制台输出失败", e);
        }
    }

    @Override
    protected NotificationTestResult doTestConnection() {
        long startTime = System.currentTimeMillis();

        try {
            // 测试控制台输出
            System.out.println("[测试] 控制台通知提供者连接正常");

            long responseTime = System.currentTimeMillis() - startTime;

            return new NotificationTestResult.Builder()
                    .status(NotificationTestResult.Status.SUCCESS)
                    .message("控制台输出测试成功")
                    .providerType(getProviderType())
                    .responseTimeMs(responseTime)
                    .detail("outputFormat", outputFormat)
                    .detail("timestamp", showTimestamp)
                    .detail("priority", showPriority)
                    .build();

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return new NotificationTestResult.Builder()
                    .status(NotificationTestResult.Status.FAILED)
                    .message("控制台测试失败: " + e.getMessage())
                    .providerType(getProviderType())
                    .responseTimeMs(responseTime)
                    .build();
        }
    }

    @Override
    protected void doDestroy() {
        // 控制台提供者无需特殊清理
        logger.info("控制台通知提供者已销毁");
    }

    @Override
    public NotificationValidationResult validateConfiguration(Map<String, Object> config) {
        NotificationValidationResult.Builder builder = NotificationValidationResult.builder();

        // 验证输出格式
        String format = (String) config.get("outputFormat");
        if (format != null && !isValidOutputFormat(format)) {
            builder.error("outputFormat", "不支持的输出格式: " + format +
                    "，支持的格式: simple, detailed, json");
        }

        // 验证时间戳格式
        String timestampFmt = (String) config.get("timestampFormat");
        if (timestampFmt != null) {
            try {
                DateTimeFormatter.ofPattern(timestampFmt);
            } catch (Exception e) {
                builder.error("timestampFormat", "无效的时间戳格式: " + timestampFmt);
            }
        }

        return builder.build();
    }

    /**
     * 格式化消息
     */
    private String formatMessage(NotificationMessage notification) {
        StringBuilder sb = new StringBuilder();

        switch (outputFormat.toLowerCase()) {
            case "simple":
                formatSimpleMessage(sb, notification);
                break;
            case "detailed":
                formatDetailedMessage(sb, notification);
                break;
            case "json":
                formatJsonMessage(sb, notification);
                break;
            default:
                formatSimpleMessage(sb, notification);
        }

        return sb.toString();
    }

    /**
     * 简单格式
     */
    private void formatSimpleMessage(StringBuilder sb, NotificationMessage notification) {
        if (showTimestamp) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(timestampFormat));
            sb.append("[").append(timestamp).append("] ");
        }

        if (showPriority) {
            sb.append("[").append(notification.getPriority()).append("] ");
        }

        if (notification.getTitle() != null) {
            sb.append(notification.getTitle()).append(" - ");
        }

        sb.append(notification.getContent());
    }

    /**
     * 详细格式
     */
    private void formatDetailedMessage(StringBuilder sb, NotificationMessage notification) {
        sb.append("=== 通知消息 ===\n");

        if (showTimestamp) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(timestampFormat));
            sb.append("时间: ").append(timestamp).append("\n");
        }

        sb.append("ID: ").append(notification.getMessageId()).append("\n");

        if (notification.getTitle() != null) {
            sb.append("标题: ").append(notification.getTitle()).append("\n");
        }

        if (showPriority) {
            sb.append("优先级: ").append(notification.getPriority()).append("\n");
        }

        sb.append("类型: ").append(notification.getMessageType()).append("\n");

        if (notification.getRecipients() != null && !notification.getRecipients().isEmpty()) {
            sb.append("收件人: ").append(String.join(", ", notification.getRecipients())).append("\n");
        }

        sb.append("内容: ").append(notification.getContent()).append("\n");

        if (notification.getVariables() != null && !notification.getVariables().isEmpty()) {
            sb.append("变量: ").append(notification.getVariables()).append("\n");
        }

        sb.append("==================");
    }

    /**
     * JSON格式
     */
    private void formatJsonMessage(StringBuilder sb, NotificationMessage notification) {
        sb.append("{\n");
        sb.append("  \"messageId\": \"").append(notification.getMessageId()).append("\",\n");
        sb.append("  \"title\": \"").append(escapeJson(notification.getTitle())).append("\",\n");
        sb.append("  \"content\": \"").append(escapeJson(notification.getContent())).append("\",\n");
        sb.append("  \"messageType\": \"").append(notification.getMessageType()).append("\",\n");
        sb.append("  \"priority\": \"").append(notification.getPriority()).append("\",\n");

        if (showTimestamp) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(timestampFormat));
            sb.append("  \"timestamp\": \"").append(timestamp).append("\",\n");
        }

        if (notification.getRecipients() != null) {
            sb.append("  \"recipients\": [");
            for (int i = 0; i < notification.getRecipients().size(); i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append("\"").append(escapeJson(notification.getRecipients().get(i))).append("\"");
            }
            sb.append("]\n");
        } else {
            sb.append("  \"recipients\": []\n");
        }

        sb.append("}");
    }

    /**
     * 检查输出格式是否有效
     */
    private boolean isValidOutputFormat(String format) {
        return "simple".equalsIgnoreCase(format) ||
                "detailed".equalsIgnoreCase(format) ||
                "json".equalsIgnoreCase(format);
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String str) {
        if (str == null)
            return "";
        return str.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
