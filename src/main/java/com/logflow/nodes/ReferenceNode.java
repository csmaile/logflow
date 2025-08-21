package com.logflow.nodes;

import com.logflow.core.*;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;
import com.logflow.engine.WorkflowExecutionResult;
import com.logflow.registry.WorkflowRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 关联节点
 * 用于引用和执行其他工作流，实现工作流的组合和复用
 */
public class ReferenceNode extends AbstractWorkflowNode {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceNode.class);

    private final WorkflowRegistry workflowRegistry;
    private final WorkflowEngine workflowEngine;

    // 执行模式枚举
    public enum ExecutionMode {
        SYNC, // 同步执行
        ASYNC, // 异步执行
        CONDITIONAL, // 条件执行
        LOOP, // 循环执行
        PARALLEL // 并行执行多个工作流
    }

    // 执行状态枚举
    public enum ExecutionStatus {
        PENDING, // 等待执行
        RUNNING, // 正在执行
        COMPLETED, // 执行完成
        FAILED, // 执行失败
        SKIPPED, // 跳过执行
        TIMEOUT // 执行超时
    }

    public ReferenceNode(String id, String name) {
        super(id, name, NodeType.REFERENCE);
        this.workflowRegistry = WorkflowRegistry.getInstance();
        this.workflowEngine = new WorkflowEngine();
    }

    @Override
    protected NodeExecutionResult doExecute(WorkflowContext context) throws WorkflowException {
        logger.info("开始执行关联节点: {}", id);

        ExecutionMode mode = getExecutionMode();

        try {
            switch (mode) {
                case SYNC:
                    return executeSynchronous(context);
                case ASYNC:
                    return executeAsynchronous(context);
                case CONDITIONAL:
                    return executeConditional(context);
                case LOOP:
                    return executeLoop(context);
                case PARALLEL:
                    return executeParallel(context);
                default:
                    throw new WorkflowException(id, "不支持的执行模式: " + mode);
            }
        } catch (Exception e) {
            logger.error("关联节点执行失败: {}", id, e);
            throw new WorkflowException(id, "关联节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 同步执行模式
     */
    private NodeExecutionResult executeSynchronous(WorkflowContext context) throws WorkflowException {
        String workflowId = getConfigValue("workflowId", String.class);
        if (workflowId == null) {
            throw new WorkflowException(id, "未指定要执行的工作流ID");
        }

        logger.debug("同步执行工作流: {}", workflowId);

        // 获取工作流
        Workflow targetWorkflow = workflowRegistry.getWorkflow(workflowId);
        if (targetWorkflow == null) {
            throw new WorkflowException(id, "找不到工作流: " + workflowId);
        }

        // 准备执行参数
        Map<String, Object> executionParams = prepareExecutionParameters(context);

        // 执行工作流
        WorkflowExecutionResult result = workflowEngine.execute(targetWorkflow, executionParams);

        // 处理执行结果
        return processExecutionResult(context, result, workflowId);
    }

    /**
     * 异步执行模式
     */
    private NodeExecutionResult executeAsynchronous(WorkflowContext context) throws WorkflowException {
        String workflowId = getConfigValue("workflowId", String.class);
        if (workflowId == null) {
            throw new WorkflowException(id, "未指定要执行的工作流ID");
        }

        logger.debug("异步执行工作流: {}", workflowId);

        // 获取工作流
        Workflow targetWorkflow = workflowRegistry.getWorkflow(workflowId);
        if (targetWorkflow == null) {
            throw new WorkflowException(id, "找不到工作流: " + workflowId);
        }

        // 准备执行参数
        Map<String, Object> executionParams = prepareExecutionParameters(context);

        // 异步执行工作流
        CompletableFuture<WorkflowExecutionResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                return workflowEngine.execute(targetWorkflow, executionParams);
            } catch (Exception e) {
                throw new RuntimeException("异步执行工作流失败", e);
            }
        });

        // 检查是否需要等待结果
        boolean waitForResult = getConfigValue("waitForResult", Boolean.class, false);
        long timeoutMs = getConfigValue("timeoutMs", Number.class, 30000).longValue();

        if (waitForResult) {
            try {
                WorkflowExecutionResult result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                return processExecutionResult(context, result, workflowId);
            } catch (Exception e) {
                throw new WorkflowException(id, "异步执行超时或失败: " + e.getMessage(), e);
            }
        } else {
            // 保存Future引用以供后续查询
            String futureKey = id + "_future";
            context.setData(futureKey, future);

            return NodeExecutionResult.success("异步工作流已启动", Map.of(
                    "workflowId", workflowId,
                    "executionMode", "ASYNC",
                    "futureKey", futureKey,
                    "status", ExecutionStatus.RUNNING));
        }
    }

    /**
     * 条件执行模式
     */
    private NodeExecutionResult executeConditional(WorkflowContext context) throws WorkflowException {
        String condition = getConfigValue("condition", String.class);
        if (condition == null) {
            throw new WorkflowException(id, "条件执行模式需要指定条件表达式");
        }

        logger.debug("条件执行模式，条件: {}", condition);

        // 评估条件
        boolean shouldExecute = evaluateCondition(condition, context);

        if (!shouldExecute) {
            logger.debug("条件不满足，跳过执行");
            return NodeExecutionResult.success("条件不满足，已跳过执行", Map.of(
                    "condition", condition,
                    "status", ExecutionStatus.SKIPPED));
        }

        // 条件满足，执行工作流
        return executeSynchronous(context);
    }

    /**
     * 循环执行模式
     */
    private NodeExecutionResult executeLoop(WorkflowContext context) throws WorkflowException {
        String workflowId = getConfigValue("workflowId", String.class);
        if (workflowId == null) {
            throw new WorkflowException(id, "未指定要执行的工作流ID");
        }

        String loopCondition = getConfigValue("loopCondition", String.class);
        String loopDataKey = getConfigValue("loopDataKey", String.class);
        int maxIterations = getConfigValue("maxIterations", Integer.class, 100);

        logger.debug("循环执行工作流: {}, 最大迭代次数: {}", workflowId, maxIterations);

        Workflow targetWorkflow = workflowRegistry.getWorkflow(workflowId);
        if (targetWorkflow == null) {
            throw new WorkflowException(id, "找不到工作流: " + workflowId);
        }

        List<Map<String, Object>> loopResults = new ArrayList<>();
        int iteration = 0;

        // 获取循环数据
        Object loopData = null;
        if (loopDataKey != null) {
            loopData = context.getData(loopDataKey);
        }

        if (loopData instanceof List) {
            // 基于数据列表的循环
            List<?> dataList = (List<?>) loopData;
            for (Object item : dataList) {
                if (iteration >= maxIterations) {
                    logger.warn("达到最大迭代次数限制: {}", maxIterations);
                    break;
                }

                Map<String, Object> iterationParams = prepareExecutionParameters(context);
                iterationParams.put("loopItem", item);
                iterationParams.put("loopIndex", iteration);

                WorkflowExecutionResult result = workflowEngine.execute(targetWorkflow, iterationParams);
                loopResults.add(createIterationResult(iteration, result));

                iteration++;
            }
        } else {
            // 基于条件的循环
            while (iteration < maxIterations) {
                if (loopCondition != null && !evaluateCondition(loopCondition, context)) {
                    break;
                }

                Map<String, Object> iterationParams = prepareExecutionParameters(context);
                iterationParams.put("loopIndex", iteration);

                WorkflowExecutionResult result = workflowEngine.execute(targetWorkflow, iterationParams);
                loopResults.add(createIterationResult(iteration, result));

                // 更新上下文，供下次条件评估使用
                if (result.isSuccess() && result.getContext() != null) {
                    context.setData("lastLoopResult", result.getContext().getAllData());
                }

                iteration++;
            }
        }

        // 汇总循环结果
        return NodeExecutionResult.success("循环执行完成", Map.of(
                "workflowId", workflowId,
                "totalIterations", iteration,
                "loopResults", loopResults,
                "status", ExecutionStatus.COMPLETED));
    }

    /**
     * 并行执行模式
     */
    private NodeExecutionResult executeParallel(WorkflowContext context) throws WorkflowException {
        @SuppressWarnings("unchecked")
        List<String> workflowIds = (List<String>) getConfigValue("workflowIds", List.class);
        if (workflowIds == null || workflowIds.isEmpty()) {
            throw new WorkflowException(id, "并行执行模式需要指定工作流ID列表");
        }

        logger.debug("并行执行 {} 个工作流", workflowIds.size());

        // 验证所有工作流都存在
        for (String workflowId : workflowIds) {
            if (workflowRegistry.getWorkflow(workflowId) == null) {
                throw new WorkflowException(id, "找不到工作流: " + workflowId);
            }
        }

        // 准备并行执行
        Map<String, Object> executionParams = prepareExecutionParameters(context);
        List<CompletableFuture<WorkflowExecutionResult>> futures = new ArrayList<>();

        // 启动所有工作流
        for (String workflowId : workflowIds) {
            Workflow targetWorkflow = workflowRegistry.getWorkflow(workflowId);

            CompletableFuture<WorkflowExecutionResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return workflowEngine.execute(targetWorkflow, executionParams);
                } catch (Exception e) {
                    throw new RuntimeException("并行执行工作流失败: " + workflowId, e);
                }
            });

            futures.add(future);
        }

        // 等待所有工作流完成
        long timeoutMs = getConfigValue("parallelTimeoutMs", Number.class, 60000).longValue();

        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));

            allFutures.get(timeoutMs, TimeUnit.MILLISECONDS);

            // 收集所有结果
            Map<String, Object> parallelResults = new HashMap<>();
            for (int i = 0; i < workflowIds.size(); i++) {
                String workflowId = workflowIds.get(i);
                WorkflowExecutionResult result = futures.get(i).get();
                parallelResults.put(workflowId, createParallelResult(workflowId, result));
            }

            return NodeExecutionResult.success("并行执行完成", Map.of(
                    "workflowIds", workflowIds,
                    "parallelResults", parallelResults,
                    "status", ExecutionStatus.COMPLETED));

        } catch (Exception e) {
            throw new WorkflowException(id, "并行执行超时或失败: " + e.getMessage(), e);
        }
    }

    /**
     * 准备执行参数
     */
    private Map<String, Object> prepareExecutionParameters(WorkflowContext context) {
        Map<String, Object> params = new HashMap<>();

        // 传递输入参数
        Map<String, String> inputMappings = getInputMappings();
        for (Map.Entry<String, String> mapping : inputMappings.entrySet()) {
            String sourceKey = mapping.getKey();
            String targetKey = mapping.getValue();
            Object value = context.getData(sourceKey);
            if (value != null) {
                params.put(targetKey, value);
            }
        }

        // 传递固定参数
        Map<String, Object> fixedParams = getFixedParameters();
        params.putAll(fixedParams);

        // 添加元数据
        params.put("_sourceWorkflowId", context.getWorkflowId());
        params.put("_sourceExecutionId", context.getExecutionId());
        params.put("_referenceNodeId", id);

        return params;
    }

    /**
     * 处理执行结果
     */
    private NodeExecutionResult processExecutionResult(WorkflowContext context,
            WorkflowExecutionResult result,
            String workflowId) throws WorkflowException {
        // 映射输出数据
        Map<String, String> outputMappings = getOutputMappings();
        if (result.getContext() != null) {
            Map<String, Object> resultData = result.getContext().getAllData();
            for (Map.Entry<String, String> mapping : outputMappings.entrySet()) {
                String sourceKey = mapping.getKey();
                String targetKey = mapping.getValue();
                Object value = resultData.get(sourceKey);
                if (value != null) {
                    context.setData(targetKey, value);
                }
            }
        }

        // 记录执行信息
        Map<String, Object> executionInfo = Map.of(
                "workflowId", workflowId,
                "success", result.isSuccess(),
                "executionTime", result.getExecutionDurationMs(),
                "status", result.isSuccess() ? ExecutionStatus.COMPLETED : ExecutionStatus.FAILED);

        if (result.isSuccess()) {
            return NodeExecutionResult.success(
                    "关联工作流执行成功: " + workflowId,
                    executionInfo);
        } else {
            throw new WorkflowException(id,
                    "关联工作流执行失败: " + workflowId + " - " + result.getMessage());
        }
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String condition, WorkflowContext context) {
        // 简单的条件评估实现
        // 支持基本的比较操作，如：data.count > 0, status == 'success' 等

        try {
            // 替换上下文变量
            String evaluatedCondition = replaceContextVariables(condition, context);

            // 简单的条件解析
            if (evaluatedCondition.contains(">")) {
                return evaluateComparison(evaluatedCondition, ">");
            } else if (evaluatedCondition.contains(">=")) {
                return evaluateComparison(evaluatedCondition, ">=");
            } else if (evaluatedCondition.contains("<")) {
                return evaluateComparison(evaluatedCondition, "<");
            } else if (evaluatedCondition.contains("<=")) {
                return evaluateComparison(evaluatedCondition, "<=");
            } else if (evaluatedCondition.contains("==")) {
                return evaluateEquality(evaluatedCondition, "==");
            } else if (evaluatedCondition.contains("!=")) {
                return evaluateEquality(evaluatedCondition, "!=");
            } else {
                // 布尔值或存在性检查
                return Boolean.parseBoolean(evaluatedCondition.trim());
            }

        } catch (Exception e) {
            logger.warn("条件评估失败: {}, 默认返回false", condition, e);
            return false;
        }
    }

    /**
     * 替换上下文变量
     */
    private String replaceContextVariables(String expression, WorkflowContext context) {
        String result = expression;

        // 查找 ${variable} 格式的变量
        while (result.contains("${")) {
            int start = result.indexOf("${");
            int end = result.indexOf("}", start);

            if (end > start) {
                String variable = result.substring(start + 2, end);
                Object value = context.getData(variable);
                String replacement = value != null ? value.toString() : "null";
                result = result.replace("${" + variable + "}", replacement);
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * 评估比较表达式
     */
    private boolean evaluateComparison(String expression, String operator) {
        String[] parts = expression.split(operator);
        if (parts.length != 2) {
            return false;
        }

        try {
            double left = Double.parseDouble(parts[0].trim());
            double right = Double.parseDouble(parts[1].trim());

            switch (operator) {
                case ">":
                    return left > right;
                case ">=":
                    return left >= right;
                case "<":
                    return left < right;
                case "<=":
                    return left <= right;
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 评估相等表达式
     */
    private boolean evaluateEquality(String expression, String operator) {
        String[] parts = expression.split(operator);
        if (parts.length != 2) {
            return false;
        }

        String left = parts[0].trim();
        String right = parts[1].trim();

        // 移除引号
        left = left.replaceAll("^['\"]|['\"]$", "");
        right = right.replaceAll("^['\"]|['\"]$", "");

        boolean equal = left.equals(right);
        return "==".equals(operator) ? equal : !equal;
    }

    /**
     * 创建迭代结果
     */
    private Map<String, Object> createIterationResult(int iteration, WorkflowExecutionResult result) {
        return Map.of(
                "iteration", iteration,
                "success", result.isSuccess(),
                "executionTime", result.getExecutionDurationMs(),
                "message", result.getMessage() != null ? result.getMessage() : "");
    }

    /**
     * 创建并行结果
     */
    private Map<String, Object> createParallelResult(String workflowId, WorkflowExecutionResult result) {
        return Map.of(
                "workflowId", workflowId,
                "success", result.isSuccess(),
                "executionTime", result.getExecutionDurationMs(),
                "message", result.getMessage() != null ? result.getMessage() : "");
    }

    // 配置获取方法

    private ExecutionMode getExecutionMode() {
        String mode = getConfigValue("executionMode", String.class, "SYNC");
        try {
            return ExecutionMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("无效的执行模式: {}, 使用默认模式 SYNC", mode);
            return ExecutionMode.SYNC;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getInputMappings() {
        return (Map<String, String>) getConfigValue("inputMappings", Map.class, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getOutputMappings() {
        return (Map<String, String>) getConfigValue("outputMappings", Map.class, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getFixedParameters() {
        return (Map<String, Object>) getConfigValue("fixedParameters", Map.class, new HashMap<>());
    }

    @Override
    public ValidationResult validate() {
        ValidationResult.Builder builder = ValidationResult.builder();

        ExecutionMode mode = getExecutionMode();

        // 验证基本配置
        switch (mode) {
            case SYNC:
            case ASYNC:
                String workflowId = getConfigValue("workflowId", String.class);
                if (workflowId == null || workflowId.trim().isEmpty()) {
                    builder.error("执行模式 " + mode + " 需要指定 workflowId");
                }
                break;

            case CONDITIONAL:
                String condition = getConfigValue("condition", String.class);
                if (condition == null || condition.trim().isEmpty()) {
                    builder.error("条件执行模式需要指定 condition");
                }
                String condWorkflowId = getConfigValue("workflowId", String.class);
                if (condWorkflowId == null || condWorkflowId.trim().isEmpty()) {
                    builder.error("条件执行模式需要指定 workflowId");
                }
                break;

            case LOOP:
                String loopWorkflowId = getConfigValue("workflowId", String.class);
                if (loopWorkflowId == null || loopWorkflowId.trim().isEmpty()) {
                    builder.error("循环执行模式需要指定 workflowId");
                }
                break;

            case PARALLEL:
                @SuppressWarnings("unchecked")
                List<String> workflowIds = (List<String>) getConfigValue("workflowIds", List.class);
                if (workflowIds == null || workflowIds.isEmpty()) {
                    builder.error("并行执行模式需要指定 workflowIds 列表");
                }
                break;
        }

        return builder.build();
    }
}
