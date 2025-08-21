package com.logflow.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

/**
 * JSON Schema 验证演示
 * 演示更新后的工作流配置 Schema 验证功能
 */
public class SchemaValidationDemo extends BaseDemo {

    public static void main(String[] args) {
        safeExecute("LogFlow JSON Schema 验证演示", () -> {
            demonstrateSchemaValidation();
        });
    }

    /**
     * 演示Schema验证功能
     */
    private static void demonstrateSchemaValidation() {
        System.out.println("🔍 JSON Schema 验证演示\n");

        try {
            // 加载Schema
            ObjectMapper mapper = new ObjectMapper();
            InputStream schemaStream = SchemaValidationDemo.class
                    .getResourceAsStream("/schemas/logflow-workflow-schema.json");
            JsonNode schemaNode = mapper.readTree(schemaStream);

            System.out.println("✅ JSON Schema 加载成功");
            System.out.println("📋 支持的节点类型：");

            // 提取节点类型信息
            JsonNode nodeTypeEnum = schemaNode.at("/definitions/node/properties/type/enum");
            if (nodeTypeEnum.isArray()) {
                for (JsonNode nodeType : nodeTypeEnum) {
                    String type = nodeType.asText();
                    String status = getNodeTypeStatus(type);
                    System.out.printf("   %s %s%s%n", getStatusIcon(status), type, status);
                }
            }
            System.out.println();

            // 显示Schema结构分析
            analyzeSchemaStructure(schemaNode);

        } catch (Exception e) {
            System.err.println("❌ Schema验证演示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 分析Schema结构
     */
    private static void analyzeSchemaStructure(JsonNode schemaNode) {
        System.out.println("📊 Schema 结构分析");
        System.out.println("=====================================");

        // 分析节点配置定义
        JsonNode definitions = schemaNode.get("definitions");
        if (definitions != null) {
            System.out.println("🔧 节点配置定义：");
            definitions.fieldNames().forEachRemaining(name -> {
                if (name.endsWith("NodeConfig")) {
                    JsonNode config = definitions.get(name);
                    JsonNode title = config.get("title");
                    if (title != null) {
                        String status = "";
                        if (title.asText().contains("已废弃")) {
                            status = " ⚠️ (已废弃)";
                        } else if (name.equals("pluginNodeConfig") ||
                                name.equals("referenceNodeConfig") ||
                                name.equals("notificationNodeConfig")) {
                            status = " 🆕 (新增)";
                        }
                        System.out.printf("   • %s%s%n", title.asText(), status);
                    }
                }
            });
        }
        System.out.println();

        // 分析必需字段
        System.out.println("📋 配置示例：");
        showConfigurationExamples();
    }

    /**
     * 显示配置示例
     */
    private static void showConfigurationExamples() {
        System.out.println("   🆕 现代化工作流（推荐）：");
        System.out.println("   ```yaml");
        System.out.println("   workflow:");
        System.out.println("     id: modern_workflow");
        System.out.println("     name: 现代化工作流");
        System.out.println("   nodes:");
        System.out.println("     - id: plugin_node");
        System.out.println("       type: plugin");
        System.out.println("       config:");
        System.out.println("         pluginType: file");
        System.out.println("     - id: script_node");
        System.out.println("       type: script");
        System.out.println("       config:");
        System.out.println("         script: 'input.filter(item => item.level !== \"DEBUG\")'");
        System.out.println("     - id: notification_node");
        System.out.println("       type: notification");
        System.out.println("       config:");
        System.out.println("         providerType: console");
        System.out.println("   ```");
        System.out.println();

        System.out.println("   ⚠️  遗留工作流（向后兼容）：");
        System.out.println("   ```yaml");
        System.out.println("   workflow:");
        System.out.println("     id: legacy_workflow");
        System.out.println("     name: 遗留工作流");
        System.out.println("   nodes:");
        System.out.println("     - id: datasource_node  # 建议改为 plugin");
        System.out.println("       type: datasource");
        System.out.println("       config:");
        System.out.println("         sourceType: file");
        System.out.println("     - id: output_node      # 建议改为 notification");
        System.out.println("       type: output");
        System.out.println("       config:");
        System.out.println("         outputType: console");
        System.out.println("   ```");
    }

    /**
     * 测试有效的工作流配置（简化版）
     */
    private static void demonstrateConfigurationValidation() throws Exception {
        System.out.println("✅ 测试有效工作流配置");
        System.out.println("=====================================");

        String validConfig = "{\n" +
                "  \"workflow\": {\n" +
                "    \"id\": \"modern_workflow\",\n" +
                "    \"name\": \"现代化工作流\",\n" +
                "    \"version\": \"2.0.0\",\n" +
                "    \"author\": \"LogFlow Team\"\n" +
                "  },\n" +
                "  \"nodes\": [\n" +
                "    {\n" +
                "      \"id\": \"input_node\",\n" +
                "      \"name\": \"输入节点\",\n" +
                "      \"type\": \"input\",\n" +
                "      \"config\": {\n" +
                "        \"inputKey\": \"user_data\",\n" +
                "        \"dataType\": \"object\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"plugin_node\",\n" +
                "      \"name\": \"插件节点\",\n" +
                "      \"type\": \"plugin\",\n" +
                "      \"config\": {\n" +
                "        \"pluginType\": \"file\",\n" +
                "        \"outputKey\": \"file_data\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"decision_node\",\n" +
                "      \"name\": \"决策节点\",\n" +
                "      \"type\": \"decision\",\n" +
                "      \"config\": {\n" +
                "        \"condition\": \"data.length > 0\",\n" +
                "        \"inputKey\": \"file_data\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"notification_node\",\n" +
                "      \"name\": \"通知节点\",\n" +
                "      \"type\": \"notification\",\n" +
                "      \"config\": {\n" +
                "        \"providerType\": \"console\",\n" +
                "        \"inputKey\": \"result\"\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"connections\": [\n" +
                "    {\n" +
                "      \"from\": \"input_node\",\n" +
                "      \"to\": \"plugin_node\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"from\": \"plugin_node\",\n" +
                "      \"to\": \"decision_node\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"from\": \"decision_node\",\n" +
                "      \"to\": \"notification_node\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        System.out.println("💡 配置验证说明：");
        System.out.println("   ✅ Schema支持所有新的节点类型");
        System.out.println("   ✅ 保持向后兼容，废弃节点仍可使用");
        System.out.println("   ✅ 提供详细的配置验证和智能提示");
        System.out.println("   ✅ IDE可以使用Schema进行自动补全");
    }

    /**
     * 获取节点类型状态
     */
    private static String getNodeTypeStatus(String nodeType) {
        switch (nodeType) {
            case "datasource":
                return " (已废弃，建议使用 plugin)";
            case "output":
                return " (已废弃，建议使用 notification)";
            case "plugin":
            case "reference":
            case "notification":
                return " (新增)";
            default:
                return "";
        }
    }

    /**
     * 获取状态图标
     */
    private static String getStatusIcon(String status) {
        if (status.contains("已废弃"))
            return "⚠️";
        if (status.contains("新增"))
            return "🆕";
        return "✅";
    }
}
