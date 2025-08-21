package com.logflow.notification;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知消息类
 * 包含发送通知所需的所有信息
 */
public class NotificationMessage {

    public enum Priority {
        LOW, // 低优先级
        NORMAL, // 普通优先级
        HIGH, // 高优先级
        URGENT // 紧急
    }

    public enum MessageType {
        TEXT, // 纯文本
        HTML, // HTML格式
        MARKDOWN, // Markdown格式
        JSON, // JSON格式
        TEMPLATE // 模板格式
    }

    private String messageId;
    private String title;
    private String content;
    private MessageType messageType;
    private Priority priority;
    private List<String> recipients;
    private List<String> ccRecipients;
    private Map<String, Object> variables;
    private Map<String, Object> metadata;
    private LocalDateTime createTime;
    private LocalDateTime scheduleTime;
    private String templateId;
    private Map<String, String> attachments;

    public NotificationMessage() {
        this.messageId = generateMessageId();
        this.messageType = MessageType.TEXT;
        this.priority = Priority.NORMAL;
        this.variables = new HashMap<>();
        this.metadata = new HashMap<>();
        this.attachments = new HashMap<>();
        this.createTime = LocalDateTime.now();
    }

    public NotificationMessage(String title, String content) {
        this();
        this.title = title;
        this.content = content;
    }

    public NotificationMessage(String title, String content, List<String> recipients) {
        this(title, content);
        this.recipients = recipients;
    }

    private String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" +
                (int) (Math.random() * 10000);
    }

    // Getters and Setters

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public List<String> getCcRecipients() {
        return ccRecipients;
    }

    public void setCcRecipients(List<String> ccRecipients) {
        this.ccRecipients = ccRecipients;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public void addVariable(String key, Object value) {
        this.variables.put(key, value);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(LocalDateTime scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(String name, String path) {
        this.attachments.put(name, path);
    }

    // 便捷方法

    /**
     * 检查是否为定时消息
     */
    public boolean isScheduled() {
        return scheduleTime != null && scheduleTime.isAfter(LocalDateTime.now());
    }

    /**
     * 检查是否使用模板
     */
    public boolean isTemplate() {
        return templateId != null && !templateId.trim().isEmpty();
    }

    /**
     * 检查是否有附件
     */
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }

    /**
     * 获取变量值
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 获取变量值（带默认值）
     */
    public Object getVariable(String key, Object defaultValue) {
        return variables.getOrDefault(key, defaultValue);
    }

    /**
     * 构建器模式
     */
    public static class Builder {
        private final NotificationMessage message;

        public Builder() {
            this.message = new NotificationMessage();
        }

        public Builder title(String title) {
            message.setTitle(title);
            return this;
        }

        public Builder content(String content) {
            message.setContent(content);
            return this;
        }

        public Builder messageType(MessageType messageType) {
            message.setMessageType(messageType);
            return this;
        }

        public Builder priority(Priority priority) {
            message.setPriority(priority);
            return this;
        }

        public Builder recipients(List<String> recipients) {
            message.setRecipients(recipients);
            return this;
        }

        public Builder ccRecipients(List<String> ccRecipients) {
            message.setCcRecipients(ccRecipients);
            return this;
        }

        public Builder variable(String key, Object value) {
            message.addVariable(key, value);
            return this;
        }

        public Builder metadata(String key, Object value) {
            message.addMetadata(key, value);
            return this;
        }

        public Builder scheduleTime(LocalDateTime scheduleTime) {
            message.setScheduleTime(scheduleTime);
            return this;
        }

        public Builder templateId(String templateId) {
            message.setTemplateId(templateId);
            return this;
        }

        public Builder attachment(String name, String path) {
            message.addAttachment(name, path);
            return this;
        }

        public NotificationMessage build() {
            return message;
        }
    }

    @Override
    public String toString() {
        return String.format("NotificationMessage{id='%s', title='%s', type=%s, priority=%s, recipients=%d}",
                messageId, title, messageType, priority,
                recipients != null ? recipients.size() : 0);
    }
}
