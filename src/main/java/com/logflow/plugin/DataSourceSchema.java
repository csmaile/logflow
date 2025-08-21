package com.logflow.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据源模式信息
 * 描述数据源的结构和字段信息
 */
public class DataSourceSchema {

    private String name;
    private String description;
    private List<SchemaField> fields;
    private Map<String, Object> metadata;

    public DataSourceSchema() {
        this.fields = new ArrayList<>();
    }

    public DataSourceSchema(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    public static DataSourceSchema create(String name) {
        return new DataSourceSchema(name, null);
    }

    public static DataSourceSchema create(String name, String description) {
        return new DataSourceSchema(name, description);
    }

    public DataSourceSchema addField(String name, FieldType type, boolean required) {
        this.fields.add(new SchemaField(name, type, required));
        return this;
    }

    public DataSourceSchema addField(String name, FieldType type, boolean required, String description) {
        this.fields.add(new SchemaField(name, type, required, description));
        return this;
    }

    public DataSourceSchema addField(SchemaField field) {
        this.fields.add(field);
        return this;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<SchemaField> getFields() {
        return fields;
    }

    public void setFields(List<SchemaField> fields) {
        this.fields = fields;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * 查找指定名称的字段
     */
    public SchemaField findField(String fieldName) {
        return fields.stream()
                .filter(field -> field.getName().equals(fieldName))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有字段名称
     */
    public List<String> getFieldNames() {
        return fields.stream()
                .map(SchemaField::getName)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 模式字段定义
     */
    public static class SchemaField {
        private String name;
        private String displayName;
        private FieldType type;
        private boolean required;
        private String description;
        private Object defaultValue;
        private String format;
        private Map<String, Object> properties;

        public SchemaField() {
        }

        public SchemaField(String name, FieldType type, boolean required) {
            this.name = name;
            this.type = type;
            this.required = required;
        }

        public SchemaField(String name, FieldType type, boolean required, String description) {
            this(name, type, required);
            this.description = description;
        }

        public static SchemaField create(String name, FieldType type) {
            return new SchemaField(name, type, false);
        }

        public static SchemaField createRequired(String name, FieldType type) {
            return new SchemaField(name, type, true);
        }

        // Fluent API
        public SchemaField displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public SchemaField description(String description) {
            this.description = description;
            return this;
        }

        public SchemaField defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public SchemaField format(String format) {
            this.format = format;
            return this;
        }

        public SchemaField property(String key, Object value) {
            if (this.properties == null) {
                this.properties = Map.of();
            }
            this.properties.put(key, value);
            return this;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public FieldType getType() {
            return type;
        }

        public void setType(FieldType type) {
            this.type = type;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
    }

    /**
     * 字段类型枚举
     */
    public enum FieldType {
        STRING,
        INTEGER,
        LONG,
        DOUBLE,
        BOOLEAN,
        DATE,
        DATETIME,
        TIMESTAMP,
        JSON,
        BINARY,
        UUID,
        ARRAY,
        OBJECT
    }
}
