package com.logflow.examples;

import com.logflow.config.WorkflowConfigLoader;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;
import com.logflow.engine.WorkflowExecutionResult;

/**
 * 简单的YAML配置演示
 * 展示如何使用YAML配置快速创建和运行工作流
 */
public class SimpleYamlDemo {

    public static void main(String[] args) {
        System.out.println("=== LogFlow YAML配置演示 ===");

        // 创建工作流引擎和配置加载器
        WorkflowEngine engine = new WorkflowEngine();
        WorkflowConfigLoader configLoader = new WorkflowConfigLoader();

        try {
            // 从资源文件加载YAML配置
            System.out.println("1. 加载YAML配置文件...");
            Workflow workflow = configLoader.loadFromResource("workflows/basic-error-detection.yaml");

            System.out.println("2. 工作流信息:");
            System.out.println("   - ID: " + workflow.getId());
            System.out.println("   - 名称: " + workflow.getName());
            System.out.println("   - 描述: " + workflow.getDescription());
            System.out.println("   - 节点数: " + workflow.getNodeCount());
            System.out.println("   - 连接数: " + workflow.getConnectionCount());

            // 执行工作流
            System.out.println("\n3. 执行工作流...");
            WorkflowExecutionResult result = engine.execute(workflow, null);

            // 显示执行结果
            System.out.println("\n4. 执行结果:");
            System.out.println("   - 成功: " + result.isSuccess());
            System.out.println("   - 执行ID: " + result.getExecutionId());

            if (result.isSuccess()) {
                var stats = result.getStatistics();
                System.out.printf("   - 统计: 总节点=%d, 成功=%d, 成功率=%.1f%%\n",
                        stats.getTotalNodes(), stats.getSuccessfulNodes(), stats.getSuccessRate());
                System.out.printf("   - 平均执行时间: %.1fms\n", stats.getAverageNodeExecutionTime());
            } else {
                System.out.println("   - 错误信息: " + result.getMessage());
            }

            System.out.println("\n✅ YAML配置演示完成！");

        } catch (Exception e) {
            System.err.println("❌ 演示失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            engine.shutdown();
        }
    }
}
