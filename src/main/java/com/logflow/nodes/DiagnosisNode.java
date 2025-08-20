package com.logflow.nodes;

import com.logflow.core.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 诊断节点
 * 执行日志诊断和分析逻辑
 */
public class DiagnosisNode extends AbstractWorkflowNode {
    
    public DiagnosisNode(String id, String name) {
        super(id, name, NodeType.DIAGNOSIS);
    }
    
    @Override
    protected NodeExecutionResult doExecute(WorkflowContext context) throws WorkflowException {
        String inputKey = getConfigValue("inputKey", String.class, "data");
        String diagnosisType = getConfigValue("diagnosisType", String.class);
        
        // 获取输入数据
        Object inputData = context.getData(inputKey);
        if (inputData == null) {
            throw new WorkflowException(id, "诊断输入数据为空");
        }
        
        try {
            DiagnosisResult result = null;
            
            switch (diagnosisType.toLowerCase()) {
                case "error_detection":
                    result = detectErrors(inputData);
                    break;
                case "pattern_analysis":
                    result = analyzePatterns(inputData);
                    break;
                case "performance_analysis":
                    result = analyzePerformance(inputData);
                    break;
                case "anomaly_detection":
                    result = detectAnomalies(inputData);
                    break;
                case "trend_analysis":
                    result = analyzeTrends(inputData);
                    break;
                default:
                    throw new WorkflowException(id, "不支持的诊断类型: " + diagnosisType);
            }
            
            // 将诊断结果存储到上下文
            String outputKey = getConfigValue("outputKey", String.class, "diagnosis_result");
            context.setData(outputKey, result);
            
            logger.info("诊断完成, 类型: {}, 发现问题: {}", diagnosisType, result.getIssueCount());
            
            return NodeExecutionResult.builder(id)
                    .success(true)
                    .data(result)
                    .metadata("diagnosisType", diagnosisType)
                    .metadata("issueCount", result.getIssueCount())
                    .metadata("severity", result.getMaxSeverity())
                    .build();
                    
        } catch (Exception e) {
            throw new WorkflowException(id, "诊断执行失败", e);
        }
    }
    
    @Override
    public ValidationResult validate() {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        String diagnosisType = getConfigValue("diagnosisType", String.class);
        if (diagnosisType == null || diagnosisType.trim().isEmpty()) {
            builder.error("必须配置诊断类型 (diagnosisType)");
        }
        
        return builder.build();
    }
    
    /**
     * 错误检测
     */
    private DiagnosisResult detectErrors(Object data) {
        DiagnosisResult result = new DiagnosisResult("错误检测");
        
        List<String> patterns = getConfigValue("errorPatterns", List.class, 
            Arrays.asList("ERROR", "FATAL", "Exception", "Failed", "Timeout"));
        
        List<String> lines = extractLines(data);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            for (String pattern : patterns) {
                if (line.toUpperCase().contains(pattern.toUpperCase())) {
                    DiagnosisIssue issue = new DiagnosisIssue(
                        "ERROR_FOUND",
                        "发现错误: " + pattern,
                        "行 " + (i + 1) + ": " + line,
                        determineSeverity(pattern),
                        i + 1
                    );
                    result.addIssue(issue);
                    break;
                }
            }
        }
        
        return result;
    }
    
    /**
     * 模式分析
     */
    private DiagnosisResult analyzePatterns(Object data) {
        DiagnosisResult result = new DiagnosisResult("模式分析");
        
        List<String> lines = extractLines(data);
        Map<String, Integer> patternCounts = new HashMap<>();
        
        // 统计各种模式出现的频率
        for (String line : lines) {
            // 提取时间戳模式
            if (line.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
                patternCounts.merge("timestamp_pattern", 1, Integer::sum);
            }
            
            // 提取IP地址模式
            if (line.matches(".*\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b.*")) {
                patternCounts.merge("ip_pattern", 1, Integer::sum);
            }
            
            // 提取HTTP状态码模式
            if (line.matches(".*\\b[1-5]\\d{2}\\b.*")) {
                patternCounts.merge("http_status_pattern", 1, Integer::sum);
            }
        }
        
        // 生成模式分析报告
        for (Map.Entry<String, Integer> entry : patternCounts.entrySet()) {
            if (entry.getValue() > 0) {
                DiagnosisIssue issue = new DiagnosisIssue(
                    "PATTERN_FOUND",
                    "发现模式: " + entry.getKey(),
                    "出现次数: " + entry.getValue(),
                    "INFO",
                    0
                );
                result.addIssue(issue);
            }
        }
        
        return result;
    }
    
    /**
     * 性能分析
     */
    private DiagnosisResult analyzePerformance(Object data) {
        DiagnosisResult result = new DiagnosisResult("性能分析");
        
        List<String> lines = extractLines(data);
        List<Double> responseTimes = new ArrayList<>();
        
        // 提取响应时间信息
        Pattern timePattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*ms");
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            java.util.regex.Matcher matcher = timePattern.matcher(line);
            if (matcher.find()) {
                try {
                    double time = Double.parseDouble(matcher.group(1));
                    responseTimes.add(time);
                    
                    // 检查慢请求
                    double slowThreshold = getConfigValue("slowThreshold", Double.class, 1000.0);
                    if (time > slowThreshold) {
                        DiagnosisIssue issue = new DiagnosisIssue(
                            "SLOW_RESPONSE",
                            "发现慢响应",
                            String.format("行 %d: 响应时间 %.2fms (阈值: %.0fms)", i + 1, time, slowThreshold),
                            "WARN",
                            i + 1
                        );
                        result.addIssue(issue);
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }
        
        // 生成性能统计
        if (!responseTimes.isEmpty()) {
            double avg = responseTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double max = responseTimes.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double min = responseTimes.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            
            DiagnosisIssue statsIssue = new DiagnosisIssue(
                "PERFORMANCE_STATS",
                "性能统计",
                String.format("平均: %.2fms, 最大: %.2fms, 最小: %.2fms, 样本数: %d", 
                    avg, max, min, responseTimes.size()),
                "INFO",
                0
            );
            result.addIssue(statsIssue);
        }
        
        return result;
    }
    
    /**
     * 异常检测
     */
    private DiagnosisResult detectAnomalies(Object data) {
        DiagnosisResult result = new DiagnosisResult("异常检测");
        
        List<String> lines = extractLines(data);
        Map<String, Integer> hourCounts = new HashMap<>();
        
        // 按小时统计日志数量
        for (String line : lines) {
            // 简化的时间提取，实际应该使用更精确的正则表达式
            if (line.length() > 10) {
                String hourKey = line.substring(0, Math.min(13, line.length())); // 假设前13个字符包含小时信息
                hourCounts.merge(hourKey, 1, Integer::sum);
            }
        }
        
        // 检测异常的日志量波动
        if (hourCounts.size() > 1) {
            double avg = hourCounts.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            double threshold = avg * 2; // 简化的异常阈值
            
            for (Map.Entry<String, Integer> entry : hourCounts.entrySet()) {
                if (entry.getValue() > threshold) {
                    DiagnosisIssue issue = new DiagnosisIssue(
                        "LOG_SPIKE",
                        "日志量异常",
                        String.format("时间段 %s: %d 条日志 (平均: %.0f)", 
                            entry.getKey(), entry.getValue(), avg),
                        "WARN",
                        0
                    );
                    result.addIssue(issue);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 趋势分析
     */
    private DiagnosisResult analyzeTrends(Object data) {
        DiagnosisResult result = new DiagnosisResult("趋势分析");
        
        List<String> lines = extractLines(data);
        
        // 分析错误率趋势
        int totalLines = lines.size();
        long errorLines = lines.stream()
            .filter(line -> line.toUpperCase().contains("ERROR") || 
                           line.toUpperCase().contains("FATAL"))
            .count();
        
        double errorRate = totalLines > 0 ? (double) errorLines / totalLines * 100 : 0;
        
        String severity = "INFO";
        if (errorRate > 10) {
            severity = "CRITICAL";
        } else if (errorRate > 5) {
            severity = "WARN";
        }
        
        DiagnosisIssue trendIssue = new DiagnosisIssue(
            "ERROR_RATE_TREND",
            "错误率趋势",
            String.format("总行数: %d, 错误行数: %d, 错误率: %.2f%%", 
                totalLines, errorLines, errorRate),
            severity,
            0
        );
        result.addIssue(trendIssue);
        
        return result;
    }
    
    /**
     * 从输入数据中提取文本行
     */
    private List<String> extractLines(Object data) {
        if (data instanceof List) {
            return ((List<?>) data).stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        } else if (data instanceof String) {
            return Arrays.asList(((String) data).split("\n"));
        } else if (data instanceof Map) {
            Object lines = ((Map<?, ?>) data).get("lines");
            if (lines instanceof List) {
                return ((List<?>) lines).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            }
        }
        
        return Arrays.asList(data.toString());
    }
    
    /**
     * 根据模式确定严重级别
     */
    private String determineSeverity(String pattern) {
        switch (pattern.toUpperCase()) {
            case "FATAL":
            case "CRITICAL":
                return "CRITICAL";
            case "ERROR":
                return "ERROR";
            case "WARN":
            case "WARNING":
                return "WARN";
            default:
                return "INFO";
        }
    }
    
    /**
     * 诊断结果类
     */
    public static class DiagnosisResult {
        private final String type;
        private final List<DiagnosisIssue> issues;
        private final long timestamp;
        
        public DiagnosisResult(String type) {
            this.type = type;
            this.issues = new ArrayList<>();
            this.timestamp = System.currentTimeMillis();
        }
        
        public void addIssue(DiagnosisIssue issue) {
            issues.add(issue);
        }
        
        public String getType() { return type; }
        public List<DiagnosisIssue> getIssues() { return new ArrayList<>(issues); }
        public int getIssueCount() { return issues.size(); }
        public long getTimestamp() { return timestamp; }
        
        public String getMaxSeverity() {
            return issues.stream()
                .map(DiagnosisIssue::getSeverity)
                .max(this::compareSeverity)
                .orElse("INFO");
        }
        
        private int compareSeverity(String s1, String s2) {
            Map<String, Integer> severityOrder = Map.of(
                "INFO", 1, "WARN", 2, "ERROR", 3, "CRITICAL", 4
            );
            return Integer.compare(
                severityOrder.getOrDefault(s1, 0),
                severityOrder.getOrDefault(s2, 0)
            );
        }
    }
    
    /**
     * 诊断问题类
     */
    public static class DiagnosisIssue {
        private final String code;
        private final String title;
        private final String description;
        private final String severity;
        private final int lineNumber;
        
        public DiagnosisIssue(String code, String title, String description, String severity, int lineNumber) {
            this.code = code;
            this.title = title;
            this.description = description;
            this.severity = severity;
            this.lineNumber = lineNumber;
        }
        
        // Getters
        public String getCode() { return code; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getSeverity() { return severity; }
        public int getLineNumber() { return lineNumber; }
    }
}
