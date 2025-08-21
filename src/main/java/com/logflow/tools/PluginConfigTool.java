package com.logflow.tools;

import com.logflow.plugin.PluginConfigurationGenerator;
import com.logflow.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * 插件配置工具
 * 提供命令行接口来生成插件配置模板、文档和JSON Schema
 */
public class PluginConfigTool {

    private static final Logger logger = LoggerFactory.getLogger(PluginConfigTool.class);

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                printUsage();
                return;
            }

            PluginManager pluginManager = PluginManager.getInstance();
            pluginManager.initialize();

            PluginConfigurationGenerator generator = new PluginConfigurationGenerator(pluginManager);

            String command = args[0];

            switch (command) {
                case "list":
                    listPlugins(pluginManager);
                    break;

                case "generate-template":
                    if (args.length < 3 || !args[1].equals("--plugin")) {
                        System.err.println("用法: generate-template --plugin <plugin-id> [--output <file>]");
                        System.exit(1);
                    }
                    generateTemplate(generator, args);
                    break;

                case "generate-all-templates":
                    generateAllTemplates(generator, args);
                    break;

                case "generate-schema":
                    if (args.length < 3 || !args[1].equals("--plugin")) {
                        System.err.println("用法: generate-schema --plugin <plugin-id> [--output <file>]");
                        System.exit(1);
                    }
                    generateSchema(generator, args);
                    break;

                case "generate-docs":
                    if (args.length < 3 || !args[1].equals("--plugin")) {
                        System.err.println("用法: generate-docs --plugin <plugin-id> [--output <file>]");
                        System.exit(1);
                    }
                    generateDocs(generator, args);
                    break;

                case "generate-overview":
                    generateOverview(generator, args);
                    break;

                case "help":
                    printUsage();
                    break;

                default:
                    System.err.println("未知命令: " + command);
                    printUsage();
                    System.exit(1);
            }

        } catch (Exception e) {
            logger.error("工具执行失败", e);
            System.err.println("执行失败: " + e.getMessage());
            System.exit(1);
        } finally {
            // 清理资源
            try {
                PluginManager.getInstance().destroy();
            } catch (Exception e) {
                logger.warn("清理资源时出错", e);
            }
        }
    }

    private static void printUsage() {
        System.out.println("LogFlow 插件配置工具");
        System.out.println();
        System.out.println(
                "用法: mvn exec:java -Dexec.mainClass=\"com.logflow.tools.PluginConfigTool\" -Dexec.args=\"<command> [options]\"");
        System.out.println();
        System.out.println("命令:");
        System.out.println("  list                                    列出所有可用插件");
        System.out.println("  generate-template --plugin <id>        生成指定插件的配置模板");
        System.out.println("                   [--output <file>]     输出到文件");
        System.out.println();
        System.out.println("  generate-all-templates                  生成所有插件的配置模板");
        System.out.println("                        [--output <file>] 输出到文件");
        System.out.println();
        System.out.println("  generate-schema --plugin <id>          生成插件的JSON Schema");
        System.out.println("                 [--output <file>]       输出到文件");
        System.out.println();
        System.out.println("  generate-docs --plugin <id>            生成插件文档");
        System.out.println("               [--output <file>]         输出到文件");
        System.out.println();
        System.out.println("  generate-overview                       生成所有插件概览");
        System.out.println("                   [--output <file>]     输出到文件");
        System.out.println();
        System.out.println("  help                                    显示此帮助信息");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  # 列出所有插件");
        System.out.println(
                "  mvn exec:java -Dexec.mainClass=\"com.logflow.tools.PluginConfigTool\" -Dexec.args=\"list\"");
        System.out.println();
        System.out.println("  # 生成 file 插件的配置模板");
        System.out.println("  mvn exec:java -Dexec.mainClass=\"com.logflow.tools.PluginConfigTool\" \\");
        System.out.println("      -Dexec.args=\"generate-template --plugin file\"");
        System.out.println();
        System.out.println("  # 生成配置模板并保存到文件");
        System.out.println("  mvn exec:java -Dexec.mainClass=\"com.logflow.tools.PluginConfigTool\" \\");
        System.out.println("      -Dexec.args=\"generate-template --plugin file --output file-plugin-config.yml\"");
    }

    private static void listPlugins(PluginManager pluginManager) {
        System.out.println("可用插件列表:");
        System.out.println("========================================");

        Collection<PluginManager.PluginInfo> plugins = pluginManager.getPluginInfos();

        if (plugins.isEmpty()) {
            System.out.println("没有找到可用的插件。");
            return;
        }

        for (PluginManager.PluginInfo pluginInfo : plugins) {
            try {
                var plugin = pluginManager.getPlugin(pluginInfo.getId());
                System.out.printf("插件ID: %s%n", plugin.getPluginId());
                System.out.printf("名称: %s%n", plugin.getPluginName());
                System.out.printf("版本: %s%n", plugin.getVersion());
                System.out.printf("描述: %s%n", plugin.getDescription());
                System.out.printf("作者: %s%n", plugin.getAuthor());
                System.out.printf("参数数量: %d%n", plugin.getSupportedParameters().size());
                System.out.println("----------------------------------------");
            } catch (Exception e) {
                System.err.printf("获取插件信息失败: %s - %s%n", pluginInfo.getId(), e.getMessage());
            }
        }
    }

    private static void generateTemplate(PluginConfigurationGenerator generator, String[] args) {
        String pluginId = getArgumentValue(args, "--plugin");
        String outputFile = getArgumentValue(args, "--output");

        try {
            String template = generator.generatePluginTemplate(pluginId, true);

            if (outputFile != null) {
                writeToFile(template, outputFile);
                System.out.println("配置模板已生成: " + outputFile);
            } else {
                System.out.println("配置模板:");
                System.out.println("========================================");
                System.out.println(template);
            }
        } catch (Exception e) {
            System.err.println("生成配置模板失败: " + e.getMessage());
        }
    }

    private static void generateAllTemplates(PluginConfigurationGenerator generator, String[] args) {
        String outputFile = getArgumentValue(args, "--output");

        try {
            String templates = generator.generateAllPluginTemplates();

            if (outputFile != null) {
                writeToFile(templates, outputFile);
                System.out.println("所有配置模板已生成: " + outputFile);
            } else {
                System.out.println("所有插件配置模板:");
                System.out.println("========================================");
                System.out.println(templates);
            }
        } catch (Exception e) {
            System.err.println("生成配置模板失败: " + e.getMessage());
        }
    }

    private static void generateSchema(PluginConfigurationGenerator generator, String[] args) {
        String pluginId = getArgumentValue(args, "--plugin");
        String outputFile = getArgumentValue(args, "--output");

        try {
            String schema = generator.generateJsonSchema(pluginId);

            if (outputFile != null) {
                writeToFile(schema, outputFile);
                System.out.println("JSON Schema 已生成: " + outputFile);
            } else {
                System.out.println("JSON Schema:");
                System.out.println("========================================");
                System.out.println(schema);
            }
        } catch (Exception e) {
            System.err.println("生成 JSON Schema 失败: " + e.getMessage());
        }
    }

    private static void generateDocs(PluginConfigurationGenerator generator, String[] args) {
        String pluginId = getArgumentValue(args, "--plugin");
        String outputFile = getArgumentValue(args, "--output");

        try {
            String docs = generator.generatePluginDocumentation(pluginId);

            if (outputFile != null) {
                writeToFile(docs, outputFile);
                System.out.println("插件文档已生成: " + outputFile);
            } else {
                System.out.println("插件文档:");
                System.out.println("========================================");
                System.out.println(docs);
            }
        } catch (Exception e) {
            System.err.println("生成插件文档失败: " + e.getMessage());
        }
    }

    private static void generateOverview(PluginConfigurationGenerator generator, String[] args) {
        String outputFile = getArgumentValue(args, "--output");

        try {
            String overview = generator.generatePluginsOverview();

            if (outputFile != null) {
                writeToFile(overview, outputFile);
                System.out.println("插件概览已生成: " + outputFile);
            } else {
                System.out.println("插件概览:");
                System.out.println("========================================");
                System.out.println(overview);
            }
        } catch (Exception e) {
            System.err.println("生成插件概览失败: " + e.getMessage());
        }
    }

    private static String getArgumentValue(String[] args, String argument) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(argument)) {
                return args[i + 1];
            }
        }
        return null;
    }

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
