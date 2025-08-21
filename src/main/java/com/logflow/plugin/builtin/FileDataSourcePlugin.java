package com.logflow.plugin.builtin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logflow.core.WorkflowContext;
import com.logflow.plugin.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 文件数据源插件
 * 支持读取JSON、CSV、文本等格式的文件
 */
public class FileDataSourcePlugin extends AbstractDataSourcePlugin {

    private ObjectMapper objectMapper;

    @Override
    protected void doInitialize(Map<String, Object> globalConfig) throws Exception {
        this.objectMapper = new ObjectMapper();
        logger.info("文件数据源插件初始化完成");
    }

    @Override
    public String getPluginId() {
        return "file";
    }

    @Override
    public String getPluginName() {
        return "File Data Source";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "读取本地文件数据的数据源插件，支持JSON、CSV、文本等格式";
    }

    @Override
    public List<PluginParameter> getSupportedParameters() {
        return Arrays.asList(
                param("filePath")
                        .displayName("文件路径")
                        .description("要读取的文件路径，支持相对路径和绝对路径")
                        .type(PluginParameter.ParameterType.FILE_PATH)
                        .required()
                        .build(),

                param("format")
                        .displayName("文件格式")
                        .description("文件数据格式")
                        .type(PluginParameter.ParameterType.ENUM)
                        .options("json", "csv", "text", "xml")
                        .defaultValue("json")
                        .required()
                        .build(),

                param("encoding")
                        .displayName("文件编码")
                        .description("文件字符编码")
                        .type(PluginParameter.ParameterType.ENUM)
                        .options("UTF-8", "GBK", "ISO-8859-1")
                        .defaultValue("UTF-8")
                        .optional()
                        .build(),

                param("csvDelimiter")
                        .displayName("CSV分隔符")
                        .description("CSV文件的字段分隔符")
                        .type(PluginParameter.ParameterType.STRING)
                        .defaultValue(",")
                        .optional()
                        .build(),

                param("csvHeader")
                        .displayName("CSV包含表头")
                        .description("CSV文件第一行是否为表头")
                        .type(PluginParameter.ParameterType.BOOLEAN)
                        .defaultValue(true)
                        .optional()
                        .build(),

                param("jsonArrayPath")
                        .displayName("JSON数组路径")
                        .description("JSON文件中数组数据的路径(JSONPath格式)")
                        .type(PluginParameter.ParameterType.STRING)
                        .optional()
                        .build(),

                param("maxLines")
                        .displayName("最大行数")
                        .description("最多读取的行数，0表示读取全部")
                        .type(PluginParameter.ParameterType.INTEGER)
                        .defaultValue(0)
                        .optional()
                        .build(),

                param("skipLines")
                        .displayName("跳过行数")
                        .description("从文件开头跳过的行数")
                        .type(PluginParameter.ParameterType.INTEGER)
                        .defaultValue(0)
                        .optional()
                        .build());
    }

    @Override
    protected void doValidateConfiguration(Map<String, Object> config, PluginValidationResult result) {
        String filePath = getStringConfig(config, "filePath", null);
        if (filePath == null || filePath.trim().isEmpty()) {
            result.addError("filePath", "文件路径不能为空");
            return;
        }

        // 检查文件是否存在
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            result.addError("filePath", "文件不存在: " + filePath);
            return;
        }

        if (!Files.isRegularFile(path)) {
            result.addError("filePath", "路径不是一个文件: " + filePath);
            return;
        }

        if (!Files.isReadable(path)) {
            result.addError("filePath", "文件不可读: " + filePath);
            return;
        }

        // 验证格式特定参数
        String format = getStringConfig(config, "format", "json");
        if ("csv".equals(format)) {
            String delimiter = getStringConfig(config, "csvDelimiter", ",");
            if (delimiter.length() != 1) {
                result.addWarning("csvDelimiter", "CSV分隔符应该是单个字符");
            }
        }

        // 验证行数参数
        int maxLines = getIntConfig(config, "maxLines", 0);
        int skipLines = getIntConfig(config, "skipLines", 0);

        if (maxLines < 0) {
            result.addError("maxLines", "最大行数不能为负数");
        }

        if (skipLines < 0) {
            result.addError("skipLines", "跳过行数不能为负数");
        }
    }

    @Override
    protected PluginTestResult doTestConnection(Map<String, Object> config) {
        try {
            String filePath = getStringConfig(config, "filePath", null);
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                return PluginTestResult.failure("文件不存在: " + filePath);
            }

            if (!Files.isReadable(path)) {
                return PluginTestResult.failure("文件不可读: " + filePath);
            }

            // 尝试读取文件头几行
            String encoding = getStringConfig(config, "encoding", "UTF-8");

            try (BufferedReader reader = Files.newBufferedReader(path,
                    java.nio.charset.Charset.forName(encoding))) {

                String firstLine = reader.readLine();
                if (firstLine == null) {
                    return PluginTestResult.failure("文件为空");
                }

                long fileSize = Files.size(path);

                return PluginTestResult.success("文件连接测试成功")
                        .withDetail("filePath", filePath)
                        .withDetail("fileSize", fileSize)
                        .withDetail("encoding", encoding)
                        .withDetail("firstLinePreview",
                                firstLine.length() > 100 ? firstLine.substring(0, 100) + "..." : firstLine);
            }

        } catch (Exception e) {
            return PluginTestResult.failure("文件测试失败", e);
        }
    }

    @Override
    public DataSourceConnection createConnection(Map<String, Object> config, WorkflowContext context)
            throws PluginException {
        checkInitialized();
        return new FileDataSourceConnection(config);
    }

    @Override
    public DataSourceSchema getSchema(Map<String, Object> config) {
        String format = getStringConfig(config, "format", "json");

        switch (format) {
            case "json":
                return DataSourceSchema.create("JsonData", "JSON格式数据")
                        .addField("data", DataSourceSchema.FieldType.JSON, true, "JSON数据内容");

            case "csv":
                return DataSourceSchema.create("CsvData", "CSV格式数据")
                        .addField("row", DataSourceSchema.FieldType.OBJECT, true, "CSV行数据");

            case "text":
                return DataSourceSchema.create("TextData", "文本数据")
                        .addField("line", DataSourceSchema.FieldType.STRING, true, "文本行内容")
                        .addField("lineNumber", DataSourceSchema.FieldType.INTEGER, true, "行号");

            default:
                return DataSourceSchema.create("FileData", "文件数据")
                        .addField("content", DataSourceSchema.FieldType.STRING, true, "文件内容");
        }
    }

    /**
     * 文件数据源连接实现
     */
    private class FileDataSourceConnection implements DataSourceConnection {

        private final Map<String, Object> config;
        private final String filePath;
        private final String format;
        private final String encoding;

        public FileDataSourceConnection(Map<String, Object> config) {
            this.config = config;
            this.filePath = getStringConfig(config, "filePath", null);
            this.format = getStringConfig(config, "format", "json");
            this.encoding = getStringConfig(config, "encoding", "UTF-8");
        }

        @Override
        public Object readData(WorkflowContext context) throws PluginException {
            try {
                logger.info("正在读取文件: {} (格式: {})", filePath, format);

                switch (format.toLowerCase()) {
                    case "json":
                        return readJsonFile();
                    case "csv":
                        return readCsvFile();
                    case "text":
                        return readTextFile();
                    case "xml":
                        return readXmlFile();
                    default:
                        throw new PluginException(getPluginId(), "不支持的文件格式: " + format);
                }

            } catch (Exception e) {
                throw PluginException.readFailed(getPluginId(), "读取文件失败: " + filePath, e);
            }
        }

        @Override
        public boolean isConnected() {
            try {
                Path path = Paths.get(filePath);
                return Files.exists(path) && Files.isReadable(path);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public Map<String, Object> getConnectionInfo() {
            Map<String, Object> info = new HashMap<>();
            info.put("pluginId", getPluginId());
            info.put("filePath", filePath);
            info.put("format", format);
            info.put("encoding", encoding);
            info.put("connected", isConnected());

            try {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    info.put("fileSize", Files.size(path));
                    info.put("lastModified", Files.getLastModifiedTime(path).toMillis());
                }
            } catch (Exception e) {
                logger.debug("获取文件信息失败", e);
            }

            return info;
        }

        @Override
        public void close() throws Exception {
            // 文件连接无需关闭资源
            logger.debug("文件数据源连接已关闭: {}", filePath);
        }

        /**
         * 读取JSON文件
         */
        private Object readJsonFile() throws Exception {
            Path path = Paths.get(filePath);

            try (Reader reader = Files.newBufferedReader(path,
                    java.nio.charset.Charset.forName(encoding))) {

                Object data = objectMapper.readValue(reader, Object.class);

                // 如果指定了JSON数组路径，尝试提取
                String arrayPath = getStringConfig(config, "jsonArrayPath", null);
                if (arrayPath != null && !arrayPath.trim().isEmpty()) {
                    // 简单的JSONPath实现（仅支持根级数组字段）
                    if (data instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) data;
                        data = map.get(arrayPath.startsWith("$.") ? arrayPath.substring(2) : arrayPath);
                    }
                }

                return data;
            }
        }

        /**
         * 读取CSV文件
         */
        private List<Map<String, Object>> readCsvFile() throws Exception {
            List<Map<String, Object>> records = new ArrayList<>();

            String delimiter = getStringConfig(config, "csvDelimiter", ",");
            boolean hasHeader = getBooleanConfig(config, "csvHeader", true);
            int skipLines = getIntConfig(config, "skipLines", 0);
            int maxLines = getIntConfig(config, "maxLines", 0);

            Path path = Paths.get(filePath);

            try (BufferedReader reader = Files.newBufferedReader(path,
                    java.nio.charset.Charset.forName(encoding))) {

                // 跳过指定行数
                for (int i = 0; i < skipLines; i++) {
                    reader.readLine();
                }

                String[] headers = null;
                String line;
                int lineCount = 0;

                while ((line = reader.readLine()) != null) {
                    if (maxLines > 0 && lineCount >= maxLines) {
                        break;
                    }

                    String[] values = line.split(delimiter, -1);

                    if (hasHeader && headers == null) {
                        headers = values;
                        continue;
                    }

                    Map<String, Object> record = new HashMap<>();

                    if (hasHeader && headers != null) {
                        for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                            record.put(headers[i].trim(), parseValue(values[i].trim()));
                        }
                    } else {
                        for (int i = 0; i < values.length; i++) {
                            record.put("col_" + i, parseValue(values[i].trim()));
                        }
                    }

                    records.add(record);
                    lineCount++;
                }
            }

            return records;
        }

        /**
         * 读取文本文件
         */
        private List<Map<String, Object>> readTextFile() throws Exception {
            List<Map<String, Object>> lines = new ArrayList<>();

            int skipLines = getIntConfig(config, "skipLines", 0);
            int maxLines = getIntConfig(config, "maxLines", 0);

            Path path = Paths.get(filePath);

            try (BufferedReader reader = Files.newBufferedReader(path,
                    java.nio.charset.Charset.forName(encoding))) {

                // 跳过指定行数
                for (int i = 0; i < skipLines; i++) {
                    reader.readLine();
                }

                String line;
                int lineNumber = skipLines + 1;
                int readCount = 0;

                while ((line = reader.readLine()) != null) {
                    if (maxLines > 0 && readCount >= maxLines) {
                        break;
                    }

                    Map<String, Object> record = new HashMap<>();
                    record.put("line", line);
                    record.put("lineNumber", lineNumber);

                    lines.add(record);
                    lineNumber++;
                    readCount++;
                }
            }

            return lines;
        }

        /**
         * 读取XML文件（简单实现）
         */
        private String readXmlFile() throws Exception {
            Path path = Paths.get(filePath);
            return Files.readString(path, java.nio.charset.Charset.forName(encoding));
        }

        /**
         * 解析CSV值
         */
        private Object parseValue(String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }

            // 尝试解析为数字
            try {
                if (value.contains(".")) {
                    return Double.parseDouble(value);
                } else {
                    return Long.parseLong(value);
                }
            } catch (NumberFormatException e) {
                // 不是数字，继续其他解析
            }

            // 尝试解析为布尔值
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.parseBoolean(value);
            }

            // 默认作为字符串
            return value;
        }
    }
}
