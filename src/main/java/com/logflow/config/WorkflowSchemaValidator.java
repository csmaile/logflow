package com.logflow.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LogFlow工作流配置的JSON Schema验证器
 * 提供YAML配置文件的结构验证和错误检查功能
 */
public class WorkflowSchemaValidator {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowSchemaValidator.class);
    private static final String SCHEMA_RESOURCE_PATH = "/schemas/logflow-workflow-schema.json";

    private final JsonSchema schema;
    private final ObjectMapper yamlMapper;
    private final ObjectMapper jsonMapper;

    public WorkflowSchemaValidator() throws IOException {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.jsonMapper = new ObjectMapper();
        this.schema = loadSchema();
    }

    /**
     * 验证YAML配置文件
     * 
     * @param yamlFilePath YAML文件路径
     * @return 验证结果
     */
    public ValidationResult validateYamlFile(String yamlFilePath) {
        try {
            logger.debug("验证YAML文件: {}", yamlFilePath);

            // 读取YAML文件并转换为JsonNode
            JsonNode yamlNode = yamlMapper.readTree(
                    getClass().getClassLoader().getResourceAsStream(yamlFilePath));

            return validateJsonNode(yamlNode, yamlFilePath);

        } catch (IOException e) {
            logger.error("读取YAML文件失败: {}", yamlFilePath, e);
            return ValidationResult.error("文件读取失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("验证过程中发生异常: {}", yamlFilePath, e);
            return ValidationResult.error("验证异常: " + e.getMessage());
        }
    }

    /**
     * 验证YAML内容字符串
     * 
     * @param yamlContent YAML内容
     * @return 验证结果
     */
    public ValidationResult validateYamlContent(String yamlContent) {
        try {
            logger.debug("验证YAML内容，长度: {}", yamlContent.length());

            // 解析YAML内容
            JsonNode yamlNode = yamlMapper.readTree(yamlContent);

            return validateJsonNode(yamlNode, "<字符串内容>");

        } catch (IOException e) {
            logger.error("解析YAML内容失败", e);
            return ValidationResult.error("YAML解析失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("验证YAML内容时发生异常", e);
            return ValidationResult.error("验证异常: " + e.getMessage());
        }
    }

    /**
     * 验证JsonNode对象
     * 
     * @param jsonNode 要验证的JsonNode
     * @param source   数据源描述
     * @return 验证结果
     */
    private ValidationResult validateJsonNode(JsonNode jsonNode, String source) {
        if (jsonNode == null) {
            return ValidationResult.error("YAML内容为空");
        }

        // 执行schema验证
        Set<ValidationMessage> validationMessages = schema.validate(jsonNode);

        if (validationMessages.isEmpty()) {
            logger.info("配置验证通过: {}", source);
            return ValidationResult.success();
        } else {
            logger.warn("配置验证失败: {}，发现 {} 个错误", source, validationMessages.size());

            // 转换验证消息
            String errors = validationMessages.stream()
                    .map(this::formatValidationMessage)
                    .collect(Collectors.joining("\n"));

            return ValidationResult.error(errors);
        }
    }

    /**
     * 格式化验证错误消息
     * 
     * @param message 验证消息
     * @return 格式化后的错误消息
     */
    private String formatValidationMessage(ValidationMessage message) {
        String path = message.getPath();
        String errorMessage = message.getMessage();

        // 简化路径显示
        if (path.startsWith("$.")) {
            path = path.substring(2);
        }
        if (path.isEmpty()) {
            path = "根节点";
        }

        return String.format("[%s] %s", path, errorMessage);
    }

    /**
     * 加载JSON Schema
     * 
     * @return JsonSchema对象
     * @throws IOException 加载失败时抛出异常
     */
    private JsonSchema loadSchema() throws IOException {
        logger.debug("加载Schema文件: {}", SCHEMA_RESOURCE_PATH);

        try (InputStream schemaStream = getClass().getResourceAsStream(SCHEMA_RESOURCE_PATH)) {
            if (schemaStream == null) {
                throw new IOException("Schema文件不存在: " + SCHEMA_RESOURCE_PATH);
            }

            // 读取schema内容
            JsonNode schemaNode = jsonMapper.readTree(schemaStream);

            // 创建JsonSchemaFactory
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode));

            // 创建JsonSchema
            JsonSchema schema = factory.getSchema(schemaNode);

            logger.info("Schema加载成功");
            return schema;
        }
    }

    /**
     * 获取Schema的JSON表示（用于调试）
     * 
     * @return Schema的JSON字符串
     */
    public String getSchemaJson() {
        try (InputStream schemaStream = getClass().getResourceAsStream(SCHEMA_RESOURCE_PATH)) {
            if (schemaStream == null) {
                return "Schema文件不存在";
            }
            return new String(schemaStream.readAllBytes());
        } catch (IOException e) {
            logger.error("读取Schema文件失败", e);
            return "读取Schema失败: " + e.getMessage();
        }
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            if (valid) {
                return "验证通过";
            } else {
                return "验证失败: " + errorMessage;
            }
        }
    }
}
