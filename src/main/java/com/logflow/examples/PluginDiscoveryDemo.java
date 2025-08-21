package com.logflow.examples;

import com.logflow.plugin.PluginConfigurationGenerator;
import com.logflow.plugin.PluginManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 插件发现和配置演示
 * 展示完整的插件发现、配置生成和文档生成流程
 */
public class PluginDiscoveryDemo extends BaseDemo {

    public static void main(String[] args) {
        safeExecute("LogFlow 插件发现和配置演示", () -> {
            demonstratePluginDiscovery();
        });
    }

    /**
     * 演示插件发现和配置生成流程
     */
    private static void demonstratePluginDiscovery() {
        System.out.println("🔍 插件发现和配置生成完整流程演示\n");

        try {
            PluginManager pluginManager = PluginManager.getInstance();
            pluginManager.initialize();

            PluginConfigurationGenerator generator = new PluginConfigurationGenerator(pluginManager);

            // 步骤 1: 发现可用插件
            System.out.println("第1步：发现可用插件");
            System.out.println("=====================================");
            var plugins = pluginManager.getPluginInfos();
            for (var pluginInfo : plugins) {
                var plugin = pluginManager.getPlugin(pluginInfo.getId());
                System.out.printf("✅ 发现插件: %s (%s)%n", plugin.getPluginName(), plugin.getPluginId());
                System.out.printf("   版本: %s | 参数数量: %d%n",
                        plugin.getVersion(), plugin.getSupportedParameters().size());
            }
            System.out.println();

            // 步骤 2: 生成配置模板
            System.out.println("第2步：生成插件配置模板");
            System.out.println("=====================================");

            String outputDir = "generated-configs";
            new File(outputDir).mkdirs();

            for (var pluginInfo : plugins) {
                String pluginId = pluginInfo.getId();
                System.out.printf("📝 正在生成 %s 插件的配置模板...%n", pluginId);

                // 生成 YAML 配置模板
                String template = generator.generatePluginTemplate(pluginId, true);
                writeToFile(template, outputDir + "/" + pluginId + "-config-template.yml");

                // 生成 JSON Schema
                String schema = generator.generateJsonSchema(pluginId);
                writeToFile(schema, outputDir + "/" + pluginId + "-schema.json");

                // 生成文档
                String docs = generator.generatePluginDocumentation(pluginId);
                writeToFile(docs, outputDir + "/" + pluginId + "-documentation.md");

                System.out.printf("   ✅ %s-config-template.yml%n", pluginId);
                System.out.printf("   ✅ %s-schema.json%n", pluginId);
                System.out.printf("   ✅ %s-documentation.md%n", pluginId);
            }
            System.out.println();

            // 步骤 3: 生成插件概览
            System.out.println("第3步：生成插件概览文档");
            System.out.println("=====================================");
            String overview = generator.generatePluginsOverview();
            writeToFile(overview, outputDir + "/plugins-overview.md");
            System.out.println("✅ plugins-overview.md");
            System.out.println();

            // 步骤 4: 展示使用场景
            System.out.println("第4步：使用场景演示");
            System.out.println("=====================================");
            demonstrateUsageScenarios(outputDir);

            // 步骤 5: 提供用户指导
            System.out.println("第5步：用户使用指导");
            System.out.println("=====================================");
            provideUserGuidance(outputDir);

        } catch (Exception e) {
            System.err.println("❌ 演示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 演示不同的使用场景
     */
    private static void demonstrateUsageScenarios(String outputDir) {
        System.out.println("💡 常见使用场景:");
        System.out.println();

        System.out.println("场景1️⃣：新手用户");
        System.out.println("   🎯 目标：快速开始使用插件");
        System.out.println("   📖 查看：plugins-overview.md");
        System.out.println("   🛠️  选择插件后，查看对应的 *-documentation.md");
        System.out.println("   📋 复制 *-config-template.yml 中的配置");
        System.out.println();

        System.out.println("场景2️⃣：IDE用户");
        System.out.println("   🎯 目标：在IDE中获得智能提示");
        System.out.println("   ⚙️  将 *-schema.json 配置到IDE的JSON Schema映射");
        System.out.println("   ✨ 享受参数自动补全和验证");
        System.out.println();

        System.out.println("场景3️⃣：高级用户");
        System.out.println("   🎯 目标：深度定制和集成");
        System.out.println("   🔧 使用CLI工具生成特定配置");
        System.out.println("   📚 参考文档进行高级配置");
        System.out.println("   🚀 集成到CI/CD流程");
        System.out.println();
    }

    /**
     * 提供用户使用指导
     */
    private static void provideUserGuidance(String outputDir) {
        System.out.println("📋 下一步操作指南:");
        System.out.println();

        System.out.println("1️⃣ 浏览生成的文件:");
        System.out.printf("   📂 所有文件已生成到: %s/%n", new File(outputDir).getAbsolutePath());
        System.out.println("   📖 先查看 plugins-overview.md 了解所有可用插件");
        System.out.println();

        System.out.println("2️⃣ 选择并配置插件:");
        System.out.println("   🔍 选择适合你需求的插件");
        System.out.println("   📋 复制对应的配置模板到你的 workflow.yml");
        System.out.println("   ⚙️  根据实际需求修改参数值");
        System.out.println();

        System.out.println("3️⃣ IDE智能提示配置:");
        System.out.println("   📄 将 *-schema.json 文件路径添加到IDE的JSON Schema设置");
        System.out.println("   🎯 在编辑YAML时享受自动补全和参数验证");
        System.out.println();

        System.out.println("4️⃣ 命令行工具使用:");
        System.out.println("   🛠️  使用 PluginConfigTool 生成特定插件的配置");
        System.out.println("   💾 直接保存配置到文件:");
        System.out.println("   mvn exec:java -Dexec.mainClass=\"com.logflow.tools.PluginConfigTool\" \\");
        System.out.println("       -Dexec.args=\"generate-template --plugin file --output my-config.yml\"");
        System.out.println();

        System.out.println("5️⃣ 获取帮助:");
        System.out.println("   ❓ 查看插件文档了解详细参数说明");
        System.out.println("   🔧 使用 --help 查看CLI工具的完整选项");
        System.out.println("   💬 遇到问题时参考故障排除指南");
    }

    /**
     * 写入文件的辅助方法
     */
    private static void writeToFile(String content, String filePath) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
