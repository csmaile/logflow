package com.logflow.examples;

import com.logflow.config.WorkflowConfigLoader;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;
import com.logflow.engine.WorkflowExecutionResult;

import java.util.HashMap;
import java.util.Map;

/**
 * YAML工作流配置示例
 * 演示如何使用YAML配置文件来定义和执行工作流
 */
public class YamlWorkflowExample {

    public static void main(String[] args) {
        // 创建工作流引擎
        WorkflowEngine engine = new WorkflowEngine(true, 4);
        WorkflowConfigLoader configLoader = new WorkflowConfigLoader();

        try {
            // 示例1：基础错误检测工作流
            runBasicErrorDetectionFromYaml(engine, configLoader);

            // 示例2：复杂日志分析工作流
            runComplexLogAnalysisFromYaml(engine, configLoader);

            // 示例3：从YAML字符串加载工作流
            runWorkflowFromYamlString(engine, configLoader);

        } catch (Exception e) {
            System.err.println("执行YAML工作流时发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            engine.shutdown();
        }
    }

    /**
     * 示例1：从YAML文件加载基础错误检测工作流
     */
    private static void runBasicErrorDetectionFromYaml(WorkflowEngine engine, WorkflowConfigLoader configLoader) {
        System.out.println("=== 示例1：基础错误检测工作流（YAML配置） ===");

        try {
            // 从资源文件加载YAML配置
            Workflow workflow = configLoader.loadFromResource("workflows/basic-error-detection.yaml");

            // 执行工作流
            WorkflowExecutionResult result = engine.execute(workflow, null);

            // 输出执行结果
            System.out.println("执行成功: " + result.isSuccess());
            System.out.println("执行ID: " + result.getExecutionId());

            if (result.isSuccess()) {
                var stats = result.getStatistics();
                System.out.printf("节点统计: 总数=%d, 成功=%d, 失败=%d, 成功率=%.1f%%\n",
                        stats.getTotalNodes(), stats.getSuccessfulNodes(),
                        stats.getFailedNodes(), stats.getSuccessRate());
            } else {
                System.out.println("错误信息: " + result.getMessage());
            }

        } catch (Exception e) {
            System.err.println("加载或执行工作流失败: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * 示例2：从YAML文件加载复杂日志分析工作流
     */
    private static void runComplexLogAnalysisFromYaml(WorkflowEngine engine, WorkflowConfigLoader configLoader) {
        System.out.println("=== 示例2：复杂日志分析工作流（YAML配置） ===");

        try {
            // 从资源文件加载YAML配置
            Workflow workflow = configLoader.loadFromResource("workflows/complex-log-analysis.yaml");

            // 准备初始数据
            Map<String, Object> initialData = new HashMap<>();
            initialData.put("analysis_config", Map.of(
                    "minLevel", "INFO",
                    "enableAdvancedAnalysis", true,
                    "maxErrors", 20));

            // 执行工作流
            WorkflowExecutionResult result = engine.execute(workflow, initialData);

            // 输出执行结果
            System.out.println("执行成功: " + result.isSuccess());
            System.out.println("执行ID: " + result.getExecutionId());

            if (result.isSuccess()) {
                var stats = result.getStatistics();
                System.out.printf("执行统计: 总节点=%d, 成功=%d, 平均耗时=%.1fms\n",
                        stats.getTotalNodes(), stats.getSuccessfulNodes(),
                        stats.getAverageNodeExecutionTime());
                System.out.println("复杂分析报告已保存到 complex_analysis_report.json");

                // 显示上下文中的预处理统计信息
                Object preprocessingStats = result.getContext().getData("preprocessing_stats");
                if (preprocessingStats != null) {
                    System.out.println("数据预处理统计: " + preprocessingStats);
                }
            } else {
                System.out.println("错误信息: " + result.getMessage());
                // 输出失败节点的详细信息
                result.getFailedNodeResults().forEach(
                        (nodeId, nodeResult) -> System.out.println("节点 " + nodeId + " 失败: " + nodeResult.getMessage()));
            }

        } catch (Exception e) {
            System.err.println("加载或执行复杂工作流失败: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
    }

    /**
     * 示例3：从YAML字符串加载工作流
     */
    private static void runWorkflowFromYamlString(WorkflowEngine engine, WorkflowConfigLoader configLoader) {
        System.out.println("=== 示例3：从YAML字符串加载工作流 ===");

        String yamlContent = "workflow:\n" +
                "  id: \"simple_yaml_test\"\n" +
                "  name: \"简单YAML测试\"\n" +
                "  description: \"从字符串加载的简单测试工作流\"\n" +
                "  version: \"1.0.0\"\n" +
                "\n" +
                "nodes:\n" +
                "  - id: \"mock_data\"\n" +
                "    name: \"模拟数据生成\"\n" +
                "    type: \"datasource\"\n" +
                "    config:\n" +
                "      sourceType: \"mock\"\n" +
                "      mockType: \"simple\"\n" +
                "      count: 10\n" +
                "      outputKey: \"test_data\"\n" +
                "  \n" +
                "  - id: \"data_processor\"\n" +
                "    name: \"数据处理器\"\n" +
                "    type: \"script\"\n" +
                "    config:\n" +
                "      scriptEngine: \"javascript\"\n" +
                "      inputKey: \"test_data\"\n" +
                "      outputKey: \"processed_data\"\n" +
                "      script: |\n" +
                "        var data = input;\n" +
                "        var processed = data.map(function(item, index) {\n" +
                "          return {\n" +
                "            id: item.id,\n" +
                "            message: item.message,\n" +
                "            processed_at: utils.now(),\n" +
                "            index: index\n" +
                "          };\n" +
                "        });\n" +
                "        logger.info('处理了 ' + processed.length + ' 条数据');\n" +
                "        processed;\n" +
                "  \n" +
                "  - id: \"result_output\"\n" +
                "    name: \"结果输出\"\n" +
                "    type: \"output\"\n" +
                "    config:\n" +
                "      inputKey: \"processed_data\"\n" +
                "      outputType: \"console\"\n" +
                "      format: \"json\"\n" +
                "\n" +
                "connections:\n" +
                "  - from: \"mock_data\"\n" +
                "    to: \"data_processor\"\n" +
                "  - from: \"data_processor\"\n" +
                "    to: \"result_output\"\n";

        try {
            // 从YAML字符串加载工作流
            Workflow workflow = configLoader.loadFromYamlString(yamlContent);

            // 执行工作流
            WorkflowExecutionResult result = engine.execute(workflow, null);

            // 输出执行结果
            System.out.println("执行成功: " + result.isSuccess());
            System.out.println("执行ID: " + result.getExecutionId());

            if (result.isSuccess()) {
                System.out.println("YAML字符串工作流执行成功！");
            } else {
                System.out.println("错误信息: " + result.getMessage());
            }

        } catch (Exception e) {
            System.err.println("从YAML字符串加载工作流失败: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * 示例：创建一个文件监控工作流的YAML配置（仅展示配置生成）
     */
    public static void demonstrateFileMonitoringWorkflow() {
        System.out.println("=== 文件监控工作流YAML配置示例 ===");

        String fileMonitoringYaml = "workflow:\n" +
                "  id: \"file_monitoring\"\n" +
                "  name: \"文件监控分析\"\n" +
                "  description: \"监控文件变化并进行日志分析\"\n" +
                "  version: \"1.0.0\"\n" +
                "  author: \"LogFlow Team\"\n" +
                "\n" +
                "globalConfig:\n" +
                "  monitorInterval: 5000\n" +
                "  maxFileSize: 104857600  # 100MB\n" +
                "  \n" +
                "nodes:\n" +
                "  - id: \"file_monitor\"\n" +
                "    name: \"文件监控器\"\n" +
                "    type: \"datasource\"\n" +
                "    config:\n" +
                "      sourceType: \"file\"\n" +
                "      filePath: \"/var/log/application.log\"\n" +
                "      format: \"lines\"\n" +
                "      outputKey: \"log_lines\"\n" +
                "      watchMode: true\n" +
                "      \n" +
                "  # 更多节点配置...";

        System.out.println("文件监控工作流YAML配置:");
        System.out.println(fileMonitoringYaml);
        System.out.println();
    }
}
