package com.logflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.logflow.core.NodeType;
import com.logflow.core.WorkflowNode;
import com.logflow.engine.Workflow;
import com.logflow.nodes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流YAML配置加载器
 * 将YAML配置文件转换为Workflow对象
 */
public class WorkflowConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowConfigLoader.class);
    private final ObjectMapper yamlMapper;

    public WorkflowConfigLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * 从文件加载工作流配置
     */
    public Workflow loadFromFile(String filePath) throws IOException, WorkflowConfigException {
        logger.info("从文件加载工作流配置: {}", filePath);
        File file = new File(filePath);
        if (!file.exists()) {
            throw new WorkflowConfigException("配置文件不存在: " + filePath);
        }

        WorkflowConfig config = yamlMapper.readValue(file, WorkflowConfig.class);
        return buildWorkflow(config);
    }

    /**
     * 从类路径资源加载工作流配置
     */
    public Workflow loadFromResource(String resourcePath) throws IOException, WorkflowConfigException {
        logger.info("从资源加载工作流配置: {}", resourcePath);
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new WorkflowConfigException("资源文件不存在: " + resourcePath);
        }

        WorkflowConfig config = yamlMapper.readValue(inputStream, WorkflowConfig.class);
        return buildWorkflow(config);
    }

    /**
     * 从YAML字符串加载工作流配置
     */
    public Workflow loadFromYamlString(String yamlContent) throws IOException, WorkflowConfigException {
        logger.info("从YAML字符串加载工作流配置");
        WorkflowConfig config = yamlMapper.readValue(yamlContent, WorkflowConfig.class);
        return buildWorkflow(config);
    }

    /**
     * 将WorkflowConfig转换为Workflow对象
     */
    private Workflow buildWorkflow(WorkflowConfig config) throws WorkflowConfigException {
        validateConfig(config);

        // 创建工作流对象
        WorkflowConfig.WorkflowInfo workflowInfo = config.getWorkflow();
        Workflow workflow = new Workflow(
                workflowInfo.getId(),
                workflowInfo.getName(),
                workflowInfo.getDescription() != null ? workflowInfo.getDescription() : "");

        // 设置元数据
        if (workflowInfo.getVersion() != null) {
            workflow.setMetadata("version", workflowInfo.getVersion());
        }
        if (workflowInfo.getAuthor() != null) {
            workflow.setMetadata("author", workflowInfo.getAuthor());
        }
        if (workflowInfo.getMetadata() != null) {
            workflowInfo.getMetadata().forEach(workflow::setMetadata);
        }

        // 设置全局配置
        if (config.getGlobalConfig() != null) {
            config.getGlobalConfig().forEach(workflow::setMetadata);
        }

        // 创建节点
        Map<String, WorkflowNode> nodeMap = new HashMap<>();
        for (WorkflowConfig.NodeConfig nodeConfig : config.getNodes()) {
            WorkflowNode node = createNode(nodeConfig);
            workflow.addNode(node);
            nodeMap.put(nodeConfig.getId(), node);
            logger.debug("创建节点: {} ({})", nodeConfig.getName(), nodeConfig.getType());
        }

        // 创建连接
        for (WorkflowConfig.ConnectionConfig connectionConfig : config.getConnections()) {
            if (!nodeMap.containsKey(connectionConfig.getFrom())) {
                throw new WorkflowConfigException("连接中的源节点不存在: " + connectionConfig.getFrom());
            }
            if (!nodeMap.containsKey(connectionConfig.getTo())) {
                throw new WorkflowConfigException("连接中的目标节点不存在: " + connectionConfig.getTo());
            }

            workflow.addConnection(connectionConfig.getFrom(), connectionConfig.getTo());
            logger.debug("创建连接: {} -> {}", connectionConfig.getFrom(), connectionConfig.getTo());
        }

        logger.info("工作流构建完成: {} (节点数: {}, 连接数: {})",
                workflow.getName(), workflow.getNodeCount(), workflow.getConnectionCount());

        return workflow;
    }

    /**
     * 创建节点实例
     */
    private WorkflowNode createNode(WorkflowConfig.NodeConfig nodeConfig) throws WorkflowConfigException {
        String nodeType = nodeConfig.getType().toLowerCase();
        WorkflowNode node;

        switch (nodeType) {
            case "input":
                node = new InputNode(nodeConfig.getId(), nodeConfig.getName());
                break;
            case "notification":
                node = new NotificationNode(nodeConfig.getId(), nodeConfig.getName());
                break;
            case "plugin":
                node = new PluginNode(nodeConfig.getId(), nodeConfig.getName());
                break;
            case "diagnosis":
                node = new DiagnosisNode(nodeConfig.getId(), nodeConfig.getName());
                break;
            case "script":
                node = new ScriptNode(nodeConfig.getId(), nodeConfig.getName());
                break;
            case "reference":
                node = new ReferenceNode(nodeConfig.getId(), nodeConfig.getName());
                break;
            default:
                throw new WorkflowConfigException("不支持的节点类型: " + nodeConfig.getType());
        }

        // 设置节点配置
        if (nodeConfig.getConfig() != null) {
            node.setConfiguration(nodeConfig.getConfig());
        }

        return node;
    }

    /**
     * 验证配置有效性
     */
    private void validateConfig(WorkflowConfig config) throws WorkflowConfigException {
        if (config.getWorkflow() == null) {
            throw new WorkflowConfigException("工作流信息不能为空");
        }

        WorkflowConfig.WorkflowInfo workflowInfo = config.getWorkflow();
        if (workflowInfo.getId() == null || workflowInfo.getId().trim().isEmpty()) {
            throw new WorkflowConfigException("工作流ID不能为空");
        }
        if (workflowInfo.getName() == null || workflowInfo.getName().trim().isEmpty()) {
            throw new WorkflowConfigException("工作流名称不能为空");
        }

        if (config.getNodes() == null || config.getNodes().isEmpty()) {
            throw new WorkflowConfigException("工作流必须包含至少一个节点");
        }

        // 验证节点配置
        for (WorkflowConfig.NodeConfig nodeConfig : config.getNodes()) {
            if (nodeConfig.getId() == null || nodeConfig.getId().trim().isEmpty()) {
                throw new WorkflowConfigException("节点ID不能为空");
            }
            if (nodeConfig.getName() == null || nodeConfig.getName().trim().isEmpty()) {
                throw new WorkflowConfigException("节点名称不能为空");
            }
            if (nodeConfig.getType() == null || nodeConfig.getType().trim().isEmpty()) {
                throw new WorkflowConfigException("节点类型不能为空");
            }
        }

        // 验证连接配置
        if (config.getConnections() != null) {
            for (WorkflowConfig.ConnectionConfig connectionConfig : config.getConnections()) {
                if (connectionConfig.getFrom() == null || connectionConfig.getFrom().trim().isEmpty()) {
                    throw new WorkflowConfigException("连接的源节点ID不能为空");
                }
                if (connectionConfig.getTo() == null || connectionConfig.getTo().trim().isEmpty()) {
                    throw new WorkflowConfigException("连接的目标节点ID不能为空");
                }
            }
        }
    }

    /**
     * 导出工作流为YAML配置
     */
    public String exportToYaml(Workflow workflow) throws IOException {
        WorkflowConfig config = new WorkflowConfig();

        // 设置工作流信息
        WorkflowConfig.WorkflowInfo workflowInfo = new WorkflowConfig.WorkflowInfo();
        workflowInfo.setId(workflow.getId());
        workflowInfo.setName(workflow.getName());
        workflowInfo.setDescription(workflow.getDescription());

        Map<String, Object> metadata = workflow.getMetadata();
        if (metadata.containsKey("version")) {
            workflowInfo.setVersion((String) metadata.get("version"));
        }
        if (metadata.containsKey("author")) {
            workflowInfo.setAuthor((String) metadata.get("author"));
        }

        config.setWorkflow(workflowInfo);

        // TODO: 实现完整的导出功能（节点和连接）
        // 这里可以根据需要实现

        return yamlMapper.writeValueAsString(config);
    }

    /**
     * 工作流配置异常
     */
    public static class WorkflowConfigException extends Exception {
        public WorkflowConfigException(String message) {
            super(message);
        }

        public WorkflowConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
