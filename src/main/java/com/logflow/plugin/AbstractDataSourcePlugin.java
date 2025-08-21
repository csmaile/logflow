package com.logflow.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 数据源插件抽象基类
 * 提供插件的通用功能实现，简化插件开发
 */
public abstract class AbstractDataSourcePlugin implements DataSourcePlugin {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected Map<String, Object> globalConfig;
    protected volatile boolean initialized = false;

    @Override
    public void initialize(Map<String, Object> globalConfig) throws PluginException {
        if (initialized) {
            logger.warn("插件 {} 已经初始化", getPluginId());
            return;
        }

        logger.info("正在初始化插件: {} v{}", getPluginName(), getVersion());
        this.globalConfig = globalConfig;

        try {
            doInitialize(globalConfig);
            initialized = true;
            logger.info("插件 {} 初始化成功", getPluginId());
        } catch (Exception e) {
            logger.error("插件 {} 初始化失败", getPluginId(), e);
            throw new PluginException(getPluginId(), "初始化失败", e);
        }
    }

    @Override
    public PluginValidationResult validateConfiguration(Map<String, Object> config) {
        PluginValidationResult result = new PluginValidationResult();

        try {
            // 检查必需参数
            List<PluginParameter> parameters = getSupportedParameters();
            for (PluginParameter param : parameters) {
                if (param.isRequired()) {
                    Object value = config.get(param.getName());
                    if (value == null) {
                        result.addError(param.getName(), "必需参数不能为空");
                        continue;
                    }

                    // 类型验证
                    if (!validateParameterType(param, value)) {
                        result.addError(param.getName(),
                                "参数类型不匹配，期望: " + param.getType() + "，实际: " + value.getClass().getSimpleName());
                    }
                }
            }

            // 调用子类自定义验证
            doValidateConfiguration(config, result);

        } catch (Exception e) {
            logger.error("验证配置时出错", e);
            result.addError("general", "配置验证失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public PluginTestResult testConnection(Map<String, Object> config) {
        long startTime = System.currentTimeMillis();

        try {
            // 先验证配置
            PluginValidationResult validation = validateConfiguration(config);
            if (!validation.isValid()) {
                return PluginTestResult.failure("配置验证失败: " + validation.getErrorSummary());
            }

            // 执行连接测试
            PluginTestResult result = doTestConnection(config);

            // 设置响应时间
            long responseTime = System.currentTimeMillis() - startTime;
            result.setResponseTime(responseTime);

            return result;

        } catch (Exception e) {
            logger.error("测试连接失败", e);
            return PluginTestResult.failure("连接测试失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {
        if (!initialized) {
            return;
        }

        logger.info("正在销毁插件: {}", getPluginId());

        try {
            doDestroy();
            initialized = false;
            logger.info("插件 {} 已销毁", getPluginId());
        } catch (Exception e) {
            logger.error("销毁插件 {} 时出错", getPluginId(), e);
        }
    }

    @Override
    public String getAuthor() {
        return "LogFlow Team";
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }

    // 抽象方法，子类必须实现

    /**
     * 执行插件初始化
     * 子类可以重写此方法来执行特定的初始化逻辑
     */
    protected void doInitialize(Map<String, Object> globalConfig) throws Exception {
        // 默认实现为空
    }

    /**
     * 执行自定义配置验证
     * 子类可以重写此方法来添加特定的验证逻辑
     */
    protected void doValidateConfiguration(Map<String, Object> config, PluginValidationResult result) {
        // 默认实现为空
    }

    /**
     * 执行连接测试
     * 子类必须实现此方法
     */
    protected abstract PluginTestResult doTestConnection(Map<String, Object> config);

    /**
     * 执行插件销毁
     * 子类可以重写此方法来清理资源
     */
    protected void doDestroy() throws Exception {
        // 默认实现为空
    }

    // 辅助方法

    /**
     * 验证参数类型
     */
    protected boolean validateParameterType(PluginParameter param, Object value) {
        if (value == null) {
            return !param.isRequired();
        }

        switch (param.getType()) {
            case STRING:
                return value instanceof String;
            case INTEGER:
                return value instanceof Integer || value instanceof Long;
            case LONG:
                return value instanceof Long || value instanceof Integer;
            case DOUBLE:
                return value instanceof Double || value instanceof Float || value instanceof Integer;
            case BOOLEAN:
                return value instanceof Boolean;
            case PASSWORD:
                return value instanceof String;
            case FILE_PATH:
                return value instanceof String;
            case URL:
                return value instanceof String;
            case JSON:
                return value instanceof String || value instanceof Map || value instanceof List;
            case LIST:
                return value instanceof List;
            case ENUM:
                if (value instanceof String && param.getOptions() != null) {
                    String stringValue = (String) value;
                    for (String option : param.getOptions()) {
                        if (option.equals(stringValue)) {
                            return true;
                        }
                    }
                    return false;
                }
                return false;
            default:
                return true;
        }
    }

    /**
     * 获取配置值（带默认值）
     */
    protected Object getConfigValue(Map<String, Object> config, String key, Object defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    /**
     * 获取字符串配置值
     */
    protected String getStringConfig(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 获取整数配置值
     */
    protected int getIntConfig(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warn("无法解析整数配置 {}: {}", key, value);
            }
        }
        return defaultValue;
    }

    /**
     * 获取布尔配置值
     */
    protected boolean getBooleanConfig(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    /**
     * 检查是否已初始化
     */
    protected void checkInitialized() throws PluginException {
        if (!initialized) {
            throw new PluginException(getPluginId(), "插件未初始化");
        }
    }

    /**
     * 创建插件参数构建器
     */
    protected static PluginParameterBuilder param(String name) {
        return new PluginParameterBuilder(name);
    }

    /**
     * 插件参数构建器
     */
    protected static class PluginParameterBuilder {
        private final PluginParameter parameter;

        public PluginParameterBuilder(String name) {
            this.parameter = new PluginParameter();
            this.parameter.setName(name);
        }

        public PluginParameterBuilder displayName(String displayName) {
            parameter.setDisplayName(displayName);
            return this;
        }

        public PluginParameterBuilder description(String description) {
            parameter.setDescription(description);
            return this;
        }

        public PluginParameterBuilder type(PluginParameter.ParameterType type) {
            parameter.setType(type);
            return this;
        }

        public PluginParameterBuilder required() {
            parameter.setRequired(true);
            return this;
        }

        public PluginParameterBuilder optional() {
            parameter.setRequired(false);
            return this;
        }

        public PluginParameterBuilder defaultValue(Object defaultValue) {
            parameter.setDefaultValue(defaultValue);
            return this;
        }

        public PluginParameterBuilder options(String... options) {
            parameter.setOptions(options);
            return this;
        }

        public PluginParameterBuilder category(String category) {
            parameter.setCategory(category);
            return this;
        }

        public PluginParameterBuilder sensitive() {
            parameter.setSensitive(true);
            return this;
        }

        public PluginParameter build() {
            return parameter;
        }
    }
}
