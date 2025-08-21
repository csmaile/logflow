package com.logflow.plugin.builtin;

import com.logflow.core.WorkflowContext;
import com.logflow.plugin.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock数据源插件
 * 用于生成模拟测试数据，支持多种数据类型和模式
 */
public class MockDataSourcePlugin extends AbstractDataSourcePlugin {

    private static final String[] LOG_LEVELS = { "DEBUG", "INFO", "WARN", "ERROR", "FATAL" };
    private static final String[] MODULES = { "UserService", "OrderService", "PaymentService", "AuthService",
            "NotificationService" };
    private static final String[] ERROR_MESSAGES = {
            "Connection timeout", "Invalid parameter", "Database error",
            "Authentication failed", "Resource not found", "Service unavailable"
    };

    @Override
    public String getPluginId() {
        return "mock";
    }

    @Override
    public String getPluginName() {
        return "Mock Data Source";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "生成模拟测试数据的数据源插件，支持多种数据类型和场景";
    }

    @Override
    public List<PluginParameter> getSupportedParameters() {
        return Arrays.asList(
                param("mockType")
                        .displayName("模拟数据类型")
                        .description("要生成的模拟数据类型")
                        .type(PluginParameter.ParameterType.ENUM)
                        .options("mixed_logs", "error_logs", "performance_logs", "user_data", "order_data", "custom")
                        .defaultValue("mixed_logs")
                        .required()
                        .build(),

                param("recordCount")
                        .displayName("记录数量")
                        .description("要生成的记录数量")
                        .type(PluginParameter.ParameterType.INTEGER)
                        .defaultValue(1000)
                        .optional()
                        .build(),

                param("errorRate")
                        .displayName("错误率(%)")
                        .description("错误日志的百分比(0-100)")
                        .type(PluginParameter.ParameterType.INTEGER)
                        .defaultValue(10)
                        .optional()
                        .build(),

                param("timeRange")
                        .displayName("时间范围(小时)")
                        .description("生成数据的时间跨度(小时)")
                        .type(PluginParameter.ParameterType.INTEGER)
                        .defaultValue(24)
                        .optional()
                        .build(),

                param("seed")
                        .displayName("随机种子")
                        .description("随机数生成种子，用于生成可重现的数据")
                        .type(PluginParameter.ParameterType.LONG)
                        .optional()
                        .build(),

                param("customSchema")
                        .displayName("自定义数据模式")
                        .description("自定义数据结构(JSON格式)")
                        .type(PluginParameter.ParameterType.JSON)
                        .optional()
                        .build());
    }

    @Override
    protected PluginTestResult doTestConnection(Map<String, Object> config) {
        try {
            // Mock插件总是可以连接
            String mockType = getStringConfig(config, "mockType", "mixed_logs");
            int recordCount = getIntConfig(config, "recordCount", 1000);

            return PluginTestResult.success("Mock数据源连接成功")
                    .withDetail("mockType", mockType)
                    .withDetail("recordCount", recordCount)
                    .withResponseTime(1); // 模拟1ms响应时间

        } catch (Exception e) {
            return PluginTestResult.failure("Mock数据源测试失败", e);
        }
    }

    @Override
    public DataSourceConnection createConnection(Map<String, Object> config, WorkflowContext context)
            throws PluginException {
        checkInitialized();
        return new MockDataSourceConnection(config);
    }

    @Override
    public DataSourceSchema getSchema(Map<String, Object> config) {
        String mockType = getStringConfig(config, "mockType", "mixed_logs");

        switch (mockType) {
            case "mixed_logs":
            case "error_logs":
                return DataSourceSchema.create("LogRecord", "系统日志记录")
                        .addField("id", DataSourceSchema.FieldType.LONG, true, "日志ID")
                        .addField("timestamp", DataSourceSchema.FieldType.TIMESTAMP, true, "时间戳")
                        .addField("level", DataSourceSchema.FieldType.STRING, true, "日志级别")
                        .addField("module", DataSourceSchema.FieldType.STRING, true, "模块名称")
                        .addField("message", DataSourceSchema.FieldType.STRING, true, "日志消息")
                        .addField("thread", DataSourceSchema.FieldType.STRING, false, "线程名称");

            case "performance_logs":
                return DataSourceSchema.create("PerformanceRecord", "性能监控记录")
                        .addField("id", DataSourceSchema.FieldType.LONG, true, "记录ID")
                        .addField("timestamp", DataSourceSchema.FieldType.TIMESTAMP, true, "时间戳")
                        .addField("operation", DataSourceSchema.FieldType.STRING, true, "操作名称")
                        .addField("responseTime", DataSourceSchema.FieldType.LONG, true, "响应时间(ms)")
                        .addField("status", DataSourceSchema.FieldType.STRING, true, "状态")
                        .addField("userId", DataSourceSchema.FieldType.STRING, false, "用户ID");

            case "user_data":
                return DataSourceSchema.create("UserRecord", "用户数据记录")
                        .addField("id", DataSourceSchema.FieldType.LONG, true, "用户ID")
                        .addField("username", DataSourceSchema.FieldType.STRING, true, "用户名")
                        .addField("email", DataSourceSchema.FieldType.STRING, true, "邮箱")
                        .addField("createdAt", DataSourceSchema.FieldType.TIMESTAMP, true, "创建时间")
                        .addField("lastLogin", DataSourceSchema.FieldType.TIMESTAMP, false, "最后登录时间")
                        .addField("status", DataSourceSchema.FieldType.STRING, true, "状态");

            default:
                return DataSourceSchema.create("GenericRecord", "通用数据记录")
                        .addField("id", DataSourceSchema.FieldType.LONG, true, "记录ID")
                        .addField("data", DataSourceSchema.FieldType.JSON, true, "数据内容")
                        .addField("timestamp", DataSourceSchema.FieldType.TIMESTAMP, true, "时间戳");
        }
    }

    /**
     * Mock数据源连接实现
     */
    private class MockDataSourceConnection implements DataSourceConnection {

        private final Map<String, Object> config;
        private final Random random;
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        public MockDataSourceConnection(Map<String, Object> config) {
            this.config = config;

            // 初始化随机数生成器
            Long seed = (Long) config.get("seed");
            this.random = seed != null ? new Random(seed) : ThreadLocalRandom.current();
        }

        @Override
        public Object readData(WorkflowContext context) throws PluginException {
            String mockType = getStringConfig(config, "mockType", "mixed_logs");
            int recordCount = getIntConfig(config, "recordCount", 1000);

            logger.info("生成 {} 类型的模拟数据，数量: {}", mockType, recordCount);

            List<Map<String, Object>> data = new ArrayList<>();

            for (int i = 0; i < recordCount; i++) {
                Map<String, Object> record = generateRecord(mockType, i + 1);
                data.add(record);
            }

            logger.info("成功生成 {} 条模拟数据", data.size());
            return data;
        }

        @Override
        public boolean isConnected() {
            return true; // Mock连接总是可用
        }

        @Override
        public Map<String, Object> getConnectionInfo() {
            return Map.of(
                    "pluginId", "mock",
                    "connected", true,
                    "mockType", getStringConfig(config, "mockType", "mixed_logs"),
                    "recordCount", getIntConfig(config, "recordCount", 1000));
        }

        @Override
        public Map<String, Object> getDataStatistics() {
            int recordCount = getIntConfig(config, "recordCount", 1000);
            return Map.of(
                    "totalRecords", recordCount,
                    "estimatedSize", recordCount * 200 // 估算每条记录200字节
            );
        }

        @Override
        public void close() throws Exception {
            // Mock连接无需关闭资源
            logger.debug("Mock数据源连接已关闭");
        }

        /**
         * 生成单条记录
         */
        private Map<String, Object> generateRecord(String mockType, long id) {
            Map<String, Object> record = new HashMap<>();

            switch (mockType) {
                case "mixed_logs":
                    return generateLogRecord(id, false);
                case "error_logs":
                    return generateLogRecord(id, true);
                case "performance_logs":
                    return generatePerformanceRecord(id);
                case "user_data":
                    return generateUserRecord(id);
                case "order_data":
                    return generateOrderRecord(id);
                default:
                    return generateGenericRecord(id);
            }
        }

        /**
         * 生成日志记录
         */
        private Map<String, Object> generateLogRecord(long id, boolean errorOnly) {
            Map<String, Object> record = new HashMap<>();

            record.put("id", id);
            record.put("timestamp", generateRandomTimestamp());

            // 生成日志级别
            String level;
            if (errorOnly) {
                level = random.nextBoolean() ? "ERROR" : "FATAL";
            } else {
                int errorRate = getIntConfig(config, "errorRate", 10);
                if (random.nextInt(100) < errorRate) {
                    level = random.nextBoolean() ? "ERROR" : "WARN";
                } else {
                    level = random.nextBoolean() ? "INFO" : "DEBUG";
                }
            }
            record.put("level", level);

            record.put("module", MODULES[random.nextInt(MODULES.length)]);
            record.put("thread", "thread-" + (random.nextInt(10) + 1));

            // 生成消息
            String message;
            if ("ERROR".equals(level) || "FATAL".equals(level)) {
                message = ERROR_MESSAGES[random.nextInt(ERROR_MESSAGES.length)] +
                        " in operation " + generateRandomOperation();
            } else {
                message = "Successfully processed " + generateRandomOperation() +
                        " for user " + generateRandomUserId();
            }
            record.put("message", message);

            return record;
        }

        /**
         * 生成性能记录
         */
        private Map<String, Object> generatePerformanceRecord(long id) {
            Map<String, Object> record = new HashMap<>();

            record.put("id", id);
            record.put("timestamp", generateRandomTimestamp());
            record.put("operation", generateRandomOperation());

            // 生成响应时间(大部分正常，少部分慢)
            long responseTime;
            if (random.nextInt(100) < 20) { // 20%慢请求
                responseTime = 1000 + random.nextInt(5000); // 1-6秒
            } else {
                responseTime = 10 + random.nextInt(500); // 10-510ms
            }
            record.put("responseTime", responseTime);

            record.put("status", responseTime > 1000 ? "SLOW" : "OK");
            record.put("userId", generateRandomUserId());

            return record;
        }

        /**
         * 生成用户记录
         */
        private Map<String, Object> generateUserRecord(long id) {
            Map<String, Object> record = new HashMap<>();

            record.put("id", id);
            record.put("username", "user" + id);
            record.put("email", "user" + id + "@example.com");
            record.put("createdAt", generateRandomTimestamp());

            if (random.nextBoolean()) {
                record.put("lastLogin", generateRandomTimestamp());
            }

            record.put("status", random.nextBoolean() ? "ACTIVE" : "INACTIVE");

            return record;
        }

        /**
         * 生成订单记录
         */
        private Map<String, Object> generateOrderRecord(long id) {
            Map<String, Object> record = new HashMap<>();

            record.put("id", id);
            record.put("userId", generateRandomUserId());
            record.put("amount", 10.0 + (random.nextDouble() * 1000));
            record.put("status", random.nextBoolean() ? "COMPLETED" : "PENDING");
            record.put("createdAt", generateRandomTimestamp());

            return record;
        }

        /**
         * 生成通用记录
         */
        private Map<String, Object> generateGenericRecord(long id) {
            Map<String, Object> record = new HashMap<>();

            record.put("id", id);
            record.put("timestamp", generateRandomTimestamp());
            record.put("data", Map.of(
                    "value", random.nextInt(1000),
                    "category", "category_" + (random.nextInt(5) + 1),
                    "flag", random.nextBoolean()));

            return record;
        }

        /**
         * 生成随机时间戳
         */
        private String generateRandomTimestamp() {
            int timeRangeHours = getIntConfig(config, "timeRange", 24);
            long timeRangeMillis = timeRangeHours * 60L * 60L * 1000L;

            long now = System.currentTimeMillis();
            long randomTime = now - random.nextLong(timeRangeMillis);

            return LocalDateTime.now().minusNanos((now - randomTime) * 1_000_000)
                    .format(dateFormatter);
        }

        /**
         * 生成随机操作名称
         */
        private String generateRandomOperation() {
            String[] operations = { "login", "logout", "query", "update", "delete", "create", "process", "validate" };
            return operations[random.nextInt(operations.length)];
        }

        /**
         * 生成随机用户ID
         */
        private String generateRandomUserId() {
            return "user_" + (1000 + random.nextInt(9000));
        }
    }
}
