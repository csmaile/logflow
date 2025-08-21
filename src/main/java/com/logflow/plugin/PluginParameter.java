package com.logflow.plugin;

/**
 * 插件参数定义
 */
public class PluginParameter {

    private String name;
    private String displayName;
    private String description;
    private ParameterType type;
    private boolean required;
    private Object defaultValue;
    private String validation;
    private String[] options;
    private String category;
    private boolean sensitive;

    public enum ParameterType {
        STRING,
        INTEGER,
        LONG,
        DOUBLE,
        BOOLEAN,
        PASSWORD,
        FILE_PATH,
        URL,
        JSON,
        ENUM,
        LIST
    }

    public PluginParameter() {
    }

    public PluginParameter(String name, String displayName, String description,
            ParameterType type, boolean required) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.required = required;
    }

    public static PluginParameter create(String name, String displayName,
            ParameterType type, boolean required) {
        return new PluginParameter(name, displayName, null, type, required);
    }

    public static PluginParameter createString(String name, String displayName, boolean required) {
        return new PluginParameter(name, displayName, null, ParameterType.STRING, required);
    }

    public static PluginParameter createInteger(String name, String displayName, boolean required) {
        return new PluginParameter(name, displayName, null, ParameterType.INTEGER, required);
    }

    public static PluginParameter createBoolean(String name, String displayName, boolean required) {
        return new PluginParameter(name, displayName, null, ParameterType.BOOLEAN, required);
    }

    public static PluginParameter createPassword(String name, String displayName, boolean required) {
        PluginParameter param = new PluginParameter(name, displayName, null, ParameterType.PASSWORD, required);
        param.setSensitive(true);
        return param;
    }

    public static PluginParameter createEnum(String name, String displayName,
            boolean required, String... options) {
        PluginParameter param = new PluginParameter(name, displayName, null, ParameterType.ENUM, required);
        param.setOptions(options);
        return param;
    }

    // Fluent API methods
    public PluginParameter description(String description) {
        this.description = description;
        return this;
    }

    public PluginParameter defaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public PluginParameter validation(String validation) {
        this.validation = validation;
        return this;
    }

    public PluginParameter category(String category) {
        this.category = category;
        return this;
    }

    public PluginParameter sensitive(boolean sensitive) {
        this.sensitive = sensitive;
        return this;
    }

    public PluginParameter options(String... options) {
        this.options = options;
        return this;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ParameterType getType() {
        return type;
    }

    public void setType(ParameterType type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }
}
