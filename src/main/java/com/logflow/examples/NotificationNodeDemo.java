package com.logflow.examples;

import com.logflow.builder.WorkflowBuilder;
import com.logflow.core.WorkflowContext;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;
import com.logflow.engine.WorkflowExecutionResult;
import com.logflow.nodes.NotificationNode;
import com.logflow.notification.NotificationTestResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 通知节点演示程序
 * 展示LogFlow通知系统的各种功能
 */
public class NotificationNodeDemo {

    public static void main(String[] args) {
        System.out.println("=== LogFlow 通知节点功能演示 ===\n");

        try {
            // 演示控制台通知
            demonstrateConsoleNotification();
            System.out.println();

            // 演示不同的消息格式
            demonstrateMessageFormats();
            System.out.println();

            // 演示通知提供者测试
            demonstrateProviderTesting();
            System.out.println();

            // 演示工作流集成
            demonstrateWorkflowIntegration();
            System.out.println();

            // 显示系统信息
            displaySystemInfo();

        } catch (Exception e) {
            System.err.println("演示过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 演示控制台通知
     */
    private static void demonstrateConsoleNotification() {
        System.out.println("🖥️ 控制台通知演示：\n");

        // 创建简单的工作流
        Workflow workflow = WorkflowBuilder.create("console-notification", "控制台通知演示")
                .addInputNode("input", "数据输入")
                .addNotificationNode("notify", "控制台通知")
                .withConfig(Map.of(
                        "providerType", "console",
                        "providerConfig", Map.of(
                                "outputFormat", "detailed",
                                "showTimestamp", true,
                                "showPriority", true),
                        "title", "LogFlow工作流执行通知",
                        "contentTemplate", "工作流 ${workflowId} 执行完成\n" +
                                "执行ID: ${executionId}\n" +
                                "开始时间: ${startTime}\n" +
                                "节点: ${nodeName}\n" +
                                "处理数据: ${demo_data}",
                        "messageType", "TEXT",
                        "priority", "NORMAL",
                        "inputKey", "demo_data"))
                .connect("input", "notify")
                .build();

        // 执行工作流
        WorkflowEngine engine = new WorkflowEngine();

        Map<String, Object> inputData = Map.of(
                "demo_data", Map.of(
                        "message", "这是一条测试消息",
                        "timestamp", System.currentTimeMillis(),
                        "status", "SUCCESS",
                        "count", 42));

        System.out.println("📥 输入数据: " + inputData.get("demo_data"));
        System.out.println("🎯 通知类型: 控制台通知 (详细格式)");
        System.out.println("📝 执行结果:\n");

        WorkflowExecutionResult result = engine.execute(workflow, inputData);

        if (result.isSuccess()) {
            System.out.println("\n✅ 控制台通知发送成功");
            System.out.printf("⏱️ 执行时间: %dms\n", result.getExecutionDurationMs());
        } else {
            System.out.println("❌ 控制台通知发送失败: " + result.getMessage());
        }

        engine.shutdown();
    }

    /**
     * 演示不同的消息格式
     */
    private static void demonstrateMessageFormats() {
        System.out.println("📋 消息格式演示：\n");

        WorkflowEngine engine = new WorkflowEngine();

        // 演示JSON格式
        demonstrateFormat(engine, "JSON", "json", Map.of(
                "user", "张三",
                "action", "登录",
                "timestamp", System.currentTimeMillis(),
                "success", true));

        // 演示简单格式
        demonstrateFormat(engine, "简单文本", "simple", "Hello World");

        engine.shutdown();
    }

    /**
     * 演示特定格式
     */
    private static void demonstrateFormat(WorkflowEngine engine, String formatName,
            String outputFormat, Object data) {
        System.out.println("   📝 " + formatName + " 格式:");

        Workflow workflow = WorkflowBuilder.create("format-" + outputFormat, formatName + "格式演示")
                .addInputNode("input", "数据输入")
                .addNotificationNode("notify", formatName + "通知")
                .withConfig(Map.of(
                        "providerType", "console",
                        "providerConfig", Map.of(
                                "outputFormat", outputFormat,
                                "showTimestamp", false,
                                "showPriority", false),
                        "title", formatName + "消息",
                        "contentTemplate", "${format_data}",
                        "messageType", "TEXT",
                        "priority", "LOW",
                        "inputKey", "format_data"))
                .connect("input", "notify")
                .build();

        Map<String, Object> inputData = Map.of("format_data", data);
        WorkflowExecutionResult result = engine.execute(workflow, inputData);

        System.out.printf("      %s (%dms)\n",
                result.isSuccess() ? "✅ 成功" : "❌ 失败",
                result.getExecutionDurationMs());
    }

    /**
     * 演示通知提供者测试
     */
    private static void demonstrateProviderTesting() {
        System.out.println("🔧 通知提供者测试：\n");

        // 测试控制台提供者
        testProvider("console", "控制台通知", Map.of(
                "outputFormat", "simple",
                "showTimestamp", true));

        // 测试钉钉提供者（配置不完整，会显示配置错误）
        testProvider("dingtalk", "钉钉通知", Map.of(
                "webhookUrl", "https://oapi.dingtalk.com/robot/send?access_token=test",
                "timeoutSeconds", 10));

        // 测试邮件提供者（配置不完整，会显示配置错误）
        testProvider("email", "邮件通知", Map.of(
                "smtpHost", "smtp.example.com",
                "smtpPort", 587,
                "fromAddress", "test@example.com"));
    }

    /**
     * 测试特定提供者
     */
    private static void testProvider(String providerType, String providerName,
            Map<String, Object> config) {
        System.out.printf("   🔍 测试 %s:\n", providerName);

        try {
            NotificationNode node = new NotificationNode("test", "测试节点");
            node.setConfiguration(Map.of(
                    "providerType", providerType,
                    "providerConfig", config));

            // 执行验证
            var validation = node.validate();

            if (validation.isValid()) {
                System.out.println("      ✅ 配置验证通过");

                // 执行连接测试
                NotificationTestResult testResult = node.testNotification();
                System.out.printf("      🔗 连接测试: %s (%dms)\n",
                        testResult.isSuccess() ? "成功" : "失败 - " + testResult.getMessage(),
                        testResult.getResponseTimeMs());
            } else {
                System.out.println("      ❌ 配置验证失败:");
                validation.getErrors().forEach(error -> System.out.println("         - " + error));
            }

            if (validation.hasWarnings()) {
                System.out.println("      ⚠️ 配置警告:");
                validation.getWarnings().forEach(warning -> System.out.println("         - " + warning));
            }

            node.destroy();

        } catch (Exception e) {
            System.out.println("      ❌ 测试异常: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * 演示工作流集成
     */
    private static void demonstrateWorkflowIntegration() {
        System.out.println("🔗 工作流集成演示：\n");

        // 创建复杂的工作流，包含多个通知节点
        Workflow workflow = WorkflowBuilder.create("integrated-notifications", "集成通知演示")
                .addInputNode("input", "数据输入")

                // 数据处理脚本
                .addScriptNode("process", "数据处理")
                .withScript("" +
                        "var input = context.getData('demo_data');\n" +
                        "var processed = {\n" +
                        "    original: input,\n" +
                        "    processed_time: new Date().toISOString(),\n" +
                        "    status: 'processed',\n" +
                        "    count: (input.items ? input.items.length : 0)\n" +
                        "};\n" +
                        "context.setData('processed_data', processed);\n" +
                        "context.setData('notify_start', true);")

                // 开始处理通知
                .addNotificationNode("notify-start", "开始处理通知")
                .withConfig(Map.of(
                        "providerType", "console",
                        "providerConfig", Map.of("outputFormat", "simple"),
                        "title", "📥 开始处理数据",
                        "contentTemplate", "开始处理数据: ${demo_data}",
                        "messageType", "TEXT",
                        "priority", "LOW",
                        "inputKey", "demo_data"))

                // 处理完成通知
                .addNotificationNode("notify-complete", "处理完成通知")
                .withConfig(Map.of(
                        "providerType", "console",
                        "providerConfig", Map.of("outputFormat", "detailed"),
                        "title", "✅ 数据处理完成",
                        "contentTemplate", "处理完成!\n" +
                                "原始数据: ${ctx.demo_data}\n" +
                                "处理结果: ${ctx.processed_data}\n" +
                                "处理节点: ${nodeName}",
                        "messageType", "TEXT",
                        "priority", "NORMAL",
                        "inputKey", "processed_data"))

                // 连接节点
                .connect("input", "notify-start")
                .connect("notify-start", "process")
                .connect("process", "notify-complete")
                .build();

        // 执行工作流
        WorkflowEngine engine = new WorkflowEngine();

        Map<String, Object> inputData = Map.of(
                "demo_data", Map.of(
                        "name", "数据处理任务",
                        "type", "批处理",
                        "items", Arrays.asList("item1", "item2", "item3"),
                        "priority", "HIGH"));

        System.out.println("📥 输入数据: " + inputData.get("demo_data"));
        System.out.println("🎯 工作流: 数据处理 + 多重通知");
        System.out.println("📝 执行过程:\n");

        long startTime = System.currentTimeMillis();
        WorkflowExecutionResult result = engine.execute(workflow, inputData);
        long endTime = System.currentTimeMillis();

        System.out.printf("\n📊 执行结果: %s (%dms)\n",
                result.isSuccess() ? "✅ 成功" : "❌ 失败", endTime - startTime);

        if (result.isSuccess()) {
            System.out.printf("   📋 总节点数: %d\n", result.getStatistics().getTotalNodes());
            System.out.printf("   ✅ 成功节点: %d\n", result.getStatistics().getSuccessfulNodes());
            System.out.printf("   ❌ 失败节点: %d\n", result.getStatistics().getFailedNodes());
        } else {
            System.out.printf("   💥 失败原因: %s\n", result.getMessage());
        }

        engine.shutdown();
    }

    /**
     * 显示系统信息
     */
    private static void displaySystemInfo() {
        System.out.println("📊 通知系统信息：\n");

        System.out.println("🔧 支持的通知提供者:");
        String[] providers = NotificationNode.getRegisteredProviderTypes();
        for (String provider : providers) {
            System.out.println("   - " + provider);
        }

        System.out.println("\n📝 支持的消息类型:");
        System.out.println("   - TEXT (纯文本)");
        System.out.println("   - HTML (HTML格式)");
        System.out.println("   - MARKDOWN (Markdown格式)");
        System.out.println("   - JSON (JSON格式)");
        System.out.println("   - TEMPLATE (模板格式)");

        System.out.println("\n⚡ 支持的优先级:");
        System.out.println("   - LOW (低优先级)");
        System.out.println("   - NORMAL (普通优先级)");
        System.out.println("   - HIGH (高优先级)");
        System.out.println("   - URGENT (紧急)");

        System.out.println("\n🎯 核心特性:");
        System.out.println("   📤 多种通知提供者 (控制台、邮件、钉钉)");
        System.out.println("   📋 丰富的消息格式支持");
        System.out.println("   🔧 灵活的配置系统");
        System.out.println("   ✅ 完整的验证和测试机制");
        System.out.println("   📊 详细的执行统计");
        System.out.println("   🔗 无缝的工作流集成");
        System.out.println("   📝 变量模板系统");
        System.out.println("   ⏰ 定时发送支持");
        System.out.println("   🎛️ 优先级管理");

        System.out.println("\n💡 使用场景:");
        System.out.println("   🚨 工作流执行状态通知");
        System.out.println("   📊 数据处理结果报告");
        System.out.println("   ⚠️ 异常和错误告警");
        System.out.println("   📈 定期状态更新");
        System.out.println("   🔔 用户操作确认");
        System.out.println("   📋 任务完成提醒");
    }
}
