package com.logflow.nodes;

import com.logflow.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 输出节点
 * 用于输出处理结果到不同的目标
 * 
 * @deprecated 该类已被 {@link NotificationNode} 替代。
 *             NotificationNode 提供了更强大的通知功能，包括文件输出、上下文输出、邮件通知、钉钉通知等。
 *             建议迁移到 NotificationNode：
 *             - 文件输出：使用 FileNotificationProvider
 *             - 上下文输出：使用 ContextNotificationProvider
 *             - 控制台输出：使用 ConsoleNotificationProvider
 * 
 *             该类将在未来版本中移除。
 */
@Deprecated
public class OutputNode extends AbstractWorkflowNode {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OutputNode(String id, String name) {
        super(id, name, NodeType.OUTPUT);
    }

    @Override
    protected NodeExecutionResult doExecute(WorkflowContext context) throws WorkflowException {
        String inputKey = getConfigValue("inputKey", String.class, "input");
        String outputType = getConfigValue("outputType", String.class, "console");

        // 获取要输出的数据
        Object data = context.getData(inputKey);
        if (data == null) {
            logger.warn("输出数据为空, 输入键: {}", inputKey);
            data = "null";
        }

        try {
            switch (outputType.toLowerCase()) {
                case "console":
                    outputToConsole(data);
                    break;
                case "file":
                    outputToFile(data);
                    break;
                case "json":
                    outputToJson(data);
                    break;
                case "context":
                    outputToContext(data, context);
                    break;
                default:
                    throw new WorkflowException(id, "不支持的输出类型: " + outputType);
            }

            logger.info("数据输出完成, 类型: {}", outputType);
            return NodeExecutionResult.success(id, "输出成功");

        } catch (Exception e) {
            throw new WorkflowException(id, "输出失败", e);
        }
    }

    @Override
    public ValidationResult validate() {
        ValidationResult.Builder builder = ValidationResult.builder();

        String outputType = getConfigValue("outputType", String.class, "console");

        // 根据输出类型验证相应配置
        switch (outputType.toLowerCase()) {
            case "file":
                String filePath = getConfigValue("filePath", String.class);
                if (filePath == null || filePath.trim().isEmpty()) {
                    builder.error("文件输出类型需要配置 filePath");
                }
                break;
            case "context":
                String contextKey = getConfigValue("contextKey", String.class);
                if (contextKey == null || contextKey.trim().isEmpty()) {
                    builder.error("上下文输出类型需要配置 contextKey");
                }
                break;
        }

        return builder.build();
    }

    /**
     * 输出到控制台
     */
    private void outputToConsole(Object data) {
        String format = getConfigValue("format", String.class, "text");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        if ("json".equals(format)) {
            try {
                String jsonString = objectMapper.writeValueAsString(data);
                System.out.println("[" + timestamp + "] " + jsonString);
            } catch (Exception e) {
                System.out.println("[" + timestamp + "] " + data.toString());
            }
        } else {
            System.out.println("[" + timestamp + "] " + data.toString());
        }
    }

    /**
     * 输出到文件
     */
    private void outputToFile(Object data) throws IOException {
        String filePath = getConfigValue("filePath", String.class);
        boolean append = getConfigValue("append", Boolean.class, true);

        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(file, append)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            writer.write("[" + timestamp + "] " + data.toString() + "\n");
        }
    }

    /**
     * 输出为JSON格式
     */
    private void outputToJson(Object data) throws IOException {
        String filePath = getConfigValue("filePath", String.class);
        if (filePath != null) {
            // 输出到文件
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                parentDir.mkdirs();
            }
            objectMapper.writeValue(file, data);
        } else {
            // 输出到控制台
            String jsonString = objectMapper.writeValueAsString(data);
            System.out.println(jsonString);
        }
    }

    /**
     * 输出到上下文
     */
    private void outputToContext(Object data, WorkflowContext context) {
        String contextKey = getConfigValue("contextKey", String.class);
        context.setData(contextKey, data);
        logger.info("数据已保存到上下文, 键: {}", contextKey);
    }
}
