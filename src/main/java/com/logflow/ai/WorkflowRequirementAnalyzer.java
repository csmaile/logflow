package com.logflow.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工作流需求分析器
 * 分析用户的一句话需求，解析出工作流结构和组件
 */
public class WorkflowRequirementAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowRequirementAnalyzer.class);

    // 关键词模式匹配
    private static final Map<Pattern, String> NODE_TYPE_PATTERNS = new HashMap<>();
    private static final Map<Pattern, String> DATA_SOURCE_PATTERNS = new HashMap<>();
    private static final Map<Pattern, String> PROCESSING_PATTERNS = new HashMap<>();
    private static final Map<Pattern, String> OUTPUT_PATTERNS = new HashMap<>();

    static {
        // 节点类型识别模式
        NODE_TYPE_PATTERNS.put(Pattern.compile("(日志|log|错误|异常|error|监控|分析)"), "log_analysis");
        NODE_TYPE_PATTERNS.put(Pattern.compile("(性能|performance|响应时间|延迟|吞吐量)"), "performance_analysis");
        NODE_TYPE_PATTERNS.put(Pattern.compile("(数据清洗|清理|标准化|格式化|转换)"), "data_cleaning");
        NODE_TYPE_PATTERNS.put(Pattern.compile("(统计|聚合|汇总|报告|dashboard)"), "data_aggregation");
        NODE_TYPE_PATTERNS.put(Pattern.compile("(实时|流式|stream|kafka|消息)"), "real_time_processing");
        NODE_TYPE_PATTERNS.put(Pattern.compile("(批处理|batch|定时|调度)"), "batch_processing");

        // 数据源识别模式
        DATA_SOURCE_PATTERNS.put(Pattern.compile("(文件|file|csv|json|txt)"), "file");
        DATA_SOURCE_PATTERNS.put(Pattern.compile("(数据库|database|mysql|postgresql|oracle)"), "database");
        DATA_SOURCE_PATTERNS.put(Pattern.compile("(api|接口|rest|http)"), "api");
        DATA_SOURCE_PATTERNS.put(Pattern.compile("(kafka|消息队列|mq|rabbitmq)"), "message_queue");
        DATA_SOURCE_PATTERNS.put(Pattern.compile("(elasticsearch|es|搜索引擎)"), "elasticsearch");
        DATA_SOURCE_PATTERNS.put(Pattern.compile("(模拟|测试|mock|示例)"), "mock");

        // 处理模式识别
        PROCESSING_PATTERNS.put(Pattern.compile("(过滤|筛选|filter)"), "filter");
        PROCESSING_PATTERNS.put(Pattern.compile("(转换|transform|映射|map)"), "transform");
        PROCESSING_PATTERNS.put(Pattern.compile("(验证|校验|validate)"), "validate");
        PROCESSING_PATTERNS.put(Pattern.compile("(去重|dedupe|唯一)"), "deduplicate");
        PROCESSING_PATTERNS.put(Pattern.compile("(排序|sort|order)"), "sort");
        PROCESSING_PATTERNS.put(Pattern.compile("(分组|group|聚合)"), "group");
        PROCESSING_PATTERNS.put(Pattern.compile("(计算|calculate|统计)"), "calculate");

        // 输出模式识别
        OUTPUT_PATTERNS.put(Pattern.compile("(文件|file|保存|导出)"), "file");
        OUTPUT_PATTERNS.put(Pattern.compile("(数据库|database|存储|insert)"), "database");
        OUTPUT_PATTERNS.put(Pattern.compile("(邮件|email|通知|alert)"), "notification");
        OUTPUT_PATTERNS.put(Pattern.compile("(dashboard|图表|可视化|展示)"), "dashboard");
        OUTPUT_PATTERNS.put(Pattern.compile("(api|接口|推送|webhook)"), "api");
        OUTPUT_PATTERNS.put(Pattern.compile("(控制台|console|打印|显示)"), "console");
    }

    /**
     * 分析用户需求，解析出工作流结构
     * 
     * @param userRequirement 用户的一句话需求
     * @return 需求分析结果
     */
    public RequirementAnalysisResult analyzeRequirement(String userRequirement) {
        logger.info("开始分析用户需求: {}", userRequirement);

        RequirementAnalysisResult result = new RequirementAnalysisResult();
        result.setOriginalRequirement(userRequirement);

        String normalizedReq = userRequirement.toLowerCase();

        // 1. 分析工作流类型和领域
        result.setWorkflowType(identifyWorkflowType(normalizedReq));
        result.setDomain(identifyDomain(normalizedReq));

        // 2. 分析数据源需求
        result.setDataSources(identifyDataSources(normalizedReq, userRequirement));

        // 3. 分析处理步骤
        result.setProcessingSteps(identifyProcessingSteps(normalizedReq, userRequirement));

        // 4. 分析输出需求
        result.setOutputRequirements(identifyOutputRequirements(normalizedReq, userRequirement));

        // 5. 提取业务规则和参数
        result.setBusinessRules(extractBusinessRules(userRequirement));
        result.setParameters(extractParameters(userRequirement));

        // 6. 估算复杂度
        result.setComplexity(estimateComplexity(result));

        // 7. 生成工作流建议名称和描述
        result.setSuggestedName(generateWorkflowName(result));
        result.setSuggestedDescription(generateWorkflowDescription(result));

        logger.info("需求分析完成: 类型={}, 复杂度={}, 数据源={}, 处理步骤={}, 输出={}",
                result.getWorkflowType(), result.getComplexity(),
                result.getDataSources().size(), result.getProcessingSteps().size(),
                result.getOutputRequirements().size());

        return result;
    }

    /**
     * 识别工作流类型
     */
    private String identifyWorkflowType(String requirement) {
        for (Map.Entry<Pattern, String> entry : NODE_TYPE_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(requirement).find()) {
                return entry.getValue();
            }
        }
        return "general_processing";
    }

    /**
     * 识别应用领域
     */
    private String identifyDomain(String requirement) {
        if (requirement.contains("日志") || requirement.contains("log")) {
            return "logging";
        } else if (requirement.contains("监控") || requirement.contains("metric")) {
            return "monitoring";
        } else if (requirement.contains("数据") || requirement.contains("data")) {
            return "data_processing";
        } else if (requirement.contains("业务") || requirement.contains("business")) {
            return "business_intelligence";
        }
        return "general";
    }

    /**
     * 识别数据源需求
     */
    private List<DataSourceRequirement> identifyDataSources(String normalizedReq, String originalReq) {
        List<DataSourceRequirement> dataSources = new ArrayList<>();

        for (Map.Entry<Pattern, String> entry : DATA_SOURCE_PATTERNS.entrySet()) {
            Matcher matcher = entry.getKey().matcher(normalizedReq);
            if (matcher.find()) {
                DataSourceRequirement ds = new DataSourceRequirement();
                ds.setType(entry.getValue());
                ds.setDescription(extractDataSourceDescription(originalReq, entry.getValue()));
                ds.setRequired(true);

                // 根据类型设置特定参数
                ds.setParameters(generateDataSourceParameters(entry.getValue(), originalReq));

                dataSources.add(ds);
            }
        }

        // 如果没有明确指定数据源，默认添加模拟数据源
        if (dataSources.isEmpty()) {
            DataSourceRequirement mockDs = new DataSourceRequirement();
            mockDs.setType("mock");
            mockDs.setDescription("模拟数据源，用于演示");
            mockDs.setRequired(true);
            mockDs.setParameters(Map.of("mockType", inferMockDataType(normalizedReq)));
            dataSources.add(mockDs);
        }

        return dataSources;
    }

    /**
     * 识别处理步骤
     */
    private List<ProcessingStepRequirement> identifyProcessingSteps(String normalizedReq, String originalReq) {
        List<ProcessingStepRequirement> steps = new ArrayList<>();

        // 按照处理的逻辑顺序识别步骤
        for (Map.Entry<Pattern, String> entry : PROCESSING_PATTERNS.entrySet()) {
            Matcher matcher = entry.getKey().matcher(normalizedReq);
            if (matcher.find()) {
                ProcessingStepRequirement step = new ProcessingStepRequirement();
                step.setType(entry.getValue());
                step.setDescription(extractProcessingDescription(originalReq, entry.getValue()));
                step.setOrder(steps.size() + 1);
                step.setRequired(true);
                step.setLogic(extractProcessingLogic(originalReq, entry.getValue()));

                steps.add(step);
            }
        }

        // 如果没有明确的处理步骤，根据工作流类型添加默认步骤
        if (steps.isEmpty()) {
            steps.addAll(generateDefaultProcessingSteps(normalizedReq));
        }

        // 确保步骤的逻辑顺序
        sortProcessingSteps(steps);

        return steps;
    }

    /**
     * 识别输出需求
     */
    private List<OutputRequirement> identifyOutputRequirements(String normalizedReq, String originalReq) {
        List<OutputRequirement> outputs = new ArrayList<>();

        for (Map.Entry<Pattern, String> entry : OUTPUT_PATTERNS.entrySet()) {
            Matcher matcher = entry.getKey().matcher(normalizedReq);
            if (matcher.find()) {
                OutputRequirement output = new OutputRequirement();
                output.setType(entry.getValue());
                output.setDescription(extractOutputDescription(originalReq, entry.getValue()));
                output.setRequired(true);
                output.setFormat(inferOutputFormat(originalReq, entry.getValue()));

                outputs.add(output);
            }
        }

        // 如果没有明确的输出要求，添加默认控制台输出
        if (outputs.isEmpty()) {
            OutputRequirement consoleOutput = new OutputRequirement();
            consoleOutput.setType("console");
            consoleOutput.setDescription("控制台输出结果");
            consoleOutput.setRequired(true);
            consoleOutput.setFormat("text");
            outputs.add(consoleOutput);
        }

        return outputs;
    }

    /**
     * 提取业务规则
     */
    private List<String> extractBusinessRules(String requirement) {
        List<String> rules = new ArrayList<>();

        // 查找数字阈值
        Pattern numberPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(秒|分钟|小时|天|%|个|条|次)");
        Matcher matcher = numberPattern.matcher(requirement);
        while (matcher.find()) {
            rules.add("阈值规则: " + matcher.group());
        }

        // 查找条件语句
        Pattern conditionPattern = Pattern.compile("(如果|当|大于|小于|等于|超过|低于)([^，。]+)");
        matcher = conditionPattern.matcher(requirement);
        while (matcher.find()) {
            rules.add("条件规则: " + matcher.group());
        }

        // 查找时间窗口
        Pattern timePattern = Pattern.compile("(每|最近|过去)\\s*(\\d+)\\s*(分钟|小时|天)");
        matcher = timePattern.matcher(requirement);
        while (matcher.find()) {
            rules.add("时间规则: " + matcher.group());
        }

        return rules;
    }

    /**
     * 提取参数
     */
    private Map<String, Object> extractParameters(String requirement) {
        Map<String, Object> parameters = new HashMap<>();

        // 提取数字参数
        Pattern numberPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)");
        Matcher matcher = numberPattern.matcher(requirement);
        List<String> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(matcher.group(1));
        }

        if (!numbers.isEmpty()) {
            parameters.put("extracted_numbers", numbers);
        }

        // 提取级别参数
        if (requirement.contains("ERROR") || requirement.contains("错误")) {
            parameters.put("log_level", "ERROR");
        } else if (requirement.contains("WARN") || requirement.contains("警告")) {
            parameters.put("log_level", "WARN");
        }

        // 提取时间相关参数
        if (requirement.contains("实时") || requirement.contains("即时")) {
            parameters.put("processing_mode", "real_time");
        } else if (requirement.contains("批处理") || requirement.contains("定时")) {
            parameters.put("processing_mode", "batch");
        }

        return parameters;
    }

    /**
     * 估算复杂度
     */
    private String estimateComplexity(RequirementAnalysisResult result) {
        int score = 0;

        // 基于数据源数量
        score += result.getDataSources().size() * 2;

        // 基于处理步骤数量
        score += result.getProcessingSteps().size() * 3;

        // 基于输出数量
        score += result.getOutputRequirements().size() * 2;

        // 基于业务规则复杂度
        score += result.getBusinessRules().size() * 2;

        if (score <= 5)
            return "SIMPLE";
        if (score <= 15)
            return "MEDIUM";
        if (score <= 25)
            return "COMPLEX";
        return "VERY_COMPLEX";
    }

    // 辅助方法实现
    private String extractDataSourceDescription(String req, String type) {
        switch (type) {
            case "file":
                return "从文件读取数据";
            case "database":
                return "从数据库查询数据";
            case "api":
                return "从API接口获取数据";
            case "mock":
                return "生成模拟测试数据";
            default:
                return "数据源: " + type;
        }
    }

    private Map<String, Object> generateDataSourceParameters(String type, String req) {
        Map<String, Object> params = new HashMap<>();

        switch (type) {
            case "mock":
                params.put("mockType", inferMockDataType(req.toLowerCase()));
                params.put("recordCount", extractRecordCount(req));
                break;
            case "file":
                params.put("filePath", "data/input.json");
                params.put("format", inferFileFormat(req));
                break;
            case "database":
                params.put("table", "logs");
                params.put("query", "SELECT * FROM logs WHERE created_at > NOW() - INTERVAL 1 DAY");
                break;
        }

        return params;
    }

    private String inferMockDataType(String req) {
        if (req.contains("错误") || req.contains("error"))
            return "error_logs";
        if (req.contains("性能") || req.contains("performance"))
            return "performance_logs";
        if (req.contains("日志") || req.contains("log"))
            return "mixed_logs";
        return "general_data";
    }

    private int extractRecordCount(String req) {
        Pattern pattern = Pattern.compile("(\\d+)\\s*(条|个|行)");
        Matcher matcher = pattern.matcher(req);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1000; // 默认值
    }

    private String inferFileFormat(String req) {
        if (req.contains("json"))
            return "json";
        if (req.contains("csv"))
            return "csv";
        if (req.contains("xml"))
            return "xml";
        return "json";
    }

    private String extractProcessingDescription(String req, String type) {
        switch (type) {
            case "filter":
                return "过滤数据";
            case "transform":
                return "转换数据格式";
            case "validate":
                return "验证数据完整性";
            case "calculate":
                return "执行计算和统计";
            default:
                return "处理数据: " + type;
        }
    }

    private String extractProcessingLogic(String req, String type) {
        // 根据需求提取具体的处理逻辑描述
        return String.format("实现%s逻辑，基于需求: %s", type, req);
    }

    private List<ProcessingStepRequirement> generateDefaultProcessingSteps(String req) {
        List<ProcessingStepRequirement> steps = new ArrayList<>();

        // 根据需求类型生成默认步骤
        if (req.contains("分析") || req.contains("analysis")) {
            ProcessingStepRequirement analysis = new ProcessingStepRequirement();
            analysis.setType("calculate");
            analysis.setDescription("数据分析处理");
            analysis.setOrder(1);
            analysis.setRequired(true);
            analysis.setLogic("执行数据分析和统计计算");
            steps.add(analysis);
        }

        return steps;
    }

    private void sortProcessingSteps(List<ProcessingStepRequirement> steps) {
        // 定义处理步骤的逻辑顺序
        Map<String, Integer> orderMap = Map.of(
                "validate", 1,
                "filter", 2,
                "transform", 3,
                "deduplicate", 4,
                "sort", 5,
                "group", 6,
                "calculate", 7);

        steps.sort((a, b) -> {
            int orderA = orderMap.getOrDefault(a.getType(), 999);
            int orderB = orderMap.getOrDefault(b.getType(), 999);
            return Integer.compare(orderA, orderB);
        });

        // 重新设置order
        for (int i = 0; i < steps.size(); i++) {
            steps.get(i).setOrder(i + 1);
        }
    }

    private String extractOutputDescription(String req, String type) {
        switch (type) {
            case "file":
                return "保存结果到文件";
            case "database":
                return "存储结果到数据库";
            case "console":
                return "在控制台显示结果";
            case "dashboard":
                return "在仪表盘展示结果";
            default:
                return "输出结果: " + type;
        }
    }

    private String inferOutputFormat(String req, String type) {
        if (type.equals("file")) {
            if (req.contains("json"))
                return "json";
            if (req.contains("csv"))
                return "csv";
            if (req.contains("xml"))
                return "xml";
            return "json";
        }
        return "text";
    }

    private String generateWorkflowName(RequirementAnalysisResult result) {
        String domain = result.getDomain();
        String type = result.getWorkflowType();

        return String.format("%s_%s_workflow", domain, type);
    }

    private String generateWorkflowDescription(RequirementAnalysisResult result) {
        return String.format("基于需求自动生成的%s工作流，包含%d个数据源，%d个处理步骤，%d个输出目标",
                result.getWorkflowType(),
                result.getDataSources().size(),
                result.getProcessingSteps().size(),
                result.getOutputRequirements().size());
    }

    // 数据结构类定义

    public static class RequirementAnalysisResult {
        private String originalRequirement;
        private String workflowType;
        private String domain;
        private String complexity;
        private List<DataSourceRequirement> dataSources = new ArrayList<>();
        private List<ProcessingStepRequirement> processingSteps = new ArrayList<>();
        private List<OutputRequirement> outputRequirements = new ArrayList<>();
        private List<String> businessRules = new ArrayList<>();
        private Map<String, Object> parameters = new HashMap<>();
        private String suggestedName;
        private String suggestedDescription;

        // Getters and Setters
        public String getOriginalRequirement() {
            return originalRequirement;
        }

        public void setOriginalRequirement(String originalRequirement) {
            this.originalRequirement = originalRequirement;
        }

        public String getWorkflowType() {
            return workflowType;
        }

        public void setWorkflowType(String workflowType) {
            this.workflowType = workflowType;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getComplexity() {
            return complexity;
        }

        public void setComplexity(String complexity) {
            this.complexity = complexity;
        }

        public List<DataSourceRequirement> getDataSources() {
            return dataSources;
        }

        public void setDataSources(List<DataSourceRequirement> dataSources) {
            this.dataSources = dataSources;
        }

        public List<ProcessingStepRequirement> getProcessingSteps() {
            return processingSteps;
        }

        public void setProcessingSteps(List<ProcessingStepRequirement> processingSteps) {
            this.processingSteps = processingSteps;
        }

        public List<OutputRequirement> getOutputRequirements() {
            return outputRequirements;
        }

        public void setOutputRequirements(List<OutputRequirement> outputRequirements) {
            this.outputRequirements = outputRequirements;
        }

        public List<String> getBusinessRules() {
            return businessRules;
        }

        public void setBusinessRules(List<String> businessRules) {
            this.businessRules = businessRules;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        public String getSuggestedName() {
            return suggestedName;
        }

        public void setSuggestedName(String suggestedName) {
            this.suggestedName = suggestedName;
        }

        public String getSuggestedDescription() {
            return suggestedDescription;
        }

        public void setSuggestedDescription(String suggestedDescription) {
            this.suggestedDescription = suggestedDescription;
        }
    }

    public static class DataSourceRequirement {
        private String type;
        private String description;
        private boolean required;
        private Map<String, Object> parameters = new HashMap<>();

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }

    public static class ProcessingStepRequirement {
        private String type;
        private String description;
        private int order;
        private boolean required;
        private String logic;

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getLogic() {
            return logic;
        }

        public void setLogic(String logic) {
            this.logic = logic;
        }
    }

    public static class OutputRequirement {
        private String type;
        private String description;
        private boolean required;
        private String format;

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }
}
