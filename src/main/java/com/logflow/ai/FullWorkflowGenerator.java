package com.logflow.ai;

import com.logflow.ai.WorkflowDesigner.*;
import com.logflow.ai.WorkflowRequirementAnalyzer.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 完整工作流生成器
 * 根据用户一句话需求，生成完整的LogFlow工作流配置，包括所有脚本逻辑
 */
public class FullWorkflowGenerator {

    private static final Logger logger = LoggerFactory.getLogger(FullWorkflowGenerator.class);
    private static final String WORKFLOW_PROMPT_TEMPLATE_PATH = "/prompts/full-workflow-generation-prompt.md";

    private final WorkflowRequirementAnalyzer requirementAnalyzer;
    private final WorkflowDesigner workflowDesigner;
    private final LLMScriptGenerator scriptGenerator;
    private final ObjectMapper yamlMapper;
    private String workflowPromptTemplate;

    public FullWorkflowGenerator(LLMScriptGenerator.LLMProvider llmProvider) {
        this.requirementAnalyzer = new WorkflowRequirementAnalyzer();
        this.workflowDesigner = new WorkflowDesigner();
        this.scriptGenerator = new LLMScriptGenerator(llmProvider);

        // 配置YAML映射器
        this.yamlMapper = new ObjectMapper(new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS));

        loadWorkflowPromptTemplate();
    }

    /**
     * 根据用户一句话需求生成完整工作流
     * 
     * @param userRequirement   用户的一句话需求
     * @param additionalContext 额外上下文（可选）
     * @return 完整工作流生成结果
     */
    public FullWorkflowGenerationResult generateFullWorkflow(String userRequirement,
            Map<String, Object> additionalContext) {
        logger.info("开始生成完整工作流，用户需求: {}", userRequirement);

        long startTime = System.currentTimeMillis();
        FullWorkflowGenerationResult result = new FullWorkflowGenerationResult();
        result.setUserRequirement(userRequirement);
        result.setAdditionalContext(additionalContext);

        try {
            // 1. 分析用户需求
            logger.info("步骤1: 分析用户需求");
            RequirementAnalysisResult requirementResult = requirementAnalyzer.analyzeRequirement(userRequirement);
            result.setRequirementAnalysis(requirementResult);

            // 2. 设计工作流结构
            logger.info("步骤2: 设计工作流结构");
            WorkflowDesignResult designResult = workflowDesigner.designWorkflow(requirementResult);
            result.setWorkflowDesign(designResult);

            // 3. 生成脚本节点的JavaScript代码
            logger.info("步骤3: 生成脚本代码");
            Map<String, ScriptGenerationInfo> generatedScripts = generateScriptsForNodes(
                    designResult, userRequirement, additionalContext);
            result.setGeneratedScripts(generatedScripts);

            // 4. 构建完整的YAML配置
            logger.info("步骤4: 构建YAML配置");
            String yamlConfig = buildYamlConfiguration(designResult, generatedScripts);
            result.setYamlConfiguration(yamlConfig);

            // 5. 生成元数据和统计信息
            long generationTime = System.currentTimeMillis() - startTime;
            result.setGenerationMetadata(buildGenerationMetadata(designResult, generatedScripts, generationTime));

            result.setSuccess(true);
            logger.info("完整工作流生成成功，耗时: {}ms", generationTime);

        } catch (Exception e) {
            logger.error("完整工作流生成失败", e);
            result.setSuccess(false);
            result.setErrorMessage("工作流生成失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 为脚本节点生成JavaScript代码
     */
    private Map<String, ScriptGenerationInfo> generateScriptsForNodes(WorkflowDesignResult design,
            String userRequirement,
            Map<String, Object> additionalContext) {
        Map<String, ScriptGenerationInfo> generatedScripts = new HashMap<>();

        // 找到所有脚本节点
        List<NodeDesign> scriptNodes = design.getNodes().stream()
                .filter(node -> "script".equals(node.getType()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        for (NodeDesign scriptNode : scriptNodes) {
            try {
                logger.debug("生成脚本节点代码: {}", scriptNode.getId());

                // 构建针对该节点的脚本生成需求
                String scriptRequirement = buildScriptRequirement(scriptNode, userRequirement, design);

                // 构建节点特定的上下文
                Map<String, Object> nodeContext = buildNodeContext(scriptNode, design, additionalContext);

                // 生成脚本代码
                String scriptCode = generateScriptCode(scriptNode, scriptRequirement, nodeContext);

                // 创建脚本信息
                ScriptGenerationInfo scriptInfo = new ScriptGenerationInfo();
                scriptInfo.setNodeId(scriptNode.getId());
                scriptInfo.setNodeName(scriptNode.getName());
                scriptInfo.setScriptType((String) scriptNode.getConfig().get("processingType"));
                scriptInfo.setRequirement(scriptRequirement);
                scriptInfo.setGeneratedScript(scriptCode);
                scriptInfo.setExpectedInput(determineExpectedInput(scriptNode, design));
                scriptInfo.setExpectedOutput(determineExpectedOutput(scriptNode, design));

                generatedScripts.put(scriptNode.getId(), scriptInfo);

            } catch (Exception e) {
                logger.error("生成脚本节点 {} 的代码失败", scriptNode.getId(), e);

                // 创建失败信息
                ScriptGenerationInfo errorInfo = new ScriptGenerationInfo();
                errorInfo.setNodeId(scriptNode.getId());
                errorInfo.setNodeName(scriptNode.getName());
                errorInfo.setGeneratedScript("// 脚本生成失败: " + e.getMessage());
                errorInfo.setError(true);
                errorInfo.setErrorMessage(e.getMessage());

                generatedScripts.put(scriptNode.getId(), errorInfo);
            }
        }

        return generatedScripts;
    }

    /**
     * 构建脚本生成需求
     */
    private String buildScriptRequirement(NodeDesign scriptNode, String userRequirement, WorkflowDesignResult design) {
        String processingType = (String) scriptNode.getConfig().get("processingType");
        String requirementLogic = (String) scriptNode.getConfig().get("requirementLogic");

        StringBuilder requirement = new StringBuilder();

        // 基础需求描述
        requirement.append(String.format("为%s节点生成JavaScript脚本。", scriptNode.getName()));
        requirement.append("\n\n");

        // 处理类型特定需求
        switch (processingType) {
            case "filter":
                requirement.append("实现数据过滤功能：");
                requirement.append("\n- 根据指定条件过滤输入数据");
                requirement.append("\n- 统计过滤前后的数量");
                requirement.append("\n- 保存过滤统计信息到上下文");
                break;

            case "transform":
                requirement.append("实现数据转换功能：");
                requirement.append("\n- 转换输入数据的格式或结构");
                requirement.append("\n- 添加处理时间戳等元数据");
                requirement.append("\n- 确保输出数据格式符合后续节点要求");
                break;

            case "calculate":
                requirement.append("实现数据分析计算功能：");
                requirement.append("\n- 对输入数据进行统计分析");
                requirement.append("\n- 计算关键指标和趋势");
                requirement.append("\n- 生成分析报告和建议");
                break;

            case "aggregation":
                requirement.append("实现结果聚合功能：");
                requirement.append("\n- 收集和整合多个处理步骤的结果");
                requirement.append("\n- 生成综合分析报告");
                requirement.append("\n- 提供统一的输出格式");
                break;

            default:
                requirement.append("实现自定义处理功能：");
                requirement.append("\n- 根据业务需求处理数据");
        }

        requirement.append("\n\n");

        // 添加原始需求上下文
        requirement.append("原始用户需求: ").append(userRequirement);
        requirement.append("\n\n");

        // 添加特定逻辑要求
        if (requirementLogic != null && !requirementLogic.isEmpty()) {
            requirement.append("特定逻辑要求: ").append(requirementLogic);
        }

        return requirement.toString();
    }

    /**
     * 构建节点特定上下文
     */
    private Map<String, Object> buildNodeContext(NodeDesign scriptNode, WorkflowDesignResult design,
            Map<String, Object> additionalContext) {
        Map<String, Object> nodeContext = new HashMap<>();

        // 添加节点配置信息
        nodeContext.put("nodeConfig", scriptNode.getConfig());

        // 添加工作流全局配置
        nodeContext.put("workflowConfig", design.getGlobalConfig());

        // 添加输入输出键信息
        nodeContext.put("inputKey", scriptNode.getConfig().get("inputKey"));
        nodeContext.put("outputKey", scriptNode.getConfig().get("outputKey"));

        // 添加节点在工作流中的位置信息
        nodeContext.put("nodePosition", determineNodePosition(scriptNode, design));

        // 添加额外上下文
        if (additionalContext != null) {
            nodeContext.putAll(additionalContext);
        }

        return nodeContext;
    }

    /**
     * 生成脚本代码
     */
    private String generateScriptCode(NodeDesign scriptNode, String requirement, Map<String, Object> nodeContext) {
        String processingType = (String) scriptNode.getConfig().get("processingType");

        // 使用预定义模板生成脚本
        switch (processingType) {
            case "filter":
                return generateFilterScript(scriptNode, requirement, nodeContext);
            case "transform":
                return generateTransformScript(scriptNode, requirement, nodeContext);
            case "calculate":
                return generateAnalysisScript(scriptNode, requirement, nodeContext);
            case "aggregation":
                return generateAggregationScript(scriptNode, requirement, nodeContext);
            default:
                return generateGenericScript(scriptNode, requirement, nodeContext);
        }
    }

    /**
     * 生成数据过滤脚本
     */
    private String generateFilterScript(NodeDesign scriptNode, String requirement, Map<String, Object> nodeContext) {
        String inputKey = (String) scriptNode.getConfig().get("inputKey");
        String outputKey = (String) scriptNode.getConfig().get("outputKey");

        return String.format(
                "// %s - 数据过滤脚本\n" +
                        "var config = context.get('config') || {};\n" +
                        "var data = input;\n" +
                        "var filtered = [];\n" +
                        "\n" +
                        "// 输入验证\n" +
                        "if (!data || !Array.isArray(data)) {\n" +
                        "  logger.warn('输入数据无效或为空');\n" +
                        "  return [];\n" +
                        "}\n" +
                        "\n" +
                        "logger.info('开始过滤数据，共 ' + data.length + ' 条记录');\n" +
                        "\n" +
                        "// 过滤逻辑\n" +
                        "for (var i = 0; i < data.length; i++) {\n" +
                        "  var item = data[i];\n" +
                        "  \n" +
                        "  // 过滤条件（根据需求调整）\n" +
                        "  if (item && item.level && item.level !== 'DEBUG') {\n" +
                        "    filtered.push(item);\n" +
                        "  }\n" +
                        "}\n" +
                        "\n" +
                        "// 保存统计信息\n" +
                        "var stats = {\n" +
                        "  totalInput: data.length,\n" +
                        "  totalOutput: filtered.length,\n" +
                        "  filterRate: ((data.length - filtered.length) / data.length * 100).toFixed(2),\n" +
                        "  processedAt: utils.now()\n" +
                        "};\n" +
                        "context.set('%s_stats', stats);\n" +
                        "\n" +
                        "logger.info('过滤完成: 输入 ' + data.length + ' 条，输出 ' + filtered.length + ' 条');\n" +
                        "filtered;",
                scriptNode.getName(), outputKey);
    }

    /**
     * 生成数据转换脚本
     */
    private String generateTransformScript(NodeDesign scriptNode, String requirement, Map<String, Object> nodeContext) {
        String inputKey = (String) scriptNode.getConfig().get("inputKey");
        String outputKey = (String) scriptNode.getConfig().get("outputKey");

        return String.format(
                "// %s - 数据转换脚本\n" +
                        "var data = input;\n" +
                        "var transformed = [];\n" +
                        "\n" +
                        "// 输入验证\n" +
                        "if (!data || !Array.isArray(data)) {\n" +
                        "  logger.warn('输入数据无效');\n" +
                        "  return [];\n" +
                        "}\n" +
                        "\n" +
                        "logger.info('开始转换数据，共 ' + data.length + ' 条记录');\n" +
                        "\n" +
                        "// 转换逻辑\n" +
                        "for (var i = 0; i < data.length; i++) {\n" +
                        "  var item = data[i];\n" +
                        "  \n" +
                        "  // 创建转换后的数据项\n" +
                        "  var newItem = {\n" +
                        "    id: item.id,\n" +
                        "    level: item.level,\n" +
                        "    message: item.message,\n" +
                        "    timestamp: item.timestamp,\n" +
                        "    // 添加处理元数据\n" +
                        "    processedAt: utils.now(),\n" +
                        "    processedBy: '%s',\n" +
                        "    index: i + 1\n" +
                        "  };\n" +
                        "  \n" +
                        "  // 添加时间解析信息\n" +
                        "  if (item.timestamp) {\n" +
                        "    newItem.parsedTime = new Date(item.timestamp);\n" +
                        "    newItem.hour = newItem.parsedTime.getHours();\n" +
                        "  }\n" +
                        "  \n" +
                        "  transformed.push(newItem);\n" +
                        "}\n" +
                        "\n" +
                        "// 保存处理统计\n" +
                        "context.set('%s_stats', {\n" +
                        "  processed: transformed.length,\n" +
                        "  processedAt: utils.now()\n" +
                        "});\n" +
                        "\n" +
                        "logger.info('转换完成: 处理了 ' + transformed.length + ' 条记录');\n" +
                        "transformed;",
                scriptNode.getName(), scriptNode.getId(), outputKey);
    }

    /**
     * 生成数据分析脚本
     */
    private String generateAnalysisScript(NodeDesign scriptNode, String requirement, Map<String, Object> nodeContext) {
        String outputKey = (String) scriptNode.getConfig().get("outputKey");

        return String.format(
                "// %s - 数据分析脚本\n" +
                        "var data = input;\n" +
                        "var analysis = {\n" +
                        "  summary: {\n" +
                        "    totalRecords: data.length,\n" +
                        "    processedAt: utils.now(),\n" +
                        "    analysisType: 'comprehensive'\n" +
                        "  },\n" +
                        "  metrics: {},\n" +
                        "  issues: [],\n" +
                        "  recommendations: []\n" +
                        "};\n" +
                        "\n" +
                        "// 输入验证\n" +
                        "if (!data || !Array.isArray(data)) {\n" +
                        "  logger.warn('输入数据无效');\n" +
                        "  return analysis;\n" +
                        "}\n" +
                        "\n" +
                        "logger.info('开始分析数据，共 ' + data.length + ' 条记录');\n" +
                        "\n" +
                        "// 分析逻辑\n" +
                        "var levelCounts = {};\n" +
                        "var timeDistribution = {};\n" +
                        "var issueCount = 0;\n" +
                        "\n" +
                        "for (var i = 0; i < data.length; i++) {\n" +
                        "  var item = data[i];\n" +
                        "  \n" +
                        "  // 统计级别分布\n" +
                        "  if (item.level) {\n" +
                        "    levelCounts[item.level] = (levelCounts[item.level] || 0) + 1;\n" +
                        "    \n" +
                        "    // 检测问题\n" +
                        "    if (item.level === 'ERROR' || item.level === 'FATAL') {\n" +
                        "      issueCount++;\n" +
                        "      analysis.issues.push({\n" +
                        "        type: 'error',\n" +
                        "        level: item.level,\n" +
                        "        message: item.message,\n" +
                        "        timestamp: item.timestamp\n" +
                        "      });\n" +
                        "    }\n" +
                        "  }\n" +
                        "  \n" +
                        "  // 时间分布统计\n" +
                        "  if (item.timestamp) {\n" +
                        "    var hour = new Date(item.timestamp).getHours();\n" +
                        "    timeDistribution[hour] = (timeDistribution[hour] || 0) + 1;\n" +
                        "  }\n" +
                        "}\n" +
                        "\n" +
                        "// 设置分析结果\n" +
                        "analysis.metrics.levelDistribution = levelCounts;\n" +
                        "analysis.metrics.timeDistribution = timeDistribution;\n" +
                        "analysis.summary.issueCount = issueCount;\n" +
                        "analysis.summary.errorRate = (issueCount / data.length * 100).toFixed(2);\n" +
                        "\n" +
                        "// 生成建议\n" +
                        "if (issueCount > 10) {\n" +
                        "  analysis.recommendations.push('发现大量错误(' + issueCount + '个)，建议立即检查系统状态');\n" +
                        "} else if (issueCount > 0) {\n" +
                        "  analysis.recommendations.push('发现 ' + issueCount + ' 个错误，需要关注');\n" +
                        "} else {\n" +
                        "  analysis.recommendations.push('系统运行正常，未发现明显问题');\n" +
                        "}\n" +
                        "\n" +
                        "logger.info('分析完成: 发现 ' + issueCount + ' 个问题，错误率 ' + analysis.summary.errorRate + '%%');\n" +
                        "analysis;",
                scriptNode.getName());
    }

    /**
     * 生成结果聚合脚本
     */
    private String generateAggregationScript(NodeDesign scriptNode, String requirement,
            Map<String, Object> nodeContext) {
        return String.format(
                "// %s - 结果聚合脚本\n" +
                        "var config = context.get('config') || {};\n" +
                        "\n" +
                        "// 收集所有处理结果\n" +
                        "var filterResult = context.get('filter_result');\n" +
                        "var transformResult = context.get('transform_result');\n" +
                        "var analysisResult = context.get('analysis_result');\n" +
                        "\n" +
                        "// 生成综合报告\n" +
                        "var report = {\n" +
                        "  metadata: {\n" +
                        "    generatedAt: utils.now(),\n" +
                        "    workflowId: context.getWorkflowId(),\n" +
                        "    executionId: context.getExecutionId(),\n" +
                        "    version: '1.0'\n" +
                        "  },\n" +
                        "  summary: {\n" +
                        "    totalProcessed: 0,\n" +
                        "    issuesFound: 0,\n" +
                        "    overallStatus: 'SUCCESS',\n" +
                        "    processingSteps: []\n" +
                        "  },\n" +
                        "  details: {},\n" +
                        "  recommendations: []\n" +
                        "};\n" +
                        "\n" +
                        "logger.info('开始聚合处理结果');\n" +
                        "\n" +
                        "// 聚合过滤结果\n" +
                        "if (filterResult) {\n" +
                        "  report.summary.totalProcessed += filterResult.length || 0;\n" +
                        "  report.summary.processingSteps.push('数据过滤');\n" +
                        "  report.details.filtering = {\n" +
                        "    recordCount: filterResult.length,\n" +
                        "    status: 'completed'\n" +
                        "  };\n" +
                        "}\n" +
                        "\n" +
                        "// 聚合转换结果\n" +
                        "if (transformResult) {\n" +
                        "  report.summary.processingSteps.push('数据转换');\n" +
                        "  report.details.transformation = {\n" +
                        "    recordCount: transformResult.length,\n" +
                        "    status: 'completed'\n" +
                        "  };\n" +
                        "}\n" +
                        "\n" +
                        "// 聚合分析结果\n" +
                        "if (analysisResult && analysisResult.summary) {\n" +
                        "  report.summary.issuesFound = analysisResult.summary.issueCount || 0;\n" +
                        "  report.summary.processingSteps.push('数据分析');\n" +
                        "  report.details.analysis = analysisResult;\n" +
                        "  \n" +
                        "  // 合并建议\n" +
                        "  if (analysisResult.recommendations) {\n" +
                        "    report.recommendations = report.recommendations.concat(analysisResult.recommendations);\n"
                        +
                        "  }\n" +
                        "}\n" +
                        "\n" +
                        "// 确定整体状态\n" +
                        "if (report.summary.issuesFound > 10) {\n" +
                        "  report.summary.overallStatus = 'WARNING';\n" +
                        "} else if (report.summary.issuesFound > 0) {\n" +
                        "  report.summary.overallStatus = 'INFO';\n" +
                        "}\n" +
                        "\n" +
                        "// 添加处理统计\n" +
                        "report.summary.stepsCompleted = report.summary.processingSteps.length;\n" +
                        "\n" +
                        "logger.info('聚合完成: 处理了 ' + report.summary.stepsCompleted + ' 个步骤，发现 ' + report.summary.issuesFound + ' 个问题');\n"
                        +
                        "report;",
                scriptNode.getName());
    }

    /**
     * 生成通用脚本
     */
    private String generateGenericScript(NodeDesign scriptNode, String requirement, Map<String, Object> nodeContext) {
        String outputKey = (String) scriptNode.getConfig().get("outputKey");

        return String.format(
                "// %s - 通用处理脚本\n" +
                        "var data = input;\n" +
                        "var result = {\n" +
                        "  processedAt: utils.now(),\n" +
                        "  nodeId: '%s',\n" +
                        "  success: true,\n" +
                        "  data: data\n" +
                        "};\n" +
                        "\n" +
                        "// 输入验证\n" +
                        "if (!data) {\n" +
                        "  logger.warn('输入数据为空');\n" +
                        "  result.success = false;\n" +
                        "  return result;\n" +
                        "}\n" +
                        "\n" +
                        "logger.info('开始处理数据');\n" +
                        "\n" +
                        "// 处理逻辑（根据具体需求实现）\n" +
                        "try {\n" +
                        "  // 在这里添加具体的处理逻辑\n" +
                        "  result.processedCount = Array.isArray(data) ? data.length : 1;\n" +
                        "  \n" +
                        "  logger.info('处理完成');\n" +
                        "} catch (error) {\n" +
                        "  logger.error('处理失败: ' + error.message);\n" +
                        "  result.success = false;\n" +
                        "  result.error = error.message;\n" +
                        "}\n" +
                        "\n" +
                        "result;",
                scriptNode.getName(), scriptNode.getId());
    }

    /**
     * 构建YAML配置
     */
    private String buildYamlConfiguration(WorkflowDesignResult design,
            Map<String, ScriptGenerationInfo> generatedScripts) {
        try {
            Map<String, Object> yamlConfig = new LinkedHashMap<>();

            // 工作流基本信息
            yamlConfig.put("id", design.getWorkflowId());
            yamlConfig.put("name", design.getWorkflowName());
            yamlConfig.put("description", design.getDescription());
            yamlConfig.put("version", "1.0");

            // 全局配置
            if (!design.getGlobalConfig().isEmpty()) {
                yamlConfig.put("global", design.getGlobalConfig());
            }

            // 节点配置
            List<Map<String, Object>> nodes = new ArrayList<>();
            for (NodeDesign nodeDesign : design.getNodes()) {
                Map<String, Object> nodeConfig = buildNodeYamlConfig(nodeDesign, generatedScripts);
                nodes.add(nodeConfig);
            }
            yamlConfig.put("nodes", nodes);

            // 连接配置
            List<Map<String, Object>> connections = new ArrayList<>();
            for (ConnectionDesign connection : design.getConnections()) {
                Map<String, Object> connConfig = new LinkedHashMap<>();
                connConfig.put("from", connection.getFromNodeId());
                connConfig.put("to", connection.getToNodeId());
                connections.add(connConfig);
            }
            yamlConfig.put("connections", connections);

            // 添加Schema引用
            String yamlContent = "# yaml-language-server: $schema=../schemas/logflow-workflow-schema.json\n\n";
            yamlContent += yamlMapper.writeValueAsString(yamlConfig);

            return yamlContent;

        } catch (Exception e) {
            logger.error("构建YAML配置失败", e);
            return "# YAML配置生成失败: " + e.getMessage();
        }
    }

    /**
     * 构建节点YAML配置
     */
    private Map<String, Object> buildNodeYamlConfig(NodeDesign nodeDesign,
            Map<String, ScriptGenerationInfo> generatedScripts) {
        Map<String, Object> nodeConfig = new LinkedHashMap<>();

        nodeConfig.put("id", nodeDesign.getId());
        nodeConfig.put("name", nodeDesign.getName());
        nodeConfig.put("type", nodeDesign.getType());
        nodeConfig.put("enabled", nodeDesign.isEnabled());

        // 位置信息
        if (nodeDesign.getPosition() != null) {
            Map<String, Object> position = new LinkedHashMap<>();
            position.put("x", nodeDesign.getPosition().getX());
            position.put("y", nodeDesign.getPosition().getY());
            nodeConfig.put("position", position);
        }

        // 节点配置
        Map<String, Object> config = new LinkedHashMap<>(nodeDesign.getConfig());

        // 如果是脚本节点，添加生成的脚本代码
        if ("script".equals(nodeDesign.getType())) {
            ScriptGenerationInfo scriptInfo = generatedScripts.get(nodeDesign.getId());
            if (scriptInfo != null && !scriptInfo.isError()) {
                config.put("script", scriptInfo.getGeneratedScript());
            }
        }

        nodeConfig.put("config", config);

        return nodeConfig;
    }

    /**
     * 构建生成元数据
     */
    private Map<String, Object> buildGenerationMetadata(WorkflowDesignResult design,
            Map<String, ScriptGenerationInfo> generatedScripts,
            long generationTime) {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("generationTime", generationTime);
        metadata.put("nodeCount", design.getNodes().size());
        metadata.put("connectionCount", design.getConnections().size());
        metadata.put("scriptNodeCount", generatedScripts.size());
        metadata.put("successfulScripts", generatedScripts.values().stream()
                .mapToLong(script -> script.isError() ? 0 : 1).sum());
        metadata.put("failedScripts", generatedScripts.values().stream()
                .mapToLong(script -> script.isError() ? 1 : 0).sum());

        return metadata;
    }

    // 辅助方法

    private void loadWorkflowPromptTemplate() {
        try (InputStream is = getClass().getResourceAsStream(WORKFLOW_PROMPT_TEMPLATE_PATH)) {
            if (is != null) {
                workflowPromptTemplate = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                logger.info("加载工作流提示模板成功，长度: {}", workflowPromptTemplate.length());
            } else {
                logger.warn("工作流提示模板文件不存在: {}", WORKFLOW_PROMPT_TEMPLATE_PATH);
                workflowPromptTemplate = "# 请根据需求生成LogFlow工作流配置\n\n";
            }
        } catch (IOException e) {
            logger.error("加载工作流提示模板失败", e);
            workflowPromptTemplate = "# 请根据需求生成LogFlow工作流配置\n\n";
        }
    }

    private String determineNodePosition(NodeDesign scriptNode, WorkflowDesignResult design) {
        List<NodeDesign> nodes = design.getNodes();
        int index = nodes.indexOf(scriptNode);

        if (index == 0)
            return "first";
        if (index == nodes.size() - 1)
            return "last";
        return "middle";
    }

    private String determineExpectedInput(NodeDesign scriptNode, WorkflowDesignResult design) {
        String inputKey = (String) scriptNode.getConfig().get("inputKey");
        String processingType = (String) scriptNode.getConfig().get("processingType");

        switch (processingType) {
            case "filter":
            case "transform":
                return "数组格式的日志数据";
            case "calculate":
                return "已处理的结构化数据";
            case "aggregation":
                return "多个处理步骤的结果";
            default:
                return "任意格式数据";
        }
    }

    private String determineExpectedOutput(NodeDesign scriptNode, WorkflowDesignResult design) {
        String processingType = (String) scriptNode.getConfig().get("processingType");

        switch (processingType) {
            case "filter":
                return "过滤后的数据数组";
            case "transform":
                return "转换后的数据数组";
            case "calculate":
                return "分析结果对象";
            case "aggregation":
                return "综合报告对象";
            default:
                return "处理结果";
        }
    }

    // 数据结构类

    public static class FullWorkflowGenerationResult {
        private boolean success;
        private String userRequirement;
        private Map<String, Object> additionalContext;
        private RequirementAnalysisResult requirementAnalysis;
        private WorkflowDesignResult workflowDesign;
        private Map<String, ScriptGenerationInfo> generatedScripts = new HashMap<>();
        private String yamlConfiguration;
        private Map<String, Object> generationMetadata = new HashMap<>();
        private String errorMessage;

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getUserRequirement() {
            return userRequirement;
        }

        public void setUserRequirement(String userRequirement) {
            this.userRequirement = userRequirement;
        }

        public Map<String, Object> getAdditionalContext() {
            return additionalContext;
        }

        public void setAdditionalContext(Map<String, Object> additionalContext) {
            this.additionalContext = additionalContext;
        }

        public RequirementAnalysisResult getRequirementAnalysis() {
            return requirementAnalysis;
        }

        public void setRequirementAnalysis(RequirementAnalysisResult requirementAnalysis) {
            this.requirementAnalysis = requirementAnalysis;
        }

        public WorkflowDesignResult getWorkflowDesign() {
            return workflowDesign;
        }

        public void setWorkflowDesign(WorkflowDesignResult workflowDesign) {
            this.workflowDesign = workflowDesign;
        }

        public Map<String, ScriptGenerationInfo> getGeneratedScripts() {
            return generatedScripts;
        }

        public void setGeneratedScripts(Map<String, ScriptGenerationInfo> generatedScripts) {
            this.generatedScripts = generatedScripts;
        }

        public String getYamlConfiguration() {
            return yamlConfiguration;
        }

        public void setYamlConfiguration(String yamlConfiguration) {
            this.yamlConfiguration = yamlConfiguration;
        }

        public Map<String, Object> getGenerationMetadata() {
            return generationMetadata;
        }

        public void setGenerationMetadata(Map<String, Object> generationMetadata) {
            this.generationMetadata = generationMetadata;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    public static class ScriptGenerationInfo {
        private String nodeId;
        private String nodeName;
        private String scriptType;
        private String requirement;
        private String generatedScript;
        private String expectedInput;
        private String expectedOutput;
        private boolean error;
        private String errorMessage;

        // Getters and Setters
        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public String getScriptType() {
            return scriptType;
        }

        public void setScriptType(String scriptType) {
            this.scriptType = scriptType;
        }

        public String getRequirement() {
            return requirement;
        }

        public void setRequirement(String requirement) {
            this.requirement = requirement;
        }

        public String getGeneratedScript() {
            return generatedScript;
        }

        public void setGeneratedScript(String generatedScript) {
            this.generatedScript = generatedScript;
        }

        public String getExpectedInput() {
            return expectedInput;
        }

        public void setExpectedInput(String expectedInput) {
            this.expectedInput = expectedInput;
        }

        public String getExpectedOutput() {
            return expectedOutput;
        }

        public void setExpectedOutput(String expectedOutput) {
            this.expectedOutput = expectedOutput;
        }

        public boolean isError() {
            return error;
        }

        public void setError(boolean error) {
            this.error = error;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
