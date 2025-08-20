package com.logflow.nodes;

import com.logflow.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * 数据源节点
 * 从各种外部数据源获取数据
 */
public class DataSourceNode extends AbstractWorkflowNode {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public DataSourceNode(String id, String name) {
        super(id, name, NodeType.DATASOURCE);
    }
    
    @Override
    protected NodeExecutionResult doExecute(WorkflowContext context) throws WorkflowException {
        String sourceType = getConfigValue("sourceType", String.class);
        
        try {
            Object data = null;
            
            switch (sourceType.toLowerCase()) {
                case "file":
                    data = readFromFile();
                    break;
                case "url":
                    data = readFromUrl();
                    break;
                case "database":
                    data = readFromDatabase();
                    break;
                case "log":
                    data = readFromLogFile();
                    break;
                case "mock":
                    data = generateMockData();
                    break;
                default:
                    throw new WorkflowException(id, "不支持的数据源类型: " + sourceType);
            }
            
            // 将数据存储到上下文
            String outputKey = getConfigValue("outputKey", String.class, "data");
            context.setData(outputKey, data);
            
            logger.info("数据源读取完成, 类型: {}, 输出键: {}", sourceType, outputKey);
            
            return NodeExecutionResult.builder(id)
                    .success(true)
                    .data(data)
                    .metadata("sourceType", sourceType)
                    .metadata("recordCount", getRecordCount(data))
                    .build();
                    
        } catch (Exception e) {
            throw new WorkflowException(id, "数据源读取失败", e);
        }
    }
    
    @Override
    public ValidationResult validate() {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        // 检查数据源类型
        String sourceType = getConfigValue("sourceType", String.class);
        if (sourceType == null || sourceType.trim().isEmpty()) {
            builder.error("必须配置数据源类型 (sourceType)");
            return builder.build();
        }
        
        // 根据数据源类型验证特定配置
        switch (sourceType.toLowerCase()) {
            case "file":
                String filePath = getConfigValue("filePath", String.class);
                if (filePath == null || filePath.trim().isEmpty()) {
                    builder.error("文件数据源需要配置文件路径 (filePath)");
                }
                break;
            case "url":
                String url = getConfigValue("url", String.class);
                if (url == null || url.trim().isEmpty()) {
                    builder.error("URL数据源需要配置URL地址 (url)");
                }
                break;
            case "database":
                // 可以添加数据库连接配置验证
                break;
            case "log":
                String logPath = getConfigValue("logPath", String.class);
                if (logPath == null || logPath.trim().isEmpty()) {
                    builder.error("日志数据源需要配置日志文件路径 (logPath)");
                }
                break;
        }
        
        return builder.build();
    }
    
    /**
     * 从文件读取数据
     */
    private Object readFromFile() throws IOException {
        String filePath = getConfigValue("filePath", String.class);
        String format = getConfigValue("format", String.class, "text");
        String encoding = getConfigValue("encoding", String.class, "UTF-8");
        
        logger.info("从文件读取数据: {}, 格式: {}", filePath, format);
        
        switch (format.toLowerCase()) {
            case "json":
                return objectMapper.readValue(new File(filePath), Object.class);
            case "lines":
                return Files.readAllLines(Paths.get(filePath));
            case "text":
            default:
                return new String(Files.readAllBytes(Paths.get(filePath)), encoding);
        }
    }
    
    /**
     * 从URL读取数据
     */
    private Object readFromUrl() throws IOException {
        String urlString = getConfigValue("url", String.class);
        String format = getConfigValue("format", String.class, "text");
        
        logger.info("从URL读取数据: {}, 格式: {}", urlString, format);
        
        URL url = new URL(urlString);
        try (InputStream inputStream = url.openStream()) {
            if ("json".equals(format.toLowerCase())) {
                return objectMapper.readValue(inputStream, Object.class);
            } else {
                Scanner scanner = new Scanner(inputStream, "UTF-8");
                StringBuilder content = new StringBuilder();
                while (scanner.hasNextLine()) {
                    content.append(scanner.nextLine()).append("\n");
                }
                return content.toString();
            }
        }
    }
    
    /**
     * 从数据库读取数据（简化实现）
     */
    private Object readFromDatabase() {
        // 这里可以实现真实的数据库连接和查询
        // 目前返回模拟数据
        logger.info("从数据库读取数据（模拟）");
        
        Map<String, Object> result = new HashMap<>();
        result.put("type", "database");
        result.put("records", Arrays.asList(
            Map.of("id", 1, "message", "Database record 1"),
            Map.of("id", 2, "message", "Database record 2")
        ));
        return result;
    }
    
    /**
     * 从日志文件读取数据
     */
    private Object readFromLogFile() throws IOException {
        String logPath = getConfigValue("logPath", String.class);
        String pattern = getConfigValue("pattern", String.class);
        Integer maxLines = getConfigValue("maxLines", Integer.class, 1000);
        
        logger.info("从日志文件读取数据: {}, 最大行数: {}", logPath, maxLines);
        
        List<String> logLines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(logPath))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < maxLines) {
                if (pattern == null || line.contains(pattern)) {
                    logLines.add(line);
                    count++;
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("source", logPath);
        result.put("pattern", pattern);
        result.put("lines", logLines);
        result.put("count", logLines.size());
        
        return result;
    }
    
    /**
     * 生成模拟数据
     */
    private Object generateMockData() {
        String mockType = getConfigValue("mockType", String.class, "simple");
        Integer count = getConfigValue("count", Integer.class, 10);
        
        logger.info("生成模拟数据, 类型: {}, 数量: {}", mockType, count);
        
        List<Map<String, Object>> mockData = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", i + 1);
            record.put("timestamp", System.currentTimeMillis() + i * 1000);
            record.put("level", random.nextBoolean() ? "INFO" : "ERROR");
            record.put("message", "Mock log message " + (i + 1));
            record.put("value", random.nextDouble() * 100);
            mockData.add(record);
        }
        
        return mockData;
    }
    
    /**
     * 获取记录数量
     */
    private int getRecordCount(Object data) {
        if (data instanceof Collection) {
            return ((Collection<?>) data).size();
        } else if (data instanceof Map) {
            return ((Map<?, ?>) data).size();
        } else if (data instanceof String) {
            return ((String) data).split("\n").length;
        }
        return 1;
    }
}
