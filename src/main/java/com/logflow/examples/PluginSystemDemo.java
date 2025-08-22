package com.logflow.examples;

import com.logflow.plugin.*;
import com.logflow.core.ValidationResult;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;
import com.logflow.engine.WorkflowExecutionResult;
import com.logflow.nodes.NotificationNode;
import com.logflow.nodes.PluginNode;

import java.util.*;

/**
 * 插件系统演示程序
 * 展示LogFlow插件化数据源的功能和使用方法
 */
public class PluginSystemDemo {

    public static void main(String[] args) {
        System.out.println("=== LogFlow插件系统演示 ===\n");

        try {
            // 介绍插件系统
            introducePluginSystem();

            // 演示插件管理功能
            demonstratePluginManagement();

            // 演示插件化数据源节点
            demonstratePluginDataSource();

            // 演示完整工作流
            demonstratePluginWorkflow();

            System.out.println("\n=== 演示完成 ===");

        } catch (Exception e) {
            System.err.println("❌ 演示执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 介绍插件系统
     */
    private static void introducePluginSystem() {
        System.out.println("🚀 LogFlow插件系统介绍：");
        System.out.println();

        System.out.println("🎯 核心特性：");
        System.out.println("   1. SPI机制 - 基于Java SPI自动发现插件");
        System.out.println("   2. 动态加载 - 支持运行时加载JAR包");
        System.out.println("   3. 统一接口 - 标准化的插件接口设计");
        System.out.println("   4. 配置验证 - 智能参数验证和类型检查");
        System.out.println("   5. 连接测试 - 支持连接状态测试");
        System.out.println("   6. 生命周期 - 完整的插件生命周期管理");
        System.out.println();

        System.out.println("🔧 技术架构：");
        System.out.println("   - DataSourcePlugin：插件核心接口");
        System.out.println("   - PluginManager：插件管理器，支持动态加载");
        System.out.println("   - AbstractDataSourcePlugin：抽象基类，简化开发");
        System.out.println("   - DataSourceConnection：连接接口，支持多种读取模式");
        System.out.println();

        System.out.println("💡 扩展能力：");
        System.out.println("   - 无限扩展：通过插件支持任意数据源");
        System.out.println("   - 热插拔：运行时加载/卸载插件");
        System.out.println("   - 标准化：统一的开发和使用体验");
        System.out.println("   - 安全性：配置验证和错误处理");
        System.out.println();
    }

    /**
     * 演示插件管理功能
     */
    private static void demonstratePluginManagement() {
        System.out.println("📋 插件管理功能演示：");
        System.out.println();

        // 获取插件管理器
        PluginManager pluginManager = PluginManager.getInstance();

        // 初始化插件管理器
        System.out.println("🔄 初始化插件管理器...");
        pluginManager.initialize();

        // 显示已加载的插件
        Collection<PluginManager.PluginInfo> plugins = pluginManager.getPluginInfos();
        System.out.println("✅ 成功加载 " + plugins.size() + " 个插件：");

        for (PluginManager.PluginInfo pluginInfo : plugins) {
            System.out.println("   📦 " + pluginInfo.getName() + " (" + pluginInfo.getId() + ")");
            System.out.println("      版本: " + pluginInfo.getVersion());
            System.out.println("      作者: " + pluginInfo.getAuthor());
            System.out.println("      描述: " + pluginInfo.getDescription());

            // 显示插件参数
            DataSourcePlugin plugin = pluginManager.getPlugin(pluginInfo.getId());
            List<PluginParameter> parameters = plugin.getSupportedParameters();
            System.out.println("      参数数量: " + parameters.size());

            for (PluginParameter param : parameters) {
                System.out.println("        - " + param.getDisplayName() +
                        " (" + param.getName() + "): " + param.getType() +
                        (param.isRequired() ? " [必需]" : " [可选]"));
            }
            System.out.println();
        }
    }

    /**
     * 演示插件化数据源节点
     */
    private static void demonstratePluginDataSource() {
        System.out.println("🔌 插件化数据源节点演示：");
        System.out.println();

        // 演示Mock插件
        demonstrateMockPlugin();

        System.out.println();

        // 演示File插件
        demonstrateFilePlugin();
    }

    /**
     * 演示Mock插件
     */
    private static void demonstrateMockPlugin() {
        System.out.println("📝 Mock插件演示：");

        try {
            // 创建Mock数据源节点
            PluginNode mockNode = new PluginNode("mock_source", "Mock数据源");

            // 配置Mock插件
            Map<String, Object> mockConfig = Map.of(
                    "sourceType", "mock",
                    "mockType", "mixed_logs",
                    "recordCount", 50,
                    "errorRate", 20,
                    "outputKey", "mock_data");
            mockNode.setConfiguration(mockConfig);

            // 验证配置
            System.out.println("   🔍 验证Mock插件配置...");
            ValidationResult validation = mockNode.validate();
            if (validation.isValid()) {
                System.out.println("   ✅ 配置验证通过");
            } else {
                System.out.println("   ❌ 配置验证失败:");
                validation.getErrors().forEach(
                        error -> System.out.println("      - " + error));
                return;
            }

            // 测试连接
            System.out.println("   🧪 测试Mock插件连接...");
            PluginTestResult testResult = mockNode.testConnection();
            if (testResult.isSuccess()) {
                System.out.println("   ✅ 连接测试成功: " + testResult.getMessage());
                System.out.println("      响应时间: " + testResult.getResponseTime() + "ms");
                testResult.getDetails().forEach((key, value) -> System.out.println("      " + key + ": " + value));
            } else {
                System.out.println("   ❌ 连接测试失败: " + testResult.getMessage());
            }

            // 获取数据模式
            System.out.println("   📋 获取数据模式信息...");
            List<PluginParameter> schema = mockNode.getPluginParameters();
            System.out.println("   📊 数据模式: " + schema.size());
            for (PluginParameter param : schema) {
                System.out.println("        - " + param.getDisplayName() +
                        " (" + param.getName() + "): " + param.getType() +
                        (param.isRequired() ? " [必需]" : " [可选]"));
            }

        } catch (Exception e) {
            System.out.println("   ❌ Mock插件演示失败: " + e.getMessage());
        }
    }

    /**
     * 演示File插件
     */
    private static void demonstrateFilePlugin() {
        System.out.println("📂 File插件演示：");

        try {
            // 创建File数据源节点
            PluginNode fileNode = new PluginNode("file_source", "文件数据源");

            // 配置File插件（使用项目中的示例文件）
            Map<String, Object> fileConfig = Map.of(
                    "sourceType", "file",
                    "filePath", "src/main/resources/workflows/simple-test.yaml",
                    "format", "text",
                    "maxLines", 10,
                    "outputKey", "file_data");
            fileNode.setConfiguration(fileConfig);

            // 验证配置
            System.out.println("   🔍 验证File插件配置...");
            ValidationResult validation = fileNode.validate();
            if (validation.isValid()) {
                System.out.println("   ✅ 配置验证通过");

                // 显示警告（如果有）
                if (!validation.getWarnings().isEmpty()) {
                    System.out.println("   ⚠️ 配置警告:");
                    validation.getWarnings().forEach(warning -> System.out
                            .println("      - " + warning));
                }
            } else {
                System.out.println("   ❌ 配置验证失败:");
                validation.getErrors().forEach(
                        error -> System.out.println("      - " + error));
                return;
            }

            // 测试连接
            System.out.println("   🧪 测试File插件连接...");
            PluginTestResult testResult = fileNode.testConnection();
            if (testResult.isSuccess()) {
                System.out.println("   ✅ 连接测试成功: " + testResult.getMessage());
                testResult.getDetails().forEach((key, value) -> System.out.println("      " + key + ": " + value));
            } else {
                System.out.println("   ❌ 连接测试失败: " + testResult.getMessage());
            }

        } catch (Exception e) {
            System.out.println("   ❌ File插件演示失败: " + e.getMessage());
        }
    }

    /**
     * 演示完整工作流
     */
    private static void demonstratePluginWorkflow() {
        System.out.println("🔄 完整插件工作流演示：");
        System.out.println();

        try {
            // 创建工作流
            Workflow workflow = new Workflow("plugin_demo_workflow", "插件演示工作流");

            // 添加Mock数据源节点
            PluginNode mockNode = new PluginNode("mock_source", "Mock数据源");
            mockNode.setConfiguration(Map.of(
                    "sourceType", "mock",
                    "mockType", "error_logs",
                    "recordCount", 20,
                    "errorRate", 50,
                    "outputKey", "logs"));
            workflow.addNode(mockNode);

            // 添加输出节点
            NotificationNode outputNode = new NotificationNode("console_output", "控制台输出");
            outputNode.setConfiguration(Map.of(
                    "outputType", "console",
                    "inputKey", "logs",
                    "format", "简化格式"));
            workflow.addNode(outputNode);

            // 连接节点
            workflow.addConnection("mock_source", "console_output");

            // 验证工作流
            System.out.println("🔍 验证工作流...");
            var workflowValidation = workflow.validate();
            if (workflowValidation.isValid()) {
                System.out.println("✅ 工作流验证通过");
            } else {
                System.out.println("❌ 工作流验证失败:");
                workflowValidation.getErrors().forEach(error -> System.out.println("   - " + error));
                return;
            }

            // 执行工作流
            System.out.println("🚀 执行插件工作流...");
            WorkflowEngine engine = new WorkflowEngine();

            WorkflowExecutionResult result = engine.execute(workflow, Map.of());

            if (result.isSuccess()) {
                System.out.println("✅ 工作流执行成功!");
                System.out.println("   执行时间: " + result.getExecutionDurationMs() + "ms");
                System.out.println("   成功节点: " + result.getStatistics().getSuccessfulNodes() + "/" +
                        result.getStatistics().getTotalNodes());
            } else {
                System.out.println("❌ 工作流执行失败: " + result.getMessage());

                // 显示失败的节点信息
                Map<String, com.logflow.core.NodeExecutionResult> failedNodes = result.getFailedNodeResults();
                if (!failedNodes.isEmpty()) {
                    System.out.println("   失败节点:");
                    failedNodes.forEach((nodeId, nodeResult) -> System.out
                            .println("     - " + nodeId + ": " + nodeResult.getMessage()));
                }
            }

            // 关闭引擎
            engine.shutdown();

        } catch (Exception e) {
            System.out.println("❌ 插件工作流演示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 显示可用插件列表
     */
    private static void showAvailablePlugins() {
        System.out.println("📦 可用插件列表：");

        Collection<PluginManager.PluginInfo> plugins = PluginNode.getAvailablePlugins();

        if (plugins.isEmpty()) {
            System.out.println("   没有可用的插件");
            return;
        }

        for (PluginManager.PluginInfo plugin : plugins) {
            System.out.println("   🔌 " + plugin.getName() + " (ID: " + plugin.getId() + ")");
            System.out.println("      版本: " + plugin.getVersion());
            System.out.println("      描述: " + plugin.getDescription());
        }

        System.out.println();
    }
}
