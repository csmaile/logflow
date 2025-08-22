package com.logflow.examples;

import com.logflow.config.WorkflowConfigLoader;
import com.logflow.core.WorkflowContext;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

/**
 * 通用多输入配置演示
 * 展示所有节点类型的多输入配置功能
 */
public class UniversalMultiInputDemo extends BaseDemo {

    public static void main(String[] args) {
        safeExecute("LogFlow 通用多输入配置演示", () -> {
            demonstrateUniversalMultiInput();
        });
    }

    /**
     * 演示通用多输入配置功能
     */
    private static void demonstrateUniversalMultiInput() {
        System.out.println("🌟 LogFlow 通用多输入配置功能演示");
        System.out.println("演示所有节点类型的多输入参数配置支持\n");

        try {
            WorkflowEngine engine = new WorkflowEngine();

            // 准备综合测试数据
            Map<String, Object> initialData = prepareComprehensiveTestData();

            System.out.printf("✅ 准备综合测试数据，包含 %d 个数据源%n", initialData.size());
            displayTestDataSummary(initialData);
            System.out.println();

            // 加载通用多输入工作流
            try {
                WorkflowConfigLoader loader = new WorkflowConfigLoader();
                System.out.println("📖 加载通用多输入工作流配置...");
                Workflow workflow = loader.loadFromFile("examples/universal-multi-input-workflow.yml");

                System.out.printf("✅ 工作流加载成功: %s%n", workflow.getName());
                System.out.printf("   节点数量: %d 个%n", workflow.getNodeCount());
                System.out.printf("   连接数量: %d 个%n", workflow.getConnectionCount());
                System.out.printf("   描述: %s%n", workflow.getDescription());
                System.out.println();

                // 显示节点类型统计
                displayNodeTypeStatistics(workflow);

                // 执行工作流
                System.out.println("🚀 执行通用多输入工作流...");
                var result = engine.execute(workflow, initialData);

                if (result.isSuccess()) {
                    System.out.printf("✅ 工作流执行成功！%n");
                    System.out.printf("   执行ID: %s%n", result.getExecutionId());

                    // 显示执行结果摘要
                    displayExecutionSummary(result);

                    // 显示各节点的多输入处理结果
                    displayMultiInputResults(result.getContext());

                } else {
                    System.out.printf("❌ 工作流执行失败: %s%n", result.getMessage());

                    // 显示失败的节点详情
                    displayFailedNodes(result);
                }

            } catch (Exception e) {
                System.err.println("❌ 工作流执行异常: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.err.println("❌ 演示程序异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 准备综合测试数据
     */
    private static Map<String, Object> prepareComprehensiveTestData() {
        Map<String, Object> data = new HashMap<>();

        // 原始日志数据
        data.put("raw_logs", Arrays.asList(
                createLogEntry("INFO", "用户登录成功", "user123"),
                createLogEntry("ERROR", "数据库连接失败", null),
                createLogEntry("WARN", "内存使用率过高", null),
                createLogEntry("INFO", "数据处理完成", "batch001"),
                createLogEntry("FATAL", "系统崩溃", "system")));

        // 用户档案信息
        Map<String, Object> userProfiles = new HashMap<>();
        userProfiles.put("user123", createUserProfile("user123", "张三", "admin"));
        userProfiles.put("user456", createUserProfile("user456", "李四", "user"));
        data.put("user_profiles", userProfiles);

        // 系统配置
        data.put("system_config", createSystemConfig());

        // 处理规则
        data.put("processing_rules", createProcessingRules());

        // 输出格式配置
        data.put("output_format", "detailed_json");

        // 诊断配置
        data.put("diagnosis_config", createDiagnosisConfig());

        // 数据增强规则
        data.put("enrichment_rules", createEnrichmentRules());

        // 通知设置
        data.put("notification_settings", createNotificationSettings());

        // 验证规则
        data.put("validation_rules", createValidationRules());

        return data;
    }

    /**
     * 显示测试数据摘要
     */
    private static void displayTestDataSummary(Map<String, Object> data) {
        System.out.println("   📊 测试数据摘要:");

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String valueInfo = getValueInfo(value);
            System.out.printf("     - %s: %s%n", key, valueInfo);
        }
    }

    /**
     * 显示节点类型统计
     */
    private static void displayNodeTypeStatistics(Workflow workflow) {
        System.out.println("🏗️ 节点类型统计:");

        Map<String, Integer> nodeTypeCounts = new HashMap<>();
        workflow.getAllNodes().forEach(node -> {
            String type = node.getType().name().toLowerCase();
            nodeTypeCounts.put(type, nodeTypeCounts.getOrDefault(type, 0) + 1);
        });

        nodeTypeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    System.out.printf("   - %s: %d 个节点%n",
                            entry.getKey().toUpperCase(), entry.getValue());
                });
        System.out.println();
    }

    /**
     * 显示执行摘要
     */
    private static void displayExecutionSummary(com.logflow.engine.WorkflowExecutionResult result) {
        var nodeResults = result.getNodeResults();

        long totalDuration = nodeResults.values().stream()
                .mapToLong(nr -> nr.getExecutionDurationMs())
                .sum();

        long successCount = nodeResults.values().stream()
                .mapToLong(nr -> nr.isSuccess() ? 1 : 0)
                .sum();

        System.out.println("📈 执行摘要:");
        System.out.printf("   总节点数: %d%n", nodeResults.size());
        System.out.printf("   成功节点: %d%n", successCount);
        System.out.printf("   失败节点: %d%n", nodeResults.size() - successCount);
        System.out.printf("   总执行时间: %d ms%n", totalDuration);
        System.out.println();
    }

    /**
     * 显示多输入处理结果
     */
    private static void displayMultiInputResults(WorkflowContext context) {
        System.out.println("🎯 多输入处理结果:");

        // 显示关键输出数据
        Object consolidatedInput = context.getData("consolidated_input");
        if (consolidatedInput != null) {
            System.out.printf("   ✓ 多源输入合并: %s%n", getValueInfo(consolidatedInput));
        }

        Object processedData = context.getData("processed_data");
        if (processedData != null) {
            System.out.printf("   ✓ 脚本多输入处理: %s%n", getValueInfo(processedData));
        }

        Object diagnosisResult = context.getData("diagnosis_result");
        if (diagnosisResult != null) {
            System.out.printf("   ✓ 诊断多输入分析: %s%n", getValueInfo(diagnosisResult));
        }

        Object enrichedData = context.getData("enriched_data");
        if (enrichedData != null) {
            System.out.printf("   ✓ 插件多输入增强: %s%n", getValueInfo(enrichedData));
        }

        Object validationResult = context.getData("validation_result");
        if (validationResult != null) {
            System.out.printf("   ✓ 向后兼容验证: %s%n", getValueInfo(validationResult));
        }

        System.out.println();
    }

    /**
     * 显示失败的节点
     */
    private static void displayFailedNodes(com.logflow.engine.WorkflowExecutionResult result) {
        System.out.println("   失败节点详情:");

        result.getNodeResults().entrySet().stream()
                .filter(entry -> !entry.getValue().isSuccess())
                .forEach(entry -> {
                    var nodeResult = entry.getValue();
                    System.out.printf("     ❌ %s: %s%n",
                            entry.getKey(), nodeResult.getMessage());
                    System.out.printf("        执行时间: %d ms%n",
                            nodeResult.getExecutionDurationMs());
                });
    }

    // =====================================
    // 辅助数据创建方法
    // =====================================

    private static Map<String, Object> createLogEntry(String level, String message, String userId) {
        Map<String, Object> log = new HashMap<>();
        log.put("timestamp", java.time.LocalDateTime.now().toString());
        log.put("level", level);
        log.put("message", message);
        if (userId != null) {
            log.put("userId", userId);
        }
        return log;
    }

    private static Map<String, Object> createUserProfile(String id, String name, String role) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", id);
        profile.put("name", name);
        profile.put("role", role);
        profile.put("active", true);
        return profile;
    }

    private static Map<String, Object> createSystemConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("processing_mode", "enhanced");
        config.put("batch_size", 500);
        config.put("enable_caching", true);
        config.put("log_level", "INFO");
        return config;
    }

    private static Map<String, Object> createProcessingRules() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("error_threshold", 3);
        rules.put("warning_patterns", Arrays.asList("WARN", "WARNING", "CAUTION"));
        rules.put("info_patterns", Arrays.asList("INFO", "DEBUG", "TRACE"));
        return rules;
    }

    private static Map<String, Object> createDiagnosisConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("analysis_depth", "comprehensive");
        config.put("include_recommendations", true);
        config.put("severity_threshold", "medium");
        return config;
    }

    private static Map<String, Object> createEnrichmentRules() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("enable_external_lookup", true);
        rules.put("cache_results", true);
        rules.put("timeout_ms", 5000);
        return rules;
    }

    private static Map<String, Object> createNotificationSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("urgency", "high");
        settings.put("include_details", true);
        settings.put("format", "comprehensive");
        return settings;
    }

    private static Map<String, Object> createValidationRules() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("strict_mode", true);
        rules.put("check_completeness", true);
        rules.put("validate_types", true);
        return rules;
    }

    private static String getValueInfo(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof List) {
            return String.format("List[%d items]", ((List<?>) value).size());
        } else if (value instanceof Map) {
            return String.format("Map[%d keys]", ((Map<?, ?>) value).size());
        } else {
            return value.getClass().getSimpleName() + ": " +
                    (value.toString().length() > 50 ? value.toString().substring(0, 47) + "..." : value.toString());
        }
    }
}
