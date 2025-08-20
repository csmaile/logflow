package com.logflow.builder;

import com.logflow.core.*;
import com.logflow.engine.Workflow;
import com.logflow.nodes.*;

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
     */
    public WorkflowBuilder addOutputNode(String id, String name) {
        OutputNode node = new OutputNode(id, name);
        addNodeToWorkflow(node);
        return this;
    }
    
    /**
     * 添加输出节点（带配置）
     */
    public WorkflowBuilder addOutputNode(String id, String name, Map<String, Object> config) {
        OutputNode node = new OutputNode(id, name);
        node.setConfiguration(config);
        addNodeToWorkflow(node);
        return this;
    }
    
    /**
     * 添加数据源节点
     */
    public WorkflowBuilder addDataSourceNode(String id, String name) {
        DataSourceNode node = new DataSourceNode(id, name);
        addNodeToWorkflow(node);
        return this;
    }
    
    /**
     * 添加数据源节点（带配置）
     */
    public WorkflowBuilder addDataSourceNode(String id, String name, Map<String, Object> config) {
        DataSourceNode node = new DataSourceNode(id, name);
        node.setConfiguration(config);
        addNodeToWorkflow(node);
        return this;
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
