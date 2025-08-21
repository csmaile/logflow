package com.logflow.examples;

import com.logflow.builder.WorkflowBuilder;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;
import com.logflow.engine.WorkflowExecutionResult;
import com.logflow.nodes.DataSourceNode;
import com.logflow.nodes.PluginNode;

import java.util.Map;

/**
 * DataSourceNode 迁移到 PluginNode 演示
 * 展示插件节点的增强功能和迁移指南，并演示正确的资源管理
 */
public class PluginNodeMigrationDemo extends BaseDemo {

    public static void main(String[] args) {
        safeExecute("LogFlow DataSourceNode 迁移到 PluginNode 演示", () -> {
            WorkflowEngine engine = new WorkflowEngine();

            // 1. 展示名称变化的意义
            explainNameChangeRationale();

            // 2. 功能对比演示
            demonstrateFunctionalComparison(engine);

            // 3. 迁移指南
            demonstrateMigrationGuide();

            // 4. 新功能展示
            demonstrateEnhancedFeatures();

            System.out.println("\n🎯 迁移总结：");
            System.out.println("✅ PluginNode 完全兼容 DataSourceNode 的所有功能");
            System.out.println("🚀 提供了更强大的插件化架构和扩展能力");
            System.out.println("🔧 支持任何通过插件实现的自定义逻辑");
            System.out.println("📊 更好的配置验证和错误处理");
            System.out.println("⚠️  DataSourceNode 已标记为 @Deprecated，建议迁移");

            // 显示资源管理最佳实践
            showResourceManagementTips();
        });
    }

    /**
     * 解释重命名的意义
     */
    private static void explainNameChangeRationale() {
        System.out.println("🎯 为什么要重命名 DataSourceNode？\n");

        System.out.println("📊 当前 DataSourceNode 的实际能力：");
        System.out.println("   ✅ 数据获取 (MySQL, Kafka, Elasticsearch...)");
        System.out.println("   ✅ 外部API调用 (REST, GraphQL...)");
        System.out.println("   ✅ 文件处理 (CSV, JSON, XML...)");
        System.out.println("   ✅ 数据转换和处理");
        System.out.println("   ✅ 任何通过插件实现的自定义逻辑");

        System.out.println("\n🚀 PluginNode 的优势：");
        System.out.println("   📌 名称更准确地反映实际功能");
        System.out.println("   🔌 强调插件化的核心特性");
        System.out.println("   🎯 为未来功能扩展留下空间");
        System.out.println("   💡 开发者一目了然的功能定位\n");
    }

    /**
     * 功能对比演示
     */
    private static void demonstrateFunctionalComparison(WorkflowEngine engine) {
        System.out.println("⚖️ 功能对比演示：\n");

        // 原有 DataSourceNode 方式 (已废弃)
        System.out.println("   📤 原有 DataSourceNode (已废弃):");
        try {
            Workflow oldWorkflow = WorkflowBuilder.create("old-datasource", "旧版数据源")
                    .addInputNode("input", "输入")
                    .addDataSourceNode("datasource", "数据源", Map.of( // 废弃方法
                            "sourceType", "file",
                            "filePath", "data/sample.txt",
                            "outputKey", "file_data"))
                    .addOutputNode("output", "输出", Map.of(
                            "outputType", "console",
                            "inputKey", "file_data"))
                    .connect("input", "datasource")
                    .connect("datasource", "output")
                    .build();

            System.out.println("      ✅ 工作流创建成功 (向后兼容)");

        } catch (Exception e) {
            System.out.printf("      ❌ 创建失败: %s\n", e.getMessage());
        }

        // 新的 PluginNode 方式
        System.out.println("\n   🚀 新版 PluginNode:");
        try {
            Workflow newWorkflow = WorkflowBuilder.create("new-plugin", "新版插件节点")
                    .addInputNode("input", "输入")
                    .addPluginNode("plugin", "插件节点", "file") // 新方法
                    .addConsoleNotificationNode("notify", "通知")
                    .connect("input", "plugin")
                    .connect("plugin", "notify")
                    .build();

            System.out.println("      ✅ 工作流创建成功 (现代化语法)");

        } catch (Exception e) {
            System.out.printf("      ❌ 创建失败: %s\n", e.getMessage());
        }

        System.out.println();
    }

    /**
     * 迁移指南演示
     */
    private static void demonstrateMigrationGuide() {
        System.out.println("📋 迁移指南：\n");

        System.out.println("1️⃣ **程序化构建迁移**");
        System.out.println("   ```java");
        System.out.println("   // 旧方式 (已废弃)");
        System.out.println("   .addDataSourceNode(\"db\", \"数据库\", Map.of(");
        System.out.println("       \"sourceType\", \"mysql\",");
        System.out.println("       \"host\", \"localhost\",");
        System.out.println("       \"database\", \"logflow\"");
        System.out.println("   ))");
        System.out.println();
        System.out.println("   // 新方式 (推荐)");
        System.out.println("   .addPluginNode(\"db\", \"数据库\", Map.of(");
        System.out.println("       \"pluginType\", \"mysql\",  // sourceType -> pluginType");
        System.out.println("       \"host\", \"localhost\",");
        System.out.println("       \"database\", \"logflow\"");
        System.out.println("   ))");
        System.out.println("   ```\n");

        System.out.println("2️⃣ **YAML配置迁移**");
        System.out.println("   ```yaml");
        System.out.println("   # 旧配置 (已废弃)");
        System.out.println("   nodes:");
        System.out.println("     - id: data_source");
        System.out.println("       name: 数据源");
        System.out.println("       type: datasource  # 废弃类型");
        System.out.println("       config:");
        System.out.println("         sourceType: mysql");
        System.out.println();
        System.out.println("   # 新配置 (推荐)");
        System.out.println("   nodes:");
        System.out.println("     - id: data_plugin");
        System.out.println("       name: 数据插件");
        System.out.println("       type: plugin       # 新类型");
        System.out.println("       config:");
        System.out.println("         pluginType: mysql  # sourceType -> pluginType");
        System.out.println("   ```\n");

        System.out.println("3️⃣ **向后兼容性**");
        System.out.println("   ✅ 现有代码无需立即修改");
        System.out.println("   ⚠️  会收到废弃警告信息");
        System.out.println("   🔄 PluginNode 自动支持 sourceType 配置");
        System.out.println("   📅 建议在下个版本周期完成迁移\n");
    }

    /**
     * 新功能展示
     */
    private static void demonstrateEnhancedFeatures() {
        System.out.println("✨ PluginNode 独有的增强功能：\n");

        System.out.println("🔌 **插件化架构**");
        System.out.println("   • 动态插件加载和JAR包上传");
        System.out.println("   • 完整的依赖隔离机制");
        System.out.println("   • 自动资源管理和回收");

        System.out.println("\n🎯 **扩展能力**");
        System.out.println("   • 数据获取：MySQL、Kafka、Redis、Elasticsearch");
        System.out.println("   • API集成：REST、GraphQL、gRPC");
        System.out.println("   • 文件处理：CSV、JSON、XML、Parquet");
        System.out.println("   • 数据处理：ETL、转换、聚合");
        System.out.println("   • 自定义逻辑：任何Java实现的功能");

        System.out.println("\n🔧 **开发体验**");
        System.out.println("   • 智能配置验证和参数提示");
        System.out.println("   • 连接测试和健康检查");
        System.out.println("   • 详细的错误信息和调试支持");
        System.out.println("   • 性能监控和执行统计");

        System.out.println("\n📊 **管理功能**");
        System.out.println("   • 插件信息查询和版本管理");
        System.out.println("   • 支持参数动态发现");
        System.out.println("   • 插件生命周期管理");
        System.out.println("   • 运行时插件热更新");

        System.out.println("\n🚀 **使用示例**");
        System.out.println("   ```java");
        System.out.println("   // 创建多功能插件节点");
        System.out.println("   WorkflowBuilder.create(\"advanced-workflow\", \"高级工作流\")");
        System.out.println("       .addPluginNode(\"db\", \"数据库\", \"mysql\")");
        System.out.println("       .addPluginNode(\"api\", \"API调用\", \"rest\")");
        System.out.println("       .addPluginNode(\"file\", \"文件处理\", \"csv\")");
        System.out.println("       .addPluginNode(\"transform\", \"数据转换\", \"etl\")");
        System.out.println("       .connect(\"db\", \"transform\")");
        System.out.println("       .connect(\"api\", \"transform\")");
        System.out.println("       .connect(\"transform\", \"file\")");
        System.out.println("       .build();");
        System.out.println("   ```");
    }
}
