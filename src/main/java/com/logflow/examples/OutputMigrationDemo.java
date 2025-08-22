package com.logflow.examples;

import com.logflow.builder.WorkflowBuilder;
import com.logflow.core.WorkflowContext;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;
import com.logflow.engine.WorkflowExecutionResult;
import com.logflow.notification.providers.ContextNotificationProvider;

import java.io.File;
import java.util.Map;

/**
 * OutputNode 迁移到 NotificationNode 演示
 * 展示新的通知节点如何完全替代原有的输出节点功能
 */
public class OutputMigrationDemo {

        public static void main(String[] args) {
                System.out.println("=== LogFlow OutputNode 迁移演示 ===\n");

                WorkflowEngine engine = new WorkflowEngine();

                // 1. 控制台输出对比
                demonstrateConsoleOutput(engine);

                // 2. 文件输出对比
                demonstrateFileOutput(engine);

                // 3. 上下文输出对比
                demonstrateContextOutput(engine);

                // 4. 新增功能展示
                demonstrateEnhancedFeatures(engine);

                System.out.println("\n🎯 迁移总结：");
                System.out.println("✅ NotificationNode 完全替代了 OutputNode 的所有功能");
                System.out.println("🚀 并提供了更强大的通知能力：邮件、钉钉、丰富格式等");
                System.out.println("🔧 更好的配置验证和错误处理机制");
                System.out.println("📊 内置的性能监控和统计信息");
                System.out.println("⚠️  OutputNode 已标记为 @Deprecated，建议迁移");
        }

        /**
         * 控制台输出功能对比
         */
        private static void demonstrateConsoleOutput(WorkflowEngine engine) {
                System.out.println("🖥️ 控制台输出功能对比：\n");

                Map<String, Object> testData = Map.of(
                                "message", "Hello LogFlow!",
                                "timestamp", System.currentTimeMillis(),
                                "type", "demo");

                // 新的 NotificationNode 方式
                System.out.println("\n   🚀 新版 NotificationNode:");
                Workflow newWorkflow = WorkflowBuilder.create("new-console", "新版控制台通知")
                                .addInputNode("input", "输入")
                                .addConsoleNotificationNode("notify", "控制台通知") // 新方法
                                .connect("input", "notify")
                                .build();

                WorkflowExecutionResult newResult = engine.execute(newWorkflow, Map.of("demo_data", testData));
                System.out.printf("      结果: %s (%dms)\n\n",
                                newResult.isSuccess() ? "✅ 成功" : "❌ 失败",
                                newResult.getExecutionDurationMs());
        }

        /**
         * 文件输出功能对比
         */
        private static void demonstrateFileOutput(WorkflowEngine engine) {
                System.out.println("📁 文件输出功能对比：\n");

                String testFile = "demo-output.txt";
                Map<String, Object> testData = Map.of(
                                "content", "LogFlow 文件输出测试",
                                "version", "2.0",
                                "features", "enhanced notification system");

                // 新的 NotificationNode 方式
                System.out.println("\n   🚀 新版 NotificationNode 文件输出:");
                Workflow newFileWorkflow = WorkflowBuilder.create("new-file", "新版文件通知")
                                .addInputNode("input", "输入")
                                .addFileOutputNode("notify", "文件通知", "demo-output-new.txt") // 新方法
                                .connect("input", "notify")
                                .build();

                WorkflowExecutionResult newFileResult = engine.execute(newFileWorkflow, Map.of("demo_data", testData));
                System.out.printf("      结果: %s\n\n",
                                newFileResult.isSuccess() ? "✅ 成功" : "❌ 失败");

                // 清理测试文件
                cleanupTestFiles(testFile, "demo-output-new.txt");
        }

        /**
         * 上下文输出功能对比
         */
        private static void demonstrateContextOutput(WorkflowEngine engine) {
                System.out.println("💾 上下文输出功能对比：\n");

                Map<String, Object> testData = Map.of(
                                "result", "处理完成",
                                "count", 42,
                                "status", "SUCCESS");

                // 新的 NotificationNode 方式
                System.out.println("\n   🚀 新版 NotificationNode 上下文输出:");
                Workflow newContextWorkflow = WorkflowBuilder.create("new-context", "新版上下文通知")
                                .addInputNode("input", "输入")
                                .addContextOutputNode("notify", "上下文通知", "new_result") // 新方法
                                .connect("input", "notify")
                                .build();

                WorkflowExecutionResult newContextResult = engine.execute(newContextWorkflow,
                                Map.of("demo_data", testData));
                System.out.printf("      结果: %s\n",
                                newContextResult.isSuccess() ? "✅ 成功" : "❌ 失败");

                // 验证上下文数据
                if (newContextResult.isSuccess()) {
                        System.out.println("      📋 上下文数据已保存，键: 'new_result'");
                }
                System.out.println();
        }

        /**
         * 新增功能展示
         */
        private static void demonstrateEnhancedFeatures(WorkflowEngine engine) {
                System.out.println("✨ NotificationNode 独有的增强功能：\n");

                Map<String, Object> testData = Map.of(
                                "system", "LogFlow",
                                "feature", "Enhanced Notifications",
                                "priority", "HIGH");

                // 详细格式的控制台通知
                System.out.println("   🎨 详细格式控制台通知:");
                Workflow detailedWorkflow = WorkflowBuilder.create("detailed-console", "详细控制台通知")
                                .addInputNode("input", "输入")
                                .addDetailedConsoleNotificationNode("notify", "详细通知")
                                .connect("input", "notify")
                                .build();

                WorkflowExecutionResult detailedResult = engine.execute(detailedWorkflow,
                                Map.of("demo_data", testData));
                System.out.printf("      结果: %s\n",
                                detailedResult.isSuccess() ? "✅ 成功" : "❌ 失败");

                // JSON 格式文件输出
                System.out.println("\n   📋 JSON 格式文件输出:");
                Workflow jsonFileWorkflow = WorkflowBuilder.create("json-file", "JSON文件通知")
                                .addInputNode("input", "输入")
                                .addJsonFileOutputNode("notify", "JSON通知", "demo-output.json")
                                .connect("input", "notify")
                                .build();

                WorkflowExecutionResult jsonResult = engine.execute(jsonFileWorkflow, Map.of("demo_data", testData));
                System.out.printf("      结果: %s\n",
                                jsonResult.isSuccess() ? "✅ 成功" : "❌ 失败");

                // 多种通知类型组合
                System.out.println("\n   🔗 多种通知类型组合:");
                Workflow combinedWorkflow = WorkflowBuilder.create("combined-notifications", "组合通知")
                                .addInputNode("input", "输入")
                                .addConsoleNotificationNode("console", "控制台")
                                .addFileOutputNode("file", "文件", "combined-output.txt")
                                .addContextOutputNode("context", "上下文", "combined_result")
                                .connect("input", "console")
                                .connect("input", "file")
                                .connect("input", "context")
                                .build();

                WorkflowExecutionResult combinedResult = engine.execute(combinedWorkflow,
                                Map.of("demo_data", testData));
                System.out.printf("      结果: %s\n",
                                combinedResult.isSuccess() ? "✅ 成功" : "❌ 失败");

                // 清理测试文件
                cleanupTestFiles("demo-output.json", "combined-output.txt");
        }

        /**
         * 清理测试文件
         */
        private static void cleanupTestFiles(String... files) {
                for (String file : files) {
                        File f = new File(file);
                        if (f.exists()) {
                                boolean deleted = f.delete();
                                if (!deleted) {
                                        System.out.printf("      ⚠️ 无法删除测试文件: %s\n", file);
                                }
                        }
                }
        }
}
