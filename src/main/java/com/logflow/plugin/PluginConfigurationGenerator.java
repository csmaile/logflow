package com.logflow.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.*;

/**
 * 插件配置生成器
 * 用于生成插件配置模板、JSON Schema、文档等
 */
public class PluginConfigurationGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PluginConfigurationGenerator.class);
    private final PluginManager pluginManager;
    private final ObjectMapper yamlMapper;
    private final ObjectMapper jsonMapper;

    public PluginConfigurationGenerator(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        this.yamlMapper = new ObjectMapper(new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        this.jsonMapper = new ObjectMapper();
    }

    /**
     * 生成所有可用插件的配置模板
     */
    public String generateAllPluginTemplates() {
        StringBuilder result = new StringBuilder();
        result.append("# LogFlow 插件配置模板\n");
        result.append("# 生成时间: ").append(new Date()).append("\n\n");

        Collection<PluginManager.PluginInfo> plugins = pluginManager.getPluginInfos();
        for (PluginManager.PluginInfo pluginInfo : plugins) {
            result.append(generatePluginTemplate(pluginInfo.getId(), true));
            result.append("\n");
        }

        return result.toString();
    }

    /**
     * 生成特定插件的配置模板
     */
    public String generatePluginTemplate(String pluginId, boolean includeComments) {
        try {
            if (!pluginManager.hasPlugin(pluginId)) {
                throw new IllegalArgumentException("插件不存在: " + pluginId);
            }

            DataSourcePlugin plugin = pluginManager.getPlugin(pluginId);
            List<PluginParameter> parameters = plugin.getSupportedParameters();

            StringBuilder template = new StringBuilder();

            if (includeComments) {
                template.append("# ").append(plugin.getPluginName()).append(" v").append(plugin.getVersion())
                        .append("\n");
                template.append("# ").append(plugin.getDescription()).append("\n");
                template.append("# 作者: ").append(plugin.getAuthor()).append("\n\n");
            }

            // 生成工作流节点配置
            Map<String, Object> nodeConfig = new LinkedHashMap<>();
            nodeConfig.put("id", "my_" + pluginId + "_node");
            nodeConfig.put("name", plugin.getPluginName());
            nodeConfig.put("type", "plugin");

            Map<String, Object> config = new LinkedHashMap<>();
            config.put("pluginType", pluginId);

            // 按类别分组参数
            Map<String, List<PluginParameter>> categorizedParams = categorizeParameters(parameters);

            for (Map.Entry<String, List<PluginParameter>> entry : categorizedParams.entrySet()) {
                String category = entry.getKey();
                List<PluginParameter> categoryParams = entry.getValue();

                if (includeComments && !category.equals("未分类")) {
                    template.append("  # ").append(category).append(" 配置\n");
                }

                for (PluginParameter param : categoryParams) {
                    generateParameterTemplate(template, param, includeComments);
                }

                if (includeComments) {
                    template.append("\n");
                }
            }

            nodeConfig.put("config", config);

            // 生成完整的节点配置
            Map<String, Object> fullConfig = new LinkedHashMap<>();
            fullConfig.put("nodes", Arrays.asList(nodeConfig));

            String yaml = yamlMapper.writeValueAsString(fullConfig);

            if (includeComments) {
                template.append(addParameterComments(yaml, parameters));
            } else {
                template.append(yaml);
            }

            return template.toString();

        } catch (Exception e) {
            logger.error("生成插件配置模板失败: {}", pluginId, e);
            return "# 生成配置模板失败: " + e.getMessage();
        }
    }

    /**
     * 生成 JSON Schema 用于 IDE 智能提示
     */
    public String generateJsonSchema(String pluginId) {
        try {
            if (!pluginManager.hasPlugin(pluginId)) {
                throw new IllegalArgumentException("插件不存在: " + pluginId);
            }

            DataSourcePlugin plugin = pluginManager.getPlugin(pluginId);
            List<PluginParameter> parameters = plugin.getSupportedParameters();

            ObjectNode schema = jsonMapper.createObjectNode();
            schema.put("$schema", "http://json-schema.org/draft-07/schema#");
            schema.put("type", "object");
            schema.put("title", plugin.getPluginName() + " Configuration");
            schema.put("description", plugin.getDescription());

            ObjectNode properties = schema.putObject("properties");
            ObjectNode required = schema.putArray("required").objectNode();
            List<String> requiredFields = new ArrayList<>();

            // 添加固定字段
            properties.putObject("pluginType").put("type", "string").put("enum", pluginId);
            requiredFields.add("pluginType");

            // 添加插件参数
            for (PluginParameter param : parameters) {
                ObjectNode paramSchema = properties.putObject(param.getName());
                addParameterToSchema(paramSchema, param);

                if (param.isRequired()) {
                    requiredFields.add(param.getName());
                }
            }

            ArrayNode requiredArray = schema.putArray("required");
            for (String field : requiredFields) {
                requiredArray.add(field);
            }

            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);

        } catch (Exception e) {
            logger.error("生成 JSON Schema 失败: {}", pluginId, e);
            return "{}";
        }
    }

    /**
     * 生成插件文档
     */
    public String generatePluginDocumentation(String pluginId) {
        try {
            if (!pluginManager.hasPlugin(pluginId)) {
                throw new IllegalArgumentException("插件不存在: " + pluginId);
            }

            DataSourcePlugin plugin = pluginManager.getPlugin(pluginId);
            List<PluginParameter> parameters = plugin.getSupportedParameters();

            StringBuilder doc = new StringBuilder();

            // 标题和基本信息
            doc.append("# ").append(plugin.getPluginName()).append("\n\n");
            doc.append("**版本**: ").append(plugin.getVersion()).append("  \n");
            doc.append("**插件ID**: `").append(plugin.getPluginId()).append("`  \n");
            doc.append("**作者**: ").append(plugin.getAuthor()).append("  \n\n");

            // 描述
            doc.append("## 描述\n\n");
            doc.append(plugin.getDescription()).append("\n\n");

            // 配置参数
            doc.append("## 配置参数\n\n");

            Map<String, List<PluginParameter>> categorizedParams = categorizeParameters(parameters);

            for (Map.Entry<String, List<PluginParameter>> entry : categorizedParams.entrySet()) {
                String category = entry.getKey();
                List<PluginParameter> categoryParams = entry.getValue();

                if (!category.equals("未分类")) {
                    doc.append("### ").append(category).append("\n\n");
                }

                doc.append("| 参数名 | 类型 | 必需 | 默认值 | 描述 |\n");
                doc.append("|--------|------|------|--------|------|\n");

                for (PluginParameter param : categoryParams) {
                    doc.append("| `").append(param.getName()).append("` ");
                    doc.append("| ").append(param.getType()).append(" ");
                    doc.append("| ").append(param.isRequired() ? "✅" : "❌").append(" ");
                    doc.append("| ").append(param.getDefaultValue() != null ? "`" + param.getDefaultValue() + "`" : "-")
                            .append(" ");
                    doc.append("| ").append(param.getDescription() != null ? param.getDescription() : "-")
                            .append(" |\n");
                }
                doc.append("\n");
            }

            // 配置示例
            doc.append("## 配置示例\n\n");
            doc.append("```yaml\n");
            doc.append(generatePluginTemplate(pluginId, false));
            doc.append("```\n\n");

            // 使用示例
            doc.append("## 程序化配置示例\n\n");
            doc.append("```java\n");
            doc.append("// 使用 WorkflowBuilder\n");
            doc.append("WorkflowBuilder.create(\"my-workflow\", \"我的工作流\")\n");
            doc.append("    .addPluginNode(\"").append(plugin.getPluginId()).append("_node\", \"")
                    .append(plugin.getPluginName()).append("\", Map.of(\n");
            doc.append("        \"pluginType\", \"").append(plugin.getPluginId()).append("\"");

            // 添加必需参数示例
            for (PluginParameter param : parameters) {
                if (param.isRequired()) {
                    doc.append(",\n        \"").append(param.getName()).append("\", ");
                    if (param.getDefaultValue() != null) {
                        if (param.getType() == PluginParameter.ParameterType.STRING) {
                            doc.append("\"").append(param.getDefaultValue()).append("\"");
                        } else {
                            doc.append(param.getDefaultValue());
                        }
                    } else {
                        doc.append("\"your_value_here\"");
                    }
                }
            }

            doc.append("\n    ))\n");
            doc.append("    .build();\n");
            doc.append("```\n\n");

            return doc.toString();

        } catch (Exception e) {
            logger.error("生成插件文档失败: {}", pluginId, e);
            return "# 生成文档失败: " + e.getMessage();
        }
    }

    /**
     * 生成所有插件的概览文档
     */
    public String generatePluginsOverview() {
        StringBuilder overview = new StringBuilder();
        overview.append("# LogFlow 插件概览\n\n");
        overview.append("以下是所有可用的插件列表：\n\n");

        Collection<PluginManager.PluginInfo> plugins = pluginManager.getPluginInfos();

        overview.append("| 插件名称 | 插件ID | 版本 | 描述 | 作者 |\n");
        overview.append("|----------|--------|------|------|------|\n");

        for (PluginManager.PluginInfo pluginInfo : plugins) {
            try {
                DataSourcePlugin plugin = pluginManager.getPlugin(pluginInfo.getId());
                overview.append("| ").append(plugin.getPluginName());
                overview.append(" | `").append(plugin.getPluginId()).append("`");
                overview.append(" | ").append(plugin.getVersion());
                overview.append(" | ").append(plugin.getDescription());
                overview.append(" | ").append(plugin.getAuthor());
                overview.append(" |\n");
            } catch (Exception e) {
                logger.warn("无法获取插件信息: {}", pluginInfo.getId(), e);
            }
        }

        overview.append("\n## 快速开始\n\n");
        overview.append("### 1. 选择插件\n");
        overview.append("从上表中选择你需要的插件，记录其插件ID。\n\n");

        overview.append("### 2. 生成配置模板\n");
        overview.append("```bash\n");
        overview.append("# 生成特定插件的配置模板\n");
        overview.append("mvn exec:java -Dexec.mainClass=\"com.logflow.tools.PluginConfigTool\" \\\n");
        overview.append("    -Dexec.args=\"generate-template --plugin <plugin-id>\"\n");
        overview.append("```\n\n");

        overview.append("### 3. 配置工作流\n");
        overview.append("将生成的配置添加到你的 workflow.yml 文件中。\n\n");

        return overview.toString();
    }

    // 辅助方法
    private Map<String, List<PluginParameter>> categorizeParameters(List<PluginParameter> parameters) {
        Map<String, List<PluginParameter>> categorized = new LinkedHashMap<>();

        for (PluginParameter param : parameters) {
            String category = param.getCategory() != null ? param.getCategory() : "未分类";
            categorized.computeIfAbsent(category, k -> new ArrayList<>()).add(param);
        }

        return categorized;
    }

    private void generateParameterTemplate(StringBuilder template, PluginParameter param, boolean includeComments) {
        if (includeComments) {
            if (param.getDescription() != null) {
                template.append("  # ").append(param.getDescription()).append("\n");
            }
            if (param.getType() != null) {
                template.append("  # 类型: ").append(param.getType()).append("\n");
            }
            if (param.isRequired()) {
                template.append("  # 必需参数\n");
            }
            if (param.getDefaultValue() != null) {
                template.append("  # 默认值: ").append(param.getDefaultValue()).append("\n");
            }
        }
    }

    private String addParameterComments(String yaml, List<PluginParameter> parameters) {
        // 简单实现，在实际项目中可能需要更复杂的YAML处理
        return yaml;
    }

    private void addParameterToSchema(ObjectNode paramSchema, PluginParameter param) {
        // 设置基本类型
        switch (param.getType()) {
            case STRING:
            case FILE_PATH:
            case URL:
            case PASSWORD:
                paramSchema.put("type", "string");
                break;
            case INTEGER:
                paramSchema.put("type", "integer");
                break;
            case LONG:
                paramSchema.put("type", "integer");
                paramSchema.put("format", "int64");
                break;
            case DOUBLE:
                paramSchema.put("type", "number");
                break;
            case BOOLEAN:
                paramSchema.put("type", "boolean");
                break;
            case JSON:
                paramSchema.put("type", "object");
                break;
            case ENUM:
                paramSchema.put("type", "string");
                if (param.getOptions() != null) {
                    ArrayNode enumArray = paramSchema.putArray("enum");
                    for (String option : param.getOptions()) {
                        enumArray.add(option);
                    }
                }
                break;
            case LIST:
                paramSchema.put("type", "array");
                break;
        }

        // 添加描述
        if (param.getDescription() != null) {
            paramSchema.put("description", param.getDescription());
        }

        // 添加默认值
        if (param.getDefaultValue() != null) {
            paramSchema.set("default", jsonMapper.valueToTree(param.getDefaultValue()));
        }

        // 添加验证规则
        if (param.getValidation() != null) {
            paramSchema.put("pattern", param.getValidation());
        }
    }
}
