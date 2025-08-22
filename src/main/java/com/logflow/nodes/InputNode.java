package com.logflow.nodes;

import com.logflow.core.*;
import java.util.Map;

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
        InputDataProcessor.InputDataResult inputResult = processInputData(context);
        if (!inputResult.isSuccess()) {
            throw new WorkflowException(id, "多输入处理失败: " + inputResult.getErrorMessage());
        }

        Object inputData = inputResult.getData();
        Map<String, Object> inputMetadata = inputResult.getMetadata();

        // 设置输出数据
        setOutputData(context, inputData);

        logger.info("输入节点处理完成, 输入模式: {}, 数据: {}",
                inputMetadata.get("inputMode"), inputData);

        return NodeExecutionResult.builder(id)
                .success(true)
                .data(inputData)
                .metadata("inputMode", inputMetadata.get("inputMode"))
                .metadata("inputCount", inputMetadata.get("totalInputs"))
                .build();
    }

    @Override
    public ValidationResult validate() {
        ValidationResult.Builder builder = ValidationResult.builder();

        // 验证多输入配置
        MultiInputConfig inputConfig = InputDataProcessor.extractInputConfig(configuration);
        ValidationResult inputValidation = InputDataProcessor.validateInputConfig(inputConfig, id);
        if (!inputValidation.isValid()) {
            builder.errors(inputValidation.getErrors());
            builder.warnings(inputValidation.getWarnings());
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
