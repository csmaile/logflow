package com.logflow.ai;

import com.logflow.ai.WorkflowRequirementAnalyzer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 工作流设计器
 * 根据需求分析结果设计工作流的节点结构和连接关系
 */
public class WorkflowDesigner {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowDesigner.class);

    /**
     * 设计工作流结构
     * 
     * @param requirementResult 需求分析结果
     * @return 工作流设计结果
     */
    public WorkflowDesignResult designWorkflow(RequirementAnalysisResult requirementResult) {
        logger.info("开始设计工作流: {}", requirementResult.getSuggestedName());

        WorkflowDesignResult design = new WorkflowDesignResult();
        design.setWorkflowId(generateWorkflowId(requirementResult));
        design.setWorkflowName(requirementResult.getSuggestedName());
        design.setDescription(requirementResult.getSuggestedDescription());
        design.setRequirementAnalysis(requirementResult);

        // 1. 设计节点
        List<NodeDesign> nodes = designNodes(requirementResult);
        design.setNodes(nodes);

        // 2. 设计连接关系
        List<ConnectionDesign> connections = designConnections(nodes, requirementResult);
        design.setConnections(connections);

        // 3. 设计节点位置布局
        designNodePositions(nodes);

        // 4. 生成全局配置
        design.setGlobalConfig(generateGlobalConfig(requirementResult));

        logger.info("工作流设计完成: 节点数={}, 连接数={}", nodes.size(), connections.size());

        return design;
    }

    /**
     * 设计工作流节点
     */
    private List<NodeDesign> designNodes(RequirementAnalysisResult requirement) {
        List<NodeDesign> nodes = new ArrayList<>();
        int nodeCounter = 1;

        // 1. 创建输入配置节点
        NodeDesign configNode = createConfigInputNode(nodeCounter++);
        nodes.add(configNode);

        // 2. 创建数据源节点
        for (DataSourceRequirement dsReq : requirement.getDataSources()) {
            NodeDesign dsNode = createDataSourceNode(nodeCounter++, dsReq);
            nodes.add(dsNode);
        }

        // 3. 创建处理节点
        for (ProcessingStepRequirement stepReq : requirement.getProcessingSteps()) {
            if (isScriptProcessing(stepReq.getType())) {
                // 创建脚本节点
                NodeDesign scriptNode = createScriptNode(nodeCounter++, stepReq);
                nodes.add(scriptNode);
            } else if (isDiagnosisProcessing(stepReq.getType())) {
                // 创建诊断节点
                NodeDesign diagnosisNode = createDiagnosisNode(nodeCounter++, stepReq);
                nodes.add(diagnosisNode);
            }
        }

        // 4. 如果需要聚合多个处理结果，创建聚合脚本节点
        if (hasMultipleProcessingSteps(requirement)) {
            NodeDesign aggregatorNode = createResultAggregatorNode(nodeCounter++, requirement);
            nodes.add(aggregatorNode);
        }

        // 5. 创建输出节点
        for (OutputRequirement outputReq : requirement.getOutputRequirements()) {
            NodeDesign outputNode = createOutputNode(nodeCounter++, outputReq);
            nodes.add(outputNode);
        }

        return nodes;
    }

    /**
     * 设计节点连接关系
     */
    private List<ConnectionDesign> designConnections(List<NodeDesign> nodes, RequirementAnalysisResult requirement) {
        List<ConnectionDesign> connections = new ArrayList<>();

        // 获取不同类型的节点
        NodeDesign configNode = findNodeByType(nodes, "input");
        List<NodeDesign> dataSourceNodes = findNodesByType(nodes, "datasource");
        List<NodeDesign> processingNodes = findNodesByType(nodes, "script", "diagnosis");
        NodeDesign aggregatorNode = findNodeByTypeAndName(nodes, "script", "result_aggregator");
        List<NodeDesign> outputNodes = findNodesByType(nodes, "output");

        // 1. 配置节点连接到所有处理节点
        if (configNode != null) {
            for (NodeDesign processingNode : processingNodes) {
                if (!processingNode.getId().equals("result_aggregator")) {
                    connections.add(new ConnectionDesign(configNode.getId(), processingNode.getId()));
                }
            }
        }

        // 2. 数据源节点连接到处理节点
        if (!dataSourceNodes.isEmpty() && !processingNodes.isEmpty()) {
            // 如果只有一个数据源，连接到所有处理节点
            if (dataSourceNodes.size() == 1) {
                NodeDesign dataSource = dataSourceNodes.get(0);
                for (NodeDesign processingNode : processingNodes) {
                    if (!processingNode.getId().equals("result_aggregator")) {
                        connections.add(new ConnectionDesign(dataSource.getId(), processingNode.getId()));
                    }
                }
            } else {
                // 多个数据源，按顺序连接
                for (int i = 0; i < Math.min(dataSourceNodes.size(), processingNodes.size()); i++) {
                    connections
                            .add(new ConnectionDesign(dataSourceNodes.get(i).getId(), processingNodes.get(i).getId()));
                }
            }
        }

        // 3. 处理节点连接到聚合节点或输出节点
        if (aggregatorNode != null) {
            // 所有处理节点连接到聚合节点
            for (NodeDesign processingNode : processingNodes) {
                if (!processingNode.getId().equals("result_aggregator")) {
                    connections.add(new ConnectionDesign(processingNode.getId(), aggregatorNode.getId()));
                }
            }

            // 聚合节点连接到输出节点
            for (NodeDesign outputNode : outputNodes) {
                connections.add(new ConnectionDesign(aggregatorNode.getId(), outputNode.getId()));
            }
        } else {
            // 直接连接处理节点到输出节点
            if (!processingNodes.isEmpty() && !outputNodes.isEmpty()) {
                NodeDesign lastProcessingNode = processingNodes.get(processingNodes.size() - 1);
                for (NodeDesign outputNode : outputNodes) {
                    connections.add(new ConnectionDesign(lastProcessingNode.getId(), outputNode.getId()));
                }
            }
        }

        return connections;
    }

    /**
     * 设计节点位置布局
     */
    private void designNodePositions(List<NodeDesign> nodes) {
        int x = 150;
        int y = 100;
        int verticalSpacing = 150;
        int horizontalSpacing = 200;

        // 按类型分层布局
        List<NodeDesign> inputNodes = findNodesByType(nodes, "input");
        List<NodeDesign> dataSourceNodes = findNodesByType(nodes, "datasource");
        List<NodeDesign> processingNodes = findNodesByType(nodes, "script", "diagnosis");
        List<NodeDesign> outputNodes = findNodesByType(nodes, "output");

        // 第一列：输入和数据源节点
        for (NodeDesign node : inputNodes) {
            node.setPosition(new NodePosition(x, y));
            y += verticalSpacing;
        }
        for (NodeDesign node : dataSourceNodes) {
            node.setPosition(new NodePosition(x, y));
            y += verticalSpacing;
        }

        // 第二列：处理节点
        x += horizontalSpacing;
        y = 100;
        for (NodeDesign node : processingNodes) {
            node.setPosition(new NodePosition(x, y));
            y += verticalSpacing;
        }

        // 第三列：输出节点
        x += horizontalSpacing;
        y = 100;
        for (NodeDesign node : outputNodes) {
            node.setPosition(new NodePosition(x, y));
            y += verticalSpacing;
        }
    }

    /**
     * 生成全局配置
     */
    private Map<String, Object> generateGlobalConfig(RequirementAnalysisResult requirement) {
        Map<String, Object> config = new HashMap<>();

        config.put("version", "1.0");
        config.put("description", requirement.getSuggestedDescription());
        config.put("generated_by", "LogFlow AI Generator");
        config.put("generated_at", new Date().toString());
        config.put("complexity", requirement.getComplexity());
        config.put("domain", requirement.getDomain());

        // 添加从需求中提取的参数
        if (!requirement.getParameters().isEmpty()) {
            config.put("parameters", requirement.getParameters());
        }

        // 添加业务规则
        if (!requirement.getBusinessRules().isEmpty()) {
            config.put("business_rules", requirement.getBusinessRules());
        }

        return config;
    }

    // 节点创建方法

    private NodeDesign createConfigInputNode(int id) {
        NodeDesign node = new NodeDesign();
        node.setId("config_input");
        node.setName("配置输入");
        node.setType("input");
        node.setEnabled(true);

        Map<String, Object> config = new HashMap<>();
        config.put("inputType", "json");
        config.put("outputKey", "config");
        config.put("description", "工作流配置参数");
        node.setConfig(config);

        return node;
    }

    private NodeDesign createDataSourceNode(int id, DataSourceRequirement dsReq) {
        NodeDesign node = new NodeDesign();
        node.setId("data_source_" + id);
        node.setName(dsReq.getDescription());
        node.setType("datasource");
        node.setEnabled(true);

        Map<String, Object> config = new HashMap<>();
        config.put("sourceType", dsReq.getType());
        config.put("outputKey", "raw_data");
        config.putAll(dsReq.getParameters());
        node.setConfig(config);

        return node;
    }

    private NodeDesign createScriptNode(int id, ProcessingStepRequirement stepReq) {
        NodeDesign node = new NodeDesign();
        node.setId(generateScriptNodeId(stepReq));
        node.setName(stepReq.getDescription());
        node.setType("script");
        node.setEnabled(true);

        Map<String, Object> config = new HashMap<>();
        config.put("scriptEngine", "javascript");
        config.put("inputKey", determineInputKey(stepReq));
        config.put("outputKey", determineOutputKey(stepReq));
        config.put("processingType", stepReq.getType());
        config.put("requirementLogic", stepReq.getLogic());

        node.setConfig(config);

        return node;
    }

    private NodeDesign createDiagnosisNode(int id, ProcessingStepRequirement stepReq) {
        NodeDesign node = new NodeDesign();
        node.setId("diagnosis_" + stepReq.getType());
        node.setName(stepReq.getDescription());
        node.setType("diagnosis");
        node.setEnabled(true);

        Map<String, Object> config = new HashMap<>();
        config.put("diagnosisType", mapProcessingTypeToDiagnosis(stepReq.getType()));
        config.put("inputKey", determineInputKey(stepReq));
        config.put("outputKey", stepReq.getType() + "_result");

        node.setConfig(config);

        return node;
    }

    private NodeDesign createResultAggregatorNode(int id, RequirementAnalysisResult requirement) {
        NodeDesign node = new NodeDesign();
        node.setId("result_aggregator");
        node.setName("结果聚合器");
        node.setType("script");
        node.setEnabled(true);

        Map<String, Object> config = new HashMap<>();
        config.put("scriptEngine", "javascript");
        config.put("inputKey", "processing_results");
        config.put("outputKey", "final_result");
        config.put("processingType", "aggregation");
        config.put("requirementLogic", "聚合所有处理结果，生成最终报告");

        node.setConfig(config);

        return node;
    }

    private NodeDesign createOutputNode(int id, OutputRequirement outputReq) {
        NodeDesign node = new NodeDesign();
        node.setId("output_" + outputReq.getType());
        node.setName(outputReq.getDescription());
        node.setType("output");
        node.setEnabled(true);

        Map<String, Object> config = new HashMap<>();
        config.put("outputType", outputReq.getType());
        config.put("inputKey", "final_result");
        config.put("format", outputReq.getFormat());

        if ("file".equals(outputReq.getType())) {
            config.put("filePath", "output/result." + outputReq.getFormat());
        }

        node.setConfig(config);

        return node;
    }

    // 辅助方法

    private String generateWorkflowId(RequirementAnalysisResult requirement) {
        return requirement.getSuggestedName().toLowerCase().replace(" ", "_");
    }

    private boolean isScriptProcessing(String type) {
        return Arrays.asList("filter", "transform", "calculate", "validate").contains(type);
    }

    private boolean isDiagnosisProcessing(String type) {
        return Arrays.asList("error_analysis", "performance_analysis", "anomaly_detection").contains(type);
    }

    private boolean hasMultipleProcessingSteps(RequirementAnalysisResult requirement) {
        return requirement.getProcessingSteps().size() > 1 || requirement.getOutputRequirements().size() > 1;
    }

    private NodeDesign findNodeByType(List<NodeDesign> nodes, String type) {
        return nodes.stream()
                .filter(node -> type.equals(node.getType()))
                .findFirst()
                .orElse(null);
    }

    private List<NodeDesign> findNodesByType(List<NodeDesign> nodes, String... types) {
        Set<String> typeSet = new HashSet<>(Arrays.asList(types));
        return nodes.stream()
                .filter(node -> typeSet.contains(node.getType()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private NodeDesign findNodeByTypeAndName(List<NodeDesign> nodes, String type, String nameContains) {
        return nodes.stream()
                .filter(node -> type.equals(node.getType()) && node.getId().contains(nameContains))
                .findFirst()
                .orElse(null);
    }

    private String generateScriptNodeId(ProcessingStepRequirement stepReq) {
        return stepReq.getType() + "_processor";
    }

    private String determineInputKey(ProcessingStepRequirement stepReq) {
        switch (stepReq.getType()) {
            case "filter":
            case "transform":
            case "validate":
                return "raw_data";
            case "calculate":
                return "processed_data";
            default:
                return "input_data";
        }
    }

    private String determineOutputKey(ProcessingStepRequirement stepReq) {
        return stepReq.getType() + "_result";
    }

    private String mapProcessingTypeToDiagnosis(String processingType) {
        switch (processingType) {
            case "error_analysis":
                return "error_detection";
            case "performance_analysis":
                return "performance_analysis";
            case "anomaly_detection":
                return "anomaly_detection";
            default:
                return "general_analysis";
        }
    }

    // 数据结构类

    public static class WorkflowDesignResult {
        private String workflowId;
        private String workflowName;
        private String description;
        private RequirementAnalysisResult requirementAnalysis;
        private List<NodeDesign> nodes = new ArrayList<>();
        private List<ConnectionDesign> connections = new ArrayList<>();
        private Map<String, Object> globalConfig = new HashMap<>();

        // Getters and Setters
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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public RequirementAnalysisResult getRequirementAnalysis() {
            return requirementAnalysis;
        }

        public void setRequirementAnalysis(RequirementAnalysisResult requirementAnalysis) {
            this.requirementAnalysis = requirementAnalysis;
        }

        public List<NodeDesign> getNodes() {
            return nodes;
        }

        public void setNodes(List<NodeDesign> nodes) {
            this.nodes = nodes;
        }

        public List<ConnectionDesign> getConnections() {
            return connections;
        }

        public void setConnections(List<ConnectionDesign> connections) {
            this.connections = connections;
        }

        public Map<String, Object> getGlobalConfig() {
            return globalConfig;
        }

        public void setGlobalConfig(Map<String, Object> globalConfig) {
            this.globalConfig = globalConfig;
        }
    }

    public static class NodeDesign {
        private String id;
        private String name;
        private String type;
        private boolean enabled;
        private NodePosition position;
        private Map<String, Object> config = new HashMap<>();

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public NodePosition getPosition() {
            return position;
        }

        public void setPosition(NodePosition position) {
            this.position = position;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }
    }

    public static class ConnectionDesign {
        private String fromNodeId;
        private String toNodeId;

        public ConnectionDesign(String fromNodeId, String toNodeId) {
            this.fromNodeId = fromNodeId;
            this.toNodeId = toNodeId;
        }

        // Getters and Setters
        public String getFromNodeId() {
            return fromNodeId;
        }

        public void setFromNodeId(String fromNodeId) {
            this.fromNodeId = fromNodeId;
        }

        public String getToNodeId() {
            return toNodeId;
        }

        public void setToNodeId(String toNodeId) {
            this.toNodeId = toNodeId;
        }
    }

    public static class NodePosition {
        private int x;
        private int y;

        public NodePosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        // Getters and Setters
        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }
}
