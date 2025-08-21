package com.logflow.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 插件清单
 * 用于描述插件的元数据、参数定义、配置示例等
 */
public class PluginManifest {

    @JsonProperty("plugin")
    private PluginInfo pluginInfo;

    @JsonProperty("parameters")
    private List<ParameterDefinition> parameters;

    @JsonProperty("examples")
    private List<ConfigurationExample> examples;

    @JsonProperty("documentation")
    private Documentation documentation;

    @JsonProperty("dependencies")
    private List<String> dependencies;

    @JsonProperty("compatibility")
    private Compatibility compatibility;

    /**
     * 插件基本信息
     */
    public static class PluginInfo {
        private String id;
        private String name;
        private String version;
        private String description;
        private String author;
        private String homepage;
        private String license;
        private List<String> tags;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getHomepage() {
            return homepage;
        }

        public void setHomepage(String homepage) {
            this.homepage = homepage;
        }

        public String getLicense() {
            return license;
        }

        public void setLicense(String license) {
            this.license = license;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }

    /**
     * 参数定义 (增强版)
     */
    public static class ParameterDefinition {
        private String name;
        private String displayName;
        private String description;
        private String type;
        private boolean required;
        private Object defaultValue;
        private String validation;
        private List<String> options;
        private String category;
        private boolean sensitive;
        private String placeholder;
        private String helpText;
        private List<String> dependsOn;
        private Map<String, Object> conditions;

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

        public String getType() {
            return type;
        }

        public void setType(String type) {
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

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
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

        public String getPlaceholder() {
            return placeholder;
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
        }

        public String getHelpText() {
            return helpText;
        }

        public void setHelpText(String helpText) {
            this.helpText = helpText;
        }

        public List<String> getDependsOn() {
            return dependsOn;
        }

        public void setDependsOn(List<String> dependsOn) {
            this.dependsOn = dependsOn;
        }

        public Map<String, Object> getConditions() {
            return conditions;
        }

        public void setConditions(Map<String, Object> conditions) {
            this.conditions = conditions;
        }
    }

    /**
     * 配置示例
     */
    public static class ConfigurationExample {
        private String name;
        private String description;
        private String useCase;
        private Map<String, Object> config;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getUseCase() {
            return useCase;
        }

        public void setUseCase(String useCase) {
            this.useCase = useCase;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }
    }

    /**
     * 文档信息
     */
    public static class Documentation {
        private String readme;
        private String quickStart;
        private String troubleshooting;
        private List<String> tutorials;

        // Getters and Setters
        public String getReadme() {
            return readme;
        }

        public void setReadme(String readme) {
            this.readme = readme;
        }

        public String getQuickStart() {
            return quickStart;
        }

        public void setQuickStart(String quickStart) {
            this.quickStart = quickStart;
        }

        public String getTroubleshooting() {
            return troubleshooting;
        }

        public void setTroubleshooting(String troubleshooting) {
            this.troubleshooting = troubleshooting;
        }

        public List<String> getTutorials() {
            return tutorials;
        }

        public void setTutorials(List<String> tutorials) {
            this.tutorials = tutorials;
        }
    }

    /**
     * 兼容性信息
     */
    public static class Compatibility {
        private String minLogFlowVersion;
        private String maxLogFlowVersion;
        private List<String> supportedPlatforms;
        private Map<String, String> requiredDependencies;

        // Getters and Setters
        public String getMinLogFlowVersion() {
            return minLogFlowVersion;
        }

        public void setMinLogFlowVersion(String minLogFlowVersion) {
            this.minLogFlowVersion = minLogFlowVersion;
        }

        public String getMaxLogFlowVersion() {
            return maxLogFlowVersion;
        }

        public void setMaxLogFlowVersion(String maxLogFlowVersion) {
            this.maxLogFlowVersion = maxLogFlowVersion;
        }

        public List<String> getSupportedPlatforms() {
            return supportedPlatforms;
        }

        public void setSupportedPlatforms(List<String> supportedPlatforms) {
            this.supportedPlatforms = supportedPlatforms;
        }

        public Map<String, String> getRequiredDependencies() {
            return requiredDependencies;
        }

        public void setRequiredDependencies(Map<String, String> requiredDependencies) {
            this.requiredDependencies = requiredDependencies;
        }
    }

    /**
     * 从输入流加载插件清单
     */
    public static PluginManifest loadFromStream(InputStream inputStream) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(inputStream, PluginManifest.class);
    }

    /**
     * 从 JAR 包中的 plugin.yml 文件加载清单
     */
    public static PluginManifest loadFromJar(Class<?> pluginClass) throws Exception {
        InputStream stream = pluginClass.getResourceAsStream("/plugin.yml");
        if (stream == null) {
            // 尝试加载 plugin.yaml
            stream = pluginClass.getResourceAsStream("/plugin.yaml");
        }
        if (stream == null) {
            throw new PluginException("插件清单文件 plugin.yml 或 plugin.yaml 未找到");
        }

        try (InputStream is = stream) {
            return loadFromStream(is);
        }
    }

    // Main Getters and Setters
    public PluginInfo getPluginInfo() {
        return pluginInfo;
    }

    public void setPluginInfo(PluginInfo pluginInfo) {
        this.pluginInfo = pluginInfo;
    }

    public List<ParameterDefinition> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterDefinition> parameters) {
        this.parameters = parameters;
    }

    public List<ConfigurationExample> getExamples() {
        return examples;
    }

    public void setExamples(List<ConfigurationExample> examples) {
        this.examples = examples;
    }

    public Documentation getDocumentation() {
        return documentation;
    }

    public void setDocumentation(Documentation documentation) {
        this.documentation = documentation;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public Compatibility getCompatibility() {
        return compatibility;
    }

    public void setCompatibility(Compatibility compatibility) {
        this.compatibility = compatibility;
    }
}
