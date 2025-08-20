package com.logflow.examples;

import com.logflow.config.WorkflowSchemaValidator;

/**
 * Schema验证示例
 * 展示如何使用WorkflowSchemaValidator来验证YAML配置文件
 */
public class SchemaValidationExample {

    public static void main(String[] args) {
        System.out.println("=== LogFlow YAML Schema验证示例 ===\n");

        try {
            // 创建schema验证器
            WorkflowSchemaValidator validator = new WorkflowSchemaValidator();
            System.out.println("✅ Schema验证器初始化成功\n");

            // 验证各个示例配置文件
            validateConfigFile(validator, "workflows/basic-error-detection.yaml", "基础错误检测工作流");
            validateConfigFile(validator, "workflows/complex-log-analysis.yaml", "复杂日志分析工作流");
            validateConfigFile(validator, "workflows/simple-test.yaml", "简单测试工作流");
            validateConfigFile(validator, "workflows/schema-example.yaml", "Schema示例工作流");

            // 验证无效的配置内容
            System.out.println("📋 测试无效配置验证:");
            testInvalidConfigurations(validator);

        } catch (Exception e) {
            System.err.println("❌ 初始化Schema验证器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 验证指定的配置文件
     */
    private static void validateConfigFile(WorkflowSchemaValidator validator, String filePath, String description) {
        System.out.printf("🔍 验证 %s (%s):\n", description, filePath);

        WorkflowSchemaValidator.ValidationResult result = validator.validateYamlFile(filePath);

        if (result.isValid()) {
            System.out.println("   ✅ 验证通过");
        } else {
            System.out.println("   ❌ 验证失败:");
            System.out.println("   " + result.getErrorMessage().replace("\n", "\n   "));
        }
        System.out.println();
    }

    /**
     * 测试无效配置的验证
     */
    private static void testInvalidConfigurations(WorkflowSchemaValidator validator) {

        // 测试1: 缺少必需字段
        System.out.println("   测试1: 缺少必需字段");
        String invalidConfig1 = "workflow:\n" +
                "  name: \"测试工作流\"\n" +
                "  # 缺少必需的id字段\n" +
                "nodes:\n" +
                "  - id: \"test\"\n" +
                "    name: \"测试节点\"\n" +
                "    type: \"input\"\n";

        WorkflowSchemaValidator.ValidationResult result1 = validator.validateYamlContent(invalidConfig1);
        System.out.println("   结果: " + (result1.isValid() ? "❌ 应该失败但通过了" : "✅ 正确检测到错误"));
        if (!result1.isValid()) {
            System.out.println("   错误: " + result1.getErrorMessage().split("\n")[0]);
        }
        System.out.println();

        // 测试2: 无效的节点类型
        System.out.println("   测试2: 无效的节点类型");
        String invalidConfig2 = "workflow:\n" +
                "  id: \"test_workflow\"\n" +
                "  name: \"测试工作流\"\n" +
                "nodes:\n" +
                "  - id: \"test\"\n" +
                "    name: \"测试节点\"\n" +
                "    type: \"invalid_type\"  # 无效的节点类型\n";

        WorkflowSchemaValidator.ValidationResult result2 = validator.validateYamlContent(invalidConfig2);
        System.out.println("   结果: " + (result2.isValid() ? "❌ 应该失败但通过了" : "✅ 正确检测到错误"));
        if (!result2.isValid()) {
            System.out.println("   错误: " + result2.getErrorMessage().split("\n")[0]);
        }
        System.out.println();

        // 测试3: 无效的配置值
        System.out.println("   测试3: 无效的配置值范围");
        String invalidConfig3 = "workflow:\n" +
                "  id: \"test_workflow\"\n" +
                "  name: \"测试工作流\"\n" +
                "globalConfig:\n" +
                "  timeout: -1000  # 负数超时时间\n" +
                "  maxConcurrentNodes: 100  # 超出最大值\n" +
                "nodes:\n" +
                "  - id: \"test\"\n" +
                "    name: \"测试节点\"\n" +
                "    type: \"input\"\n";

        WorkflowSchemaValidator.ValidationResult result3 = validator.validateYamlContent(invalidConfig3);
        System.out.println("   结果: " + (result3.isValid() ? "❌ 应该失败但通过了" : "✅ 正确检测到错误"));
        if (!result3.isValid()) {
            System.out.println("   错误: " + result3.getErrorMessage().split("\n")[0]);
        }
        System.out.println();

        // 测试4: 正确的最小配置
        System.out.println("   测试4: 正确的最小配置");
        String validConfig = "workflow:\n" +
                "  id: \"minimal_workflow\"\n" +
                "  name: \"最小工作流\"\n" +
                "nodes:\n" +
                "  - id: \"input\"\n" +
                "    name: \"输入节点\"\n" +
                "    type: \"input\"\n";

        WorkflowSchemaValidator.ValidationResult result4 = validator.validateYamlContent(validConfig);
        System.out.println("   结果: " + (result4.isValid() ? "✅ 验证通过" : "❌ 不应该失败: " + result4.getErrorMessage()));
        System.out.println();
    }
}
