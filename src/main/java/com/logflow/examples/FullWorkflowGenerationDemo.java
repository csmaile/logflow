package com.logflow.examples;

import com.logflow.ai.FullWorkflowGenerator;
import com.logflow.ai.MockLLMProvider;
import com.logflow.ai.WorkflowRequirementAnalyzer.*;
import com.logflow.ai.WorkflowDesigner.*;
import com.logflow.ai.FullWorkflowGenerator.*;
import com.logflow.config.WorkflowConfigLoader;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;
import com.logflow.engine.WorkflowExecutionResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 完整工作流生成功能演示
 * 展示如何通过一句话需求自动生成完整的LogFlow工作流配置
 */
public class FullWorkflowGenerationDemo {

    public static void main(String[] args) {
        System.out.println("=== LogFlow完整工作流生成功能演示 ===\n");

        try {
            // 介绍功能
            introduceFullWorkflowGeneration();

            // 演示不同场景的工作流生成
            demonstrateWorkflowGeneration();

            System.out.println("\n=== 演示完成 ===");
            System.out.println("💡 提示：生成的工作流文件保存在 generated_workflows/ 目录中");

        } catch (Exception e) {
            System.err.println("❌ 演示执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 介绍完整工作流生成功能
     */
    private static void introduceFullWorkflowGeneration() {
        System.out.println("🚀 完整工作流生成功能介绍：");
        System.out.println();

        System.out.println("🎯 终极目标：");
        System.out.println("   通过一句话描述，自动生成包含所有脚本逻辑的完整工作流配置");
        System.out.println();

        System.out.println("🔧 核心能力：");
        System.out.println("   1. 自然语言需求理解 - 解析用户的一句话需求");
        System.out.println("   2. 智能工作流设计 - 自动设计节点结构和连接关系");
        System.out.println("   3. 完整脚本生成 - 为每个脚本节点生成JavaScript代码");
        System.out.println("   4. YAML配置输出 - 生成完整的可执行工作流配置");
        System.out.println();

        System.out.println("💡 工作原理：");
        System.out.println("   用户需求 → 需求分析 → 工作流设计 → 脚本生成 → YAML配置");
        System.out.println("   \"分析日志错误\" → 识别组件 → 设计架构 → 生成脚本 → 完整配置");
        System.out.println();

        System.out.println("🌟 价值创新：");
        System.out.println("   - 零配置：无需手动设计工作流结构");
        System.out.println("   - 零编程：无需编写任何JavaScript代码");
        System.out.println("   - 即时可用：生成的配置可直接运行");
        System.out.println("   - 智能优化：自动应用最佳实践");
        System.out.println();
    }

    /**
     * 演示工作流生成
     */
    private static void demonstrateWorkflowGeneration() throws Exception {
        System.out.println("🚀 开始工作流生成演示：");
        System.out.println();

        // 创建完整工作流生成器
        MockLLMProvider mockProvider = new MockLLMProvider();
        FullWorkflowGenerator generator = new FullWorkflowGenerator(mockProvider);

        // 创建输出目录
        File outputDir = new File("generated_workflows");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // 演示不同场景的工作流生成
        demonstrateLogAnalysisWorkflow(generator, outputDir);
        demonstratePerformanceMonitoringWorkflow(generator, outputDir);
        demonstrateDataCleaningWorkflow(generator, outputDir);
    }

    /**
     * 演示日志分析工作流生成
     */
    private static void demonstrateLogAnalysisWorkflow(FullWorkflowGenerator generator, File outputDir)
            throws Exception {
        System.out.println("📝 场景1：日志分析工作流生成");
        System.out.println();

        String userRequirement = "分析系统日志文件，过滤出错误级别的日志，统计错误类型和频率，" +
                "生成错误分析报告并保存到文件中";

        System.out.println("用户需求: " + userRequirement);
        System.out.println();

        System.out.println("🔄 开始生成工作流...");
        long startTime = System.currentTimeMillis();

        FullWorkflowGenerationResult result = generator.generateFullWorkflow(userRequirement, null);

        long generationTime = System.currentTimeMillis() - startTime;

        if (result.isSuccess()) {
            System.out.println("✅ 工作流生成成功！");
            System.out.println("   生成耗时: " + generationTime + "ms");
            System.out.println();

            // 显示需求分析结果
            displayRequirementAnalysis(result.getRequirementAnalysis());

            // 显示工作流设计结果
            displayWorkflowDesign(result.getWorkflowDesign());

            // 显示脚本生成结果
            displayScriptGeneration(result.getGeneratedScripts());

            // 保存生成的YAML配置
            String fileName = "log_analysis_workflow.yaml";
            saveWorkflowToFile(result.getYamlConfiguration(), outputDir, fileName);

            // 尝试加载和执行生成的工作流
            testGeneratedWorkflow(outputDir, fileName);

        } else {
            System.out.println("❌ 工作流生成失败");
            System.out.println("   错误信息: " + result.getErrorMessage());
        }
    }

    /**
     * 演示性能监控工作流生成
     */
    private static void demonstratePerformanceMonitoringWorkflow(FullWorkflowGenerator generator, File outputDir)
            throws Exception {
        System.out.println("\n" + "=".repeat(80) + "\n");
        System.out.println("📝 场景2：性能监控工作流生成");
        System.out.println();

        String userRequirement = "监控应用性能数据，检测响应时间超过1秒的慢请求，" +
                "按小时统计性能指标，当慢请求比例超过20%时生成告警";

        System.out.println("用户需求: " + userRequirement);
        System.out.println();

        // 添加额外上下文
        Map<String, Object> additionalContext = new HashMap<>();
        additionalContext.put("阈值配置", "响应时间>1000ms为慢请求");
        additionalContext.put("告警条件", "慢请求比例>20%");
        additionalContext.put("统计周期", "按小时聚合");

        System.out.println("🔄 开始生成工作流...");
        long startTime = System.currentTimeMillis();

        FullWorkflowGenerationResult result = generator.generateFullWorkflow(userRequirement, additionalContext);

        long generationTime = System.currentTimeMillis() - startTime;

        if (result.isSuccess()) {
            System.out.println("✅ 工作流生成成功！");
            System.out.println("   生成耗时: " + generationTime + "ms");
            System.out.println();

            // 显示关键信息
            displayWorkflowSummary(result);

            // 保存配置
            String fileName = "performance_monitoring_workflow.yaml";
            saveWorkflowToFile(result.getYamlConfiguration(), outputDir, fileName);

        } else {
            System.out.println("❌ 工作流生成失败: " + result.getErrorMessage());
        }
    }

    /**
     * 演示数据清洗工作流生成
     */
    private static void demonstrateDataCleaningWorkflow(FullWorkflowGenerator generator, File outputDir)
            throws Exception {
        System.out.println("\n" + "=".repeat(80) + "\n");
        System.out.println("📝 场景3：数据清洗工作流生成");
        System.out.println();

        String userRequirement = "清洗CSV数据文件，去除重复记录，验证数据完整性，" +
                "标准化日期格式，生成数据质量报告";

        System.out.println("用户需求: " + userRequirement);
        System.out.println();

        Map<String, Object> additionalContext = new HashMap<>();
        additionalContext.put("数据源", "CSV文件");
        additionalContext.put("质量要求", "去重、验证、标准化");
        additionalContext.put("输出格式", "清洗后的数据 + 质量报告");

        System.out.println("🔄 开始生成工作流...");
        long startTime = System.currentTimeMillis();

        FullWorkflowGenerationResult result = generator.generateFullWorkflow(userRequirement, additionalContext);

        long generationTime = System.currentTimeMillis() - startTime;

        if (result.isSuccess()) {
            System.out.println("✅ 工作流生成成功！");
            System.out.println("   生成耗时: " + generationTime + "ms");
            System.out.println();

            // 显示关键信息
            displayWorkflowSummary(result);

            // 保存配置
            String fileName = "data_cleaning_workflow.yaml";
            saveWorkflowToFile(result.getYamlConfiguration(), outputDir, fileName);

        } else {
            System.out.println("❌ 工作流生成失败: " + result.getErrorMessage());
        }
    }

    /**
     * 显示需求分析结果
     */
    private static void displayRequirementAnalysis(RequirementAnalysisResult analysis) {
        System.out.println("🔍 需求分析结果:");
        System.out.println("   - 工作流类型: " + analysis.getWorkflowType());
        System.out.println("   - 应用领域: " + analysis.getDomain());
        System.out.println("   - 复杂度: " + analysis.getComplexity());
        System.out.println("   - 数据源: " + analysis.getDataSources().size() + " 个");
        System.out.println("   - 处理步骤: " + analysis.getProcessingSteps().size() + " 个");
        System.out.println("   - 输出要求: " + analysis.getOutputRequirements().size() + " 个");
        System.out.println("   - 业务规则: " + analysis.getBusinessRules().size() + " 条");
        System.out.println();
    }

    /**
     * 显示工作流设计结果
     */
    private static void displayWorkflowDesign(WorkflowDesignResult design) {
        System.out.println("🏗️ 工作流设计结果:");
        System.out.println("   - 工作流ID: " + design.getWorkflowId());
        System.out.println("   - 工作流名称: " + design.getWorkflowName());
        System.out.println("   - 节点总数: " + design.getNodes().size());
        System.out.println("   - 连接总数: " + design.getConnections().size());

        // 显示节点类型分布
        Map<String, Long> nodeTypeCount = new HashMap<>();
        for (NodeDesign node : design.getNodes()) {
            nodeTypeCount.merge(node.getType(), 1L, Long::sum);
        }

        System.out.println("   - 节点类型分布:");
        nodeTypeCount.forEach((type, count) -> System.out.println("     * " + type + ": " + count + " 个"));
        System.out.println();
    }

    /**
     * 显示脚本生成结果
     */
    private static void displayScriptGeneration(Map<String, ScriptGenerationInfo> scripts) {
        System.out.println("📄 脚本生成结果:");
        System.out.println("   - 脚本节点总数: " + scripts.size());

        long successCount = scripts.values().stream().mapToLong(s -> s.isError() ? 0 : 1).sum();
        long errorCount = scripts.values().stream().mapToLong(s -> s.isError() ? 1 : 0).sum();

        System.out.println("   - 成功生成: " + successCount + " 个");
        System.out.println("   - 生成失败: " + errorCount + " 个");

        // 显示脚本详情
        scripts.forEach((nodeId, scriptInfo) -> {
            if (!scriptInfo.isError()) {
                System.out.println("   - " + scriptInfo.getNodeName() + " (" + scriptInfo.getScriptType() + "):");
                String[] lines = scriptInfo.getGeneratedScript().split("\n");
                System.out.println("     代码行数: " + lines.length);
                System.out.println("     首行: " + (lines.length > 0 ? lines[0] : ""));
            }
        });
        System.out.println();
    }

    /**
     * 显示工作流摘要
     */
    private static void displayWorkflowSummary(FullWorkflowGenerationResult result) {
        System.out.println("📊 工作流生成摘要:");

        Map<String, Object> metadata = result.getGenerationMetadata();
        metadata.forEach((key, value) -> System.out.println("   - " + key + ": " + value));

        System.out.println();
    }

    /**
     * 保存工作流到文件
     */
    private static void saveWorkflowToFile(String yamlContent, File outputDir, String fileName) {
        try {
            File yamlFile = new File(outputDir, fileName);
            try (FileWriter writer = new FileWriter(yamlFile)) {
                writer.write(yamlContent);
            }

            System.out.println("💾 工作流配置已保存:");
            System.out.println("   文件: " + yamlFile.getAbsolutePath());
            System.out.println("   大小: " + yamlContent.length() + " 字符");
            System.out.println();

        } catch (IOException e) {
            System.err.println("❌ 保存工作流文件失败: " + e.getMessage());
        }
    }

    /**
     * 测试生成的工作流
     */
    private static void testGeneratedWorkflow(File outputDir, String fileName) {
        try {
            System.out.println("🧪 测试生成的工作流:");

            // 加载生成的工作流
            WorkflowConfigLoader configLoader = new WorkflowConfigLoader();
            File workflowFile = new File(outputDir, fileName);
            Workflow workflow = configLoader.loadFromFile(workflowFile.getAbsolutePath());

            System.out.println("   ✅ 工作流加载成功");
            System.out.println("   - 节点数: " + workflow.getNodeCount());
            System.out.println("   - 工作流名称: " + workflow.getName());

            // 创建工作流引擎并执行
            WorkflowEngine engine = new WorkflowEngine();

            // 准备执行参数
            Map<String, Object> initialData = Map.of(
                    "test_mode", true,
                    "sample_size", 100);

            System.out.println("   🚀 开始执行工作流...");
            long execStartTime = System.currentTimeMillis();

            WorkflowExecutionResult execResult = engine.execute(workflow, initialData);

            long execTime = System.currentTimeMillis() - execStartTime;

            if (execResult.isSuccess()) {
                System.out.println("   ✅ 工作流执行成功!");
                System.out.println("   - 执行时间: " + execTime + "ms");
                System.out.println("   - 成功节点: " + execResult.getStatistics().getSuccessfulNodes());
                System.out.println("   - 总节点数: " + execResult.getStatistics().getTotalNodes());
            } else {
                System.out.println("   ❌ 工作流执行失败");
                System.out.println("   - 错误信息: " + execResult.getMessage());
            }

            engine.shutdown();

        } catch (Exception e) {
            System.out.println("   ❌ 工作流测试失败: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * 显示生成的YAML配置预览
     */
    private static void displayYamlPreview(String yamlContent) {
        System.out.println("📄 YAML配置预览 (前20行):");
        String[] lines = yamlContent.split("\n");
        for (int i = 0; i < Math.min(20, lines.length); i++) {
            System.out.println("   " + (i + 1) + "| " + lines[i]);
        }
        if (lines.length > 20) {
            System.out.println("   ... (共" + lines.length + "行，显示前20行)");
        }
        System.out.println();
    }
}
