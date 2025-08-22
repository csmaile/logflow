package com.logflow.ai;

import com.logflow.core.WorkflowNode;
import com.logflow.engine.Workflow;
import com.logflow.nodes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流上下文分析器
 * 分析当前工作流环境，为LLM提供准确的上下文信息
 */
public class WorkflowContextAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowContextAnalyzer.class);

    /**
     * 分析脚本节点的上下文环境
     * 
     * @param workflow     工作流对象
     * @param scriptNodeId 脚本节点ID
     * @return 上下文分析结果
     */
    public ContextAnalysisResult analyzeScriptContext(Workflow workflow, String scriptNodeId) {
        logger.info("分析脚本节点上下文: {}", scriptNodeId);

        ContextAnalysisResult result = new ContextAnalysisResult();
        result.setNodeId(scriptNodeId);
        result.setWorkflowId(workflow.getId());
        result.setWorkflowName(workflow.getName());

        // 获取脚本节点
        WorkflowNode scriptNode = workflow.getNode(scriptNodeId);
        if (scriptNode == null) {
            throw new IllegalArgumentException("脚本节点不存在: " + scriptNodeId);
        }

        // 分析输入来源
        analyzeInputSources(workflow, scriptNodeId, result);

        // 分析上下文数据
        analyzeContextData(workflow, scriptNodeId, result);

        // 分析输出目标
        analyzeOutputTargets(workflow, scriptNodeId, result);

        // 分析节点配置
        analyzeNodeConfiguration(scriptNode, result);

        // 生成数据流描述
        generateDataFlowDescription(result);

        logger.info("上下文分析完成: 输入源={}, 上下文键={}, 输出目标={}",
                result.getInputSources().size(),
                result.getContextKeys().size(),
                result.getOutputTargets().size());

        return result;
    }

    /**
     * 分析输入数据来源
     */
    private void analyzeInputSources(Workflow workflow, String scriptNodeId, ContextAnalysisResult result) {
        Set<String> predecessors = workflow.getSourceNodes(scriptNodeId);
        List<InputSourceInfo> inputSources = new ArrayList<>();

        for (String predId : predecessors) {
            WorkflowNode predNode = workflow.getNode(predId);
            if (predNode != null) {
                InputSourceInfo sourceInfo = new InputSourceInfo();
                sourceInfo.setNodeId(predId);
                sourceInfo.setNodeName(predNode.getName());
                sourceInfo.setNodeType(predNode.getType().name());

                // 根据节点类型推断输出数据格式
                sourceInfo.setExpectedDataType(inferDataType(predNode));
                sourceInfo.setDescription(generateNodeDescription(predNode));

                inputSources.add(sourceInfo);
            }
        }

        result.setInputSources(inputSources);
    }

    /**
     * 分析可用的上下文数据
     */
    private void analyzeContextData(Workflow workflow, String scriptNodeId, ContextAnalysisResult result) {
        Set<String> contextKeys = new HashSet<>();
        List<ContextDataInfo> contextDataList = new ArrayList<>();

        // 遍历所有前置节点，分析可能存储到上下文的数据
        Set<String> allPredecessors = getAllPredecessors(workflow, scriptNodeId);

        for (String nodeId : allPredecessors) {
            WorkflowNode node = workflow.getNode(nodeId);
            if (node != null) {
                // 分析节点可能产生的上下文数据
                List<String> nodeContextKeys = inferContextKeys(node);
                for (String key : nodeContextKeys) {
                    if (!contextKeys.contains(key)) {
                        contextKeys.add(key);

                        ContextDataInfo contextInfo = new ContextDataInfo();
                        contextInfo.setKey(key);
                        contextInfo.setProducerNodeId(nodeId);
                        contextInfo.setProducerNodeName(node.getName());
                        contextInfo.setDataType(inferContextDataType(node, key));
                        contextInfo.setDescription(generateContextDescription(node, key));

                        contextDataList.add(contextInfo);
                    }
                }
            }
        }

        result.setContextKeys(contextKeys);
        result.setContextData(contextDataList);
    }

    /**
     * 分析输出目标
     */
    private void analyzeOutputTargets(Workflow workflow, String scriptNodeId, ContextAnalysisResult result) {
        Set<String> successors = workflow.getTargetNodes(scriptNodeId);
        List<OutputTargetInfo> outputTargets = new ArrayList<>();

        for (String succId : successors) {
            WorkflowNode succNode = workflow.getNode(succId);
            if (succNode != null) {
                OutputTargetInfo targetInfo = new OutputTargetInfo();
                targetInfo.setNodeId(succId);
                targetInfo.setNodeName(succNode.getName());
                targetInfo.setNodeType(succNode.getType().name());
                targetInfo.setExpectedDataType(inferExpectedInputType(succNode));
                targetInfo.setDescription(generateTargetDescription(succNode));

                outputTargets.add(targetInfo);
            }
        }

        result.setOutputTargets(outputTargets);
    }

    /**
     * 获取所有前置节点（递归）
     */
    private Set<String> getAllPredecessors(Workflow workflow, String nodeId) {
        Set<String> allPredecessors = new HashSet<>();
        Set<String> visited = new HashSet<>();

        findAllPredecessors(workflow, nodeId, allPredecessors, visited);

        return allPredecessors;
    }

    /**
     * 递归查找所有前置节点
     */
    private void findAllPredecessors(Workflow workflow, String nodeId, Set<String> allPredecessors,
            Set<String> visited) {
        if (visited.contains(nodeId)) {
            return;
        }
        visited.add(nodeId);

        Set<String> directPredecessors = workflow.getSourceNodes(nodeId);
        for (String predId : directPredecessors) {
            allPredecessors.add(predId);
            findAllPredecessors(workflow, predId, allPredecessors, visited);
        }
    }

    /**
     * 分析节点配置
     */
    private void analyzeNodeConfiguration(WorkflowNode node, ContextAnalysisResult result) {
        Map<String, Object> config = node.getConfiguration();

        ScriptConfigInfo configInfo = new ScriptConfigInfo();
        configInfo.setInputKey((String) config.get("inputKey"));
        configInfo.setOutputKey((String) config.get("outputKey"));
        configInfo.setParameters((Map<String, Object>) config.get("parameters"));

        result.setScriptConfig(configInfo);
    }

    /**
     * 根据节点类型推断数据类型
     */
    private String inferDataType(WorkflowNode node) {
        switch (node.getType()) {

            case DIAGNOSIS:
                return "DiagnosisResult";

            case SCRIPT:
                return "Any";

            case INPUT:
                return "Any";

            default:
                return "Unknown";
        }
    }

    /**
     * 推断节点可能产生的上下文键
     */
    private List<String> inferContextKeys(WorkflowNode node) {
        List<String> keys = new ArrayList<>();
        Map<String, Object> config = node.getConfiguration();

        // 添加输出键
        String outputKey = (String) config.get("outputKey");
        if (outputKey != null) {
            keys.add(outputKey);
        }

        // 根据节点类型添加特定的上下文键
        switch (node.getType()) {

            case DIAGNOSIS:
                keys.add("diagnosis_stats");
                keys.add("issue_summary");
                break;

            case SCRIPT:
                // 脚本节点可能产生各种上下文数据
                keys.add("processing_stats");
                keys.add("custom_metrics");
                break;

            case INPUT:
                keys.add("config");
                keys.add("parameters");
                break;
        }

        return keys;
    }

    /**
     * 推断上下文数据类型
     */
    private String inferContextDataType(WorkflowNode node, String key) {
        if (key.endsWith("_stats") || key.endsWith("_metrics")) {
            return "Object";
        }
        if (key.equals("config") || key.equals("parameters")) {
            return "Object";
        }
        if (key.contains("result") || key.contains("data")) {
            return inferDataType(node);
        }
        return "Any";
    }

    /**
     * 推断后续节点期望的输入类型
     */
    private String inferExpectedInputType(WorkflowNode node) {
        switch (node.getType()) {
            case DIAGNOSIS:
                return "LogEntry[] | Object[]";

            case SCRIPT:
                return "Any";

            default:
                return "Any";
        }
    }

    /**
     * 生成节点描述
     */
    private String generateNodeDescription(WorkflowNode node) {
        Map<String, Object> config = node.getConfiguration();

        switch (node.getType()) {

            case DIAGNOSIS:
                String diagnosisType = (String) config.get("diagnosisType");
                return String.format("诊断节点，类型: %s", diagnosisType);

            case INPUT:
                return "输入节点，提供初始数据或配置";

            case SCRIPT:
                return "脚本节点，执行自定义处理逻辑";

            default:
                return node.getName();
        }
    }

    /**
     * 生成上下文数据描述
     */
    private String generateContextDescription(WorkflowNode node, String key) {
        if (key.equals("config")) {
            return "工作流配置参数";
        }
        if (key.equals("parameters")) {
            return "节点参数配置";
        }
        if (key.endsWith("_stats")) {
            return String.format("来自%s的统计信息", node.getName());
        }
        if (key.endsWith("_result")) {
            return String.format("来自%s的处理结果", node.getName());
        }
        return String.format("来自%s的数据：%s", node.getName(), key);
    }

    /**
     * 生成目标节点描述
     */
    private String generateTargetDescription(WorkflowNode node) {
        switch (node.getType()) {
            case DIAGNOSIS:
                return "诊断节点，需要日志或结构化数据进行分析";

            case SCRIPT:
                return "后续脚本节点，可接受任意格式数据";

            default:
                return node.getName();
        }
    }

    /**
     * 生成数据流描述
     */
    private void generateDataFlowDescription(ContextAnalysisResult result) {
        StringBuilder description = new StringBuilder();

        description.append("数据流分析:\n");

        // 输入描述
        if (!result.getInputSources().isEmpty()) {
            description.append("输入来源:\n");
            for (InputSourceInfo source : result.getInputSources()) {
                description.append(String.format("- %s (%s): %s\n",
                        source.getNodeName(), source.getNodeType(), source.getDescription()));
            }
        }

        // 上下文描述
        if (!result.getContextData().isEmpty()) {
            description.append("可用上下文数据:\n");
            for (ContextDataInfo context : result.getContextData()) {
                description.append(String.format("- %s: %s\n",
                        context.getKey(), context.getDescription()));
            }
        }

        // 输出目标描述
        if (!result.getOutputTargets().isEmpty()) {
            description.append("输出目标:\n");
            for (OutputTargetInfo target : result.getOutputTargets()) {
                description.append(String.format("- %s (%s): %s\n",
                        target.getNodeName(), target.getNodeType(), target.getDescription()));
            }
        }

        result.setDataFlowDescription(description.toString());
    }

    // 内部数据结构类

    public static class ContextAnalysisResult {
        private String nodeId;
        private String workflowId;
        private String workflowName;
        private List<InputSourceInfo> inputSources = new ArrayList<>();
        private Set<String> contextKeys = new HashSet<>();
        private List<ContextDataInfo> contextData = new ArrayList<>();
        private List<OutputTargetInfo> outputTargets = new ArrayList<>();
        private ScriptConfigInfo scriptConfig;
        private String dataFlowDescription;

        // Getters and Setters
        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getWorkflowId() {
            return workflowId;
        }

        public void setWorkflowId(String workflowId) {
            this.workflowId = workflowId;
        }

        public String getWorkflowName() {
            return workflowName;
        }

        public void setWorkflowName(String workflowName) {
            this.workflowName = workflowName;
        }

        public List<InputSourceInfo> getInputSources() {
            return inputSources;
        }

        public void setInputSources(List<InputSourceInfo> inputSources) {
            this.inputSources = inputSources;
        }

        public Set<String> getContextKeys() {
            return contextKeys;
        }

        public void setContextKeys(Set<String> contextKeys) {
            this.contextKeys = contextKeys;
        }

        public List<ContextDataInfo> getContextData() {
            return contextData;
        }

        public void setContextData(List<ContextDataInfo> contextData) {
            this.contextData = contextData;
        }

        public List<OutputTargetInfo> getOutputTargets() {
            return outputTargets;
        }

        public void setOutputTargets(List<OutputTargetInfo> outputTargets) {
            this.outputTargets = outputTargets;
        }

        public ScriptConfigInfo getScriptConfig() {
            return scriptConfig;
        }

        public void setScriptConfig(ScriptConfigInfo scriptConfig) {
            this.scriptConfig = scriptConfig;
        }

        public String getDataFlowDescription() {
            return dataFlowDescription;
        }

        public void setDataFlowDescription(String dataFlowDescription) {
            this.dataFlowDescription = dataFlowDescription;
        }
    }

    public static class InputSourceInfo {
        private String nodeId;
        private String nodeName;
        private String nodeType;
        private String expectedDataType;
        private String description;

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

        public String getNodeType() {
            return nodeType;
        }

        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
        }

        public String getExpectedDataType() {
            return expectedDataType;
        }

        public void setExpectedDataType(String expectedDataType) {
            this.expectedDataType = expectedDataType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class ContextDataInfo {
        private String key;
        private String producerNodeId;
        private String producerNodeName;
        private String dataType;
        private String description;

        // Getters and Setters
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getProducerNodeId() {
            return producerNodeId;
        }

        public void setProducerNodeId(String producerNodeId) {
            this.producerNodeId = producerNodeId;
        }

        public String getProducerNodeName() {
            return producerNodeName;
        }

        public void setProducerNodeName(String producerNodeName) {
            this.producerNodeName = producerNodeName;
        }

        public String getDataType() {
            return dataType;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class OutputTargetInfo {
        private String nodeId;
        private String nodeName;
        private String nodeType;
        private String expectedDataType;
        private String description;

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

        public String getNodeType() {
            return nodeType;
        }

        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
        }

        public String getExpectedDataType() {
            return expectedDataType;
        }

        public void setExpectedDataType(String expectedDataType) {
            this.expectedDataType = expectedDataType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class ScriptConfigInfo {
        private String inputKey;
        private String outputKey;
        private Map<String, Object> parameters;

        // Getters and Setters
        public String getInputKey() {
            return inputKey;
        }

        public void setInputKey(String inputKey) {
            this.inputKey = inputKey;
        }

        public String getOutputKey() {
            return outputKey;
        }

        public void setOutputKey(String outputKey) {
            this.outputKey = outputKey;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }
}
