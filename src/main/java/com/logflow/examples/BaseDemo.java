package com.logflow.examples;

import com.logflow.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 演示程序基类
 * 提供正确的资源管理模式，确保所有后台线程正确关闭
 */
public abstract class BaseDemo {

    private static final Logger logger = LoggerFactory.getLogger(BaseDemo.class);
    private static volatile boolean shutdownHookAdded = false;

    /**
     * 初始化演示程序
     * 添加 JVM shutdown hook 确保资源正确关闭
     */
    protected static void initializeDemo(String demoName) {
        System.out.println("=== " + demoName + " ===\n");

        // 确保 shutdown hook 只添加一次
        if (!shutdownHookAdded) {
            synchronized (BaseDemo.class) {
                if (!shutdownHookAdded) {
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        System.out.println("\n🔄 正在关闭系统资源...");
                        cleanupResources();
                    }, "LogFlow-Shutdown"));
                    shutdownHookAdded = true;
                    logger.debug("已添加 JVM shutdown hook");
                }
            }
        }
    }

    /**
     * 清理系统资源
     * 确保插件管理器和所有后台线程正确关闭
     */
    protected static void cleanupResources() {
        try {
            PluginManager pluginManager = PluginManager.getInstance();
            if (pluginManager != null) {
                pluginManager.destroy();
                System.out.println("✅ 系统资源已安全关闭");
            }
        } catch (Exception e) {
            System.err.println("❌ 关闭系统资源时出错: " + e.getMessage());
            logger.error("关闭系统资源失败", e);
        }
    }

    /**
     * 完成演示程序
     * 手动清理资源并显示完成信息
     */
    protected static void finalizeDemo() {
        System.out.println("\n🔧 演示完成：正确的资源管理");
        cleanupResources();
        System.out.println("🎉 演示程序执行完毕");
    }

    /**
     * 安全执行演示逻辑
     * 自动处理异常和资源清理
     */
    protected static void safeExecute(String demoName, Runnable demoLogic) {
        try {
            initializeDemo(demoName);
            demoLogic.run();
        } catch (Exception e) {
            System.err.println("❌ 演示执行失败: " + e.getMessage());
            logger.error("演示执行异常", e);
        } finally {
            finalizeDemo();
        }
    }

    /**
     * 显示资源管理提示
     */
    protected static void showResourceManagementTips() {
        System.out.println("\n💡 资源管理最佳实践：");
        System.out.println("   ✅ 添加 JVM shutdown hook 处理意外退出");
        System.out.println("   ✅ 在 finally 块中清理资源");
        System.out.println("   ✅ 调用 PluginManager.destroy() 关闭插件");
        System.out.println("   ✅ 使用 try-with-resources 管理连接");
        System.out.println("   ✅ 监控后台线程的生命周期");
        System.out.println("   ⚠️  避免在静态初始化中创建线程");
    }
}
