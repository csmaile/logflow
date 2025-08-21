package com.logflow.notification.providers;

import com.logflow.notification.*;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 邮件通知提供者
 * 通过SMTP协议发送邮件通知
 */
public class EmailNotificationProvider extends AbstractNotificationProvider {

    private Session mailSession;
    private String smtpHost;
    private int smtpPort;
    private String username;
    private String password;
    private boolean enableTls;
    private boolean enableAuth;
    private String fromAddress;
    private String fromName;

    @Override
    public String getProviderType() {
        return "email";
    }

    @Override
    public String getProviderName() {
        return "邮件通知";
    }

    @Override
    public String getProviderDescription() {
        return "通过SMTP协议发送邮件通知，支持HTML格式和附件";
    }

    @Override
    public String[] getSupportedMessageTypes() {
        return new String[] { "TEXT", "HTML", "MARKDOWN" };
    }

    @Override
    protected void doInitialize() throws NotificationException {
        // 加载配置
        this.smtpHost = getConfigValue("smtpHost", String.class);
        this.smtpPort = getConfigValue("smtpPort", Integer.class, 587);
        this.username = getConfigValue("username", String.class);
        this.password = getConfigValue("password", String.class);
        this.enableTls = getConfigValue("enableTls", Boolean.class, true);
        this.enableAuth = getConfigValue("enableAuth", Boolean.class, true);
        this.fromAddress = getConfigValue("fromAddress", String.class);
        this.fromName = getConfigValue("fromName", String.class, "LogFlow通知系统");

        // 创建邮件会话
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", enableAuth);
        props.put("mail.smtp.starttls.enable", enableTls);

        if (enableAuth && username != null && password != null) {
            this.mailSession = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        } else {
            this.mailSession = Session.getInstance(props);
        }

        logger.info("邮件通知提供者初始化完成 - SMTP: {}:{}, TLS: {}, Auth: {}",
                smtpHost, smtpPort, enableTls, enableAuth);
    }

    @Override
    protected NotificationResult doSendNotification(NotificationMessage notification) throws NotificationException {
        long startTime = System.currentTimeMillis();

        try {
            Message message = createEmailMessage(notification);
            Transport.send(message);

            long executionTime = System.currentTimeMillis() - startTime;

            return new NotificationResult.Builder()
                    .messageId(notification.getMessageId())
                    .status(NotificationResult.Status.SUCCESS)
                    .message("邮件发送成功")
                    .providerType(getProviderType())
                    .executionTimeMs(executionTime)
                    .detail("smtpHost", smtpHost)
                    .detail("recipientCount",
                            notification.getRecipients() != null ? notification.getRecipients().size() : 0)
                    .detail("messageType", notification.getMessageType().name())
                    .build();

        } catch (MessagingException e) {
            throw new NotificationException(getProviderType(), "EMAIL_SEND_FAILED",
                    "邮件发送失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected NotificationTestResult doTestConnection() {
        long startTime = System.currentTimeMillis();

        try {
            // 创建测试邮件
            Message testMessage = new MimeMessage(mailSession);
            testMessage.setFrom(new InternetAddress(fromAddress, fromName));
            testMessage.setSubject("LogFlow邮件通知测试");
            testMessage.setText("这是一条测试邮件，用于验证邮件通知配置是否正确。");

            // 测试连接（不实际发送）
            Transport transport = mailSession.getTransport("smtp");
            transport.connect(smtpHost, smtpPort, username, password);
            transport.close();

            long responseTime = System.currentTimeMillis() - startTime;

            return new NotificationTestResult.Builder()
                    .status(NotificationTestResult.Status.SUCCESS)
                    .message("SMTP连接测试成功")
                    .providerType(getProviderType())
                    .responseTimeMs(responseTime)
                    .detail("smtpHost", smtpHost)
                    .detail("smtpPort", smtpPort)
                    .detail("enableTls", enableTls)
                    .detail("enableAuth", enableAuth)
                    .build();

        } catch (MessagingException e) {
            long responseTime = System.currentTimeMillis() - startTime;

            NotificationTestResult.Status status;
            if (e.getMessage().contains("Authentication")) {
                status = NotificationTestResult.Status.UNAUTHORIZED;
            } else if (e.getMessage().contains("timeout") || e.getMessage().contains("connect")) {
                status = NotificationTestResult.Status.TIMEOUT;
            } else {
                status = NotificationTestResult.Status.FAILED;
            }

            return new NotificationTestResult.Builder()
                    .status(status)
                    .message("SMTP连接测试失败: " + e.getMessage())
                    .providerType(getProviderType())
                    .responseTimeMs(responseTime)
                    .build();

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return new NotificationTestResult.Builder()
                    .status(NotificationTestResult.Status.FAILED)
                    .message("连接测试异常: " + e.getMessage())
                    .providerType(getProviderType())
                    .responseTimeMs(responseTime)
                    .build();
        }
    }

    @Override
    protected void doDestroy() {
        this.mailSession = null;
        logger.info("邮件通知提供者已销毁");
    }

    @Override
    public NotificationValidationResult validateConfiguration(Map<String, Object> config) {
        NotificationValidationResult.Builder builder = NotificationValidationResult.builder();

        // 验证必需的配置
        if (config.get("smtpHost") == null || config.get("smtpHost").toString().trim().isEmpty()) {
            builder.error("smtpHost", "SMTP服务器地址不能为空");
        }

        if (config.get("fromAddress") == null || config.get("fromAddress").toString().trim().isEmpty()) {
            builder.error("fromAddress", "发件人邮箱地址不能为空");
        } else {
            String fromAddr = config.get("fromAddress").toString();
            if (!isValidEmail(fromAddr)) {
                builder.error("fromAddress", "发件人邮箱地址格式无效: " + fromAddr);
            }
        }

        // 验证端口号
        Object portObj = config.get("smtpPort");
        if (portObj != null) {
            try {
                int port = Integer.parseInt(portObj.toString());
                if (port <= 0 || port > 65535) {
                    builder.error("smtpPort", "SMTP端口号必须在1-65535之间");
                }
            } catch (NumberFormatException e) {
                builder.error("smtpPort", "SMTP端口号格式无效");
            }
        }

        // 验证认证配置
        Boolean enableAuth = (Boolean) config.get("enableAuth");
        if (enableAuth != null && enableAuth) {
            if (config.get("username") == null || config.get("username").toString().trim().isEmpty()) {
                builder.error("username", "启用认证时用户名不能为空");
            }
            if (config.get("password") == null || config.get("password").toString().trim().isEmpty()) {
                builder.error("password", "启用认证时密码不能为空");
            }
        }

        return builder.build();
    }

    /**
     * 创建邮件消息
     */
    private Message createEmailMessage(NotificationMessage notification) throws MessagingException {
        Message message = new MimeMessage(mailSession);

        // 设置发件人
        try {
            message.setFrom(new InternetAddress(fromAddress, fromName));
        } catch (Exception e) {
            message.setFrom(new InternetAddress(fromAddress));
        }

        // 设置收件人
        if (notification.getRecipients() != null && !notification.getRecipients().isEmpty()) {
            InternetAddress[] recipients = new InternetAddress[notification.getRecipients().size()];
            for (int i = 0; i < notification.getRecipients().size(); i++) {
                recipients[i] = new InternetAddress(notification.getRecipients().get(i));
            }
            message.setRecipients(Message.RecipientType.TO, recipients);
        }

        // 设置抄送
        if (notification.getCcRecipients() != null && !notification.getCcRecipients().isEmpty()) {
            InternetAddress[] ccRecipients = new InternetAddress[notification.getCcRecipients().size()];
            for (int i = 0; i < notification.getCcRecipients().size(); i++) {
                ccRecipients[i] = new InternetAddress(notification.getCcRecipients().get(i));
            }
            message.setRecipients(Message.RecipientType.CC, ccRecipients);
        }

        // 设置主题
        message.setSubject(notification.getTitle() != null ? notification.getTitle() : "LogFlow通知");

        // 设置内容
        String content = processContent(notification);
        if (notification.getMessageType() == NotificationMessage.MessageType.HTML) {
            message.setContent(content, "text/html; charset=utf-8");
        } else {
            message.setText(content);
        }

        // 设置优先级
        switch (notification.getPriority()) {
            case URGENT:
                message.setHeader("X-Priority", "1");
                message.setHeader("Importance", "High");
                break;
            case HIGH:
                message.setHeader("X-Priority", "2");
                message.setHeader("Importance", "High");
                break;
            case NORMAL:
                message.setHeader("X-Priority", "3");
                message.setHeader("Importance", "Normal");
                break;
            case LOW:
                message.setHeader("X-Priority", "4");
                message.setHeader("Importance", "Low");
                break;
        }

        return message;
    }

    /**
     * 处理邮件内容
     */
    private String processContent(NotificationMessage notification) {
        String content = notification.getContent();

        // 处理变量替换
        if (notification.getVariables() != null && !notification.getVariables().isEmpty()) {
            for (Map.Entry<String, Object> entry : notification.getVariables().entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                content = content.replace(placeholder, value);
            }
        }

        // 如果是Markdown格式，转换为HTML
        if (notification.getMessageType() == NotificationMessage.MessageType.MARKDOWN) {
            content = convertMarkdownToHtml(content);
        }

        return content;
    }

    /**
     * 简单的Markdown到HTML转换
     */
    private String convertMarkdownToHtml(String markdown) {
        // 简单的Markdown转换实现
        String html = markdown
                .replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>") // 粗体
                .replaceAll("\\*(.*?)\\*", "<em>$1</em>") // 斜体
                .replaceAll("```(.*?)```", "<pre><code>$1</code></pre>") // 代码块
                .replaceAll("`(.*?)`", "<code>$1</code>") // 行内代码
                .replaceAll("\\n", "<br>"); // 换行

        return "<html><body>" + html + "</body></html>";
    }

    /**
     * 验证邮箱地址格式
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
}
