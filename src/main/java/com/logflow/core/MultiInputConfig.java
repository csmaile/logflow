package com.logflow.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 多输入配置类
 * 支持节点从多个数据源获取输入
 */
public class MultiInputConfig {

    /**
     * 输入参数定义
     */
    public static class InputParameter {
        @JsonProperty("key")
        private String key; // 输入参数在上下文中的键名

        @JsonProperty("alias")
        private String alias; // 在脚本中的别名（可选）

        @JsonProperty("required")
        private Boolean required = true; // 是否必需

        @JsonProperty("defaultValue")
        private Object defaultValue; // 默认值

        @JsonProperty("dataType")
        private String dataType = "object"; // 数据类型：object, array, string, number, boolean

        @JsonProperty("description")
        private String description; // 参数描述

        // Getters and Setters
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getAlias() {
            return alias != null ? alias : key;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public Boolean getRequired() {
            return required != null ? required : true;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDataType() {
            return dataType != null ? dataType : "object";
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * 输入配置模式枚举
     */
    public enum InputMode {
        MULTIPLE, // 多输入模式
        MERGED // 合并输入模式（将所有输入合并到一个对象中）
    }

    @JsonProperty("mode")
    private InputMode mode = InputMode.MULTIPLE;

    @JsonProperty("inputs")
    private List<InputParameter> inputs;

    @JsonProperty("mergeKey")
    private String mergeKey = "inputs"; // 合并模式下的键名

    @JsonProperty("outputKey")
    private String outputKey; // 输出键名

    // Getters and Setters
    public InputMode getMode() {
        return mode != null ? mode : InputMode.MULTIPLE;
    }

    public void setMode(InputMode mode) {
        this.mode = mode;
    }

    public List<InputParameter> getInputs() {
        return inputs;
    }

    public void setInputs(List<InputParameter> inputs) {
        this.inputs = inputs;
    }

    public String getMergeKey() {
        return mergeKey != null ? mergeKey : "inputs";
    }

    public void setMergeKey(String mergeKey) {
        this.mergeKey = mergeKey;
    }

    public String getOutputKey() {
        return outputKey;
    }

    public void setOutputKey(String outputKey) {
        this.outputKey = outputKey;
    }

    /**
     * 验证配置有效性
     */
    public boolean isValid() {
        return inputs != null && !inputs.isEmpty() &&
                inputs.stream().allMatch(input -> input.getKey() != null && !input.getKey().trim().isEmpty());
    }

    /**
     * 获取所有输入键名
     */
    public java.util.Set<String> getAllInputKeys() {
        java.util.Set<String> keys = new java.util.HashSet<>();
        if (inputs != null) {
            inputs.forEach(input -> keys.add(input.getKey()));
        }
        return keys;
    }
}
