package com.logflow.notification;

import java.util.Map;

/**
 * 通知提供者接口
 * 定义通知系统的核心功能
 */
public interface NotificationProvider {

    /**
     * 获取提供者类型
     */
    String getProviderType();

    /**
     * 获取提供者名称
     */
    String getProviderName();

    /**
     * 获取提供者描述
     */
    String getProviderDescription();

    /**
     * 初始化提供者
     * 
     * @param config 配置参数
     */
    void initialize(Map<String, Object> config) throws NotificationException;

    /**
     * 发送通知
     * 
     * @param notification 通知内容
     * @return 发送结果
     */
    NotificationResult sendNotification(NotificationMessage notification) throws NotificationException;

    /**
     * 验证配置
     * 
     * @param config 配置参数
     * @return 验证结果
     */
    NotificationValidationResult validateConfiguration(Map<String, Object> config);

    /**
     * 测试连接
     * 
     * @return 测试结果
     */
    NotificationTestResult testConnection();

    /**
     * 检查提供者是否已初始化
     */
    boolean isInitialized();

    /**
     * 获取支持的消息类型
     */
    String[] getSupportedMessageTypes();

    /**
     * 销毁提供者，释放资源
     */
    void destroy();
}
