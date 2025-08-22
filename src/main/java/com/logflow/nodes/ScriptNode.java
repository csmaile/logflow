package com.logflow.nodes;

import com.logflow.core.*;

import javax.script.*;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 脚本节点
 * 支持执行动态脚本（JavaScript、Groovy等）
 */
public class ScriptNode extends AbstractWorkflowNode {

    private static final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    public ScriptNode(String id, String name) {
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

            // 处理输入数据（多输入）
            InputDataProcessor.InputDataResult inputResult = processInputData(context);
            if (!inputResult.isSuccess()) {
                throw new WorkflowException(id, "脚本节点多输入处理失败: " + inputResult.getErrorMessage());
            }

            Object inputData = inputResult.getData();
            Map<String, Object> inputMetadata = inputResult.getMetadata();

            // 准备脚本上下文
            Bindings bindings = engine.createBindings();
            setupEnhancedScriptContext(bindings, context, inputData, inputMetadata);

            // 执行脚本
            long startTime = System.currentTimeMillis();
            Object result = engine.eval(script, bindings);
            long duration = System.currentTimeMillis() - startTime;

            // 设置输出数据
            setOutputData(context, result);

            logger.info("脚本执行完成, 引擎: {}, 耗时: {}ms, 输入模式: {}",
                    scriptEngine, duration, inputMetadata.get("inputMode"));

            return NodeExecutionResult.builder(id)
                    .success(true)
                    .data(result)
                    .executionDuration(duration)
                    .metadata("scriptEngine", scriptEngine)
                    .metadata("scriptLength", script.length())
                    .metadata("inputMode", inputMetadata.get("inputMode"))
                    .build();

        } catch (ScriptException e) {
            throw new WorkflowException(id, "脚本执行错误: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new WorkflowException(id, "脚本节点执行失败", e);
        }
    }

    @Override
    public ValidationResult validate() {
        ValidationResult.Builder builder = ValidationResult.builder();

        String script = getConfigValue("script", String.class);
        if (script == null || script.trim().isEmpty()) {
            builder.error("必须配置脚本内容 (script)");
        }

        String scriptEngine = getConfigValue("scriptEngine", String.class, "javascript");
        if (getScriptEngine(scriptEngine) == null) {
            builder.error("不支持的脚本引擎: " + scriptEngine);
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
                    jsEngine = scriptEngineManager.getEngineByName("js");
                }
                return jsEngine;
            case "groovy":
                return scriptEngineManager.getEngineByName("groovy");
            case "python":
                return scriptEngineManager.getEngineByName("python");
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
        bindings.put("logger", new ScriptLogger());
        bindings.put("utils", new ScriptUtils());

        // 添加常用的Java类
        bindings.put("System", System.class);
        bindings.put("Math", Math.class);
        bindings.put("String", String.class);

        // 输入数据和元数据
        bindings.put("input", inputData);
        bindings.put("data", inputData); // 向后兼容
        if (inputMetadata != null) {
            bindings.put("inputMetadata", inputMetadata);
        }

        // 根据输入模式设置特定变量
        if (inputMetadata != null) {
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

        // 添加配置参数
        Map<String, Object> scriptParams = getConfigValue("parameters", Map.class, new HashMap<>());
        bindings.put("params", scriptParams);
    }

    /**
     * 设置脚本执行上下文（向后兼容）
     */
    private void setupScriptContext(Bindings bindings, WorkflowContext context, String inputKey) {
        // 添加输入数据
        Object inputData = context.getData(inputKey);
        bindings.put("input", inputData);
        bindings.put("data", inputData);

        // 添加上下文访问
        bindings.put("context", new ScriptContextWrapper(context));

        // 添加工具函数
        bindings.put("logger", new ScriptLogger());
        bindings.put("utils", new ScriptUtils());

        // 添加常用的Java类
        bindings.put("System", System.class);
        bindings.put("Math", Math.class);
        bindings.put("String", String.class);

        // 添加配置参数
        Map<String, Object> scriptParams = getConfigValue("parameters", Map.class, new HashMap<>());
        bindings.put("params", scriptParams);
    }

    /**
     * 脚本上下文包装器
     * 为脚本提供安全的上下文访问
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

        public boolean has(String key) {
            return context.hasData(key);
        }

        public boolean hasData(String key) {
            return context.hasData(key);
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
     * 脚本日志记录器
     */
    public static class ScriptLogger {
        private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("ScriptNode");

        public void info(String message) {
            logger.info("[Script] {}", message);
        }

        public void warn(String message) {
            logger.warn("[Script] {}", message);
        }

        public void error(String message) {
            logger.error("[Script] {}", message);
        }

        public void debug(String message) {
            logger.debug("[Script] {}", message);
        }
    }

    /**
     * 脚本工具类
     */
    public static class ScriptUtils {

        /**
         * 字符串包含检查（忽略大小写）
         */
        public boolean containsIgnoreCase(String source, String target) {
            if (source == null || target == null) {
                return false;
            }
            return source.toLowerCase().contains(target.toLowerCase());
        }

        /**
         * 正则表达式匹配
         */
        public boolean matches(String text, String pattern) {
            if (text == null || pattern == null) {
                return false;
            }
            return text.matches(pattern);
        }

        /**
         * 提取数字
         */
        public Double extractNumber(String text) {
            if (text == null) {
                return null;
            }
            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[-+]?\\d*\\.?\\d+");
                java.util.regex.Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    return Double.parseDouble(matcher.group());
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
            return null;
        }

        /**
         * 当前时间戳
         */
        public long now() {
            return System.currentTimeMillis();
        }

        /**
         * 格式化字符串
         */
        public String format(String template, Object... args) {
            return String.format(template, args);
        }

        /**
         * JSON字符串解析（简化版）
         */
        public Object parseJson(String json) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(json, Object.class);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * 对象转JSON字符串
         */
        public String toJson(Object obj) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.writeValueAsString(obj);
            } catch (Exception e) {
                return obj.toString();
            }
        }
    }
}
