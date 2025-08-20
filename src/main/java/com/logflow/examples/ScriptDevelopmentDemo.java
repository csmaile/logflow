package com.logflow.examples;

import com.logflow.config.WorkflowConfigLoader;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;
import com.logflow.engine.WorkflowExecutionResult;

import java.util.Map;

/**
 * 脚本开发功能演示
 * 展示如何使用改进的脚本开发体验
 */
public class ScriptDevelopmentDemo {

    public static void main(String[] args) {
        System.out.println("=== LogFlow脚本开发功能演示 ===\n");

        // 显示脚本开发改进功能介绍
        showScriptDevelopmentFeatures();

        // 演示使用开发好的脚本配置
        demonstrateScriptWorkflow();

        System.out.println("\n=== 演示完成 ===");
        System.out.println("💡 提示：查看 SCRIPT_DEVELOPMENT_GUIDE.md 了解完整的脚本开发指南");
    }

    /**
     * 介绍脚本开发改进功能
     */
    private static void showScriptDevelopmentFeatures() {
        System.out.println("🎯 脚本开发问题解决方案：");
        System.out.println();

        System.out.println("❌ 原有问题：");
        System.out.println("   - 在YAML中编写JavaScript缺乏智能提示");
        System.out.println("   - IDE无法识别LogFlow特有的API（context, logger, utils）");
        System.out.println("   - 出现未定义变量的编译警告");
        System.out.println("   - 缺少代码补全和错误检查");
        System.out.println();

        System.out.println("✅ 解决方案：");
        System.out.println("   1. TypeScript定义文件 (logflow.d.ts)");
        System.out.println("      - 提供完整的API类型定义");
        System.out.println("      - 智能提示和参数说明");
        System.out.println("      - 类型检查和错误预防");
        System.out.println();

        System.out.println("   2. VS Code代码片段");
        System.out.println("      - 7个常用脚本模板");
        System.out.println("      - 快速生成数据过滤、转换、分析脚本");
        System.out.println("      - 输入前缀自动补全");
        System.out.println();

        System.out.println("   3. 独立开发环境");
        System.out.println("      - 在IDE中开发完整脚本");
        System.out.println("      - 享受完整的智能提示和调试功能");
        System.out.println("      - 开发完成后复制到YAML配置");
        System.out.println();

        System.out.println("   4. 改进的Schema文档");
        System.out.println("      - 脚本字段包含详细的API说明");
        System.out.println("      - 提供实用的脚本示例");
        System.out.println("      - 引导用户使用开发工具");
        System.out.println();

        System.out.println("📁 文件位置：");
        System.out.println("   - TypeScript定义：src/main/resources/scripts/logflow.d.ts");
        System.out.println("   - 代码片段：src/main/resources/scripts/vscode-snippets.json");
        System.out.println("   - 开发示例：src/main/resources/scripts/script-development-example.js");
        System.out.println("   - 开发指南：SCRIPT_DEVELOPMENT_GUIDE.md");
        System.out.println();
    }

    /**
     * 演示使用脚本的工作流
     */
    private static void demonstrateScriptWorkflow() {
        System.out.println("🚀 演示脚本工作流执行：");
        System.out.println();

        // 创建工作流引擎和配置加载器
        WorkflowEngine engine = new WorkflowEngine();
        WorkflowConfigLoader configLoader = new WorkflowConfigLoader();

        try {
            // 使用包含脚本的复杂工作流
            System.out.println("📋 加载包含脚本节点的复杂工作流...");
            Workflow workflow = configLoader.loadFromResource("workflows/complex-log-analysis.yaml");

            System.out.println("✅ 工作流加载成功");
            System.out.println("   - 工作流ID: " + workflow.getId());
            System.out.println("   - 节点总数: " + workflow.getNodeCount());
            System.out.println("   - 脚本节点: 包含数据预处理和结果聚合脚本");
            System.out.println();

            // 准备执行参数
            Map<String, Object> initialData = Map.of(
                    "analysis_config", Map.of(
                            "minLevel", "INFO",
                            "enableAdvancedAnalysis", true,
                            "scriptType", "advanced"));

            System.out.println("⚡ 执行工作流（包含JavaScript脚本处理）...");
            long startTime = System.currentTimeMillis();

            WorkflowExecutionResult result = engine.execute(workflow, initialData);

            long executionTime = System.currentTimeMillis() - startTime;

            // 显示执行结果
            if (result.isSuccess()) {
                System.out.println("✅ 工作流执行成功");
                System.out.println("   - 执行时间: " + executionTime + "ms");
                System.out.println("   - 成功节点: " + result.getStatistics().getSuccessfulNodes());
                System.out.println("   - 脚本处理: 数据预处理和结果聚合脚本执行完成");

                // 显示脚本处理的统计信息
                Object preprocessingStats = result.getContext().getData("preprocessing_stats");
                if (preprocessingStats != null) {
                    System.out.println("   - 预处理统计: " + preprocessingStats);
                }

                System.out.println("   - 输出文件: complex_analysis_report.json");
                System.out.println();

                System.out.println("🔍 脚本执行特点：");
                System.out.println("   - 数据预处理脚本: 过滤和转换输入数据");
                System.out.println("   - 结果聚合脚本: 合并多个诊断结果");
                System.out.println("   - 上下文操作: 脚本间通过context共享数据");
                System.out.println("   - 日志输出: 脚本使用logger记录处理过程");
                System.out.println("   - 工具函数: 使用utils.now()获取时间戳");

            } else {
                System.out.println("❌ 工作流执行失败");
                System.out.println("   - 错误信息: " + result.getMessage());
                result.getFailedNodeResults().forEach((nodeId, nodeResult) -> System.out
                        .println("   - 失败节点 " + nodeId + ": " + nodeResult.getMessage()));
            }

        } catch (Exception e) {
            System.err.println("❌ 演示执行失败: " + e.getMessage());
        } finally {
            engine.shutdown();
        }

        System.out.println();
        System.out.println("💡 脚本开发提示：");
        System.out.println("   1. 复制 logflow.d.ts 到您的IDE工作区以获得智能提示");
        System.out.println("   2. 使用代码片段快速生成常用脚本模板");
        System.out.println("   3. 在独立的.js文件中开发复杂脚本，然后复制到YAML");
        System.out.println("   4. 参考 script-development-example.js 了解高级脚本模式");
        System.out.println("   5. 阅读 SCRIPT_DEVELOPMENT_GUIDE.md 获得完整指南");
    }
}
