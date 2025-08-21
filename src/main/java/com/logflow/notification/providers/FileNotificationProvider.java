package com.logflow.notification.providers;

import com.logflow.notification.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 文件通知提供者
 * 将通知消息写入文件系统
 */
public class FileNotificationProvider extends AbstractNotificationProvider {

    private static final String PROVIDER_TYPE = "file";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String filePath;
    private boolean append;
    private String format;
    private boolean includeTimestamp;

    @Override
    public String getProviderType() {
        return PROVIDER_TYPE;
    }

    @Override
    public String getProviderName() {
        return "文件通知";
    }

    @Override
    public String getProviderDescription() {
        return "将通知消息写入文件系统，支持文本和JSON格式";
    }

    @Override
    public String[] getSupportedMessageTypes() {
        return new String[] { "TEXT", "JSON" };
    }

    @Override
    protected void doInitialize() throws NotificationException {
        // 必需配置
        this.filePath = getConfigValue("filePath", String.class);
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new NotificationException(PROVIDER_TYPE, "CONFIG_MISSING", "文件路径(filePath)是必需的配置", null);
        }

        // 可选配置
        this.append = getConfigValue("append", Boolean.class, true);
        this.format = getConfigValue("format", String.class, "text"); // text, json
        this.includeTimestamp = getConfigValue("includeTimestamp", Boolean.class, true);

        // 确保目录存在
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                logger.warn("无法创建目录: {}", parentDir.getAbsolutePath());
            }
        }

        logger.info("文件通知提供者初始化完成 - 路径: {}, 追加模式: {}, 格式: {}, 时间戳: {}",
                filePath, append, format, includeTimestamp);
    }

    @Override
    protected NotificationResult doSendNotification(NotificationMessage message) throws NotificationException {
        try {
            String content = formatMessage(message);
            writeToFile(content);

            return new NotificationResult.Builder()
                    .messageId(message.getMessageId())
                    .providerType(PROVIDER_TYPE)
                    .status(NotificationResult.Status.SUCCESS)
                    .message("文件写入成功")
                    .build();

        } catch (IOException e) {
            throw new NotificationException(PROVIDER_TYPE, "FILE_WRITE_FAILED", "文件写入失败", e);
        }
    }

    @Override
    public NotificationValidationResult validateConfiguration(Map<String, Object> config) {
        List<NotificationValidationResult.ValidationError> errors = new java.util.ArrayList<>();
        List<NotificationValidationResult.ValidationWarning> warnings = new java.util.ArrayList<>();

        // 验证文件路径
        String path = getConfigValue("filePath", String.class);
        if (path == null || path.trim().isEmpty()) {
            errors.add(new NotificationValidationResult.ValidationError("filePath", "文件路径是必需的"));
        } else {
            File file = new File(path);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                warnings.add(new NotificationValidationResult.ValidationWarning("filePath",
                        "目录不存在且无法创建: " + parentDir.getAbsolutePath()));
            }
        }

        // 验证格式
        String fmt = getConfigValue("format", String.class, "text");
        if (!List.of("text", "json").contains(fmt.toLowerCase())) {
            warnings.add(new NotificationValidationResult.ValidationWarning("format", "不支持的格式，支持: text, json"));
        }

        return new NotificationValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    protected NotificationTestResult doTestConnection() {
        long startTime = System.currentTimeMillis();

        try {
            // 测试写入权限
            File testFile = new File(filePath + ".test");
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("LogFlow文件通知测试 - " + LocalDateTime.now());
            }

            // 清理测试文件
            boolean deleted = testFile.delete();
            if (!deleted) {
                logger.warn("无法删除测试文件: {}", testFile.getAbsolutePath());
            }

            return new NotificationTestResult.Builder()
                    .status(NotificationTestResult.Status.SUCCESS)
                    .message("文件写入测试成功")
                    .providerType(PROVIDER_TYPE)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (IOException e) {
            return new NotificationTestResult.Builder()
                    .status(NotificationTestResult.Status.FAILED)
                    .message("文件写入测试失败: " + e.getMessage())
                    .providerType(PROVIDER_TYPE)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    protected void doDestroy() {
        logger.info("文件通知提供者已销毁");
    }

    /**
     * 格式化消息内容
     */
    private String formatMessage(NotificationMessage message) throws IOException {
        String timestamp = includeTimestamp
                ? "[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] "
                : "";

        switch (format.toLowerCase()) {
            case "json":
                // JSON格式输出
                return timestamp + objectMapper.writeValueAsString(Map.of(
                        "messageId", message.getMessageId(),
                        "title", message.getTitle() != null ? message.getTitle() : "",
                        "content", message.getContent() != null ? message.getContent() : "",
                        "priority", message.getPriority().name(),
                        "messageType", message.getMessageType().name(),
                        "recipients", message.getRecipients(),
                        "timestamp", java.time.LocalDateTime.now()));

            case "text":
            default:
                // 文本格式输出
                StringBuilder sb = new StringBuilder();
                sb.append(timestamp);

                if (message.getTitle() != null && !message.getTitle().isEmpty()) {
                    sb.append("[").append(message.getTitle()).append("] ");
                }

                if (message.getPriority() != NotificationMessage.Priority.NORMAL) {
                    sb.append("[").append(message.getPriority()).append("] ");
                }

                sb.append(message.getContent() != null ? message.getContent() : "");

                return sb.toString();
        }
    }

    /**
     * 写入文件
     */
    private void writeToFile(String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath, append)) {
            writer.write(content);
            writer.write(System.lineSeparator());
        }
    }
}
