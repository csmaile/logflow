package com.logflow.nodes;

import com.logflow.core.*;

/**
 * 输入节点
 * 用于接收外部输入数据
 */
public class InputNode extends AbstractWorkflowNode {
    
    public InputNode(String id, String name) {
        super(id, name, NodeType.INPUT);
    }
    
    @Override
    protected NodeExecutionResult doExecute(WorkflowContext context) throws WorkflowException {
        String inputKey = getConfigValue("inputKey", String.class, "input");
        String defaultValue = getConfigValue("defaultValue", String.class);
        
        // 从上下文获取输入数据
        Object inputData = context.getData(inputKey);
        if (inputData == null && defaultValue != null) {
            inputData = defaultValue;
            logger.info("使用默认值: {}", defaultValue);
        }
        
        // 数据类型转换（如果配置了）
        String dataType = getConfigValue("dataType", String.class, "string");
        Object processedData = convertDataType(inputData, dataType);
        
        // 将数据存储到上下文中
        String outputKey = getConfigValue("outputKey", String.class, "output");
        context.setData(outputKey, processedData);
        
        logger.info("输入节点处理完成, 输入: {}, 输出键: {}", inputData, outputKey);
        
        return NodeExecutionResult.success(id, processedData);
    }
    
    @Override
    public ValidationResult validate() {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        // 检查输出键配置
        String outputKey = getConfigValue("outputKey", String.class);
        if (outputKey == null || outputKey.trim().isEmpty()) {
            builder.warning("未配置输出键，将使用默认值 'output'");
        }
        
        return builder.build();
    }
    
    /**
     * 数据类型转换
     */
    private Object convertDataType(Object data, String dataType) {
        if (data == null) {
            return null;
        }
        
        try {
            switch (dataType.toLowerCase()) {
                case "string":
                    return data.toString();
                case "integer":
                case "int":
                    if (data instanceof Number) {
                        return ((Number) data).intValue();
                    }
                    return Integer.parseInt(data.toString());
                case "long":
                    if (data instanceof Number) {
                        return ((Number) data).longValue();
                    }
                    return Long.parseLong(data.toString());
                case "double":
                    if (data instanceof Number) {
                        return ((Number) data).doubleValue();
                    }
                    return Double.parseDouble(data.toString());
                case "boolean":
                    if (data instanceof Boolean) {
                        return data;
                    }
                    return Boolean.parseBoolean(data.toString());
                default:
                    return data;
            }
        } catch (Exception e) {
            logger.warn("数据类型转换失败，保持原始类型: {}", e.getMessage());
            return data;
        }
    }
}
