package com.logflow.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 输入数据处理器
 * 处理节点的多输入参数配置和数据获取
 */
public class InputDataProcessor {

    private static final Logger logger = LoggerFactory.getLogger(InputDataProcessor.class);

    /**
     * 输入数据结果
     */
    public static class InputDataResult {
        private final boolean success;
        private final Object data;
        private final String errorMessage;
        private final Map<String, Object> metadata;

        private InputDataResult(boolean success, Object data, String errorMessage, Map<String, Object> metadata) {
            this.success = success;
            this.data = data;
            this.errorMessage = errorMessage;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        public static InputDataResult success(Object data) {
            return new InputDataResult(true, data, null, null);
        }

        public static InputDataResult success(Object data, Map<String, Object> metadata) {
            return new InputDataResult(true, data, null, metadata);
        }

        public static InputDataResult failure(String errorMessage) {
            return new InputDataResult(false, null, errorMessage, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public Object getData() {
            return data;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    /**
     * 处理输入数据
     * 
     * @param context     工作流上下文
     * @param inputConfig 输入配置
     * @param nodeId      节点ID（用于日志）
     * @return 处理结果
     */
    public static InputDataResult processInputData(WorkflowContext context, MultiInputConfig inputConfig,
            String nodeId) {
        if (inputConfig == null) {
            return InputDataResult.failure("输入配置不能为空");
        }

        if (!inputConfig.isValid()) {
            return InputDataResult.failure("输入配置无效");
        }

        try {
            switch (inputConfig.getMode()) {
                case MULTIPLE:
                    return processMultipleInputs(context, inputConfig, nodeId);
                case MERGED:
                    return processMergedInputs(context, inputConfig, nodeId);
                default:
                    return InputDataResult.failure("不支持的输入模式: " + inputConfig.getMode());
            }
        } catch (Exception e) {
            logger.error("处理输入数据失败，节点: {}", nodeId, e);
            return InputDataResult.failure("处理输入数据异常: " + e.getMessage());
        }
    }

    /**
     * 处理多输入模式
     */
    private static InputDataResult processMultipleInputs(WorkflowContext context, MultiInputConfig inputConfig,
            String nodeId) {
        Map<String, Object> inputData = new HashMap<>();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("inputMode", "MULTIPLE");

        int requiredCount = 0;
        int availableCount = 0;
        int totalCount = inputConfig.getInputs().size();

        for (MultiInputConfig.InputParameter inputParam : inputConfig.getInputs()) {
            String key = inputParam.getKey();
            String alias = inputParam.getAlias();
            boolean required = inputParam.getRequired();
            Object defaultValue = inputParam.getDefaultValue();

            if (required) {
                requiredCount++;
            }

            Object data = context.getData(key);
            if (data != null) {
                availableCount++;
                inputData.put(alias, data);
                logger.debug("节点 {} 获取输入参数: {} -> {} (别名: {})", nodeId, key, data.getClass().getSimpleName(), alias);
            } else if (defaultValue != null) {
                inputData.put(alias, defaultValue);
                logger.debug("节点 {} 使用默认值: {} -> {} (别名: {})", nodeId, key, defaultValue, alias);
            } else if (required) {
                String errorMsg = String.format("必需的输入参数缺失: %s (别名: %s)", key, alias);
                logger.error("节点 {} {}", nodeId, errorMsg);
                return InputDataResult.failure(errorMsg);
            }
        }

        metadata.put("totalInputs", totalCount);
        metadata.put("requiredInputs", requiredCount);
        metadata.put("availableInputs", availableCount);

        logger.info("节点 {} 处理多输入完成: 总数={}, 必需={}, 可用={}", nodeId, totalCount, requiredCount, availableCount);

        return InputDataResult.success(inputData, metadata);
    }

    /**
     * 处理合并输入模式
     */
    private static InputDataResult processMergedInputs(WorkflowContext context, MultiInputConfig inputConfig,
            String nodeId) {
        // 先获取多输入数据
        InputDataResult multipleResult = processMultipleInputs(context, inputConfig, nodeId);
        if (!multipleResult.isSuccess()) {
            return multipleResult;
        }

        // 将所有输入合并到一个键下
        String mergeKey = inputConfig.getMergeKey();
        Object mergedData = multipleResult.getData();

        Map<String, Object> metadata = new HashMap<>(multipleResult.getMetadata());
        metadata.put("inputMode", "MERGED");
        metadata.put("mergeKey", mergeKey);

        logger.info("节点 {} 使用合并输入模式，合并键: {}", nodeId, mergeKey);

        return InputDataResult.success(mergedData, metadata);
    }

    /**
     * 从配置中提取MultiInputConfig
     * 
     * @param configuration 节点配置
     * @return MultiInputConfig对象
     */
    public static MultiInputConfig extractInputConfig(Map<String, Object> configuration) {
        MultiInputConfig inputConfig = new MultiInputConfig();

        // 解析输入参数列表
        Object inputsObj = configuration.get("inputs");
        if (inputsObj instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> inputsList = (java.util.List<Map<String, Object>>) inputsObj;
            java.util.List<MultiInputConfig.InputParameter> inputParams = new java.util.ArrayList<>();

            for (Map<String, Object> inputMap : inputsList) {
                MultiInputConfig.InputParameter param = new MultiInputConfig.InputParameter();
                param.setKey((String) inputMap.get("key"));
                param.setAlias((String) inputMap.get("alias"));
                param.setRequired((Boolean) inputMap.getOrDefault("required", true));
                param.setDefaultValue(inputMap.get("defaultValue"));
                param.setDataType((String) inputMap.getOrDefault("dataType", "object"));
                param.setDescription((String) inputMap.get("description"));
                inputParams.add(param);
            }
            inputConfig.setInputs(inputParams);
        }

        // 自动推断输入模式：有 mergeKey 就是 MERGED 模式，否则是 MULTIPLE 模式
        Object mergeKeyObj = configuration.get("mergeKey");
        if (mergeKeyObj != null && !mergeKeyObj.toString().trim().isEmpty()) {
            inputConfig.setMode(MultiInputConfig.InputMode.MERGED);
            inputConfig.setMergeKey(mergeKeyObj.toString());
        } else {
            inputConfig.setMode(MultiInputConfig.InputMode.MULTIPLE);
        }

        // 设置输出键
        Object outputKeyObj = configuration.get("outputKey");
        if (outputKeyObj != null) {
            inputConfig.setOutputKey(outputKeyObj.toString());
        }

        return inputConfig;
    }

    /**
     * 验证多输入配置
     * 
     * @param inputConfig 输入配置
     * @param nodeId      节点ID（用于错误消息）
     * @return 验证结果
     */
    public static ValidationResult validateInputConfig(MultiInputConfig inputConfig, String nodeId) {
        ValidationResult.Builder builder = ValidationResult.builder();

        if (inputConfig == null) {
            builder.error("输入配置不能为空");
            return builder.build();
        }

        MultiInputConfig.InputMode mode = inputConfig.getMode();
        if (mode == null) {
            builder.error("输入模式不能为空");
            return builder.build();
        }

        switch (mode) {
            case MULTIPLE:
            case MERGED:
                // 验证多输入/合并输入模式
                java.util.List<MultiInputConfig.InputParameter> inputs = inputConfig.getInputs();
                if (inputs == null || inputs.isEmpty()) {
                    builder.error("多输入模式下必须配置 inputs 列表");
                    return builder.build();
                }

                for (int i = 0; i < inputs.size(); i++) {
                    MultiInputConfig.InputParameter param = inputs.get(i);
                    if (param.getKey() == null || param.getKey().trim().isEmpty()) {
                        builder.error(String.format("inputs[%d]: 参数键不能为空", i));
                    }

                    // 检查别名是否唯一
                    String alias = param.getAlias();
                    for (int j = i + 1; j < inputs.size(); j++) {
                        MultiInputConfig.InputParameter otherParam = inputs.get(j);
                        if (alias.equals(otherParam.getAlias())) {
                            builder.error(String.format("inputs[%d] 和 inputs[%d]: 别名 '%s' 重复", i, j, alias));
                        }
                    }
                }

                // 验证合并模式的合并键
                if (mode == MultiInputConfig.InputMode.MERGED) {
                    String mergeKey = inputConfig.getMergeKey();
                    if (mergeKey == null || mergeKey.trim().isEmpty()) {
                        builder.warning("合并模式下未配置 mergeKey，将使用默认值 'inputs'");
                    }
                }
                break;

            default:
                builder.error("不支持的输入模式: " + mode);
                break;
        }

        return builder.build();
    }
}
