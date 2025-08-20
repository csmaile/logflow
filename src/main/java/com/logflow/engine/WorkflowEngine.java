package com.logflow.engine;

import com.logflow.core.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 工作流引擎
 * 使用JGraphT进行图遍历和执行
 */
public class WorkflowEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkflowEngine.class);
    
    private final ExecutorService executorService;
    private final boolean parallelExecution;
    private final int maxConcurrentNodes;
    
    public WorkflowEngine() {
        this(true, Runtime.getRuntime().availableProcessors());
    }
    
    public WorkflowEngine(boolean parallelExecution, int maxConcurrentNodes) {
        this.parallelExecution = parallelExecution;
        this.maxConcurrentNodes = maxConcurrentNodes;
        this.executorService = parallelExecution ? 
            Executors.newFixedThreadPool(maxConcurrentNodes) : null;
    }
    
    /**
     * 执行工作流
     */
    public WorkflowExecutionResult execute(Workflow workflow, Map<String, Object> initialData) {
        String executionId = generateExecutionId();
        logger.info("开始执行工作流: {} (执行ID: {})", workflow.getName(), executionId);
        
        // 验证工作流
        WorkflowValidationResult validation = workflow.validate();
        if (!validation.isValid()) {
            return WorkflowExecutionResult.failure(executionId, 
                "工作流验证失败: " + String.join(", ", validation.getErrors()));
        }
        
        // 创建执行上下文
        WorkflowContext context = new WorkflowContext(workflow.getId(), executionId);
        if (initialData != null) {
            initialData.forEach(context::setData);
        }
        
        try {
            // 构建JGraphT图
            Graph<String, DefaultEdge> graph = buildGraph(workflow);
            
            // 执行工作流
            Map<String, NodeExecutionResult> nodeResults = new HashMap<>();
            
            if (parallelExecution) {
                nodeResults = executeParallel(workflow, graph, context);
            } else {
                nodeResults = executeSequential(workflow, graph, context);
            }
            
            // 分析执行结果
            boolean success = nodeResults.values().stream().allMatch(NodeExecutionResult::isSuccess);
            
            logger.info("工作流执行完成: {} 成功: {}", executionId, success);
            
            return WorkflowExecutionResult.builder(executionId)
                    .success(success)
                    .nodeResults(nodeResults)
                    .context(context)
                    .build();
                    
        } catch (Exception e) {
            logger.error("工作流执行失败: {}", executionId, e);
            return WorkflowExecutionResult.failure(executionId, "执行异常: " + e.getMessage());
        }
    }
    
    /**
     * 构建JGraphT图
     */
    private Graph<String, DefaultEdge> buildGraph(Workflow workflow) {
        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        
        // 添加节点
        for (WorkflowNode node : workflow.getAllNodes()) {
            graph.addVertex(node.getId());
        }
        
        // 添加边
        for (WorkflowNode node : workflow.getAllNodes()) {
            Set<String> targetNodes = workflow.getTargetNodes(node.getId());
            for (String targetNodeId : targetNodes) {
                graph.addEdge(node.getId(), targetNodeId);
            }
        }
        
        return graph;
    }
    
    /**
     * 顺序执行
     */
    private Map<String, NodeExecutionResult> executeSequential(
            Workflow workflow, Graph<String, DefaultEdge> graph, WorkflowContext context) {
        
        Map<String, NodeExecutionResult> results = new HashMap<>();
        TopologicalOrderIterator<String, DefaultEdge> iterator = 
            new TopologicalOrderIterator<>(graph);
        
        while (iterator.hasNext()) {
            String nodeId = iterator.next();
            WorkflowNode node = workflow.getNode(nodeId);
            
            try {
                // 检查前置节点是否都执行成功
                if (!checkPredecessors(graph, nodeId, results)) {
                    results.put(nodeId, NodeExecutionResult.failure(nodeId, "前置节点执行失败"));
                    continue;
                }
                
                // 执行节点
                NodeExecutionResult result = node.execute(context);
                results.put(nodeId, result);
                
                if (!result.isSuccess()) {
                    logger.warn("节点执行失败: {} - {}", nodeId, result.getMessage());
                    // 可以根据配置决定是否继续执行
                }
                
            } catch (Exception e) {
                logger.error("节点执行异常: {}", nodeId, e);
                results.put(nodeId, NodeExecutionResult.failure(nodeId, "执行异常: " + e.getMessage()));
            }
        }
        
        return results;
    }
    
    /**
     * 并行执行
     */
    private Map<String, NodeExecutionResult> executeParallel(
            Workflow workflow, Graph<String, DefaultEdge> graph, WorkflowContext context) {
        
        Map<String, NodeExecutionResult> results = new ConcurrentHashMap<>();
        Map<String, Future<NodeExecutionResult>> futures = new HashMap<>();
        Set<String> completedNodes = ConcurrentHashMap.newKeySet();
        
        // 获取可执行的节点队列
        Queue<String> readyNodes = new LinkedList<>();
        Set<String> allNodes = new HashSet<>(graph.vertexSet());
        
        // 初始化：添加没有前置依赖的节点
        for (String nodeId : allNodes) {
            if (graph.inDegreeOf(nodeId) == 0) {
                readyNodes.offer(nodeId);
            }
        }
        
        while (!readyNodes.isEmpty() || !futures.isEmpty()) {
            // 提交可执行的节点
            while (!readyNodes.isEmpty()) {
                String nodeId = readyNodes.poll();
                WorkflowNode node = workflow.getNode(nodeId);
                
                Future<NodeExecutionResult> future = executorService.submit(() -> {
                    try {
                        return node.execute(context);
                    } catch (Exception e) {
                        logger.error("节点执行异常: {}", nodeId, e);
                        return NodeExecutionResult.failure(nodeId, "执行异常: " + e.getMessage());
                    }
                });
                
                futures.put(nodeId, future);
            }
            
            // 等待至少一个节点完成
            String completedNodeId = waitForAnyCompletion(futures, results);
            if (completedNodeId != null) {
                completedNodes.add(completedNodeId);
                futures.remove(completedNodeId);
                
                // 检查是否有新的节点可以执行
                for (String nodeId : allNodes) {
                    if (!completedNodes.contains(nodeId) && 
                        !futures.containsKey(nodeId) && 
                        !readyNodes.contains(nodeId)) {
                        
                        // 检查所有前置节点是否已完成
                        boolean allPredecessorsCompleted = true;
                        for (DefaultEdge edge : graph.incomingEdgesOf(nodeId)) {
                            String sourceNode = graph.getEdgeSource(edge);
                            if (!completedNodes.contains(sourceNode)) {
                                allPredecessorsCompleted = false;
                                break;
                            }
                        }
                        
                        if (allPredecessorsCompleted) {
                            readyNodes.offer(nodeId);
                        }
                    }
                }
            }
        }
        
        return results;
    }
    
    /**
     * 等待任意一个任务完成
     */
    private String waitForAnyCompletion(Map<String, Future<NodeExecutionResult>> futures, 
                                       Map<String, NodeExecutionResult> results) {
        while (!futures.isEmpty()) {
            for (Map.Entry<String, Future<NodeExecutionResult>> entry : futures.entrySet()) {
                if (entry.getValue().isDone()) {
                    try {
                        NodeExecutionResult result = entry.getValue().get();
                        results.put(entry.getKey(), result);
                        return entry.getKey();
                    } catch (Exception e) {
                        logger.error("获取节点执行结果失败: {}", entry.getKey(), e);
                        results.put(entry.getKey(), 
                            NodeExecutionResult.failure(entry.getKey(), "获取结果失败: " + e.getMessage()));
                        return entry.getKey();
                    }
                }
            }
            
            // 短暂等待
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }
    
    /**
     * 检查前置节点是否都执行成功
     */
    private boolean checkPredecessors(Graph<String, DefaultEdge> graph, String nodeId, 
                                    Map<String, NodeExecutionResult> results) {
        for (DefaultEdge edge : graph.incomingEdgesOf(nodeId)) {
            String sourceNode = graph.getEdgeSource(edge);
            NodeExecutionResult result = results.get(sourceNode);
            if (result == null || !result.isSuccess()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 生成执行ID
     */
    private String generateExecutionId() {
        return "exec_" + System.currentTimeMillis() + "_" + 
               Thread.currentThread().getId();
    }
    
    /**
     * 停止执行器
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
