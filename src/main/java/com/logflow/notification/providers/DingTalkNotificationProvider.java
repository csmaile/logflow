package com.logflow.notification.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logflow.notification.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 钉钉通知提供者
 * 通过钉钉机器人发送通知消息
 */
public class DingTalkNotificationProvider extends AbstractNotificationProvider {

    private String webhookUrl;
    private String secret;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private int timeoutSeconds;
    private String defaultMentionUser;
    private boolean mentionAll;

    @Override
    public String getProviderType() {
        return "dingtalk";
    }

    @Override
    public String getProviderName() {
        return "钉钉通知";
    }

    @Override
    public String getProviderDescription() {
        return "通过钉钉机器人发送通知消息，支持文本、Markdown和链接格式";
    }

    @Override
    public String[] getSupportedMessageTypes() {
        return new String[] { "TEXT", "MARKDOWN" };
    }

    @Override
    protected void doInitialize() throws NotificationException {
        // 加载配置
        this.webhookUrl = getConfigValue("webhookUrl", String.class);
        this.secret = getConfigValue("secret", String.class);
        this.timeoutSeconds = getConfigValue("timeoutSeconds", Integer.class, 30);
        this.defaultMentionUser = getConfigValue("defaultMentionUser", String.class);
        this.mentionAll = getConfigValue("mentionAll", Boolean.class, false);

        // 创建HTTP客户端
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        this.objectMapper = new ObjectMapper();

        logger.info("钉钉通知提供者初始化完成 - Webhook: {}, Secret: {}, Timeout: {}s",
                maskWebhookUrl(webhookUrl), secret != null ? "已配置" : "未配置", timeoutSeconds);
    }

    @Override
    protected NotificationResult doSendNotification(NotificationMessage notification) throws NotificationException {
        long startTime = System.currentTimeMillis();

        try {
            String requestBody = buildRequestBody(notification);
            String finalUrl = buildSignedUrl();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(finalUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            long executionTime = System.currentTimeMillis() - startTime;

            // 解析响应
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            Integer errCode = (Integer) responseMap.get("errcode");
            String errMsg = (String) responseMap.get("errmsg");

            if (errCode != null && errCode == 0) {
                return new NotificationResult.Builder()
                        .messageId(notification.getMessageId())
                        .status(NotificationResult.Status.SUCCESS)
                        .message("钉钉消息发送成功")
                        .providerType(getProviderType())
                        .executionTimeMs(executionTime)
                        .detail("responseCode", response.statusCode())
                        .detail("errcode", errCode)
                        .detail("messageType", notification.getMessageType().name())
                        .build();
            } else {
                throw new NotificationException(getProviderType(), "DINGTALK_API_ERROR",
                        "钉钉API返回错误: " + errCode + " - " + errMsg);
            }

        } catch (IOException | InterruptedException e) {
            throw new NotificationException(getProviderType(), "DINGTALK_REQUEST_FAILED",
                    "钉钉请求失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected NotificationTestResult doTestConnection() {
        long startTime = System.currentTimeMillis();

        try {
            // 创建测试消息
            Map<String, Object> testMessage = new HashMap<>();
            testMessage.put("msgtype", "text");

            Map<String, Object> text = new HashMap<>();
            text.put("content", "LogFlow钉钉通知测试 - 连接正常");
            testMessage.put("text", text);

            String requestBody = objectMapper.writeValueAsString(testMessage);
            String finalUrl;
            try {
                finalUrl = buildSignedUrl();
            } catch (NotificationException e) {
                return new NotificationTestResult.Builder()
                        .status(NotificationTestResult.Status.FAILED)
                        .message("签名生成失败: " + e.getMessage())
                        .providerType(getProviderType())
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(finalUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            long responseTime = System.currentTimeMillis() - startTime;

            // 解析响应
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            Integer errCode = (Integer) responseMap.get("errcode");
            String errMsg = (String) responseMap.get("errmsg");

            if (errCode != null && errCode == 0) {
                return new NotificationTestResult.Builder()
                        .status(NotificationTestResult.Status.SUCCESS)
                        .message("钉钉连接测试成功")
                        .providerType(getProviderType())
                        .responseTimeMs(responseTime)
                        .detail("responseCode", response.statusCode())
                        .detail("errcode", errCode)
                        .build();
            } else {
                NotificationTestResult.Status status = errCode == 310000 ? NotificationTestResult.Status.UNAUTHORIZED
                        : NotificationTestResult.Status.FAILED;

                return new NotificationTestResult.Builder()
                        .status(status)
                        .message("钉钉API错误: " + errCode + " - " + errMsg)
                        .providerType(getProviderType())
                        .responseTimeMs(responseTime)
                        .build();
            }

        } catch (IOException | InterruptedException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return new NotificationTestResult.Builder()
                    .status(NotificationTestResult.Status.FAILED)
                    .message("钉钉连接测试失败: " + e.getMessage())
                    .providerType(getProviderType())
                    .responseTimeMs(responseTime)
                    .build();
        }
    }

    @Override
    protected void doDestroy() {
        if (httpClient != null) {
            // HttpClient没有明确的关闭方法，但会自动管理资源
            httpClient = null;
        }
        logger.info("钉钉通知提供者已销毁");
    }

    @Override
    public NotificationValidationResult validateConfiguration(Map<String, Object> config) {
        NotificationValidationResult.Builder builder = NotificationValidationResult.builder();

        // 验证Webhook URL
        String webhook = (String) config.get("webhookUrl");
        if (webhook == null || webhook.trim().isEmpty()) {
            builder.error("webhookUrl", "钉钉Webhook URL不能为空");
        } else if (!webhook.startsWith("https://oapi.dingtalk.com/robot/send")) {
            builder.error("webhookUrl", "无效的钉钉Webhook URL格式");
        }

        // 验证超时时间
        Object timeoutObj = config.get("timeoutSeconds");
        if (timeoutObj != null) {
            try {
                int timeout = Integer.parseInt(timeoutObj.toString());
                if (timeout <= 0 || timeout > 300) {
                    builder.error("timeoutSeconds", "超时时间必须在1-300秒之间");
                }
            } catch (NumberFormatException e) {
                builder.error("timeoutSeconds", "超时时间格式无效");
            }
        }

        // 检查安全设置
        String secret = (String) config.get("secret");
        if (secret == null || secret.trim().isEmpty()) {
            builder.warning("secret", "建议配置钉钉机器人安全设置中的签名密钥");
        }

        return builder.build();
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(NotificationMessage notification) throws NotificationException {
        try {
            Map<String, Object> message = new HashMap<>();

            if (notification.getMessageType() == NotificationMessage.MessageType.MARKDOWN) {
                buildMarkdownMessage(message, notification);
            } else {
                buildTextMessage(message, notification);
            }

            // 添加@mention
            addMentions(message, notification);

            return objectMapper.writeValueAsString(message);

        } catch (Exception e) {
            throw new NotificationException(getProviderType(), "REQUEST_BUILD_FAILED",
                    "构建请求体失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建文本消息
     */
    private void buildTextMessage(Map<String, Object> message, NotificationMessage notification) {
        message.put("msgtype", "text");

        Map<String, Object> text = new HashMap<>();
        String content = notification.getTitle() != null ? notification.getTitle() + "\n\n" + notification.getContent()
                : notification.getContent();

        // 处理变量替换
        content = processVariables(content, notification.getVariables());

        text.put("content", content);
        message.put("text", text);
    }

    /**
     * 构建Markdown消息
     */
    private void buildMarkdownMessage(Map<String, Object> message, NotificationMessage notification) {
        message.put("msgtype", "markdown");

        Map<String, Object> markdown = new HashMap<>();
        markdown.put("title", notification.getTitle() != null ? notification.getTitle() : "通知消息");

        String content = notification.getContent();

        // 处理变量替换
        content = processVariables(content, notification.getVariables());

        markdown.put("text", content);
        message.put("markdown", markdown);
    }

    /**
     * 添加@mention设置
     */
    private void addMentions(Map<String, Object> message, NotificationMessage notification) {
        Map<String, Object> at = new HashMap<>();

        // 从通知消息中获取@用户
        List<String> atMobiles = null;
        if (notification.getRecipients() != null && !notification.getRecipients().isEmpty()) {
            atMobiles = notification.getRecipients().stream()
                    .filter(this::isMobileNumber)
                    .toList();
        }

        if (atMobiles != null && !atMobiles.isEmpty()) {
            at.put("atMobiles", atMobiles);
        } else if (defaultMentionUser != null && !defaultMentionUser.trim().isEmpty()) {
            at.put("atMobiles", List.of(defaultMentionUser));
        }

        at.put("isAtAll", mentionAll);
        message.put("at", at);
    }

    /**
     * 构建带签名的URL
     */
    private String buildSignedUrl() throws NotificationException {
        if (secret == null || secret.trim().isEmpty()) {
            return webhookUrl;
        }

        try {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] signData = digest.digest(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = Base64.getEncoder().encodeToString(signData);

            return webhookUrl + "&timestamp=" + timestamp + "&sign=" + sign;

        } catch (Exception e) {
            throw new NotificationException(getProviderType(), "SIGNATURE_FAILED",
                    "生成钉钉签名失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理变量替换
     */
    private String processVariables(String content, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return content;
        }

        String result = content;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * 检查是否为手机号码
     */
    private boolean isMobileNumber(String str) {
        return str != null && str.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 遮掩Webhook URL中的敏感信息
     */
    private String maskWebhookUrl(String url) {
        if (url == null)
            return null;

        int tokenIndex = url.indexOf("access_token=");
        if (tokenIndex > 0) {
            String beforeToken = url.substring(0, tokenIndex + 13);
            return beforeToken + "***";
        }

        return url;
    }
}
