package com.logflow.examples;

import com.logflow.ai.LLMScriptGenerator;
import com.logflow.ai.MockLLMProvider;
import com.logflow.ai.WorkflowContextAnalyzer;
import com.logflow.config.WorkflowConfigLoader;
import com.logflow.engine.Workflow;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM脚本生成功能演示
 * 展示如何使用大语言模型根据用户需求生成LogFlow脚本
 */
public class LLMScriptGenerationDemo {

    public static void main(String[] args) {
        System.out.println("=== LogFlow LLM脚本生成功能演示 ===\n");

        try {
            // 介绍功能
            introduceLLMScriptGeneration();

            // 演示不同类型的脚本生成
            demonstrateScriptGeneration();

            System.out.println("\n=== 演示完成 ===");
            System.out.println("💡 提示：实际使用时请替换MockLLMProvider为真实的LLM API调用");

        } catch (Exception e) {
            System.err.println("❌ 演示执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 介绍LLM脚本生成功能
     */
    private static void introduceLLMScriptGeneration() {
        System.out.println("🤖 LLM脚本生成功能介绍：");
        System.out.println();

        System.out.println("💡 解决的问题：");
        System.out.println("   - 用户不熟悉JavaScript编程");
        System.out.println("   - 不了解LogFlow特有的API和上下文");
        System.out.println("   - 需要快速生成符合需求的脚本代码");
        System.out.println();

        System.out.println("🎯 核心功能：");
        System.out.println("   1. 自动分析工作流上下文环境");
        System.out.println("   2. 理解用户的自然语言需求描述");
        System.out.println("   3. 生成符合LogFlow规范的JavaScript脚本");
        System.out.println("   4. 提供脚本验证和使用指导");
        System.out.println();

        System.out.println("🔧 工作原理：");
        System.out.println("   1. 分析当前节点在工作流中的位置和环境");
        System.out.println("   2. 识别可用的输入数据和上下文信息");
        System.out.println("   3. 构建包含完整上下文的LLM提示");
        System.out.println("   4. 调用LLM生成脚本并进行后处理");
        System.out.println("   5. 验证脚本质量并提供使用指导");
        System.out.println();
    }

    /**
     * 演示脚本生成
     */
    private static void demonstrateScriptGeneration() throws Exception {
        System.out.println("🚀 开始脚本生成演示：");
        System.out.println();

        // 创建LLM脚本生成器（使用模拟提供者）
        MockLLMProvider mockProvider = new MockLLMProvider();
        LLMScriptGenerator generator = new LLMScriptGenerator(mockProvider);

        // 加载示例工作流
        WorkflowConfigLoader configLoader = new WorkflowConfigLoader();
        Workflow workflow = configLoader.loadFromResource("workflows/complex-log-analysis.yaml");

        System.out.println("📋 使用工作流: " + workflow.getName());
        System.out.println("   节点数: " + workflow.getNodeCount());
        System.out.println();

        // 演示不同类型的脚本生成需求
        demonstrateFilterScript(generator, workflow);
        demonstrateAnalysisScript(generator, workflow);
        demonstrateTransformScript(generator, workflow);
    }

    /**
     * 演示数据过滤脚本生成
     */
    private static void demonstrateFilterScript(LLMScriptGenerator generator, Workflow workflow) throws Exception {
        System.out.println("📝 示例1：数据过滤脚本生成");
        System.out.println();

        String userRequirement = "我需要一个脚本来过滤日志数据，只保留ERROR和FATAL级别的日志，" +
                "并且要统计过滤前后的数量。请确保脚本有适当的错误处理。";

        System.out.println("用户需求: " + userRequirement);
        System.out.println();

        // 生成脚本（使用数据预处理节点作为示例）
        String scriptNodeId = "data_preprocessor";

        System.out.println("🔄 分析上下文环境...");
        long startTime = System.currentTimeMillis();

        LLMScriptGenerator.ScriptGenerationResult result = generator.generateScript(
                workflow, scriptNodeId, userRequirement, null);

        long generationTime = System.currentTimeMillis() - startTime;

        // 显示结果
        displayGenerationResult(result, generationTime, "数据过滤");
    }

    /**
     * 演示数据分析脚本生成
     */
    private static void demonstrateAnalysisScript(LLMScriptGenerator generator, Workflow workflow) throws Exception {
        System.out.println("\n" + "=".repeat(60) + "\n");
        System.out.println("📝 示例2：错误分析脚本生成");
        System.out.println();

        String userRequirement = "生成一个错误分析脚本，分析输入的日志数据，统计不同类型的错误，" +
                "计算错误率，并根据错误情况给出建议。需要识别Exception、Timeout、Connection等错误类型。";

        System.out.println("用户需求: " + userRequirement);
        System.out.println();

        // 使用结果聚合节点作为示例
        String scriptNodeId = "result_aggregator";

        // 添加额外上下文
        Map<String, Object> additionalContext = new HashMap<>();
        additionalContext.put("分析重点", "错误模式识别和趋势分析");
        additionalContext.put("输出格式", "结构化的分析报告");

        System.out.println("🔄 分析上下文环境...");
        long startTime = System.currentTimeMillis();

        LLMScriptGenerator.ScriptGenerationResult result = generator.generateScript(
                workflow, scriptNodeId, userRequirement, additionalContext);

        long generationTime = System.currentTimeMillis() - startTime;

        // 显示结果
        displayGenerationResult(result, generationTime, "错误分析");
    }

    /**
     * 演示数据转换脚本生成
     */
    private static void demonstrateTransformScript(LLMScriptGenerator generator, Workflow workflow) throws Exception {
        System.out.println("\n" + "=".repeat(60) + "\n");
        System.out.println("📝 示例3：数据转换脚本生成");
        System.out.println();

        String userRequirement = "创建一个数据转换脚本，为每条日志记录添加处理时间戳、小时信息、" +
                "并根据日志级别添加优先级字段（ERROR=4, WARN=3, INFO=2, DEBUG=1）。" +
                "同时统计各个级别的数量并保存到上下文中。";

        System.out.println("用户需求: " + userRequirement);
        System.out.println();

        // 使用数据预处理节点
        String scriptNodeId = "data_preprocessor";

        // 添加额外上下文
        Map<String, Object> additionalContext = new HashMap<>();
        additionalContext.put("性能要求", "处理大量数据时保持高效");
        additionalContext.put("扩展性", "支持后续添加更多字段");

        System.out.println("🔄 分析上下文环境...");
        long startTime = System.currentTimeMillis();

        LLMScriptGenerator.ScriptGenerationResult result = generator.generateScript(
                workflow, scriptNodeId, userRequirement, additionalContext);

        long generationTime = System.currentTimeMillis() - startTime;

        // 显示结果
        displayGenerationResult(result, generationTime, "数据转换");
    }

    /**
     * 显示生成结果
     */
    private static void displayGenerationResult(LLMScriptGenerator.ScriptGenerationResult result,
            long generationTime, String scriptType) {
        if (result.isSuccess()) {
            System.out.println("✅ " + scriptType + "脚本生成成功！");
            System.out.println("   生成时间: " + generationTime + "ms");
            System.out.println();

            // 显示上下文分析结果
            System.out.println("🔍 上下文分析结果:");
            WorkflowContextAnalyzer.ContextAnalysisResult context = result.getContextAnalysis();
            if (context != null) {
                System.out.println("   - 输入源数量: " + context.getInputSources().size());
                System.out.println("   - 可用上下文键: " + context.getContextKeys().size());
                System.out.println("   - 输出目标: " + context.getOutputTargets().size());

                if (!context.getInputSources().isEmpty()) {
                    System.out.println("   - 主要输入: " + context.getInputSources().get(0).getNodeName());
                }
            }
            System.out.println();

            // 显示生成的脚本（前10行）
            System.out.println("📄 生成的脚本预览:");
            String[] lines = result.getGeneratedScript().split("\n");
            for (int i = 0; i < Math.min(15, lines.length); i++) {
                System.out.println("   " + lines[i]);
            }
            if (lines.length > 15) {
                System.out.println("   ... (共" + lines.length + "行，显示前15行)");
            }
            System.out.println();

            // 显示验证结果
            if (result.getValidationIssues() != null && !result.getValidationIssues().isEmpty()) {
                System.out.println("⚠️  验证提醒:");
                for (String issue : result.getValidationIssues()) {
                    System.out.println("   - " + issue);
                }
                System.out.println();
            } else {
                System.out.println("✅ 脚本验证通过，未发现问题");
                System.out.println();
            }

            // 显示使用指导
            System.out.println("📋 下一步操作:");
            System.out.println("   1. 复制生成的脚本到YAML配置文件");
            System.out.println("   2. 根据需要调整脚本参数");
            System.out.println("   3. 在测试环境中验证脚本功能");
            System.out.println("   4. 部署到生产环境");

        } else {
            System.out.println("❌ " + scriptType + "脚本生成失败");
            System.out.println("   错误信息: " + result.getErrorMessage());
        }
    }

    /**
     * 演示上下文分析功能
     */
    public static void demonstrateContextAnalysis() throws Exception {
        System.out.println("\n🔍 上下文分析功能演示:");
        System.out.println();

        // 加载工作流
        WorkflowConfigLoader configLoader = new WorkflowConfigLoader();
        Workflow workflow = configLoader.loadFromResource("workflows/complex-log-analysis.yaml");

        // 创建上下文分析器
        WorkflowContextAnalyzer analyzer = new WorkflowContextAnalyzer();

        // 分析特定节点的上下文
        String nodeId = "result_aggregator";
        WorkflowContextAnalyzer.ContextAnalysisResult result = analyzer.analyzeScriptContext(workflow, nodeId);

        System.out.println("节点: " + nodeId);
        System.out.println("工作流: " + result.getWorkflowName());
        System.out.println();

        System.out.println("输入源:");
        for (WorkflowContextAnalyzer.InputSourceInfo source : result.getInputSources()) {
            System.out.println("  - " + source.getNodeName() + " (" + source.getNodeType() + ")");
            System.out.println("    类型: " + source.getExpectedDataType());
            System.out.println("    描述: " + source.getDescription());
        }
        System.out.println();

        System.out.println("可用上下文数据:");
        for (WorkflowContextAnalyzer.ContextDataInfo context : result.getContextData()) {
            System.out.println("  - " + context.getKey() + " (" + context.getDataType() + ")");
            System.out.println("    来源: " + context.getProducerNodeName());
            System.out.println("    描述: " + context.getDescription());
        }
        System.out.println();

        System.out.println("输出目标:");
        for (WorkflowContextAnalyzer.OutputTargetInfo target : result.getOutputTargets()) {
            System.out.println("  - " + target.getNodeName() + " (" + target.getNodeType() + ")");
            System.out.println("    期望类型: " + target.getExpectedDataType());
            System.out.println("    描述: " + target.getDescription());
        }
    }
}
