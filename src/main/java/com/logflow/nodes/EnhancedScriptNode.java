package com.logflow.nodes;

import com.logflow.core.*;

import javax.script.*;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 增强脚本节点
 * 支持多输入参数和原有的单输入模式
 */
public class EnhancedScriptNode extends AbstractWorkflowNode {

    private static final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    public EnhancedScriptNode(String id, String name) {
        super(id, name, NodeType.SCRIPT);
    }

    @Override
    protected NodeExecutionResult doExecute(WorkflowContext context) throws WorkflowException {
        String scriptEngine = getConfigValue("scriptEngine", String.class, "javascript");
        String script = getConfigValue("script", String.class);

        if (script == null || script.trim().isEmpty()) {
            throw new WorkflowException(id, "脚本内容不能为空");
        }

        try {
            // 获取脚本引擎
            ScriptEngine engine = getScriptEngine(scriptEngine);
            if (engine == null) {
                throw new WorkflowException(id, "不支持的脚本引擎: " + scriptEngine);
            }

            // 处理输入数据（支持多输入）
            InputDataProcessor.InputDataResult inputResult = processInputData(context);
            if (!inputResult.isSuccess()) {
                throw new WorkflowException(id, "获取输入数据失败: " + inputResult.getErrorMessage());
            }

            Object inputData = inputResult.getData();
            Map<String, Object> inputMetadata = inputResult.getMetadata();

            logger.debug("脚本节点 {} 获取输入数据，模式: {}, 元数据: {}",
                    id, inputMetadata.get("inputMode"), inputMetadata);

            // 准备脚本上下文
            Bindings bindings = engine.createBindings();
            setupEnhancedScriptContext(bindings, context, inputData, inputMetadata);

            // 执行脚本
            long startTime = System.currentTimeMillis();
            Object result = engine.eval(script, bindings);
            long duration = System.currentTimeMillis() - startTime;

            // 处理脚本执行结果
            setOutputData(context, result);

            logger.info("增强脚本执行完成, 引擎: {}, 耗时: {}ms, 输入模式: {}",
                    scriptEngine, duration, inputMetadata.get("inputMode"));

            return NodeExecutionResult.builder(id)
                    .success(true)
                    .data(result)
                    .executionDuration(duration)
                    .metadata("scriptEngine", scriptEngine)
                    .metadata("scriptLength", script.length())
                    .metadata("inputMode", inputMetadata.get("inputMode"))
                    .metadata("inputMetadata", inputMetadata)
                    .build();

        } catch (ScriptException e) {
            throw new WorkflowException(id, "脚本执行错误: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new WorkflowException(id, "增强脚本节点执行失败", e);
        }
    }

    @Override
    public ValidationResult validate() {
        ValidationResult.Builder builder = ValidationResult.builder();

        // 验证脚本内容
        String script = getConfigValue("script", String.class);
        if (script == null || script.trim().isEmpty()) {
            builder.error("script: 脚本内容不能为空");
        }

        // 验证脚本引擎
        String scriptEngine = getConfigValue("scriptEngine", String.class, "javascript");
        if (getScriptEngine(scriptEngine) == null) {
            builder.error("scriptEngine: 不支持的脚本引擎: " + scriptEngine);
        }

        // 验证输入配置
        MultiInputConfig inputConfig = InputDataProcessor.extractInputConfig(configuration);
        if (!inputConfig.isValid()) {
            builder.error("输入配置无效，请检查 inputKey 或 inputs 配置");
        }

        // 验证输出配置
        String outputKey = getConfigValue("outputKey", String.class);
        if (outputKey == null || outputKey.trim().isEmpty()) {
            builder.warning("outputKey: 未配置输出键，脚本结果将不会保存到上下文");
        }

        return builder.build();
    }

    /**
     * 获取脚本引擎
     */
    private ScriptEngine getScriptEngine(String engineName) {
        switch (engineName.toLowerCase()) {
            case "javascript":
            case "js":
                // 尝试不同的JavaScript引擎
                ScriptEngine jsEngine = scriptEngineManager.getEngineByName("nashorn");
                if (jsEngine == null) {
                    jsEngine = scriptEngineManager.getEngineByName("javascript");
                }
                if (jsEngine == null) {
                    jsEngine = scriptEngineManager.getEngineByName("graal.js");
                }
                return jsEngine;
            default:
                return scriptEngineManager.getEngineByName(engineName);
        }
    }

    /**
     * 设置增强脚本上下文（支持多输入）
     */
    private void setupEnhancedScriptContext(Bindings bindings, WorkflowContext context,
            Object inputData, Map<String, Object> inputMetadata) {

        // 基础变量
        bindings.put("context", new ScriptContextWrapper(context));
        bindings.put("logger", new ScriptLoggerWrapper(logger));
        bindings.put("utils", new ScriptUtils());

        // 输入数据和元数据
        bindings.put("input", inputData);
        bindings.put("inputMetadata", inputMetadata);

        // 根据输入模式设置特定变量
        String inputMode = (String) inputMetadata.get("inputMode");
        if ("MULTIPLE".equals(inputMode) && inputData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> multiInputs = (Map<String, Object>) inputData;

            // 将每个输入参数作为单独的变量暴露给脚本
            multiInputs.forEach(bindings::put);

            logger.debug("脚本节点 {} 设置多输入变量: {}", id, multiInputs.keySet());
        } else if ("MERGED".equals(inputMode)) {
            // 合并模式下，输入数据放在指定键下
            MultiInputConfig inputConfig = InputDataProcessor.extractInputConfig(configuration);
            String mergeKey = inputConfig.getMergeKey();
            bindings.put(mergeKey, inputData);

            logger.debug("脚本节点 {} 设置合并输入变量: {} -> {}", id, mergeKey, inputData);
        }
    }

    /**
     * 脚本上下文包装器
     */
    public static class ScriptContextWrapper {
        private final WorkflowContext context;

        public ScriptContextWrapper(WorkflowContext context) {
            this.context = context;
        }

        public Object get(String key) {
            return context.getData(key);
        }

        public Object getData(String key) {
            return context.getData(key);
        }

        public void set(String key, Object value) {
            context.setData(key, value);
        }

        public void setData(String key, Object value) {
            context.setData(key, value);
        }

        public String getWorkflowId() {
            return context.getWorkflowId();
        }

        public String getExecutionId() {
            return context.getExecutionId();
        }

        public Map<String, Object> getAllData() {
            return context.getAllData();
        }
    }

    /**
     * 脚本日志包装器
     */
    public static class ScriptLoggerWrapper {
        private final org.slf4j.Logger logger;

        public ScriptLoggerWrapper(org.slf4j.Logger logger) {
            this.logger = logger;
        }

        public void debug(String message) {
            logger.debug("[脚本] " + message);
        }

        public void info(String message) {
            logger.info("[脚本] " + message);
        }

        public void warn(String message) {
            logger.warn("[脚本] " + message);
        }

        public void error(String message) {
            logger.error("[脚本] " + message);
        }
    }

    /**
     * 脚本工具类
     */
    public static class ScriptUtils {
        public String now() {
            return java.time.LocalDateTime.now().toString();
        }

        public long timestamp() {
            return System.currentTimeMillis();
        }

        public String uuid() {
            return java.util.UUID.randomUUID().toString();
        }

        public String format(String template, Object... args) {
            return String.format(template, args);
        }

        public Map<String, Object> map() {
            return new HashMap<>();
        }

        public java.util.List<Object> list() {
            return new java.util.ArrayList<>();
        }

        public String toJson(Object obj) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.writeValueAsString(obj);
            } catch (Exception e) {
                return obj.toString();
            }
        }

        public Object fromJson(String json, Class<?> clazz) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(json, clazz);
            } catch (Exception e) {
                throw new RuntimeException("JSON解析失败: " + e.getMessage(), e);
            }
        }
    }
}
