package com.logflow.builder;

import com.logflow.core.*;
import com.logflow.engine.Workflow;
import com.logflow.nodes.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流构建器
 * 提供便捷的API来构建工作流
 */
public class WorkflowBuilder {

    private final Workflow workflow;
    private final Map<String, WorkflowNode> nodeMap;

    public WorkflowBuilder(String id, String name) {
        this.workflow = new Workflow(id, name);
        this.nodeMap = new HashMap<>();
    }

    public WorkflowBuilder(String id, String name, String description) {
        this.workflow = new Workflow(id, name, description);
        this.nodeMap = new HashMap<>();
    }

    /**
     * 创建工作流构建器
     */
    public static WorkflowBuilder create(String id, String name) {
        return new WorkflowBuilder(id, name);
    }

    /**
     * 创建工作流构建器（带描述）
     */
    public static WorkflowBuilder create(String id, String name, String description) {
        return new WorkflowBuilder(id, name, description);
    }

    /**
     * 添加输入节点
     */
    public WorkflowBuilder addInputNode(String id, String name) {
        InputNode node = new InputNode(id, name);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加输入节点（带配置）
     */
    public WorkflowBuilder addInputNode(String id, String name, Map<String, Object> config) {
        InputNode node = new InputNode(id, name);
        node.setConfiguration(config);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加输出节点
     * 
     * @deprecated 请使用 {@link #addNotificationNode} 替代。
     *             建议使用具体的通知类型方法：{@link #addFileOutputNode},
     *             {@link #addContextOutputNode},
     *             {@link #addConsoleNotificationNode}
     */
    @Deprecated
    public WorkflowBuilder addOutputNode(String id, String name) {
        OutputNode node = new OutputNode(id, name);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加输出节点（带配置）
     * 
     * @deprecated 请使用 {@link #addNotificationNode} 替代。
     *             建议使用具体的通知类型方法：{@link #addFileOutputNode},
     *             {@link #addContextOutputNode},
     *             {@link #addConsoleNotificationNode}
     */
    @Deprecated
    public WorkflowBuilder addOutputNode(String id, String name, Map<String, Object> config) {
        OutputNode node = new OutputNode(id, name);
        node.setConfiguration(config);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加数据源节点
     * 
     * @deprecated 请使用 {@link #addPluginNode} 替代。
     *             PluginNode 提供了更强大的插件化能力，配置项 sourceType 请改为 pluginType
     */
    @Deprecated
    public WorkflowBuilder addDataSourceNode(String id, String name) {
        DataSourceNode node = new DataSourceNode(id, name);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加数据源节点（带配置）
     * 
     * @deprecated 请使用 {@link #addPluginNode} 替代。
     *             PluginNode 提供了更强大的插件化能力，配置项 sourceType 请改为 pluginType
     */
    @Deprecated
    public WorkflowBuilder addDataSourceNode(String id, String name, Map<String, Object> config) {
        DataSourceNode node = new DataSourceNode(id, name);
        node.setConfiguration(config);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加插件节点
     * 替代原有的数据源节点，提供更强大的插件化能力
     */
    public WorkflowBuilder addPluginNode(String id, String name) {
        PluginNode node = new PluginNode(id, name);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加插件节点（带配置）
     */
    public WorkflowBuilder addPluginNode(String id, String name, Map<String, Object> config) {
        PluginNode node = new PluginNode(id, name);
        node.setConfiguration(config);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加插件节点（指定插件类型）
     */
    public WorkflowBuilder addPluginNode(String id, String name, String pluginType) {
        Map<String, Object> config = Map.of("pluginType", pluginType);
        return addPluginNode(id, name, config);
    }

    /**
     * 添加诊断节点
     */
    public WorkflowBuilder addDiagnosisNode(String id, String name) {
        DiagnosisNode node = new DiagnosisNode(id, name);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加诊断节点（带配置）
     */
    public WorkflowBuilder addDiagnosisNode(String id, String name, Map<String, Object> config) {
        DiagnosisNode node = new DiagnosisNode(id, name);
        node.setConfiguration(config);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加脚本节点
     */
    public WorkflowBuilder addScriptNode(String id, String name) {
        ScriptNode node = new ScriptNode(id, name);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加脚本节点（带配置）
     */
    public WorkflowBuilder addScriptNode(String id, String name, Map<String, Object> config) {
        ScriptNode node = new ScriptNode(id, name);
        node.setConfiguration(config);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加关联节点
     */
    public WorkflowBuilder addReferenceNode(String id, String name) {
        ReferenceNode node = new ReferenceNode(id, name);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加关联节点（带配置）
     */
    public WorkflowBuilder addReferenceNode(String id, String name, Map<String, Object> config) {
        ReferenceNode node = new ReferenceNode(id, name);
        node.setConfiguration(config);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加通知节点
     */
    public WorkflowBuilder addNotificationNode(String id, String name) {
        NotificationNode node = new NotificationNode(id, name);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加通知节点（带配置）
     */
    public WorkflowBuilder addNotificationNode(String id, String name, Map<String, Object> config) {
        NotificationNode node = new NotificationNode(id, name);
        node.setConfiguration(config);
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 添加文件输出节点
     * 替代原有OutputNode的文件输出功能
     */
    public WorkflowBuilder addFileOutputNode(String id, String name, String filePath) {
        Map<String, Object> config = Map.of(
                "providerType", "file",
                "provider.filePath", filePath,
                "provider.format", "text",
                "provider.append", true,
                "provider.includeTimestamp", true);
        return addNotificationNode(id, name, config);
    }

    /**
     * 添加文件输出节点（JSON格式）
     */
    public WorkflowBuilder addJsonFileOutputNode(String id, String name, String filePath) {
        Map<String, Object> config = Map.of(
                "providerType", "file",
                "provider.filePath", filePath,
                "provider.format", "json",
                "provider.append", true,
                "provider.includeTimestamp", true);
        return addNotificationNode(id, name, config);
    }

    /**
     * 添加上下文输出节点
     * 替代原有OutputNode的上下文输出功能
     */
    public WorkflowBuilder addContextOutputNode(String id, String name, String contextKey) {
        Map<String, Object> config = Map.of(
                "providerType", "context",
                "provider.contextKey", contextKey,
                "provider.dataFormat", "content",
                "provider.overwrite", true);
        return addNotificationNode(id, name, config);
    }

    /**
     * 添加控制台通知节点
     * 替代原有OutputNode的控制台输出功能，但功能更强大
     */
    public WorkflowBuilder addConsoleNotificationNode(String id, String name) {
        Map<String, Object> config = Map.of(
                "providerType", "console",
                "provider.format", "simple",
                "provider.includeTimestamp", true,
                "provider.includePriority", false);
        return addNotificationNode(id, name, config);
    }

    /**
     * 添加控制台通知节点（详细格式）
     */
    public WorkflowBuilder addDetailedConsoleNotificationNode(String id, String name) {
        Map<String, Object> config = Map.of(
                "providerType", "console",
                "provider.format", "detailed",
                "provider.includeTimestamp", true,
                "provider.includePriority", true);
        return addNotificationNode(id, name, config);
    }

    /**
     * 添加自定义节点
     */
    public WorkflowBuilder addNode(WorkflowNode node) {
        addNodeToWorkflow(node);
        return this;
    }

    /**
     * 连接两个节点
     */
    public WorkflowBuilder connect(String fromNodeId, String toNodeId) {
        workflow.addConnection(fromNodeId, toNodeId);
        return this;
    }

    // 便捷配置方法（支持链式调用）

    /**
     * 为最后添加的节点设置配置
     */
    public WorkflowBuilder withConfig(Map<String, Object> config) {
        if (!nodeMap.isEmpty()) {
            String lastNodeId = getLastAddedNodeId();
            if (lastNodeId != null) {
                configureNode(lastNodeId, config);
            }
        }
        return this;
    }

    /**
     * 为脚本节点设置脚本内容
     */
    public WorkflowBuilder withScript(String script) {
        return withConfig(Map.of("script", script));
    }

    /**
     * 为输出节点设置输出类型
     */
    public WorkflowBuilder withOutputType(String outputType) {
        return withConfig(Map.of("outputType", outputType));
    }

    /**
     * 为输出节点设置输入键
     */
    public WorkflowBuilder withInputKey(String inputKey) {
        return withConfig(Map.of("inputKey", inputKey));
    }

    /**
     * 为数据源节点设置源类型
     */
    public WorkflowBuilder withSourceType(String sourceType) {
        return withConfig(Map.of("sourceType", sourceType));
    }

    /**
     * 为节点设置通用配置项
     */
    public WorkflowBuilder withConfigItem(String key, Object value) {
        Map<String, Object> config = new HashMap<>();
        config.put(key, value);
        return withConfig(config);
    }

    /**
     * 获取最后添加的节点ID
     */
    private String getLastAddedNodeId() {
        Collection<WorkflowNode> allNodes = workflow.getAllNodes();
        if (allNodes.isEmpty()) {
            return null;
        }

        // 获取最后添加的节点
        java.util.List<WorkflowNode> nodes = new java.util.ArrayList<>(allNodes);
        return nodes.get(nodes.size() - 1).getId();
    }

    /**
     * 配置节点
     */
    public WorkflowBuilder configureNode(String nodeId, String key, Object value) {
        WorkflowNode node = nodeMap.get(nodeId);
        if (node != null) {
            Map<String, Object> config = new HashMap<>(node.getConfiguration());
            config.put(key, value);
            node.setConfiguration(config);
        }
        return this;
    }

    /**
     * 批量配置节点
     */
    public WorkflowBuilder configureNode(String nodeId, Map<String, Object> config) {
        WorkflowNode node = nodeMap.get(nodeId);
        if (node != null) {
            Map<String, Object> existingConfig = new HashMap<>(node.getConfiguration());
            existingConfig.putAll(config);
            node.setConfiguration(existingConfig);
        }
        return this;
    }

    /**
     * 设置工作流元数据
     */
    public WorkflowBuilder metadata(String key, Object value) {
        workflow.setMetadata(key, value);
        return this;
    }

    /**
     * 构建工作流
     */
    public Workflow build() {
        return workflow;
    }

    /**
     * 创建便捷配置方法
     */
    public static Map<String, Object> config() {
        return new HashMap<>();
    }

    public static Map<String, Object> config(String key, Object value) {
        Map<String, Object> config = new HashMap<>();
        config.put(key, value);
        return config;
    }

    public static Map<String, Object> config(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> config = new HashMap<>();
        config.put(k1, v1);
        config.put(k2, v2);
        return config;
    }

    public static Map<String, Object> config(String k1, Object v1, String k2, Object v2,
            String k3, Object v3) {
        Map<String, Object> config = new HashMap<>();
        config.put(k1, v1);
        config.put(k2, v2);
        config.put(k3, v3);
        return config;
    }

    private void addNodeToWorkflow(WorkflowNode node) {
        workflow.addNode(node);
        nodeMap.put(node.getId(), node);
    }
}
