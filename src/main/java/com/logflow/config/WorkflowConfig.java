package com.logflow.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 工作流YAML配置根对象
 */
public class WorkflowConfig {

    @JsonProperty("workflow")
    private WorkflowInfo workflow;

    @JsonProperty("nodes")
    private List<NodeConfig> nodes;

    @JsonProperty("connections")
    private List<ConnectionConfig> connections;

    @JsonProperty("globalConfig")
    private Map<String, Object> globalConfig;

    // Getters and Setters
    public WorkflowInfo getWorkflow() {
        return workflow;
    }

    public void setWorkflow(WorkflowInfo workflow) {
        this.workflow = workflow;
    }

    public List<NodeConfig> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeConfig> nodes) {
        this.nodes = nodes;
    }

    public List<ConnectionConfig> getConnections() {
        return connections;
    }

    public void setConnections(List<ConnectionConfig> connections) {
        this.connections = connections;
    }

    public Map<String, Object> getGlobalConfig() {
        return globalConfig;
    }

    public void setGlobalConfig(Map<String, Object> globalConfig) {
        this.globalConfig = globalConfig;
    }

    /**
     * 工作流基本信息
     */
    public static class WorkflowInfo {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("version")
        private String version;

        @JsonProperty("author")
        private String author;

        @JsonProperty("metadata")
        private Map<String, Object> metadata;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

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

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * 节点配置
     */
    public static class NodeConfig {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("type")
        private String type;

        @JsonProperty("enabled")
        private Boolean enabled = true;

        @JsonProperty("config")
        private Map<String, Object> config;

        @JsonProperty("position")
        private Position position;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }

        public Position getPosition() {
            return position;
        }

        public void setPosition(Position position) {
            this.position = position;
        }
    }

    /**
     * 连接配置
     */
    public static class ConnectionConfig {
        @JsonProperty("from")
        private String from;

        @JsonProperty("to")
        private String to;

        @JsonProperty("condition")
        private String condition;

        @JsonProperty("enabled")
        private Boolean enabled = true;

        // Getters and Setters
        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 节点位置信息（用于可视化）
     */
    public static class Position {
        @JsonProperty("x")
        private Integer x;

        @JsonProperty("y")
        private Integer y;

        public Position() {
        }

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        // Getters and Setters
        public Integer getX() {
            return x;
        }

        public void setX(Integer x) {
            this.x = x;
        }

        public Integer getY() {
            return y;
        }

        public void setY(Integer y) {
            this.y = y;
        }
    }
}
