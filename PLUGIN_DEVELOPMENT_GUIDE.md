# LogFlow插件开发指南

## 概述

LogFlow插件系统基于Java SPI机制，允许开发者轻松扩展数据源支持。通过实现标准接口，您可以创建支持任意数据源的插件，并通过JAR包方式分发和部署。

## 🎯 插件系统架构

### 核心组件
```
DataSourcePlugin (SPI接口)
    ↓
AbstractDataSourcePlugin (抽象基类)
    ↓
YourCustomPlugin (具体实现)
```

### 关键接口
- **`DataSourcePlugin`**: 插件核心接口，定义插件基本契约
- **`DataSourceConnection`**: 数据连接接口，处理具体的数据读取
- **`PluginManager`**: 插件管理器，负责插件的加载和生命周期管理

## 🚀 快速开始

### 1. 创建插件项目

```bash
mkdir my-datasource-plugin
cd my-datasource-plugin
```

创建 `pom.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>my-datasource-plugin</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <dependencies>
        <!-- LogFlow插件API -->
        <dependency>
            <groupId>com.logflow</groupId>
            <artifactId>logflow-plugin-api</artifactId>
            <version>1.0.0</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- 您的插件特定依赖 -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>your-driver</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</project>
```

### 2. 实现插件接口

```java
package com.example.plugin;

import com.logflow.plugin.*;
import com.logflow.core.WorkflowContext;
import java.util.*;

public class MyDataSourcePlugin extends AbstractDataSourcePlugin {
    
    @Override
    public String getPluginId() {
        return "my-datasource";  // 唯一标识符
    }
    
    @Override
    public String getPluginName() {
        return "My Data Source Plugin";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "自定义数据源插件，支持...";
    }
    
    @Override
    public String getAuthor() {
        return "Your Name";
    }
    
    @Override
    public List<PluginParameter> getSupportedParameters() {
        return Arrays.asList(
            param("host")
                .displayName("服务器地址")
                .description("数据源服务器地址")
                .type(PluginParameter.ParameterType.STRING)
                .required()
                .build(),
                
            param("port")
                .displayName("端口号")
                .description("服务器端口号")
                .type(PluginParameter.ParameterType.INTEGER)
                .defaultValue(8080)
                .optional()
                .build(),
                
            param("username")
                .displayName("用户名")
                .type(PluginParameter.ParameterType.STRING)
                .required()
                .build(),
                
            param("password")
                .displayName("密码")
                .type(PluginParameter.ParameterType.PASSWORD)
                .required()
                .sensitive()
                .build()
        );
    }
    
    @Override
    protected PluginTestResult doTestConnection(Map<String, Object> config) {
        try {
            String host = getStringConfig(config, "host", null);
            int port = getIntConfig(config, "port", 8080);
            
            // 实际的连接测试逻辑
            boolean connected = testActualConnection(host, port);
            
            if (connected) {
                return PluginTestResult.success("连接成功")
                    .withDetail("host", host)
                    .withDetail("port", port);
            } else {
                return PluginTestResult.failure("连接失败");
            }
            
        } catch (Exception e) {
            return PluginTestResult.failure("连接测试异常", e);
        }
    }
    
    @Override
    public DataSourceConnection createConnection(Map<String, Object> config, 
                                               WorkflowContext context) throws PluginException {
        checkInitialized();
        return new MyDataSourceConnection(config);
    }
    
    @Override
    public DataSourceSchema getSchema(Map<String, Object> config) {
        return DataSourceSchema.create("MyData", "我的数据结构")
            .addField("id", DataSourceSchema.FieldType.LONG, true, "记录ID")
            .addField("name", DataSourceSchema.FieldType.STRING, true, "名称")
            .addField("value", DataSourceSchema.FieldType.DOUBLE, false, "数值")
            .addField("timestamp", DataSourceSchema.FieldType.TIMESTAMP, true, "时间戳");
    }
    
    // 私有方法
    private boolean testActualConnection(String host, int port) {
        // 实现实际的连接测试逻辑
        return true;
    }
    
    // 内部连接类
    private class MyDataSourceConnection implements DataSourceConnection {
        private final Map<String, Object> config;
        
        public MyDataSourceConnection(Map<String, Object> config) {
            this.config = config;
        }
        
        @Override
        public Object readData(WorkflowContext context) throws PluginException {
            try {
                // 实现数据读取逻辑
                List<Map<String, Object>> data = new ArrayList<>();
                
                // 示例数据
                for (int i = 1; i <= 10; i++) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("id", (long) i);
                    record.put("name", "Record " + i);
                    record.put("value", Math.random() * 100);
                    record.put("timestamp", new Date());
                    data.add(record);
                }
                
                return data;
                
            } catch (Exception e) {
                throw PluginException.readFailed(getPluginId(), "读取数据失败", e);
            }
        }
        
        @Override
        public boolean isConnected() {
            // 检查连接状态
            return true;
        }
        
        @Override
        public void close() throws Exception {
            // 清理连接资源
        }
    }
}
```

### 3. 注册插件服务

创建 `src/main/resources/META-INF/services/com.logflow.plugin.DataSourcePlugin`：
```
com.example.plugin.MyDataSourcePlugin
```

### 4. 构建和打包

```bash
mvn clean package
```

这将生成 `target/my-datasource-plugin-1.0.0.jar`。

## 📋 详细开发指南

### 参数定义最佳实践

#### 1. 参数类型选择
```java
// 字符串参数
param("host").type(ParameterType.STRING)

// 数字参数  
param("port").type(ParameterType.INTEGER)
param("timeout").type(ParameterType.LONG)
param("rate").type(ParameterType.DOUBLE)

// 布尔参数
param("ssl").type(ParameterType.BOOLEAN)

// 密码参数（敏感信息）
param("password").type(ParameterType.PASSWORD).sensitive()

// 文件路径
param("keyFile").type(ParameterType.FILE_PATH)

// URL地址
param("endpoint").type(ParameterType.URL)

// 枚举选择
param("mode").type(ParameterType.ENUM).options("read", "write", "append")

// JSON配置
param("config").type(ParameterType.JSON)
```

#### 2. 参数验证
```java
@Override
protected void doValidateConfiguration(Map<String, Object> config, 
                                     PluginValidationResult result) {
    super.doValidateConfiguration(config, result);
    
    // 自定义验证逻辑
    String host = getStringConfig(config, "host", null);
    if (host != null && !isValidHost(host)) {
        result.addError("host", "无效的主机地址格式");
    }
    
    int port = getIntConfig(config, "port", 8080);
    if (port < 1 || port > 65535) {
        result.addError("port", "端口号必须在1-65535之间");
    }
}

private boolean isValidHost(String host) {
    // 实现主机地址验证逻辑
    return host.matches("^[a-zA-Z0-9.-]+$");
}
```

### 连接管理

#### 1. 连接池支持
```java
public class PooledDataSourceConnection implements DataSourceConnection {
    private final ConnectionPool pool;
    private Connection connection;
    
    public PooledDataSourceConnection(ConnectionPool pool) {
        this.pool = pool;
    }
    
    @Override
    public Object readData(WorkflowContext context) throws PluginException {
        if (connection == null) {
            connection = pool.getConnection();
        }
        
        try {
            return doReadData(connection);
        } catch (Exception e) {
            // 连接出错时释放连接
            releaseConnection();
            throw PluginException.readFailed(getPluginId(), "读取失败", e);
        }
    }
    
    @Override
    public void close() throws Exception {
        releaseConnection();
    }
    
    private void releaseConnection() {
        if (connection != null) {
            pool.releaseConnection(connection);
            connection = null;
        }
    }
}
```

#### 2. 分页读取
```java
@Override
public PagedResult readDataPaged(WorkflowContext context, int pageSize, 
                               int pageNumber) throws PluginException {
    try {
        // 计算偏移量
        int offset = (pageNumber - 1) * pageSize;
        
        // 执行分页查询
        List<Map<String, Object>> data = executePagedQuery(offset, pageSize);
        
        // 获取总数（可选，用于优化）
        long totalCount = getTotalCount();
        
        return new PagedResult(data, pageNumber, pageSize, totalCount);
        
    } catch (Exception e) {
        throw PluginException.readFailed(getPluginId(), "分页读取失败", e);
    }
}
```

#### 3. 流式读取
```java
@Override
public void readDataStream(WorkflowContext context, 
                         DataCallback callback) throws PluginException {
    try {
        // 打开数据流
        DataStream stream = openDataStream();
        
        try {
            while (stream.hasNext()) {
                Object data = stream.next();
                callback.onData(data);
            }
            callback.onComplete();
            
        } catch (Exception e) {
            callback.onError(e);
            throw e;
        } finally {
            stream.close();
        }
        
    } catch (Exception e) {
        throw PluginException.readFailed(getPluginId(), "流式读取失败", e);
    }
}
```

### 错误处理

#### 1. 异常分类
```java
public class MyDataSourcePlugin extends AbstractDataSourcePlugin {
    
    private void handleConnectionError(Exception e) throws PluginException {
        if (e instanceof TimeoutException) {
            throw PluginException.connectionFailed(getPluginId(), 
                "连接超时", e);
        } else if (e instanceof AuthenticationException) {
            throw PluginException.connectionFailed(getPluginId(), 
                "认证失败", e);
        } else {
            throw PluginException.connectionFailed(getPluginId(), 
                "未知连接错误", e);
        }
    }
    
    private void handleReadError(Exception e) throws PluginException {
        if (e instanceof DataFormatException) {
            throw PluginException.readFailed(getPluginId(), 
                "数据格式错误", e);
        } else if (e instanceof PermissionException) {
            throw PluginException.readFailed(getPluginId(), 
                "权限不足", e);
        } else {
            throw PluginException.readFailed(getPluginId(), 
                "读取数据失败", e);
        }
    }
}
```

#### 2. 重试机制
```java
private Object readDataWithRetry(WorkflowContext context) throws PluginException {
    int maxRetries = getIntConfig(config, "maxRetries", 3);
    long retryDelay = getLongConfig(config, "retryDelay", 1000);
    
    Exception lastException = null;
    
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            return doReadData(context);
            
        } catch (Exception e) {
            lastException = e;
            logger.warn("读取数据失败，尝试 {}/{}: {}", 
                attempt, maxRetries, e.getMessage());
            
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    throw PluginException.readFailed(getPluginId(), 
        "重试" + maxRetries + "次后仍然失败", lastException);
}
```

## 🔧 高级功能

### 1. 动态配置支持
```java
@Override
public DataSourceSchema getSchema(Map<String, Object> config) {
    String schemaType = getStringConfig(config, "schemaType", "default");
    
    switch (schemaType) {
        case "user":
            return createUserSchema();
        case "order":  
            return createOrderSchema();
        case "log":
            return createLogSchema();
        default:
            return createDefaultSchema();
    }
}

private DataSourceSchema createUserSchema() {
    return DataSourceSchema.create("User", "用户数据")
        .addField("id", FieldType.LONG, true, "用户ID")
        .addField("username", FieldType.STRING, true, "用户名")
        .addField("email", FieldType.STRING, true, "邮箱")
        .addField("createdAt", FieldType.TIMESTAMP, true, "创建时间");
}
```

### 2. 元数据获取
```java
@Override
public Map<String, Object> getConnectionInfo() {
    Map<String, Object> info = new HashMap<>();
    info.put("pluginId", getPluginId());
    info.put("version", getVersion());
    info.put("connected", isConnected());
    info.put("serverInfo", getServerInfo());
    info.put("statistics", getConnectionStatistics());
    return info;
}

@Override
public Map<String, Object> getDataStatistics() {
    return Map.of(
        "totalRecords", getTotalRecordCount(),
        "lastUpdate", getLastUpdateTime(),
        "dataSize", getEstimatedDataSize(),
        "compressionRatio", getCompressionRatio()
    );
}
```

### 3. 缓存支持
```java
public class CachedDataSourceConnection implements DataSourceConnection {
    private final DataSourceConnection delegate;
    private final Cache<String, Object> cache;
    
    @Override
    public Object readData(WorkflowContext context) throws PluginException {
        String cacheKey = generateCacheKey(context);
        
        Object cachedData = cache.get(cacheKey);
        if (cachedData != null) {
            logger.debug("从缓存返回数据: {}", cacheKey);
            return cachedData;
        }
        
        Object data = delegate.readData(context);
        cache.put(cacheKey, data);
        
        return data;
    }
    
    private String generateCacheKey(WorkflowContext context) {
        // 根据上下文生成缓存键
        return String.format("%s_%s_%d", 
            context.getWorkflowId(),
            context.getExecutionId(),
            System.currentTimeMillis() / 60000); // 按分钟缓存
    }
}
```

## 🧪 测试

### 1. 单元测试
```java
@Test
public void testPluginValidation() {
    MyDataSourcePlugin plugin = new MyDataSourcePlugin();
    
    // 测试有效配置
    Map<String, Object> validConfig = Map.of(
        "host", "localhost",
        "port", 8080,
        "username", "user",
        "password", "pass"
    );
    
    PluginValidationResult result = plugin.validateConfiguration(validConfig);
    assertTrue(result.isValid());
    
    // 测试无效配置
    Map<String, Object> invalidConfig = Map.of(
        "host", "",  // 空主机名
        "port", -1   // 无效端口
    );
    
    result = plugin.validateConfiguration(invalidConfig);
    assertFalse(result.isValid());
    assertTrue(result.hasErrors());
}

@Test
public void testDataReading() throws Exception {
    MyDataSourcePlugin plugin = new MyDataSourcePlugin();
    plugin.initialize(Map.of());
    
    Map<String, Object> config = createValidConfig();
    WorkflowContext context = new WorkflowContext("test", "exec1");
    
    try (DataSourceConnection connection = plugin.createConnection(config, context)) {
        Object data = connection.readData(context);
        
        assertNotNull(data);
        assertTrue(data instanceof List);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) data;
        assertFalse(records.isEmpty());
    }
}
```

### 2. 集成测试
```java
@Test
public void testPluginIntegration() throws Exception {
    // 创建工作流
    Workflow workflow = new Workflow("test", "测试工作流");
    
    // 添加插件数据源节点
    DataSourceNode sourceNode = new DataSourceNode("source", "数据源");
    sourceNode.setConfig(Map.of(
        "sourceType", "my-datasource",
        "host", "localhost",
        "port", 8080,
        "username", "test",
        "password", "test"
    ));
    workflow.addNode(sourceNode);
    
    // 执行工作流
    WorkflowEngine engine = new WorkflowEngine();
    WorkflowExecutionResult result = engine.execute(workflow, Map.of());
    
    assertTrue(result.isSuccess());
    
    engine.shutdown();
}
```

## 📦 打包和分发

### 1. Maven打包配置
```xml
<build>
    <plugins>
        <!-- 编译插件 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>11</source>
                <target>11</target>
            </configuration>
        </plugin>
        
        <!-- 依赖打包插件 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.4.1</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <transformers>
                            <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                        </transformers>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 2. 插件部署
```bash
# 复制JAR到插件目录
cp target/my-datasource-plugin-1.0.0.jar /path/to/logflow/plugins/

# 或通过程序加载
PluginManager pluginManager = PluginManager.getInstance();
pluginManager.loadPluginFromJar("/path/to/my-datasource-plugin-1.0.0.jar");
```

### 3. 插件信息文件
创建 `plugin.properties`：
```properties
plugin.id=my-datasource
plugin.name=My Data Source Plugin
plugin.version=1.0.0
plugin.description=A custom data source plugin for LogFlow
plugin.author=Your Name
plugin.website=https://example.com
plugin.license=MIT
plugin.dependencies=driver-lib:1.0.0,util-lib:2.0.0
```

## 🛡️ 安全和最佳实践

### 1. 安全考虑
- **敏感信息**: 使用 `ParameterType.PASSWORD` 和 `sensitive()` 标记敏感参数
- **输入验证**: 严格验证所有用户输入
- **权限检查**: 实现适当的访问权限验证
- **资源限制**: 限制内存使用和连接数量

### 2. 性能优化
- **连接池**: 使用连接池减少连接开销
- **批量读取**: 支持批量数据读取
- **异步处理**: 对于大数据量使用异步处理
- **缓存策略**: 合理使用缓存减少重复查询

### 3. 错误处理
- **异常分类**: 使用明确的异常类型
- **重试机制**: 实现智能重试逻辑
- **超时设置**: 设置合理的超时时间
- **日志记录**: 详细记录操作日志

### 4. 兼容性
- **向后兼容**: 保持API的向后兼容性
- **版本管理**: 使用语义化版本号
- **依赖管理**: 明确声明所有依赖

## 📚 示例项目

### 数据库插件示例
```java
public class DatabaseDataSourcePlugin extends AbstractDataSourcePlugin {
    
    @Override
    public String getPluginId() { return "database"; }
    
    @Override
    public String getPluginName() { return "Database Data Source"; }
    
    @Override
    public List<PluginParameter> getSupportedParameters() {
        return Arrays.asList(
            param("jdbcUrl").displayName("JDBC URL").type(STRING).required().build(),
            param("username").displayName("用户名").type(STRING).required().build(),
            param("password").displayName("密码").type(PASSWORD).required().sensitive().build(),
            param("query").displayName("SQL查询").type(STRING).required().build(),
            param("fetchSize").displayName("批量大小").type(INTEGER).defaultValue(1000).build()
        );
    }
    
    // 实现其他必需方法...
}
```

### Kafka插件示例
```java
public class KafkaDataSourcePlugin extends AbstractDataSourcePlugin {
    
    @Override
    public String getPluginId() { return "kafka"; }
    
    @Override
    public String getPluginName() { return "Kafka Data Source"; }
    
    @Override
    public List<PluginParameter> getSupportedParameters() {
        return Arrays.asList(
            param("brokers").displayName("Kafka Brokers").type(STRING).required().build(),
            param("topic").displayName("主题").type(STRING).required().build(),
            param("groupId").displayName("消费者组").type(STRING).required().build(),
            param("autoOffset").displayName("偏移量策略").type(ENUM)
                .options("earliest", "latest", "none").defaultValue("latest").build()
        );
    }
    
    // 实现其他必需方法...
}
```

## 🔗 相关资源

- [LogFlow官方文档](https://logflow.example.com/docs)
- [插件API参考](https://logflow.example.com/api)
- [示例插件仓库](https://github.com/logflow/plugins)
- [社区论坛](https://forum.logflow.example.com)

## ❓ 常见问题

### Q: 如何调试插件？
A: 可以在IDE中直接调试，或使用日志输出。建议在开发阶段启用详细日志。

### Q: 插件可以依赖其他JAR包吗？
A: 可以，使用Maven Shade插件将依赖打包到插件JAR中，或确保依赖在运行时可用。

### Q: 如何处理大数据量？
A: 使用流式读取、分页读取或实现异步处理机制。

### Q: 插件出错会影响整个系统吗？
A: 不会，插件异常会被隔离处理，不会影响其他插件或系统稳定性。

---

通过遵循本指南，您可以快速开发出高质量的LogFlow数据源插件。如有问题，欢迎查阅文档或在社区论坛寻求帮助。
