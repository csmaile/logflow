package com.logflow.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模拟LLM提供者
 * 用于演示和测试脚本生成功能
 * 实际使用时应替换为真实的LLM API调用
 */
public class MockLLMProvider implements LLMScriptGenerator.LLMProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockLLMProvider.class);

    // 预定义的脚本模板
    private static final Map<String, String> SCRIPT_TEMPLATES = new HashMap<>();

    static {
        // 数据过滤模板
        SCRIPT_TEMPLATES.put("filter",
                "// 数据过滤脚本\n" +
                        "var config = context.get('config') || {};\n" +
                        "var data = input;\n" +
                        "var filtered = [];\n" +
                        "\n" +
                        "if (!data || !Array.isArray(data)) {\n" +
                        "  logger.warn('输入数据无效或为空');\n" +
                        "  return [];\n" +
                        "}\n" +
                        "\n" +
                        "for (var i = 0; i < data.length; i++) {\n" +
                        "  var item = data[i];\n" +
                        "  \n" +
                        "  // 根据需求添加过滤条件\n" +
                        "  if (item.level && item.level !== 'DEBUG') {\n" +
                        "    filtered.push(item);\n" +
                        "  }\n" +
                        "}\n" +
                        "\n" +
                        "logger.info('过滤完成: 输入' + data.length + '条，输出' + filtered.length + '条');\n" +
                        "filtered;");

        // 数据转换模板
        SCRIPT_TEMPLATES.put("transform",
                "// 数据转换脚本\n" +
                        "var data = input;\n" +
                        "var transformed = [];\n" +
                        "\n" +
                        "if (!data || !Array.isArray(data)) {\n" +
                        "  logger.warn('输入数据无效');\n" +
                        "  return [];\n" +
                        "}\n" +
                        "\n" +
                        "for (var i = 0; i < data.length; i++) {\n" +
                        "  var item = data[i];\n" +
                        "  \n" +
                        "  var newItem = {\n" +
                        "    id: item.id,\n" +
                        "    level: item.level,\n" +
                        "    message: item.message,\n" +
                        "    timestamp: item.timestamp,\n" +
                        "    processedAt: utils.now(),\n" +
                        "    processedBy: 'transform_script'\n" +
                        "  };\n" +
                        "  \n" +
                        "  transformed.push(newItem);\n" +
                        "}\n" +
                        "\n" +
                        "logger.info('转换完成: 处理了' + transformed.length + '条记录');\n" +
                        "transformed;");

        // 错误统计模板
        SCRIPT_TEMPLATES.put("error_analysis",
                "// 错误分析脚本\n" +
                        "var logs = input;\n" +
                        "var analysis = {\n" +
                        "  totalLogs: logs.length,\n" +
                        "  errorCount: 0,\n" +
                        "  warningCount: 0,\n" +
                        "  errorTypes: {},\n" +
                        "  recommendations: []\n" +
                        "};\n" +
                        "\n" +
                        "if (!logs || !Array.isArray(logs)) {\n" +
                        "  logger.warn('输入日志数据无效');\n" +
                        "  return analysis;\n" +
                        "}\n" +
                        "\n" +
                        "for (var i = 0; i < logs.length; i++) {\n" +
                        "  var log = logs[i];\n" +
                        "  \n" +
                        "  if (log.level === 'ERROR' || log.level === 'FATAL') {\n" +
                        "    analysis.errorCount++;\n" +
                        "    \n" +
                        "    // 分析错误类型\n" +
                        "    var errorType = 'Other';\n" +
                        "    if (log.message.indexOf('Exception') >= 0) errorType = 'Exception';\n" +
                        "    else if (log.message.indexOf('Timeout') >= 0) errorType = 'Timeout';\n" +
                        "    else if (log.message.indexOf('Connection') >= 0) errorType = 'Connection';\n" +
                        "    \n" +
                        "    analysis.errorTypes[errorType] = (analysis.errorTypes[errorType] || 0) + 1;\n" +
                        "  } else if (log.level === 'WARN') {\n" +
                        "    analysis.warningCount++;\n" +
                        "  }\n" +
                        "}\n" +
                        "\n" +
                        "// 生成建议\n" +
                        "var errorRate = (analysis.errorCount / analysis.totalLogs * 100).toFixed(2);\n" +
                        "if (analysis.errorCount > 10) {\n" +
                        "  analysis.recommendations.push('错误数量较多(' + analysis.errorCount + '个)，建议立即检查');\n" +
                        "}\n" +
                        "if (errorRate > 5) {\n" +
                        "  analysis.recommendations.push('错误率(' + errorRate + '%)较高，需要关注');\n" +
                        "}\n" +
                        "if (analysis.recommendations.length === 0) {\n" +
                        "  analysis.recommendations.push('错误情况正常');\n" +
                        "}\n" +
                        "\n" +
                        "logger.info('错误分析完成: 发现' + analysis.errorCount + '个错误，错误率' + errorRate + '%');\n" +
                        "analysis;");

        // 数据聚合模板
        SCRIPT_TEMPLATES.put("aggregate",
                "// 数据聚合脚本\n" +
                        "var data = input;\n" +
                        "var summary = {\n" +
                        "  total: data.length,\n" +
                        "  levels: {},\n" +
                        "  timeDistribution: {},\n" +
                        "  statistics: {\n" +
                        "    averageValue: 0,\n" +
                        "    maxValue: 0,\n" +
                        "    minValue: Number.MAX_VALUE\n" +
                        "  }\n" +
                        "};\n" +
                        "\n" +
                        "if (!data || !Array.isArray(data)) {\n" +
                        "  logger.warn('输入数据无效');\n" +
                        "  return summary;\n" +
                        "}\n" +
                        "\n" +
                        "var totalValue = 0;\n" +
                        "var valueCount = 0;\n" +
                        "\n" +
                        "for (var i = 0; i < data.length; i++) {\n" +
                        "  var item = data[i];\n" +
                        "  \n" +
                        "  // 统计级别分布\n" +
                        "  if (item.level) {\n" +
                        "    summary.levels[item.level] = (summary.levels[item.level] || 0) + 1;\n" +
                        "  }\n" +
                        "  \n" +
                        "  // 时间分布\n" +
                        "  if (item.timestamp) {\n" +
                        "    var hour = new Date(item.timestamp).getHours();\n" +
                        "    summary.timeDistribution[hour] = (summary.timeDistribution[hour] || 0) + 1;\n" +
                        "  }\n" +
                        "  \n" +
                        "  // 数值统计\n" +
                        "  if (item.value !== undefined) {\n" +
                        "    totalValue += item.value;\n" +
                        "    valueCount++;\n" +
                        "    summary.statistics.maxValue = Math.max(summary.statistics.maxValue, item.value);\n" +
                        "    summary.statistics.minValue = Math.min(summary.statistics.minValue, item.value);\n" +
                        "  }\n" +
                        "}\n" +
                        "\n" +
                        "if (valueCount > 0) {\n" +
                        "  summary.statistics.averageValue = (totalValue / valueCount).toFixed(2);\n" +
                        "}\n" +
                        "\n" +
                        "logger.info('聚合完成: 总计' + summary.total + '条记录，' + valueCount + '个数值');\n" +
                        "summary;");
    }

    @Override
    public String generateText(String prompt) throws Exception {
        logger.info("模拟LLM处理请求，提示长度: {}", prompt.length());

        // 模拟处理延迟
        Thread.sleep(1000 + (int) (Math.random() * 2000));

        // 分析用户需求，选择合适的模板
        String requirement = extractUserRequirement(prompt);
        String scriptType = analyzeRequirementType(requirement);

        logger.debug("检测到需求类型: {}", scriptType);

        // 获取基础模板
        String baseScript = SCRIPT_TEMPLATES.getOrDefault(scriptType, SCRIPT_TEMPLATES.get("filter"));

        // 根据具体需求调整脚本
        String customizedScript = customizeScript(baseScript, requirement, prompt);

        // 添加生成说明
        StringBuilder response = new StringBuilder();
        response.append("根据您的需求，我生成了以下LogFlow脚本：\n\n");
        response.append("```javascript\n");
        response.append(customizedScript);
        response.append("\n```\n\n");
        response.append("这个脚本实现了您要求的功能，包含了适当的错误处理和日志记录。");

        return response.toString();
    }

    /**
     * 从提示中提取用户需求
     */
    private String extractUserRequirement(String prompt) {
        Pattern pattern = Pattern.compile("## 用户需求\\s*\\n\\n(.+?)(?=\\n\\n##|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(prompt);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return prompt;
    }

    /**
     * 分析需求类型
     */
    private String analyzeRequirementType(String requirement) {
        String lowerReq = requirement.toLowerCase();

        if (lowerReq.contains("过滤") || lowerReq.contains("筛选") || lowerReq.contains("filter")) {
            return "filter";
        }
        if (lowerReq.contains("转换") || lowerReq.contains("格式") || lowerReq.contains("transform")) {
            return "transform";
        }
        if (lowerReq.contains("错误") || lowerReq.contains("异常") || lowerReq.contains("error")) {
            return "error_analysis";
        }
        if (lowerReq.contains("统计") || lowerReq.contains("聚合") || lowerReq.contains("汇总")
                || lowerReq.contains("aggregate")) {
            return "aggregate";
        }

        // 默认返回过滤类型
        return "filter";
    }

    /**
     * 根据需求定制脚本
     */
    private String customizeScript(String baseScript, String requirement, String fullPrompt) {
        String customized = baseScript;

        // 根据上下文信息调整变量名
        if (fullPrompt.contains("log_data") || fullPrompt.contains("logs")) {
            customized = customized.replace("var data = input;", "var logs = input;");
            customized = customized.replace("data.length", "logs.length");
            customized = customized.replace("data[i]", "logs[i]");
        }

        // 根据需求调整过滤条件
        if (requirement.contains("ERROR") || requirement.contains("错误")) {
            customized = customized.replace("item.level !== 'DEBUG'",
                    "item.level === 'ERROR' || item.level === 'FATAL'");
        }

        // 根据需求调整处理逻辑
        if (requirement.contains("时间") || requirement.contains("小时")) {
            customized = customized.replace("processedBy: 'transform_script'",
                    "processedBy: 'transform_script',\n    hour: new Date(item.timestamp).getHours()");
        }

        // 添加特定的需求处理
        if (requirement.contains("计数") || requirement.contains("数量")) {
            customized = customized.replace("logger.info('",
                    "context.set('item_count', " + (customized.contains("filtered") ? "filtered" : "transformed")
                            + ".length);\n" +
                            "logger.info('");
        }

        return customized;
    }
}
