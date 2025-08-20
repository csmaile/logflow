package com.logflow.core;

/**
 * 节点类型枚举
 */
public enum NodeType {
    /**
     * 输入节点 - 数据输入
     */
    INPUT("input", "输入节点"),
    
    /**
     * 输出节点 - 结果输出
     */
    OUTPUT("output", "输出节点"),
    
    /**
     * 数据源节点 - 从外部系统获取数据
     */
    DATASOURCE("datasource", "数据源节点"),
    
    /**
     * 诊断节点 - 执行诊断逻辑
     */
    DIAGNOSIS("diagnosis", "诊断节点"),
    
    /**
     * 脚本节点 - 执行动态脚本
     */
    SCRIPT("script", "脚本节点"),
    
    /**
     * 决策节点 - 条件判断
     */
    DECISION("decision", "决策节点"),
    
    /**
     * 聚合节点 - 数据聚合处理
     */
    AGGREGATION("aggregation", "聚合节点");
    
    private final String code;
    private final String description;
    
    NodeType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static NodeType fromCode(String code) {
        for (NodeType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown node type code: " + code);
    }
}
