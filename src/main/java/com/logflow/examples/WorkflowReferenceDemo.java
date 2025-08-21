package com.logflow.examples;

import com.logflow.builder.WorkflowBuilder;
import com.logflow.core.NodeExecutionResult;
import com.logflow.core.WorkflowContext;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;
import com.logflow.engine.WorkflowExecutionResult;
import com.logflow.nodes.*;
import com.logflow.registry.WorkflowRegistry;

import java.util.*;

/**
 * 工作流关联节点演示程序
 * 展示LogFlow关联节点的各种功能：同步执行、异步执行、条件执行、循环执行、并行执行
 */
public class WorkflowReferenceDemo {

    public static void main(String[] args) {
        System.out.println("=== LogFlow 工作流关联节点演示 ===\n");

        try {
            // 初始化演示环境
            setupDemoEnvironment();

            // 演示各种执行模式
            demonstrateSynchronousExecution();
            System.out.println();

            demonstrateAsynchronousExecution();
            System.out.println();

            demonstrateConditionalExecution();
            System.out.println();

            demonstrateLoopExecution();
            System.out.println();

            demonstrateParallelExecution();
            System.out.println();

            demonstrateComplexWorkflowComposition();

        } catch (Exception e) {
            System.err.println("演示过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 设置演示环境
     */
    private static void setupDemoEnvironment() {
        System.out.println("🚀 设置演示环境...\n");

        WorkflowRegistry registry = WorkflowRegistry.getInstance();

        // 创建基础工作流
        createDataProcessingWorkflow(registry);
        createValidationWorkflow(registry);
        createReportGenerationWorkflow(registry);
        createCleanupWorkflow(registry);

        System.out.println("✅ 演示环境设置完成，共注册 " + registry.getActiveWorkflowIds().size() + " 个工作流");
        System.out.println("   📋 已注册工作流: " + registry.getActiveWorkflowIds());
    }

    /**
     * 创建数据处理工作流
     */
    private static void createDataProcessingWorkflow(WorkflowRegistry registry) {
        Workflow workflow = WorkflowBuilder.create("data-processing", "数据处理工作流")
                .addInputNode("input", "数据输入")
                .addScriptNode("process", "数据处理")
                .withScript("" +
                        "var input = context.getData('input_data') || [];\n" +
                        "var processed = input.map(function(item) {\n" +
                        "    return {\n" +
                        "        id: item.id,\n" +
                        "        value: item.value * 2,\n" +
                        "        processed: true,\n" +
                        "        timestamp: new Date().getTime()\n" +
                        "    };\n" +
                        "});\n" +
                        "context.setData('processed_data', processed);\n" +
                        "logger.info('处理了 ' + processed.length + ' 条数据');")
                .addOutputNode("output", "数据输出")
                .withOutputType("memory")
                .withInputKey("processed_data")
                .connect("input", "process")
                .connect("process", "output")
                .build();

        registry.registerWorkflow(workflow, WorkflowRegistry.WorkflowStatus.ACTIVE,
                "基础数据处理工作流，对输入数据进行加工处理", "1.0.0");
    }

    /**
     * 创建验证工作流
     */
    private static void createValidationWorkflow(WorkflowRegistry registry) {
        Workflow workflow = WorkflowBuilder.create("data-validation", "数据验证工作流")
                .addInputNode("input", "数据输入")
                .addScriptNode("validate", "数据验证")
                .withScript("" +
                        "var data = context.getData('input_data') || [];\n" +
                        "var validCount = 0;\n" +
                        "var invalidCount = 0;\n" +
                        "var validationResults = [];\n" +
                        "\n" +
                        "data.forEach(function(item) {\n" +
                        "    var isValid = item.value != null && item.value > 0;\n" +
                        "    if (isValid) {\n" +
                        "        validCount++;\n" +
                        "    } else {\n" +
                        "        invalidCount++;\n" +
                        "    }\n" +
                        "    validationResults.push({\n" +
                        "        id: item.id,\n" +
                        "        valid: isValid,\n" +
                        "        reason: isValid ? 'OK' : 'Invalid value'\n" +
                        "    });\n" +
                        "});\n" +
                        "\n" +
                        "context.setData('validation_results', validationResults);\n" +
                        "context.setData('valid_count', validCount);\n" +
                        "context.setData('invalid_count', invalidCount);\n" +
                        "context.setData('validation_passed', invalidCount === 0);\n" +
                        "\n" +
                        "logger.info('验证完成: 有效=' + validCount + ', 无效=' + invalidCount);")
                .addOutputNode("output", "验证结果输出")
                .withOutputType("memory")
                .withInputKey("validation_results")
                .connect("input", "validate")
                .connect("validate", "output")
                .build();

        registry.registerWorkflow(workflow, WorkflowRegistry.WorkflowStatus.ACTIVE,
                "数据验证工作流，检查数据的有效性", "1.0.0");
    }

    /**
     * 创建报告生成工作流
     */
    private static void createReportGenerationWorkflow(WorkflowRegistry registry) {
        Workflow workflow = WorkflowBuilder.create("report-generation", "报告生成工作流")
                .addInputNode("input", "数据输入")
                .addScriptNode("generate", "生成报告")
                .withScript("" +
                        "var processedData = context.getData('processed_data') || [];\n" +
                        "var validationResults = context.getData('validation_results') || [];\n" +
                        "\n" +
                        "var report = {\n" +
                        "    title: '数据处理报告',\n" +
                        "    timestamp: new Date().toISOString(),\n" +
                        "    summary: {\n" +
                        "        totalRecords: processedData.length,\n" +
                        "        validRecords: context.getData('valid_count') || 0,\n" +
                        "        invalidRecords: context.getData('invalid_count') || 0\n" +
                        "    },\n" +
                        "    details: {\n" +
                        "        processedData: processedData.slice(0, 5),\n" +
                        "        validationSample: validationResults.slice(0, 5)\n" +
                        "    }\n" +
                        "};\n" +
                        "\n" +
                        "context.setData('report', report);\n" +
                        "logger.info('报告生成完成，包含 ' + report.summary.totalRecords + ' 条记录');")
                .addOutputNode("output", "报告输出")
                .withOutputType("memory")
                .withInputKey("report")
                .connect("input", "generate")
                .connect("generate", "output")
                .build();

        registry.registerWorkflow(workflow, WorkflowRegistry.WorkflowStatus.ACTIVE,
                "报告生成工作流，生成数据处理和验证报告", "1.0.0");
    }

    /**
     * 创建清理工作流
     */
    private static void createCleanupWorkflow(WorkflowRegistry registry) {
        Workflow workflow = WorkflowBuilder.create("cleanup", "清理工作流")
                .addInputNode("input", "数据输入")
                .addScriptNode("cleanup", "执行清理")
                .withScript("" +
                        "var cleanupTasks = [\n" +
                        "    '清理临时文件',\n" +
                        "    '关闭数据库连接',\n" +
                        "    '释放内存资源',\n" +
                        "    '记录执行日志'\n" +
                        "];\n" +
                        "\n" +
                        "var completedTasks = [];\n" +
                        "cleanupTasks.forEach(function(task) {\n" +
                        "    // 模拟清理任务\n" +
                        "    completedTasks.push({\n" +
                        "        task: task,\n" +
                        "        completed: true,\n" +
                        "        timestamp: new Date().getTime()\n" +
                        "    });\n" +
                        "});\n" +
                        "\n" +
                        "context.setData('cleanup_results', completedTasks);\n" +
                        "logger.info('清理完成，执行了 ' + completedTasks.length + ' 个清理任务');")
                .addOutputNode("output", "清理结果输出")
                .withOutputType("memory")
                .withInputKey("cleanup_results")
                .connect("input", "cleanup")
                .connect("cleanup", "output")
                .build();

        registry.registerWorkflow(workflow, WorkflowRegistry.WorkflowStatus.ACTIVE,
                "清理工作流，执行系统清理任务", "1.0.0");
    }

    /**
     * 演示同步执行模式
     */
    private static void demonstrateSynchronousExecution() {
        System.out.println("🔄 同步执行模式演示：\n");

        // 创建包含关联节点的工作流
        Workflow mainWorkflow = WorkflowBuilder.create("sync-demo", "同步执行演示")
                .addInputNode("input", "主工作流输入")
                .addReferenceNode("ref-process", "关联数据处理")
                .withConfig(Map.of(
                        "executionMode", "SYNC",
                        "workflowId", "data-processing",
                        "inputMappings", Map.of("demo_data", "input_data"),
                        "outputMappings", Map.of("processed_data", "main_processed_data")))
                .addOutputNode("output", "主工作流输出")
                .withOutputType("console")
                .withInputKey("main_processed_data")
                .connect("input", "ref-process")
                .connect("ref-process", "output")
                .build();

        // 执行工作流
        WorkflowEngine engine = new WorkflowEngine();

        // 准备测试数据
        List<Map<String, Object>> testData = Arrays.asList(
                Map.of("id", 1, "value", 10),
                Map.of("id", 2, "value", 20),
                Map.of("id", 3, "value", 30));

        Map<String, Object> params = Map.of("demo_data", testData);

        System.out.println("   📥 输入数据: " + testData);
        System.out.println("   🎯 执行模式: 同步执行");
        System.out.println("   🔗 关联工作流: data-processing");

        WorkflowExecutionResult result = engine.execute(mainWorkflow, params);

        if (result.isSuccess()) {
            System.out.println("   ✅ 同步执行成功");
            System.out.printf("   ⏱️ 执行时间: %dms\n", result.getExecutionDurationMs());
            System.out.printf("   📊 执行节点: %d/%d 成功\n",
                    result.getStatistics().getSuccessfulNodes(),
                    result.getStatistics().getTotalNodes());
        } else {
            System.out.println("   ❌ 同步执行失败: " + result.getMessage());
        }

        engine.shutdown();
    }

    /**
     * 演示异步执行模式
     */
    private static void demonstrateAsynchronousExecution() {
        System.out.println("⚡ 异步执行模式演示：\n");

        Workflow mainWorkflow = WorkflowBuilder.create("async-demo", "异步执行演示")
                .addInputNode("input", "主工作流输入")
                .addReferenceNode("ref-validate", "异步数据验证")
                .withConfig(Map.of(
                        "executionMode", "ASYNC",
                        "workflowId", "data-validation",
                        "waitForResult", true,
                        "timeoutMs", 10000,
                        "inputMappings", Map.of("demo_data", "input_data"),
                        "outputMappings", Map.of("validation_results", "async_validation_results")))
                .addOutputNode("output", "异步结果输出")
                .withOutputType("console")
                .withInputKey("async_validation_results")
                .connect("input", "ref-validate")
                .connect("ref-validate", "output")
                .build();

        WorkflowEngine engine = new WorkflowEngine();

        List<Map<String, Object>> testData = Arrays.asList(
                Map.of("id", 1, "value", 100),
                Map.of("id", 2, "value", -50), // 无效数据
                Map.of("id", 3, "value", 200));

        Map<String, Object> params = Map.of("demo_data", testData);

        System.out.println("   📥 输入数据: " + testData);
        System.out.println("   🎯 执行模式: 异步执行（等待结果）");
        System.out.println("   🔗 关联工作流: data-validation");
        System.out.println("   ⏰ 超时设置: 10秒");

        WorkflowExecutionResult result = engine.execute(mainWorkflow, params);

        if (result.isSuccess()) {
            System.out.println("   ✅ 异步执行成功");
            System.out.printf("   ⏱️ 执行时间: %dms\n", result.getExecutionDurationMs());
        } else {
            System.out.println("   ❌ 异步执行失败: " + result.getMessage());
        }

        engine.shutdown();
    }

    /**
     * 演示条件执行模式
     */
    private static void demonstrateConditionalExecution() {
        System.out.println("🎯 条件执行模式演示：\n");

        Workflow mainWorkflow = WorkflowBuilder.create("conditional-demo", "条件执行演示")
                .addInputNode("input", "主工作流输入")
                .addScriptNode("prepare", "准备条件数据")
                .withScript("" +
                        "var shouldProcess = context.getData('should_process');\n" +
                        "context.setData('condition_met', shouldProcess);\n" +
                        "logger.info('条件检查: ' + shouldProcess);")
                .addReferenceNode("ref-conditional", "条件关联节点")
                .withConfig(Map.of(
                        "executionMode", "CONDITIONAL",
                        "condition", "${condition_met} == true",
                        "workflowId", "report-generation",
                        "inputMappings", Map.of("demo_data", "processed_data", "demo_validation", "validation_results"),
                        "outputMappings", Map.of("report", "conditional_report")))
                .addOutputNode("output", "条件执行结果")
                .withOutputType("console")
                .withInputKey("conditional_report")
                .connect("input", "prepare")
                .connect("prepare", "ref-conditional")
                .connect("ref-conditional", "output")
                .build();

        WorkflowEngine engine = new WorkflowEngine();

        // 测试条件满足的情况
        System.out.println("   🧪 测试1: 条件满足的情况");
        Map<String, Object> params1 = Map.of(
                "should_process", true,
                "demo_data", Arrays.asList(Map.of("id", 1, "value", 100)),
                "demo_validation", Arrays.asList(Map.of("id", 1, "valid", true)),
                "valid_count", 1,
                "invalid_count", 0);

        WorkflowExecutionResult result1 = engine.execute(mainWorkflow, params1);
        System.out.println("      " + (result1.isSuccess() ? "✅ 条件满足，执行成功" : "❌ 执行失败"));

        // 测试条件不满足的情况
        System.out.println("   🧪 测试2: 条件不满足的情况");
        Map<String, Object> params2 = Map.of(
                "should_process", false,
                "demo_data", Arrays.asList(Map.of("id", 1, "value", 100)));

        WorkflowExecutionResult result2 = engine.execute(mainWorkflow, params2);
        System.out.println("      " + (result2.isSuccess() ? "✅ 条件不满足，跳过执行" : "❌ 执行失败"));

        engine.shutdown();
    }

    /**
     * 演示循环执行模式
     */
    private static void demonstrateLoopExecution() {
        System.out.println("🔄 循环执行模式演示：\n");

        Workflow mainWorkflow = WorkflowBuilder.create("loop-demo", "循环执行演示")
                .addInputNode("input", "主工作流输入")
                .addReferenceNode("ref-loop", "循环处理节点")
                .withConfig(Map.of(
                        "executionMode", "LOOP",
                        "workflowId", "data-processing",
                        "loopDataKey", "batch_data",
                        "maxIterations", 5,
                        "inputMappings", Map.of("loopItem", "input_data"),
                        "outputMappings", Map.of("processed_data", "loop_processed_data")))
                .addOutputNode("output", "循环结果输出")
                .withOutputType("console")
                .withInputKey("loop_processed_data")
                .connect("input", "ref-loop")
                .connect("ref-loop", "output")
                .build();

        WorkflowEngine engine = new WorkflowEngine();

        // 准备批量数据
        List<List<Map<String, Object>>> batchData = Arrays.asList(
                Arrays.asList(Map.of("id", 1, "value", 10), Map.of("id", 2, "value", 20)),
                Arrays.asList(Map.of("id", 3, "value", 30), Map.of("id", 4, "value", 40)),
                Arrays.asList(Map.of("id", 5, "value", 50)));

        Map<String, Object> params = Map.of("batch_data", batchData);

        System.out.println("   📥 批量数据: " + batchData.size() + " 个批次");
        System.out.println("   🎯 执行模式: 循环执行");
        System.out.println("   🔗 关联工作流: data-processing");
        System.out.println("   🔄 最大迭代次数: 5");

        WorkflowExecutionResult result = engine.execute(mainWorkflow, params);

        if (result.isSuccess()) {
            System.out.println("   ✅ 循环执行成功");
            System.out.printf("   ⏱️ 执行时间: %dms\n", result.getExecutionDurationMs());
        } else {
            System.out.println("   ❌ 循环执行失败: " + result.getMessage());
        }

        engine.shutdown();
    }

    /**
     * 演示并行执行模式
     */
    private static void demonstrateParallelExecution() {
        System.out.println("⚡ 并行执行模式演示：\n");

        Workflow mainWorkflow = WorkflowBuilder.create("parallel-demo", "并行执行演示")
                .addInputNode("input", "主工作流输入")
                .addReferenceNode("ref-parallel", "并行处理节点")
                .withConfig(Map.of(
                        "executionMode", "PARALLEL",
                        "workflowIds", Arrays.asList("data-processing", "data-validation", "cleanup"),
                        "parallelTimeoutMs", 30000,
                        "inputMappings", Map.of("demo_data", "input_data"),
                        "outputMappings", Map.of() // 并行执行不需要单独的输出映射
                ))
                .addOutputNode("output", "并行结果输出")
                .withOutputType("console")
                .withInputKey("parallel_results")
                .connect("input", "ref-parallel")
                .connect("ref-parallel", "output")
                .build();

        WorkflowEngine engine = new WorkflowEngine();

        List<Map<String, Object>> testData = Arrays.asList(
                Map.of("id", 1, "value", 100),
                Map.of("id", 2, "value", 200),
                Map.of("id", 3, "value", 300));

        Map<String, Object> params = Map.of("demo_data", testData);

        System.out.println("   📥 输入数据: " + testData);
        System.out.println("   🎯 执行模式: 并行执行");
        System.out.println("   🔗 关联工作流: [data-processing, data-validation, cleanup]");
        System.out.println("   ⏰ 超时设置: 30秒");

        long startTime = System.currentTimeMillis();
        WorkflowExecutionResult result = engine.execute(mainWorkflow, params);
        long endTime = System.currentTimeMillis();

        if (result.isSuccess()) {
            System.out.println("   ✅ 并行执行成功");
            System.out.printf("   ⏱️ 总执行时间: %dms\n", endTime - startTime);
            System.out.println("   🚀 并行执行显著提升了处理效率");
        } else {
            System.out.println("   ❌ 并行执行失败: " + result.getMessage());
        }

        engine.shutdown();
    }

    /**
     * 演示复杂工作流组合
     */
    private static void demonstrateComplexWorkflowComposition() {
        System.out.println("🎭 复杂工作流组合演示：\n");

        // 创建一个复杂的主工作流，组合多种执行模式
        Workflow complexWorkflow = WorkflowBuilder.create("complex-composition", "复杂工作流组合")
                .addInputNode("input", "数据输入")

                // 第一步：数据处理
                .addReferenceNode("step1", "数据处理")
                .withConfig(Map.of(
                        "executionMode", "SYNC",
                        "workflowId", "data-processing",
                        "inputMappings", Map.of("input_data", "input_data"),
                        "outputMappings", Map.of("processed_data", "step1_result")))

                // 第二步：数据验证
                .addReferenceNode("step2", "数据验证")
                .withConfig(Map.of(
                        "executionMode", "SYNC",
                        "workflowId", "data-validation",
                        "inputMappings", Map.of("step1_result", "input_data"),
                        "outputMappings",
                        Map.of("validation_results", "step2_result", "validation_passed", "is_valid")))

                // 第三步：条件报告生成
                .addReferenceNode("step3", "条件报告生成")
                .withConfig(Map.of(
                        "executionMode", "CONDITIONAL",
                        "condition", "${is_valid} == true",
                        "workflowId", "report-generation",
                        "inputMappings", Map.of("step1_result", "processed_data", "step2_result", "validation_results"),
                        "outputMappings", Map.of("report", "final_report")))

                // 第四步：异步清理
                .addReferenceNode("step4", "异步清理")
                .withConfig(Map.of(
                        "executionMode", "ASYNC",
                        "workflowId", "cleanup",
                        "waitForResult", false,
                        "inputMappings", Map.of(),
                        "outputMappings", Map.of()))

                .addOutputNode("output", "最终输出")
                .withOutputType("console")
                .withInputKey("final_report")

                // 连接节点
                .connect("input", "step1")
                .connect("step1", "step2")
                .connect("step2", "step3")
                .connect("step3", "step4")
                .connect("step4", "output")
                .build();

        WorkflowEngine engine = new WorkflowEngine();

        // 准备复杂的测试数据
        List<Map<String, Object>> complexData = Arrays.asList(
                Map.of("id", 1, "value", 100, "category", "A"),
                Map.of("id", 2, "value", 200, "category", "B"),
                Map.of("id", 3, "value", 300, "category", "A"),
                Map.of("id", 4, "value", 150, "category", "C"));

        Map<String, Object> params = Map.of("input_data", complexData);

        System.out.println("   📥 复杂数据: " + complexData.size() + " 条记录");
        System.out.println("   🎯 工作流步骤:");
        System.out.println("      1️⃣ 同步数据处理");
        System.out.println("      2️⃣ 同步数据验证");
        System.out.println("      3️⃣ 条件报告生成");
        System.out.println("      4️⃣ 异步系统清理");

        System.out.println("\n   🚀 开始执行复杂工作流组合...");

        long startTime = System.currentTimeMillis();
        WorkflowExecutionResult result = engine.execute(complexWorkflow, params);
        long endTime = System.currentTimeMillis();

        if (result.isSuccess()) {
            System.out.println("   ✅ 复杂工作流组合执行成功！");
            System.out.printf("   ⏱️ 总执行时间: %dms\n", endTime - startTime);
            System.out.printf("   📊 执行节点: %d/%d 成功\n",
                    result.getStatistics().getSuccessfulNodes(),
                    result.getStatistics().getTotalNodes());

            // 显示工作流注册中心统计
            WorkflowRegistry registry = WorkflowRegistry.getInstance();
            WorkflowRegistry.RegistryStatistics stats = registry.getStatistics();
            System.out.printf("   📋 注册中心统计: 总计=%d, 活跃=%d, 依赖=%d\n",
                    stats.getTotalWorkflows(),
                    stats.getStatusCounts().getOrDefault(WorkflowRegistry.WorkflowStatus.ACTIVE, 0),
                    stats.getTotalDependencies());

        } else {
            System.out.println("   ❌ 复杂工作流组合执行失败: " + result.getMessage());
        }

        System.out.println("\n🎉 关联节点演示完成！");
        System.out.println("💡 LogFlow关联节点支持:");
        System.out.println("   🔄 同步执行 - 等待子工作流完成");
        System.out.println("   ⚡ 异步执行 - 非阻塞并行处理");
        System.out.println("   🎯 条件执行 - 基于条件的智能执行");
        System.out.println("   🔄 循环执行 - 批量数据处理");
        System.out.println("   ⚡ 并行执行 - 多工作流同时执行");
        System.out.println("   🎭 工作流组合 - 构建复杂业务流程");

        engine.shutdown();
    }
}
