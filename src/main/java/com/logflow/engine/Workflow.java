package com.logflow.engine;

import com.logflow.core.WorkflowNode;

import java.util.*;

/**
 * 工作流定义
 * 包含节点和连接关系
 */
public class Workflow {
    
    private final String id;
    private final String name;
    private final String description;
    private final Map<String, WorkflowNode> nodes;
    private final Map<String, Set<String>> connections; // nodeId -> set of target nodeIds
    private final Map<String, Object> metadata;
    
    public Workflow(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = "";
        this.nodes = new HashMap<>();
        this.connections = new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    public Workflow(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.nodes = new HashMap<>();
        this.connections = new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    /**
     * 添加节点
     */
    public void addNode(WorkflowNode node) {
        nodes.put(node.getId(), node);
        connections.putIfAbsent(node.getId(), new HashSet<>());
    }
    
    /**
     * 移除节点
     */
    public void removeNode(String nodeId) {
        nodes.remove(nodeId);
        connections.remove(nodeId);
        // 移除指向该节点的连接
        connections.values().forEach(targets -> targets.remove(nodeId));
    }
    
    /**
     * 添加连接
     */
    public void addConnection(String fromNodeId, String toNodeId) {
        if (!nodes.containsKey(fromNodeId) || !nodes.containsKey(toNodeId)) {
            throw new IllegalArgumentException("连接的节点必须存在于工作流中");
        }
        connections.computeIfAbsent(fromNodeId, k -> new HashSet<>()).add(toNodeId);
    }
    
    /**
     * 移除连接
     */
    public void removeConnection(String fromNodeId, String toNodeId) {
        Set<String> targets = connections.get(fromNodeId);
        if (targets != null) {
            targets.remove(toNodeId);
        }
    }
    
    /**
     * 获取节点
     */
    public WorkflowNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }
    
    /**
     * 获取所有节点
     */
    public Collection<WorkflowNode> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }
    
    /**
     * 获取节点的目标节点
     */
    public Set<String> getTargetNodes(String nodeId) {
        return new HashSet<>(connections.getOrDefault(nodeId, Collections.emptySet()));
    }
    
    /**
     * 获取节点的源节点
     */
    public Set<String> getSourceNodes(String nodeId) {
        Set<String> sources = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : connections.entrySet()) {
            if (entry.getValue().contains(nodeId)) {
                sources.add(entry.getKey());
            }
        }
        return sources;
    }
    
    /**
     * 获取起始节点（没有输入连接的节点）
     */
    public List<String> getStartNodes() {
        List<String> startNodes = new ArrayList<>();
        for (String nodeId : nodes.keySet()) {
            if (getSourceNodes(nodeId).isEmpty()) {
                startNodes.add(nodeId);
            }
        }
        return startNodes;
    }
    
    /**
     * 获取结束节点（没有输出连接的节点）
     */
    public List<String> getEndNodes() {
        List<String> endNodes = new ArrayList<>();
        for (String nodeId : nodes.keySet()) {
            if (getTargetNodes(nodeId).isEmpty()) {
                endNodes.add(nodeId);
            }
        }
        return endNodes;
    }
    
    /**
     * 验证工作流
     */
    public WorkflowValidationResult validate() {
        WorkflowValidationResult.Builder builder = WorkflowValidationResult.builder();
        
        // 检查是否有起始节点
        List<String> startNodes = getStartNodes();
        if (startNodes.isEmpty()) {
            builder.error("工作流必须至少有一个起始节点");
        }
        
        // 检查是否有结束节点
        List<String> endNodes = getEndNodes();
        if (endNodes.isEmpty()) {
            builder.warning("工作流没有结束节点，可能会导致执行问题");
        }
        
        // 检查循环依赖
        if (hasCycles()) {
            builder.error("工作流包含循环依赖");
        }
        
        // 验证每个节点
        for (WorkflowNode node : nodes.values()) {
            var nodeValidation = node.validate();
            if (!nodeValidation.isValid()) {
                builder.error("节点 " + node.getId() + " 验证失败: " + 
                    String.join(", ", nodeValidation.getErrors()));
            }
            nodeValidation.getWarnings().forEach(warning -> 
                builder.warning("节点 " + node.getId() + ": " + warning));
        }
        
        return builder.build();
    }
    
    /**
     * 检查是否存在循环依赖
     */
    public boolean hasCycles() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String nodeId : nodes.keySet()) {
            if (hasCyclesUtil(nodeId, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasCyclesUtil(String nodeId, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(nodeId)) {
            return true;
        }
        if (visited.contains(nodeId)) {
            return false;
        }
        
        visited.add(nodeId);
        recursionStack.add(nodeId);
        
        for (String target : getTargetNodes(nodeId)) {
            if (hasCyclesUtil(target, visited, recursionStack)) {
                return true;
            }
        }
        
        recursionStack.remove(nodeId);
        return false;
    }
    
    /**
     * 获取拓扑排序
     */
    public List<String> getTopologicalOrder() {
        Map<String, Integer> inDegree = new HashMap<>();
        
        // 计算入度
        for (String nodeId : nodes.keySet()) {
            inDegree.put(nodeId, 0);
        }
        for (Set<String> targets : connections.values()) {
            for (String target : targets) {
                inDegree.put(target, inDegree.get(target) + 1);
            }
        }
        
        // 拓扑排序
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        
        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);
            
            for (String target : getTargetNodes(current)) {
                inDegree.put(target, inDegree.get(target) - 1);
                if (inDegree.get(target) == 0) {
                    queue.offer(target);
                }
            }
        }
        
        return result;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    public int getNodeCount() {
        return nodes.size();
    }
    
    public int getConnectionCount() {
        return connections.values().stream().mapToInt(Set::size).sum();
    }
}
