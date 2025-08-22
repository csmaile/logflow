package com.logflow.examples;

import com.logflow.config.WorkflowConfigLoader;
import com.logflow.core.InputDataProcessor;
import com.logflow.core.MultiInputConfig;
import com.logflow.core.WorkflowContext;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;
import com.logflow.nodes.EnhancedScriptNode;

import java.io.File;
import java.util.*;
import java.util.List;
import java.util.ArrayList;

/**
 * 多输入参数功能演示
 * 展示节点如何支持多个输入参数
 */
public class MultiInputDemo extends BaseDemo {

    public static void main(String[] args) {
        safeExecute("LogFlow 多输入参数功能演示", () -> {
            demonstrateMultiInputFeatures();
        });
    }

    /**
     * 演示多输入功能
     */
    private static void demonstrateMultiInputFeatures() {
        System.out.println("🔧 LogFlow 多输入参数功能演示\n");

        try {
            // 1. 演示多输入配置和处理
            demonstrateInputConfiguration();

            System.out.println("\n" + "=".repeat(60) + "\n");

            // 2. 演示增强脚本节点
            demonstrateEnhancedScriptNode();

            System.out.println("\n" + "=".repeat(60) + "\n");

            // 3. 演示完整的多输入工作流
            demonstrateMultiInputWorkflow();

        } catch (Exception e) {
            System.err.println("❌ 演示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 演示输入配置和处理
     */
    private static void demonstrateInputConfiguration() {
        System.out.println("📋 多输入配置演示");
        System.out.println("=====================================");

        // 创建工作流上下文并准备测试数据
        WorkflowContext context = new WorkflowContext("demo_workflow", "demo_execution");

        // 准备测试数据
        context.setData("users", Arrays.asList(
                createUser("user1", "张三", Arrays.asList("login", "view_page")),
                createUser("user2", "李四", Arrays.asList("login", "purchase"))));

        context.setData("logs", Arrays.asList(
                createLog("INFO", "用户登录", "user1"),
                createLog("ERROR", "数据库连接失败", null),
                createLog("WARN", "内存使用率过高", null)));

        context.setData("config", createConfig(5, 2000, true));

        System.out.println("✅ 准备测试数据:");
        Object usersData = context.getData("users");
        Object logsData = context.getData("logs");
        Object configData = context.getData("config");

        System.out.printf("   用户数据: %s%n", usersData);
        if (logsData instanceof java.util.List) {
            System.out.printf("   日志数据: %d 条%n", ((java.util.List<?>) logsData).size());
        } else {
            System.out.printf("   日志数据: %s%n", logsData);
        }
        System.out.printf("   配置数据: %s%n", configData);
        System.out.println();

        // 演示不同的输入模式
        demonstrateInputMode("单输入模式 (SINGLE)", createSingleInputConfig(), context);
        demonstrateInputMode("多输入模式 (MULTIPLE)", createMultipleInputConfig(), context);
        demonstrateInputMode("合并输入模式 (MERGED)", createMergedInputConfig(), context);
    }

    /**
     * 演示特定输入模式
     */
    private static void demonstrateInputMode(String modeName, MultiInputConfig inputConfig, WorkflowContext context) {
        System.out.printf("🔍 %s%n", modeName);
        System.out.println("-".repeat(40));

        InputDataProcessor.InputDataResult result = InputDataProcessor.processInputData(context, inputConfig,
                "demo_node");

        if (result.isSuccess()) {
            System.out.printf("✅ 处理成功%n");
            System.out.printf("   数据类型: %s%n",
                    result.getData() != null ? result.getData().getClass().getSimpleName() : "null");
            System.out.printf("   元数据: %s%n", result.getMetadata());

            if (result.getData() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) result.getData();
                System.out.printf("   输入键: %s%n", dataMap.keySet());
            }
        } else {
            System.out.printf("❌ 处理失败: %s%n", result.getErrorMessage());
        }
        System.out.println();
    }

    /**
     * 演示增强脚本节点
     */
    private static void demonstrateEnhancedScriptNode() {
        System.out.println("📋 增强脚本节点演示");
        System.out.println("=====================================");

        try {
            // 创建增强脚本节点
            EnhancedScriptNode scriptNode = new EnhancedScriptNode("enhanced_script", "增强脚本测试");

            // 配置多输入脚本
            Map<String, Object> config = createEnhancedScriptConfig();
            scriptNode.setConfiguration(config);

            // 创建上下文和数据
            WorkflowContext context = new WorkflowContext("enhanced_test", "enhanced_execution");
            context.setData("input1", "第一个输入");
            context.setData("input2", Arrays.asList("数据1", "数据2", "数据3"));
            context.setData("input3", createConfig(3, 1500, false));

            System.out.println("🔧 脚本节点配置:");
            System.out.printf("   输入模式: %s%n", config.get("inputMode"));
            System.out.printf("   脚本引擎: %s%n", config.get("scriptEngine"));
            System.out.printf("   输出键: %s%n", config.get("outputKey"));
            System.out.println();

            // 验证配置
            var validation = scriptNode.validate();
            if (validation.isValid()) {
                System.out.println("✅ 节点配置验证通过");
            } else {
                System.out.printf("❌ 节点配置验证失败: %s%n", validation.getErrors());
                return;
            }

            // 执行脚本节点
            System.out.println("🚀 执行增强脚本节点...");
            var executionResult = scriptNode.execute(context);

            if (executionResult.isSuccess()) {
                System.out.printf("✅ 脚本执行成功%n");
                System.out.printf("   执行时间: %d ms%n", executionResult.getExecutionDurationMs());
                System.out.printf("   输出数据: %s%n", executionResult.getData());
                System.out.printf("   元数据: %s%n", executionResult.getMetadata());

                // 检查输出是否正确设置到上下文
                Object outputData = context.getData("enhanced_output");
                System.out.printf("   上下文输出: %s%n", outputData);
            } else {
                System.out.printf("❌ 脚本执行失败: %s%n", executionResult.getMessage());
            }

        } catch (Exception e) {
            System.err.println("❌ 增强脚本节点演示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 演示完整的多输入工作流
     */
    private static void demonstrateMultiInputWorkflow() {
        System.out.println("📋 多输入工作流演示");
        System.out.println("=====================================");

        try {
            // 检查配置文件是否存在
            File configFile = new File("examples/multi-input-workflow.yml");
            if (!configFile.exists()) {
                System.out.println("⚠️ 多输入工作流配置文件不存在，跳过此演示");
                System.out.println("   文件路径: " + configFile.getAbsolutePath());
                return;
            }

            // 加载工作流配置
            WorkflowConfigLoader loader = new WorkflowConfigLoader();
            System.out.println("📖 加载多输入工作流配置...");
            Workflow workflow = loader.loadFromFile(configFile.getAbsolutePath());

            System.out.printf("✅ 工作流加载成功: %s%n", workflow.getName());
            System.out.printf("   节点数量: %d%n", workflow.getNodeCount());
            System.out.printf("   连接数量: %d%n", workflow.getConnectionCount());
            System.out.println();

            // 准备初始数据
            Map<String, Object> initialData = new HashMap<>();
            // 注意：初始数据会被input节点的defaultValue覆盖，这里主要是为了展示

            // 执行工作流
            WorkflowEngine engine = new WorkflowEngine();
            System.out.println("🚀 执行多输入工作流...");

            var result = engine.execute(workflow, initialData);

            if (result.isSuccess()) {
                System.out.printf("✅ 工作流执行成功%n");
                System.out.printf("   执行ID: %s%n", result.getExecutionId());

                // 显示最终结果
                Object finalOutput = result.getContext().getData("formatted_output");
                if (finalOutput != null) {
                    System.out.printf("   最终输出: %s%n", finalOutput);
                }

                // 显示节点执行结果统计
                var nodeResults = result.getNodeResults();
                System.out.printf("   执行节点数: %d%n", nodeResults.size());

                long totalDuration = nodeResults.values().stream()
                        .mapToLong(nr -> nr.getExecutionDurationMs())
                        .sum();
                System.out.printf("   总执行时间: %d ms%n", totalDuration);

            } else {
                System.out.printf("❌ 工作流执行失败: %s%n", result.getMessage());
            }

        } catch (Exception e) {
            System.err.println("❌ 多输入工作流演示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =====================================
    // 辅助方法
    // =====================================

    /**
     * 创建简单多输入配置（单个输入参数）
     */
    private static MultiInputConfig createSingleInputConfig() {
        MultiInputConfig config = new MultiInputConfig();
        config.setMode(MultiInputConfig.InputMode.MULTIPLE);
        config.setOutputKey("output");

        // 创建单个输入参数（模拟单输入效果）
        List<MultiInputConfig.InputParameter> inputs = new ArrayList<>();
        MultiInputConfig.InputParameter param = new MultiInputConfig.InputParameter();
        param.setKey("users");
        param.setAlias("users");
        param.setRequired(true);
        param.setDataType("array");
        param.setDescription("用户数据");
        inputs.add(param);

        config.setInputs(inputs);
        return config;
    }

    /**
     * 创建多输入配置
     */
    private static MultiInputConfig createMultipleInputConfig() {
        MultiInputConfig config = new MultiInputConfig();
        config.setMode(MultiInputConfig.InputMode.MULTIPLE);
        config.setOutputKey("output");

        java.util.List<MultiInputConfig.InputParameter> inputs = new java.util.ArrayList<>();

        // 用户数据参数
        MultiInputConfig.InputParameter userParam = new MultiInputConfig.InputParameter();
        userParam.setKey("users");
        userParam.setAlias("userList");
        userParam.setRequired(true);
        userParam.setDataType("array");
        userParam.setDescription("用户数据列表");
        inputs.add(userParam);

        // 日志数据参数
        MultiInputConfig.InputParameter logParam = new MultiInputConfig.InputParameter();
        logParam.setKey("logs");
        logParam.setAlias("logData");
        logParam.setRequired(true);
        logParam.setDataType("array");
        logParam.setDescription("系统日志数据");
        inputs.add(logParam);

        // 配置参数（可选）
        MultiInputConfig.InputParameter configParam = new MultiInputConfig.InputParameter();
        configParam.setKey("config");
        configParam.setAlias("settings");
        configParam.setRequired(false);
        configParam.setDefaultValue(createConfig(3, 1000, true));
        configParam.setDataType("object");
        configParam.setDescription("配置参数");
        inputs.add(configParam);

        config.setInputs(inputs);
        return config;
    }

    /**
     * 创建合并输入配置
     */
    private static MultiInputConfig createMergedInputConfig() {
        MultiInputConfig config = createMultipleInputConfig();
        config.setMode(MultiInputConfig.InputMode.MERGED);
        config.setMergeKey("allData");
        return config;
    }

    /**
     * 创建增强脚本配置
     */
    private static Map<String, Object> createEnhancedScriptConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("scriptEngine", "javascript");
        config.put("outputKey", "enhanced_output");
        config.put("inputMode", "MULTIPLE");

        // 多输入配置
        java.util.List<Map<String, Object>> inputs = new java.util.ArrayList<>();

        inputs.add(Map.of(
                "key", "input1",
                "alias", "text",
                "required", true,
                "dataType", "string",
                "description", "文本输入"));

        inputs.add(Map.of(
                "key", "input2",
                "alias", "items",
                "required", true,
                "dataType", "array",
                "description", "数组输入"));

        inputs.add(Map.of(
                "key", "input3",
                "alias", "options",
                "required", false,
                "defaultValue", Map.of("default", "value"),
                "dataType", "object",
                "description", "选项配置"));

        config.put("inputs", inputs);

        // 脚本内容
        config.put("script",
                "logger.info('增强脚本开始执行');\n" +
                        "logger.info('文本输入: ' + text);\n" +
                        "logger.info('数组长度: ' + items.length);\n" +
                        "logger.info('选项配置: ' + JSON.stringify(options));\n" +
                        "\n" +
                        "var result = {\n" +
                        "  processed_at: utils.now(),\n" +
                        "  text_length: text.length,\n" +
                        "  item_count: items.length,\n" +
                        "  options: options,\n" +
                        "  combined_data: text + ' - ' + items.join(', ')\n" +
                        "};\n" +
                        "\n" +
                        "logger.info('增强脚本执行完成');\n" +
                        "result;");

        return config;
    }

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
        return config;
    }
}
