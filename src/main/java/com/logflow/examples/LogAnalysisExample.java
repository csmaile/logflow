package com.logflow.examples;

import com.logflow.builder.WorkflowBuilder;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;
import com.logflow.engine.WorkflowExecutionResult;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志分析工作流示例
 * 演示如何使用LogFlow系统进行日志诊断
 */
public class LogAnalysisExample {

    public static void main(String[] args) {
        // 创建工作流引擎
        WorkflowEngine engine = new WorkflowEngine(true, 4);

        try {
            // 示例1：基础日志错误检测
            runBasicErrorDetection(engine);

            // 示例2：性能分析工作流
            runPerformanceAnalysis(engine);

            // 示例3：复合诊断工作流
            runComplexDiagnosis(engine);

        } finally {
            engine.shutdown();
        }
    }

    /**
     * 示例1：基础日志错误检测
     */
    private static void runBasicErrorDetection(WorkflowEngine engine) {
        System.out.println("=== 示例1：基础日志错误检测 ===");

        Workflow workflow = new WorkflowBuilder("basic_error_detection", "基础错误检测")
                // 数据源节点：生成模拟日志数据
                .addPluginNode("data_source", "日志数据源",
                        Map.of(
                                "sourceType", "mock",
                                "mockType", "error_logs",
                                "count", 50,
                                "outputKey", "log_data"))

                // 诊断节点：错误检测
                .addDiagnosisNode("error_detection", "错误检测",
                        WorkflowBuilder.config(
                                "diagnosisType", "error_detection",
                                "inputKey", "log_data",
                                "outputKey", "error_results"))

                // 输出节点：控制台输出
                .addNotificationNode("console_output", "控制台输出",
                        WorkflowBuilder.config(
                                "inputKey", "error_results",
                                "outputType", "console",
                                "format", "json"))

                // 连接节点
                .connect("data_source", "error_detection")
                .connect("error_detection", "console_output")
                .build();

        // 执行工作流
        WorkflowExecutionResult result = engine.execute(workflow, null);

        // 输出执行结果
        System.out.println("执行成功: " + result.isSuccess());
        System.out.println("执行ID: " + result.getExecutionId());
        if (!result.isSuccess()) {
            System.out.println("错误信息: " + result.getMessage());
        }

        // 输出统计信息
        var stats = result.getStatistics();
        System.out.printf("节点统计: 总数=%d, 成功=%d, 失败=%d, 成功率=%.1f%%\n",
                stats.getTotalNodes(), stats.getSuccessfulNodes(),
                stats.getFailedNodes(), stats.getSuccessRate());
        System.out.println();
    }

    /**
     * 示例2：性能分析工作流
     */
    private static void runPerformanceAnalysis(WorkflowEngine engine) {
        System.out.println("=== 示例2：性能分析工作流 ===");

        Workflow workflow = new WorkflowBuilder("performance_analysis", "性能分析")
                // 数据源节点：生成模拟性能日志
                .addPluginNode("perf_data_source", "性能日志数据源",
                        Map.of(
                                "sourceType", "mock",
                                "mockType", "performance_logs",
                                "count", 100,
                                "outputKey", "perf_data"))

                // 脚本节点：数据预处理
                .addScriptNode("data_preprocessing", "数据预处理",
                        Map.of(
                                "scriptEngine", "javascript",
                                "inputKey", "perf_data",
                                "outputKey", "processed_data",
                                "script", "var data = input;" +
                                        "var processed = [];" +
                                        "for (var i = 0; i < data.length; i++) {" +
                                        "  var record = data[i];" +
                                        "  if (record.value > 80) {" +
                                        "    record.level = 'HIGH';" +
                                        "  } else if (record.value > 50) {" +
                                        "    record.level = 'MEDIUM';" +
                                        "  } else {" +
                                        "    record.level = 'LOW';" +
                                        "  }" +
                                        "  processed.push(record);" +
                                        "}" +
                                        "logger.info('处理了 ' + processed.length + ' 条记录');" +
                                        "processed;"))

                // 诊断节点：性能分析
                .addDiagnosisNode("performance_diagnosis", "性能诊断",
                        Map.of(
                                "diagnosisType", "performance_analysis",
                                "inputKey", "processed_data",
                                "outputKey", "perf_results",
                                "slowThreshold", 500.0))

                // 输出节点：文件输出
                .addNotificationNode("file_output", "文件输出",
                        WorkflowBuilder.config(
                                "inputKey", "perf_results",
                                "outputType", "json",
                                "filePath", "performance_analysis_result.json"))

                // 连接节点
                .connect("perf_data_source", "data_preprocessing")
                .connect("data_preprocessing", "performance_diagnosis")
                .connect("performance_diagnosis", "file_output")
                .build();

        // 执行工作流
        WorkflowExecutionResult result = engine.execute(workflow, null);

        System.out.println("执行成功: " + result.isSuccess());
        System.out.println("执行ID: " + result.getExecutionId());

        if (result.isSuccess()) {
            System.out.println("性能分析结果已保存到 performance_analysis_result.json");
        } else {
            System.out.println("错误信息: " + result.getMessage());
        }
        System.out.println();
    }

    /**
     * 示例3：复合诊断工作流
     */
    private static void runComplexDiagnosis(WorkflowEngine engine) {
        System.out.println("=== 示例3：复合诊断工作流 ===");

        // 初始数据
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("log_file_path", "/tmp/application.log");
        initialData.put("analysis_config", Map.of(
                "error_threshold", 10,
                "performance_threshold", 1000,
                "enable_trend_analysis", true));

        Workflow workflow = new WorkflowBuilder("complex_diagnosis", "复合诊断工作流")
                // 输入节点：获取配置参数
                .addInputNode("config_input", "配置输入",
                        WorkflowBuilder.config(
                                "inputKey", "analysis_config",
                                "outputKey", "config"))

                // 数据源节点：模拟从文件读取日志
                .addPluginNode("log_file_reader", "日志文件读取器",
                        Map.of(
                                "sourceType", "mock",
                                "mockType", "mixed_logs",
                                "count", 200,
                                "outputKey", "raw_logs"))

                // 脚本节点：日志过滤和清洗
                .addScriptNode("log_filter", "日志过滤器",
                        Map.of(
                                "scriptEngine", "javascript",
                                "inputKey", "raw_logs",
                                "outputKey", "filtered_logs",
                                "script", "var config = context.get('config');" +
                                        "var logs = input;" +
                                        "var filtered = [];" +
                                        "for (var i = 0; i < logs.length; i++) {" +
                                        "  var log = logs[i];" +
                                        "  if (log.level !== 'DEBUG') {" +
                                        "    filtered.push(log);" +
                                        "  }" +
                                        "}" +
                                        "logger.info('过滤后保留 ' + filtered.length + ' 条日志');" +
                                        "filtered;"))

                // 诊断节点1：错误检测
                .addDiagnosisNode("error_diagnosis", "错误诊断",
                        WorkflowBuilder.config(
                                "diagnosisType", "error_detection",
                                "inputKey", "filtered_logs",
                                "outputKey", "error_diagnosis_result"))

                // 诊断节点2：模式分析
                .addDiagnosisNode("pattern_diagnosis", "模式分析",
                        WorkflowBuilder.config(
                                "diagnosisType", "pattern_analysis",
                                "inputKey", "filtered_logs",
                                "outputKey", "pattern_diagnosis_result"))

                // 诊断节点3：异常检测
                .addDiagnosisNode("anomaly_diagnosis", "异常检测",
                        WorkflowBuilder.config(
                                "diagnosisType", "anomaly_detection",
                                "inputKey", "filtered_logs",
                                "outputKey", "anomaly_diagnosis_result"))

                // 脚本节点：结果聚合
                .addScriptNode("result_aggregator", "结果聚合器",
                        Map.of(
                                "scriptEngine", "javascript",
                                "inputKey", "error_diagnosis_result",
                                "outputKey", "final_report",
                                "script", "var errorResult = context.get('error_diagnosis_result');" +
                                        "var patternResult = context.get('pattern_diagnosis_result');" +
                                        "var anomalyResult = context.get('anomaly_diagnosis_result');" +
                                        "var report = {" +
                                        "  timestamp: utils.now()," +
                                        "  summary: {" +
                                        "    total_issues: errorResult.issueCount + patternResult.issueCount + anomalyResult.issueCount,"
                                        +
                                        "    error_issues: errorResult.issueCount," +
                                        "    pattern_findings: patternResult.issueCount," +
                                        "    anomaly_findings: anomalyResult.issueCount" +
                                        "  }," +
                                        "  details: {" +
                                        "    errors: errorResult," +
                                        "    patterns: patternResult," +
                                        "    anomalies: anomalyResult" +
                                        "  }" +
                                        "};" +
                                        "logger.info('生成最终报告，共发现 ' + report.summary.total_issues + ' 个问题');" +
                                        "report;"))

                // 输出节点：综合报告
                .addNotificationNode("report_output", "报告输出",
                        WorkflowBuilder.config(
                                "inputKey", "final_report",
                                "outputType", "json",
                                "filePath", "diagnosis_report.json"))

                // 输出节点：控制台摘要
                .addNotificationNode("summary_output", "摘要输出",
                        WorkflowBuilder.config(
                                "inputKey", "final_report",
                                "outputType", "console"))

                // 建立连接关系
                .connect("config_input", "log_file_reader")
                .connect("log_file_reader", "log_filter")
                .connect("log_filter", "error_diagnosis")
                .connect("log_filter", "pattern_diagnosis")
                .connect("log_filter", "anomaly_diagnosis")
                .connect("error_diagnosis", "result_aggregator")
                .connect("pattern_diagnosis", "result_aggregator")
                .connect("anomaly_diagnosis", "result_aggregator")
                .connect("result_aggregator", "report_output")
                .connect("result_aggregator", "summary_output")

                .metadata("version", "1.0")
                .metadata("author", "LogFlow System")
                .build();

        // 执行工作流
        WorkflowExecutionResult result = engine.execute(workflow, initialData);

        System.out.println("执行成功: " + result.isSuccess());
        System.out.println("执行ID: " + result.getExecutionId());

        if (result.isSuccess()) {
            var stats = result.getStatistics();
            System.out.printf("执行统计: 总节点=%d, 成功=%d, 平均耗时=%.1fms\n",
                    stats.getTotalNodes(), stats.getSuccessfulNodes(),
                    stats.getAverageNodeExecutionTime());
            System.out.println("诊断报告已保存到 diagnosis_report.json");
        } else {
            System.out.println("错误信息: " + result.getMessage());
            // 输出失败节点的详细信息
            result.getFailedNodeResults().forEach(
                    (nodeId, nodeResult) -> System.out.println("节点 " + nodeId + " 失败: " + nodeResult.getMessage()));
        }
        System.out.println();
    }
}
