package com.logflow.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * 插件安全验证器
 * 负责验证插件的安全性，包括文件完整性、权限检查、恶意代码检测等
 */
public class PluginSecurityValidator {

    private static final Logger logger = LoggerFactory.getLogger(PluginSecurityValidator.class);

    // 危险类和方法模式
    private static final Set<String> DANGEROUS_CLASSES = Set.of(
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.io.FileWriter",
            "java.io.FileOutputStream",
            "java.nio.file.Files",
            "java.lang.System",
            "java.lang.Class",
            "java.lang.reflect.Method",
            "java.net.URLClassLoader",
            "javax.script.ScriptEngine");

    private static final Set<String> DANGEROUS_METHODS = Set.of(
            "exec",
            "getRuntime",
            "loadLibrary",
            "load",
            "exit",
            "setProperty",
            "setSecurityManager",
            "forName",
            "getDeclaredMethod",
            "newInstance");

    private static final Set<String> ALLOWED_PACKAGES = Set.of(
            "java.lang",
            "java.util",
            "java.io",
            "java.nio",
            "java.time",
            "java.math",
            "java.text",
            "java.net",
            "java.security",
            "java.sql",
            "javax.sql",
            "org.slf4j",
            "com.fasterxml.jackson",
            "com.logflow.plugin",
            "com.logflow.core");

    private static final Pattern SUSPICIOUS_PATTERN = Pattern.compile(
            "(?i)(password|secret|key|token|credential|private|confidential)");

    private final Set<String> trustedAuthors = new HashSet<>();
    private final Map<String, String> knownPluginHashes = new HashMap<>();
    private boolean strictMode = false;

    public PluginSecurityValidator() {
        // 添加默认受信任的作者
        trustedAuthors.add("LogFlow Team");
        trustedAuthors.add("logflow.official");
    }

    /**
     * 启用严格模式
     * 在严格模式下，将进行更严格的安全检查
     */
    public void enableStrictMode() {
        this.strictMode = true;
        logger.info("插件安全验证器已启用严格模式");
    }

    /**
     * 添加受信任的作者
     */
    public void addTrustedAuthor(String author) {
        trustedAuthors.add(author);
        logger.debug("添加受信任作者: {}", author);
    }

    /**
     * 添加已知插件的哈希值
     */
    public void addKnownPluginHash(String pluginId, String hash) {
        knownPluginHashes.put(pluginId, hash);
        logger.debug("添加已知插件哈希: {} -> {}", pluginId, hash);
    }

    /**
     * 验证插件安全性
     */
    public PluginSecurityResult validatePlugin(DataSourcePlugin plugin, String jarPath) {
        logger.info("开始验证插件安全性: {} ({})", plugin.getPluginName(), plugin.getPluginId());

        PluginSecurityResult result = new PluginSecurityResult(plugin.getPluginId());

        try {
            // 1. 验证插件基本信息
            validateBasicInfo(plugin, result);

            // 2. 验证JAR文件（如果提供）
            if (jarPath != null && !jarPath.trim().isEmpty()) {
                validateJarFile(jarPath, result);
            }

            // 3. 验证插件实现
            validatePluginImplementation(plugin, result);

            // 4. 检查作者信任度
            validateAuthorTrust(plugin, result);

            // 5. 验证文件完整性（如果有已知哈希）
            if (jarPath != null && knownPluginHashes.containsKey(plugin.getPluginId())) {
                validateFileIntegrity(jarPath, plugin.getPluginId(), result);
            }

            // 6. 严格模式额外检查
            if (strictMode) {
                performStrictModeChecks(plugin, jarPath, result);
            }

        } catch (Exception e) {
            logger.error("插件安全验证异常", e);
            result.addCriticalIssue("验证过程异常: " + e.getMessage());
        }

        // 确定最终安全等级
        result.determineSecurityLevel();

        logger.info("插件安全验证完成: {} - 安全等级: {}, 问题数: {}",
                plugin.getPluginId(), result.getSecurityLevel(), result.getAllIssues().size());

        return result;
    }

    /**
     * 验证JAR文件
     */
    public PluginSecurityResult validateJarFile(String jarPath) {
        PluginSecurityResult result = new PluginSecurityResult(jarPath);
        validateJarFile(jarPath, result);
        result.determineSecurityLevel();
        return result;
    }

    /**
     * 验证插件基本信息
     */
    private void validateBasicInfo(DataSourcePlugin plugin, PluginSecurityResult result) {
        // 检查插件ID
        String pluginId = plugin.getPluginId();
        if (pluginId == null || pluginId.trim().isEmpty()) {
            result.addCriticalIssue("插件ID不能为空");
        } else if (!isValidPluginId(pluginId)) {
            result.addWarning("插件ID格式可疑: " + pluginId);
        }

        // 检查插件名称
        String pluginName = plugin.getPluginName();
        if (pluginName == null || pluginName.trim().isEmpty()) {
            result.addMinorIssue("插件名称为空");
        }

        // 检查版本号
        String version = plugin.getVersion();
        if (version == null || version.trim().isEmpty()) {
            result.addMinorIssue("版本号为空");
        } else if (!isValidVersion(version)) {
            result.addWarning("版本号格式不规范: " + version);
        }

        // 检查作者信息
        String author = plugin.getAuthor();
        if (author == null || author.trim().isEmpty()) {
            result.addMinorIssue("作者信息为空");
        }

        // 检查描述信息
        String description = plugin.getDescription();
        if (description == null || description.trim().isEmpty()) {
            result.addMinorIssue("描述信息为空");
        } else if (containsSuspiciousContent(description)) {
            result.addWarning("描述信息包含可疑内容");
        }
    }

    /**
     * 验证JAR文件
     */
    private void validateJarFile(String jarPath, PluginSecurityResult result) {
        File jarFile = new File(jarPath);

        // 检查文件存在性
        if (!jarFile.exists()) {
            result.addCriticalIssue("JAR文件不存在: " + jarPath);
            return;
        }

        // 检查文件大小
        long fileSize = jarFile.length();
        if (fileSize == 0) {
            result.addCriticalIssue("JAR文件为空");
            return;
        } else if (fileSize > 100 * 1024 * 1024) { // 100MB
            result.addWarning("JAR文件过大: " + (fileSize / 1024 / 1024) + "MB");
        }

        // 检查JAR文件内容
        try (JarFile jar = new JarFile(jarFile)) {
            validateJarContents(jar, result);
        } catch (IOException e) {
            result.addCriticalIssue("无法读取JAR文件: " + e.getMessage());
        }
    }

    /**
     * 验证JAR文件内容
     */
    private void validateJarContents(JarFile jarFile, PluginSecurityResult result) {
        Set<String> suspiciousEntries = new HashSet<>();
        Set<String> classFiles = new HashSet<>();
        boolean hasServiceFile = false;

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // 检查服务文件
            if (entryName.equals("META-INF/services/com.logflow.plugin.DataSourcePlugin")) {
                hasServiceFile = true;
            }

            // 检查类文件
            if (entryName.endsWith(".class")) {
                classFiles.add(entryName);

                // 检查可疑的类名
                if (containsDangerousClassName(entryName)) {
                    suspiciousEntries.add(entryName);
                }
            }

            // 检查可疑文件
            if (isSuspiciousFile(entryName)) {
                suspiciousEntries.add(entryName);
            }

            // 检查文件大小
            if (entry.getSize() > 10 * 1024 * 1024) { // 10MB单个文件
                result.addWarning("发现大文件: " + entryName + " (" + entry.getSize() / 1024 / 1024 + "MB)");
            }
        }

        // 检查结果
        if (!hasServiceFile) {
            result.addCriticalIssue("缺少SPI服务声明文件");
        }

        if (classFiles.isEmpty()) {
            result.addCriticalIssue("JAR文件中没有类文件");
        }

        if (!suspiciousEntries.isEmpty()) {
            result.addWarning("发现可疑文件: " + suspiciousEntries);
        }
    }

    /**
     * 验证插件实现
     */
    private void validatePluginImplementation(DataSourcePlugin plugin, PluginSecurityResult result) {
        try {
            // 检查参数定义
            List<PluginParameter> parameters = plugin.getSupportedParameters();
            validateParameters(parameters, result);

            // 检查依赖列表
            List<String> dependencies = plugin.getDependencies();
            validateDependencies(dependencies, result);

            // 尝试获取模式信息（可能会暴露问题）
            try {
                DataSourceSchema schema = plugin.getSchema(Map.of());
                if (schema != null) {
                    validateSchema(schema, result);
                }
            } catch (Exception e) {
                // Schema获取失败不是关键问题
                result.addMinorIssue("获取数据模式失败: " + e.getMessage());
            }

        } catch (Exception e) {
            result.addCriticalIssue("插件实现验证失败: " + e.getMessage());
        }
    }

    /**
     * 验证参数定义
     */
    private void validateParameters(List<PluginParameter> parameters, PluginSecurityResult result) {
        if (parameters == null) {
            result.addMinorIssue("参数列表为null");
            return;
        }

        Set<String> paramNames = new HashSet<>();

        for (PluginParameter param : parameters) {
            if (param.getName() == null || param.getName().trim().isEmpty()) {
                result.addWarning("发现空参数名");
                continue;
            }

            String paramName = param.getName();

            // 检查重复参数名
            if (paramNames.contains(paramName)) {
                result.addWarning("重复的参数名: " + paramName);
            }
            paramNames.add(paramName);

            // 检查敏感参数
            if (SUSPICIOUS_PATTERN.matcher(paramName).find()) {
                if (!param.isSensitive()) {
                    result.addWarning("敏感参数未标记为sensitive: " + paramName);
                }
            }

            // 检查参数验证
            if (param.getValidation() != null && param.getValidation().contains("eval")) {
                result.addCriticalIssue("参数验证包含可疑代码: " + paramName);
            }
        }
    }

    /**
     * 验证依赖列表
     */
    private void validateDependencies(List<String> dependencies, PluginSecurityResult result) {
        if (dependencies == null || dependencies.isEmpty()) {
            return;
        }

        for (String dependency : dependencies) {
            if (dependency == null || dependency.trim().isEmpty()) {
                result.addMinorIssue("发现空依赖声明");
                continue;
            }

            // 检查可疑依赖
            if (containsDangerousDependency(dependency)) {
                result.addWarning("可疑依赖: " + dependency);
            }
        }
    }

    /**
     * 验证数据模式
     */
    private void validateSchema(DataSourceSchema schema, PluginSecurityResult result) {
        if (schema.getName() == null || schema.getName().trim().isEmpty()) {
            result.addMinorIssue("数据模式名称为空");
        }

        if (schema.getFields() == null || schema.getFields().isEmpty()) {
            result.addMinorIssue("数据模式字段为空");
        }
    }

    /**
     * 验证作者信任度
     */
    private void validateAuthorTrust(DataSourcePlugin plugin, PluginSecurityResult result) {
        String author = plugin.getAuthor();

        if (author == null || author.trim().isEmpty()) {
            result.addMinorIssue("作者信息为空");
            return;
        }

        if (trustedAuthors.contains(author)) {
            result.addInfo("作者受信任: " + author);
        } else {
            result.addWarning("作者未在受信任列表中: " + author);
        }
    }

    /**
     * 验证文件完整性
     */
    private void validateFileIntegrity(String jarPath, String pluginId, PluginSecurityResult result) {
        try {
            String expectedHash = knownPluginHashes.get(pluginId);
            String actualHash = calculateFileHash(jarPath);

            if (expectedHash.equals(actualHash)) {
                result.addInfo("文件完整性验证通过");
            } else {
                result.addCriticalIssue("文件完整性验证失败，可能被篡改");
            }

        } catch (Exception e) {
            result.addWarning("文件完整性验证失败: " + e.getMessage());
        }
    }

    /**
     * 执行严格模式检查
     */
    private void performStrictModeChecks(DataSourcePlugin plugin, String jarPath, PluginSecurityResult result) {
        // 严格模式下的额外检查

        // 1. 检查插件类名是否符合规范
        String className = plugin.getClass().getName();
        if (!className.endsWith("Plugin") && !className.endsWith("DataSource")) {
            result.addWarning("插件类名不符合命名规范: " + className);
        }

        // 2. 检查包名
        if (!className.startsWith("com.") && !className.startsWith("org.")) {
            result.addWarning("插件包名不符合Java规范: " + className);
        }

        // 3. 尝试测试连接（如果实现了）
        try {
            PluginTestResult testResult = plugin.testConnection(Map.of());
            if (testResult == null) {
                result.addMinorIssue("测试连接返回null");
            }
        } catch (Exception e) {
            // 测试连接失败是正常的，因为没有提供有效配置
            result.addInfo("测试连接预期失败: " + e.getMessage());
        }
    }

    // 辅助方法

    private boolean isValidPluginId(String pluginId) {
        return pluginId.matches("^[a-z0-9_-]+$") && !pluginId.contains("..");
    }

    private boolean isValidVersion(String version) {
        return version.matches("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9-]+)?$");
    }

    private boolean containsSuspiciousContent(String content) {
        return SUSPICIOUS_PATTERN.matcher(content).find() ||
                content.contains("eval") ||
                content.contains("exec") ||
                content.contains("script");
    }

    private boolean containsDangerousClassName(String className) {
        String classNameWithoutExt = className.replace(".class", "").replace("/", ".");

        return DANGEROUS_CLASSES.stream().anyMatch(classNameWithoutExt::contains) ||
                DANGEROUS_METHODS.stream().anyMatch(classNameWithoutExt::contains);
    }

    private boolean isSuspiciousFile(String fileName) {
        return fileName.endsWith(".exe") ||
                fileName.endsWith(".dll") ||
                fileName.endsWith(".so") ||
                fileName.endsWith(".sh") ||
                fileName.endsWith(".bat") ||
                fileName.contains("..") ||
                fileName.startsWith("/") ||
                fileName.contains("META-INF/MANIFEST.MF") && fileName.contains("Main-Class");
    }

    private boolean containsDangerousDependency(String dependency) {
        String lower = dependency.toLowerCase();
        return lower.contains("runtime") ||
                lower.contains("process") ||
                lower.contains("script") ||
                lower.contains("eval") ||
                lower.contains("unsafe");
    }

    private String calculateFileHash(String filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Path path = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(path);
        byte[] hashBytes = digest.digest(fileBytes);

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * 插件安全验证结果
     */
    public static class PluginSecurityResult {
        private final String pluginId;
        private final List<SecurityIssue> issues = new ArrayList<>();
        private SecurityLevel securityLevel = SecurityLevel.UNKNOWN;

        public PluginSecurityResult(String pluginId) {
            this.pluginId = pluginId;
        }

        public void addCriticalIssue(String message) {
            issues.add(new SecurityIssue(SecurityLevel.CRITICAL, message));
        }

        public void addWarning(String message) {
            issues.add(new SecurityIssue(SecurityLevel.WARNING, message));
        }

        public void addMinorIssue(String message) {
            issues.add(new SecurityIssue(SecurityLevel.MINOR, message));
        }

        public void addInfo(String message) {
            issues.add(new SecurityIssue(SecurityLevel.INFO, message));
        }

        public void determineSecurityLevel() {
            if (issues.stream().anyMatch(issue -> issue.getLevel() == SecurityLevel.CRITICAL)) {
                securityLevel = SecurityLevel.CRITICAL;
            } else if (issues.stream().anyMatch(issue -> issue.getLevel() == SecurityLevel.WARNING)) {
                securityLevel = SecurityLevel.WARNING;
            } else if (issues.stream().anyMatch(issue -> issue.getLevel() == SecurityLevel.MINOR)) {
                securityLevel = SecurityLevel.MINOR;
            } else {
                securityLevel = SecurityLevel.SAFE;
            }
        }

        // Getters
        public String getPluginId() {
            return pluginId;
        }

        public List<SecurityIssue> getAllIssues() {
            return new ArrayList<>(issues);
        }

        public SecurityLevel getSecurityLevel() {
            return securityLevel;
        }

        public List<SecurityIssue> getIssuesByLevel(SecurityLevel level) {
            return issues.stream()
                    .filter(issue -> issue.getLevel() == level)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        public boolean isSafe() {
            return securityLevel == SecurityLevel.SAFE || securityLevel == SecurityLevel.INFO;
        }

        public boolean hasRisk() {
            return securityLevel == SecurityLevel.WARNING || securityLevel == SecurityLevel.CRITICAL;
        }

        @Override
        public String toString() {
            return String.format("PluginSecurityResult{pluginId='%s', level=%s, issues=%d}",
                    pluginId, securityLevel, issues.size());
        }
    }

    /**
     * 安全问题
     */
    public static class SecurityIssue {
        private final SecurityLevel level;
        private final String message;
        private final long timestamp;

        public SecurityIssue(SecurityLevel level, String message) {
            this.level = level;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public SecurityLevel getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s", level, message);
        }
    }

    /**
     * 安全等级
     */
    public enum SecurityLevel {
        SAFE, // 安全
        INFO, // 信息
        MINOR, // 轻微问题
        WARNING, // 警告
        CRITICAL, // 严重问题
        UNKNOWN // 未知
    }
}
