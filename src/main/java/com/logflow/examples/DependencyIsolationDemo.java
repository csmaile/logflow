package com.logflow.examples;

import com.logflow.plugin.*;
import com.logflow.plugin.IsolatedPluginClassLoader.ClassLoaderStatistics;
import com.logflow.plugin.PluginResourceManager.ResourceManagementStatistics;

import java.util.Map;

/**
 * 依赖隔离和资源管理演示程序
 * 展示LogFlow插件系统的高级功能：依赖隔离和自动资源管理
 */
public class DependencyIsolationDemo {

    public static void main(String[] args) {
        System.out.println("=== LogFlow 依赖隔离和资源管理演示 ===\n");

        try {
            demonstrateDependencyIsolation();
            System.out.println();
            demonstrateResourceManagement();

        } catch (Exception e) {
            System.err.println("演示过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 演示依赖隔离功能
     */
    private static void demonstrateDependencyIsolation() throws Exception {
        System.out.println("🔧 依赖隔离功能演示：\n");

        // 获取插件管理器
        PluginManager manager = PluginManager.getInstance();
        manager.initialize();

        System.out.println("📋 依赖隔离状态检查：");
        System.out.printf("   🔒 依赖隔离: %s\n", manager.isDependencyIsolationEnabled() ? "已启用" : "已禁用");
        System.out.printf("   🛡️ 安全验证: %s\n", manager.isSecurityEnabled() ? "已启用" : "已禁用");

        System.out.println("\n📊 类加载器统计信息：");
        Map<String, ClassLoaderStatistics> classLoaderStats = manager.getClassLoaderStatistics();

        if (classLoaderStats.isEmpty()) {
            System.out.println("   📝 没有活跃的插件类加载器");
        } else {
            for (Map.Entry<String, ClassLoaderStatistics> entry : classLoaderStats.entrySet()) {
                String pluginId = entry.getKey();
                ClassLoaderStatistics stats = entry.getValue();

                System.out.printf("   📦 插件: %s\n", pluginId);
                System.out.printf("      已加载类数量: %d\n", stats.getLoadedClassCount());
                System.out.printf("      内存使用: %d KB\n", stats.getMemoryUsage() / 1024);
                System.out.printf("      JAR文件数: %d\n", stats.getJarCount());

                Map<String, Integer> packageStats = stats.getPackageStatistics();
                if (!packageStats.isEmpty()) {
                    System.out.println("      包统计:");
                    packageStats.entrySet().stream()
                            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                            .limit(5)
                            .forEach(pkg -> System.out.printf("        %s: %d 个类\n",
                                    pkg.getKey().isEmpty() ? "(默认包)" : pkg.getKey(), pkg.getValue()));
                }
                System.out.println();
            }
        }

        System.out.println("🔧 配置插件包隔离：");

        // 为mock插件添加独占包
        if (manager.hasPlugin("mock")) {
            manager.addPluginPackageForPlugin("mock", "com.example.mock.");
            manager.addSharedPackageForPlugin("mock", "com.fasterxml.jackson.");
            System.out.println("   ✅ 为Mock插件配置了包隔离策略");
        }

        // 为file插件添加独占包
        if (manager.hasPlugin("file")) {
            manager.addPluginPackageForPlugin("file", "org.apache.commons.io.");
            manager.addSharedPackageForPlugin("file", "java.nio.");
            System.out.println("   ✅ 为File插件配置了包隔离策略");
        }

        System.out.println("\n💡 依赖隔离功能特性：");
        System.out.println("   🏗️  每个插件拥有独立的类加载器");
        System.out.println("   🔒 插件依赖相互隔离，避免版本冲突");
        System.out.println("   📚 支持共享包和独占包配置");
        System.out.println("   🎯 精确控制类加载策略");
        System.out.println("   📊 详细的类加载统计信息");
    }

    /**
     * 演示资源管理功能
     */
    private static void demonstrateResourceManagement() throws Exception {
        System.out.println("🗂️ 资源管理功能演示：\n");

        PluginManager manager = PluginManager.getInstance();

        System.out.println("📊 资源管理统计信息：");
        ResourceManagementStatistics stats = manager.getResourceStatistics();

        System.out.printf("   📦 已加载插件: %d 个\n", stats.getLoadedPluginCount());
        System.out.printf("   📝 跟踪插件: %d 个\n", stats.getTrackedPluginCount());
        System.out.printf("   🔄 自动管理: %s\n", stats.isAutoManagementEnabled() ? "已启用" : "已禁用");
        System.out.printf("   💾 内存使用: %.1f%% (%d/%d MB)\n",
                stats.getMemoryUsageRatio() * 100,
                stats.getMemoryUsed() / 1024 / 1024,
                stats.getMemoryMax() / 1024 / 1024);

        System.out.printf("\n📈 卸载统计：\n");
        System.out.printf("   总卸载次数: %d\n", stats.getTotalUnloads());
        System.out.printf("   内存压力卸载: %d\n", stats.getMemoryBasedUnloads());
        System.out.printf("   空闲超时卸载: %d\n", stats.getIdleBasedUnloads());
        System.out.printf("   容量限制卸载: %d\n", stats.getCapacityBasedUnloads());

        System.out.println("\n🔧 资源管理配置演示：");

        // 配置资源管理参数
        manager.configureResourceManagement(
                10 * 60 * 1000, // 10分钟空闲超时
                0.7, // 70%内存阈值
                20 // 最大20个插件
        );
        System.out.println("   ✅ 已配置: 空闲超时=10分钟, 内存阈值=70%, 最大插件=20");

        // 演示插件使用记录
        System.out.println("\n📝 插件使用情况：");
        Map<String, PluginResourceManager.PluginUsageInfo> pluginUsage = stats.getPluginUsage();

        if (pluginUsage.isEmpty()) {
            System.out.println("   📄 没有插件使用记录");
        } else {
            for (PluginResourceManager.PluginUsageInfo usage : pluginUsage.values()) {
                System.out.printf("   📦 插件: %s\n", usage.getPluginId());
                System.out.printf("      创建时间: %tT\n", usage.getCreateTime());
                System.out.printf("      最后访问: %tT\n", usage.getLastAccessTime());
                System.out.printf("      访问次数: %d\n", usage.getAccessCount());
                System.out.printf("      空闲时长: %.1f 分钟\n", usage.getIdleDuration() / 60000.0);
                System.out.println();
            }
        }

        // 获取系统内存信息
        System.out.println("🖥️ 系统内存信息：");
        Map<String, Object> memoryInfo = manager.getSystemMemoryInfo();
        System.out.printf("   最大内存: %d MB\n", (Long) memoryInfo.get("maxMemory") / 1024 / 1024);
        System.out.printf("   总分配内存: %d MB\n", (Long) memoryInfo.get("totalMemory") / 1024 / 1024);
        System.out.printf("   已使用内存: %d MB\n", (Long) memoryInfo.get("usedMemory") / 1024 / 1024);
        System.out.printf("   可用内存: %d MB\n", (Long) memoryInfo.get("freeMemory") / 1024 / 1024);
        System.out.printf("   使用率: %.1f%%\n", (Double) memoryInfo.get("usageRatio") * 100);

        System.out.println("\n🚀 资源管理功能特性：");
        System.out.println("   ⏰ 自动清理空闲插件");
        System.out.println("   💾 基于内存压力的智能卸载");
        System.out.println("   📊 详细的使用统计和监控");
        System.out.println("   🎯 可配置的清理策略");
        System.out.println("   🔄 插件数量限制管理");
        System.out.println("   ⚡ 实时内存使用监控");

        // 演示手动强制卸载（仅作为示例，不实际执行）
        System.out.println("\n🔧 手动管理功能：");
        System.out.println("   💡 支持强制卸载指定插件");
        System.out.println("   🔄 支持启用/禁用自动管理");
        System.out.println("   ⚙️ 支持动态调整管理参数");

        System.out.println("\n✨ 演示完成！");
        System.out.println("LogFlow插件系统提供了完整的依赖隔离和资源管理解决方案，");
        System.out.println("确保插件的稳定性和系统的高效运行。");
    }
}
