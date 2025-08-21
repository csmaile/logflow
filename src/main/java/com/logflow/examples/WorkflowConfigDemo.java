package com.logflow.examples;

import com.logflow.config.WorkflowConfigLoader;
import com.logflow.engine.Workflow;
import com.logflow.core.WorkflowContext;
import com.logflow.engine.WorkflowEngine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流配置演示
 * 展示如何加载和执行YAML配置的工作流
 */
public class WorkflowConfigDemo extends BaseDemo {

    public static void main(String[] args) {
        safeExecute("LogFlow 工作流配置演示", () -> {
            demonstrateWorkflowConfigurations();
        });
    }

    /**
     * 演示不同的工作流配置
     */
    private static void demonstrateWorkflowConfigurations() {
        System.out.println("🔧 工作流配置文件演示\n");

        try {
            WorkflowConfigLoader loader = new WorkflowConfigLoader();
            WorkflowEngine engine = new WorkflowEngine();

            // 演示简单工作流
            demonstrateSimpleWorkflow(loader, engine);

            System.out.println("\n" + "=".repeat(60) + "\n");

            // 演示综合工作流（仅验证配置，不执行）
            demonstrateComprehensiveWorkflow(loader);

        } catch (Exception e) {
            System.err.println("❌ 演示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 演示简单工作流
     */
    private static void demonstrateSimpleWorkflow(WorkflowConfigLoader loader, WorkflowEngine engine) {
        System.out.println("📋 简单工作流演示");
        System.out.println("=====================================");

        try {
            // 检查配置文件是否存在
            File configFile = new File("examples/simple-workflow.yml");
            if (!configFile.exists()) {
                System.out.println("⚠️ 配置文件不存在: " + configFile.getAbsolutePath());
                System.out.println("   请确保运行演示前已创建配置文件");
                return;
            }

            // 加载工作流配置
            System.out.println("📖 加载配置文件: " + configFile.getName());
            Workflow workflow = loader.loadFromFile(configFile.getAbsolutePath());

            System.out.printf("✅ 工作流加载成功: %s%n", workflow.getName());
            System.out.printf("   节点数量: %d%n", workflow.getNodeCount());
            System.out.printf("   连接数量: %d%n", workflow.getConnectionCount());
            System.out.println();

            // 显示工作流结构
            showWorkflowStructure(workflow);

            // 准备测试数据
            Map<String, Object> initialData = new HashMap<>();
            initialData.put("log_file_path", "examples/sample-logs.json");

            // 创建模拟日志数据
            createSampleLogData();

            // 执行工作流
            System.out.println("🚀 执行简单工作流...");

            var result = engine.execute(workflow, initialData);

            System.out.printf("✅ 工作流执行完成，状态: %s%n", result.isSuccess() ? "成功" : "失败");
            if (!result.isSuccess() && result.getMessage() != null) {
                System.out.printf("   错误信息: %s%n", result.getMessage());
            }

        } catch (Exception e) {
            System.err.println("❌ 简单工作流演示失败: " + e.getMessage());
            // 不抛出异常，继续后面的演示
        }
    }

    /**
     * 演示综合工作流（仅验证配置）
     */
    private static void demonstrateComprehensiveWorkflow(WorkflowConfigLoader loader) {
        System.out.println("📋 综合工作流配置验证");
        System.out.println("=====================================");

        try {
            // 检查配置文件是否存在
            File configFile = new File("examples/comprehensive-workflow.yml");
            if (!configFile.exists()) {
                System.out.println("⚠️ 配置文件不存在: " + configFile.getAbsolutePath());
                System.out.println("   请确保运行演示前已创建配置文件");
                return;
            }

            // 加载工作流配置
            System.out.println("📖 加载综合配置文件: " + configFile.getName());
            Workflow workflow = loader.loadFromFile(configFile.getAbsolutePath());

            System.out.printf("✅ 综合工作流加载成功: %s%n", workflow.getName());
            System.out.printf("   描述: %s%n", workflow.getDescription());
            System.out.printf("   节点数量: %d%n", workflow.getNodeCount());
            System.out.printf("   连接数量: %d%n", workflow.getConnectionCount());
            System.out.println();

            // 显示工作流结构
            showWorkflowStructure(workflow);

            // 分析工作流复杂度
            analyzeWorkflowComplexity(workflow);

            System.out.println("💡 注意：综合工作流包含关联节点和复杂脚本，仅进行配置验证");
            System.out.println("   实际执行需要对应的子工作流和完整的日志数据");

        } catch (Exception e) {
            System.err.println("❌ 综合工作流验证失败: " + e.getMessage());
        }
    }

    /**
     * 显示工作流结构
     */
    private static void showWorkflowStructure(Workflow workflow) {
        System.out.println("🔗 工作流结构:");

        var nodes = workflow.getAllNodes();

        // 显示节点信息
        System.out.println("   节点列表:");
        nodes.forEach(node -> {
            // 大多数节点默认都是启用的，暂时都显示为启用状态
            String status = "✅";
            System.out.printf("     %s %s (%s) - %s%n",
                    status, node.getId(), node.getType().getCode(), node.getName());
        });

        // 显示连接信息
        System.out.println("   连接关系:");
        boolean hasConnections = false;
        for (var node : nodes) {
            var targetNodes = workflow.getTargetNodes(node.getId());
            if (targetNodes != null && !targetNodes.isEmpty()) {
                hasConnections = true;
                for (String targetNodeId : targetNodes) {
                    System.out.printf("     ✅ %s → %s%n", node.getId(), targetNodeId);
                }
            }
        }
        if (!hasConnections) {
            System.out.println("     (无连接关系)");
        }
        System.out.println();
    }

    /**
     * 分析工作流复杂度
     */
    private static void analyzeWorkflowComplexity(Workflow workflow) {
        System.out.println("📊 工作流复杂度分析:");

        var nodes = workflow.getAllNodes();

        // 统计节点类型
        Map<String, Integer> nodeTypeCounts = new HashMap<>();
        nodes.forEach(node -> {
            String type = node.getType().getCode();
            nodeTypeCounts.put(type, nodeTypeCounts.getOrDefault(type, 0) + 1);
        });

        System.out.println("   节点类型分布:");
        nodeTypeCounts.forEach((type, count) -> {
            System.out.printf("     %s: %d 个%n", type, count);
        });

        // 计算并行度和总连接数
        int maxParallelPaths = 0;
        int totalConnections = 0;

        for (var node : nodes) {
            var targetNodes = workflow.getTargetNodes(node.getId());
            if (targetNodes != null) {
                int connectionCount = targetNodes.size();
                totalConnections += connectionCount;
                maxParallelPaths = Math.max(maxParallelPaths, connectionCount);
            }
        }

        // 计算复杂度评分
        int complexityScore = nodes.size() + totalConnections + maxParallelPaths;
        String complexityLevel;
        if (complexityScore < 10) {
            complexityLevel = "简单";
        } else if (complexityScore < 20) {
            complexityLevel = "中等";
        } else {
            complexityLevel = "复杂";
        }

        System.out.printf("   复杂度评分: %d (%s)%n", complexityScore, complexityLevel);
        System.out.printf("   最大并行路径: %d%n", maxParallelPaths);
        System.out.printf("   总连接数: %d%n", totalConnections);
        System.out.println();
    }

    /**
     * 创建示例日志数据
     */
    private static void createSampleLogData() {
        try {
            File examplesDir = new File("examples");
            if (!examplesDir.exists()) {
                examplesDir.mkdirs();
            }

            File sampleLogsFile = new File("examples/sample-logs.json");
            if (!sampleLogsFile.exists()) {
                String sampleLogs = "[\n" +
                        "  {\"timestamp\": \"2024-01-20T10:00:00Z\", \"level\": \"INFO\", \"message\": \"Application started\"},\n"
                        +
                        "  {\"timestamp\": \"2024-01-20T10:01:00Z\", \"level\": \"DEBUG\", \"message\": \"Processing request\"},\n"
                        +
                        "  {\"timestamp\": \"2024-01-20T10:02:00Z\", \"level\": \"ERROR\", \"message\": \"Database connection failed\"},\n"
                        +
                        "  {\"timestamp\": \"2024-01-20T10:03:00Z\", \"level\": \"WARN\", \"message\": \"High memory usage detected\"},\n"
                        +
                        "  {\"timestamp\": \"2024-01-20T10:04:00Z\", \"level\": \"FATAL\", \"message\": \"System critical error\"}\n"
                        +
                        "]";

                try (java.io.FileWriter writer = new java.io.FileWriter(sampleLogsFile)) {
                    writer.write(sampleLogs);
                }
                System.out.println("📝 创建示例日志数据: " + sampleLogsFile.getName());
            }
        } catch (Exception e) {
            System.err.println("⚠️ 创建示例数据失败: " + e.getMessage());
        }
    }
}
