package com.logflow.examples;

import com.logflow.config.WorkflowConfigLoader;
import com.logflow.core.WorkflowContext;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

/**
 * 简化的多输入演示
 * 直接使用配置化的节点来演示多输入功能
 */
public class SimpleMultiInputDemo extends BaseDemo {

    public static void main(String[] args) {
        safeExecute("LogFlow 简化多输入演示", () -> {
            demonstrateSimpleMultiInput();
        });
    }

    /**
     * 演示简化的多输入功能
     */
    private static void demonstrateSimpleMultiInput() {
        System.out.println("🔧 LogFlow 简化多输入功能演示\n");

        try {
            WorkflowEngine engine = new WorkflowEngine();

            // 准备测试数据
            Map<String, Object> initialData = new HashMap<>();

            // 为工作流提供输入数据
            initialData.put("user_data", Arrays.asList(
                    createUser("user1", "张三", Arrays.asList("login", "view_page", "logout")),
                    createUser("user2", "李四", Arrays.asList("login", "purchase", "logout"))));

            initialData.put("system_logs", Arrays.asList(
                    createLog("INFO", "用户登录", "user1"),
                    createLog("ERROR", "数据库连接失败", null),
                    createLog("WARN", "内存使用率过高", null)));

            initialData.put("config_params", createConfig(3, 2000, true));

            System.out.printf("✅ 准备测试数据，包含 %d 个初始数据项%n", initialData.size());
            System.out.println("   - user_data: 用户行为数据");
            System.out.println("   - system_logs: 系统日志数据");
            System.out.println("   - config_params: 配置参数");
            System.out.println();

            // 加载并执行多输入工作流
            try {
                WorkflowConfigLoader loader = new WorkflowConfigLoader();
                System.out.println("📖 加载多输入工作流配置...");
                Workflow workflow = loader.loadFromFile("examples/multi-input-workflow.yml");

                System.out.printf("✅ 工作流加载成功: %s%n", workflow.getName());
                System.out.printf("   节点数量: %d%n", workflow.getNodeCount());
                System.out.printf("   连接数量: %d%n", workflow.getConnectionCount());
                System.out.println();

                // 执行工作流
                System.out.println("🚀 执行多输入工作流...");
                var result = engine.execute(workflow, initialData);

                if (result.isSuccess()) {
                    System.out.printf("✅ 工作流执行成功%n");
                    System.out.printf("   执行ID: %s%n", result.getExecutionId());

                    // 显示关键输出数据
                    WorkflowContext resultContext = result.getContext();
                    displayWorkflowResults(resultContext);

                    // 显示执行统计
                    var nodeResults = result.getNodeResults();
                    long totalDuration = nodeResults.values().stream()
                            .mapToLong(nr -> nr.getExecutionDurationMs())
                            .sum();
                    System.out.printf("   执行节点数: %d%n", nodeResults.size());
                    System.out.printf("   总执行时间: %d ms%n", totalDuration);

                } else {
                    System.out.printf("❌ 工作流执行失败: %s%n", result.getMessage());

                    // 显示失败的节点
                    var nodeResults = result.getNodeResults();
                    System.out.println("   失败节点:");
                    nodeResults.entrySet().stream()
                            .filter(entry -> !entry.getValue().isSuccess())
                            .forEach(entry -> {
                                System.out.printf("     - %s: %s%n",
                                        entry.getKey(), entry.getValue().getMessage());
                            });
                }

            } catch (Exception e) {
                System.err.println("❌ 工作流执行失败: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.err.println("❌ 演示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 显示工作流执行结果
     */
    private static void displayWorkflowResults(WorkflowContext context) {
        System.out.println("🎯 工作流执行结果:");

        // 显示用户数据
        Object users = context.getData("users");
        if (users != null) {
            System.out.printf("   用户数据: %s%n", users);
        }

        // 显示日志数据
        Object logs = context.getData("logs");
        if (logs != null && logs instanceof java.util.List) {
            System.out.printf("   日志数据: %d 条%n", ((java.util.List<?>) logs).size());
        }

        // 显示配置数据
        Object config = context.getData("config");
        if (config != null) {
            System.out.printf("   配置数据: %s%n", config);
        }

        // 显示分析结果
        Object analysisResult = context.getData("analysis_result");
        if (analysisResult != null) {
            System.out.printf("   分析结果: %s%n", analysisResult);
        }

        // 显示处理结果
        Object processedData = context.getData("processed_data");
        if (processedData != null) {
            System.out.printf("   处理结果: %s%n", processedData);
        }

        // 显示最终输出
        Object finalOutput = context.getData("formatted_output");
        if (finalOutput != null) {
            System.out.printf("   最终输出: %s%n", finalOutput);
        }

        System.out.println();
    }

    // =====================================
    // 辅助方法
    // =====================================

    /**
     * 创建用户数据
     */
    private static Map<String, Object> createUser(String id, String name, java.util.List<String> actions) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("name", name);
        user.put("actions", actions);
        return user;
    }

    /**
     * 创建日志数据
     */
    private static Map<String, Object> createLog(String level, String message, String userId) {
        Map<String, Object> log = new HashMap<>();
        log.put("timestamp", java.time.LocalDateTime.now().toString());
        log.put("level", level);
        log.put("message", message);
        if (userId != null) {
            log.put("userId", userId);
        }
        return log;
    }

    /**
     * 创建配置数据
     */
    private static Map<String, Object> createConfig(int errorThreshold, int perfThreshold, boolean includeWarnings) {
        Map<String, Object> config = new HashMap<>();
        config.put("error_threshold", errorThreshold);
        config.put("performance_threshold", perfThreshold);
        config.put("include_warnings", includeWarnings);
        config.put("output_format", "detailed");
        return config;
    }
}
