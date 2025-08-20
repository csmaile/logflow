# LogFlow - 日志诊断工作流系统

LogFlow是一个基于Java的日志诊断工作流系统，支持灵活的节点配置、动态脚本执行和图遍历。系统使用JGraphT库进行工作流的图形化表示和执行。

## 特性

- **多种节点类型**：输入、输出、数据源、诊断、脚本等节点
- **动态脚本支持**：支持JavaScript等多种脚本语言
- **图遍历执行**：基于JGraphT的有向无环图遍历
- **并行执行**：支持节点的并行执行以提高性能
- **灵活配置**：每个节点都支持丰富的配置选项
- **可扩展架构**：易于添加新的节点类型和功能

## 快速开始

### 环境要求

- Java 11+
- Maven 3.6+

### 构建项目

```bash
mvn clean compile
```

### 运行示例

```bash
mvn exec:java -Dexec.mainClass="com.logflow.examples.LogAnalysisExample"
```

## 节点类型

### 1. 输入节点 (InputNode)
从工作流上下文获取输入数据，支持数据类型转换。

```java
Map<String, Object> config = Map.of(
    "inputKey", "user_input",
    "outputKey", "processed_input",
    "dataType", "string",
    "defaultValue", "default_value"
);
```

### 2. 输出节点 (OutputNode)
将数据输出到不同目标（控制台、文件、JSON等）。

```java
Map<String, Object> config = Map.of(
    "inputKey", "result",
    "outputType", "console",  // console, file, json, context
    "format", "json",
    "filePath", "output.txt"
);
```

### 3. 数据源节点 (DataSourceNode)
从外部数据源获取数据。

```java
Map<String, Object> config = Map.of(
    "sourceType", "file",     // file, url, database, log, mock
    "filePath", "data.txt",
    "format", "json",
    "outputKey", "data"
);
```

### 4. 诊断节点 (DiagnosisNode)
执行各种日志诊断分析。

```java
Map<String, Object> config = Map.of(
    "diagnosisType", "error_detection",  // error_detection, pattern_analysis, 
                                        // performance_analysis, anomaly_detection, trend_analysis
    "inputKey", "log_data",
    "outputKey", "diagnosis_result"
);
```

### 5. 脚本节点 (ScriptNode)
执行动态脚本进行数据处理。

```java
Map<String, Object> config = Map.of(
    "scriptEngine", "javascript",
    "inputKey", "input_data",
    "outputKey", "processed_data",
    "script", "var result = input.map(x => x * 2); result;"
);
```

## 🎯 智能配置支持

LogFlow提供了完整的JSON Schema支持，为YAML配置文件提供IDE智能提示、实时验证和自动完成功能：

### 📝 IDE智能提示
- **自动完成**：节点类型、配置选项、枚举值的智能提示
- **实时验证**：语法错误和配置问题的即时检查
- **文档支持**：鼠标悬停显示配置项说明和示例
- **类型检查**：确保配置值符合预期的数据类型

### 🔧 使用方法
在YAML文件顶部添加Schema引用：
```yaml
# yaml-language-server: $schema=../schemas/logflow-workflow-schema.json
```

支持的IDE：VS Code、IntelliJ IDEA、WebStorm等主流编辑器。

详细说明请参考：[Schema使用指南](SCHEMA_GUIDE.md)

### 🚀 脚本开发支持
LogFlow提供了完整的脚本开发支持，解决在YAML中编写JavaScript脚本缺乏智能提示的问题：

- **TypeScript定义文件**：提供完整的API类型定义和智能提示
- **代码片段模板**：常用脚本模式的快速生成
- **开发最佳实践**：性能优化、错误处理、调试技巧
- **独立开发环境**：支持在IDE中开发后复制到YAML

详细说明请参考：[脚本开发指南](SCRIPT_DEVELOPMENT_GUIDE.md)

### 🤖 LLM智能脚本生成
LogFlow集成了大语言模型，支持通过自然语言描述自动生成脚本：

- **智能上下文分析**：自动分析工作流环境和数据流
- **自然语言理解**：支持中英文需求描述，理解复杂业务逻辑
- **代码质量保证**：自动添加错误处理、日志记录和最佳实践
- **多模型支持**：兼容OpenAI、Azure OpenAI、本地LLM等

```java
// 使用LLM生成脚本
LLMScriptGenerator generator = new LLMScriptGenerator(llmProvider);
String requirement = "过滤ERROR级别日志并统计数量";
ScriptGenerationResult result = generator.generateScript(workflow, nodeId, requirement, null);
```

详细说明请参考：[LLM脚本生成指南](LLM_SCRIPT_GENERATION_GUIDE.md)

### 🌟 完整工作流一键生成（终极功能）
LogFlow的终极功能：通过一句话需求自动生成包含所有脚本逻辑的完整工作流配置：

- **零配置生成**：无需手动设计工作流结构
- **零编程开发**：无需编写任何JavaScript代码
- **智能需求理解**：深度解析自然语言需求
- **完整脚本生成**：自动生成所有节点的处理逻辑
- **即时可用配置**：生成的YAML配置可直接运行

```java
// 一句话生成完整工作流
FullWorkflowGenerator generator = new FullWorkflowGenerator(llmProvider);
String requirement = "分析系统日志，过滤错误，统计问题，生成报告";
FullWorkflowGenerationResult result = generator.generateFullWorkflow(requirement, null);

// 获取完整的YAML配置
String yamlConfig = result.getYamlConfiguration();
```

详细说明请参考：[完整工作流生成指南](FULL_WORKFLOW_GENERATION_GUIDE.md)

## 工作流构建

### 方式一：使用WorkflowBuilder（编程方式）

```java
Workflow workflow = new WorkflowBuilder("my_workflow", "我的工作流")
    // 添加数据源节点
    .addDataSourceNode("data_source", "日志数据源", 
        WorkflowBuilder.config(
            "sourceType", "mock",
            "count", 100,
            "outputKey", "log_data"
        ))
    
    // 添加诊断节点
    .addDiagnosisNode("diagnosis", "错误诊断", 
        WorkflowBuilder.config(
            "diagnosisType", "error_detection",
            "inputKey", "log_data",
            "outputKey", "diagnosis_result"
        ))
    
    // 添加输出节点
    .addOutputNode("output", "结果输出", 
        WorkflowBuilder.config(
            "inputKey", "diagnosis_result",
            "outputType", "console"
        ))
    
    // 连接节点
    .connect("data_source", "diagnosis")
    .connect("diagnosis", "output")
    .build();
```

### 方式二：使用YAML配置（推荐）

创建YAML配置文件 `workflow.yaml`：

```yaml
# 工作流基本信息
workflow:
  id: "log_analysis"
  name: "日志分析工作流"
  description: "基于YAML配置的日志分析工作流"
  version: "1.0.0"
  author: "LogFlow Team"

# 全局配置
globalConfig:
  timeout: 30000
  logLevel: "INFO"

# 节点定义
nodes:
  - id: "data_source"
    name: "日志数据源"
    type: "datasource"
    enabled: true
    config:
      sourceType: "mock"
      count: 100
      outputKey: "log_data"

  - id: "diagnosis"
    name: "错误诊断"
    type: "diagnosis"
    enabled: true
    config:
      diagnosisType: "error_detection"
      inputKey: "log_data"
      outputKey: "diagnosis_result"

  - id: "output"
    name: "结果输出"
    type: "output"
    enabled: true
    config:
      inputKey: "diagnosis_result"
      outputType: "console"

# 连接关系
connections:
  - from: "data_source"
    to: "diagnosis"
    enabled: true
  - from: "diagnosis"
    to: "output"
    enabled: true
```

然后使用Java代码加载：

```java
WorkflowConfigLoader configLoader = new WorkflowConfigLoader();

// 从文件加载
Workflow workflow = configLoader.loadFromFile("workflow.yaml");

// 从资源加载
Workflow workflow = configLoader.loadFromResource("workflows/workflow.yaml");

// 从字符串加载
Workflow workflow = configLoader.loadFromYamlString(yamlContent);
```

### 配置验证

LogFlow提供了内置的配置验证功能：

```java
// 创建Schema验证器
WorkflowSchemaValidator validator = new WorkflowSchemaValidator();

// 验证配置文件
WorkflowSchemaValidator.ValidationResult result = validator.validateYamlFile("workflow.yaml");

if (result.isValid()) {
    System.out.println("配置验证通过");
} else {
    System.out.println("配置错误: " + result.getErrorMessage());
}

// 验证配置内容
WorkflowSchemaValidator.ValidationResult result2 = validator.validateYamlContent(yamlString);
```

## 工作流执行

```java
// 创建工作流引擎
WorkflowEngine engine = new WorkflowEngine(true, 4); // 并行执行，最大4个并发节点

// 执行工作流
Map<String, Object> initialData = Map.of("config", "value");
WorkflowExecutionResult result = engine.execute(workflow, initialData);

// 检查执行结果
if (result.isSuccess()) {
    System.out.println("工作流执行成功");
    
    // 获取执行统计
    var stats = result.getStatistics();
    System.out.printf("节点数: %d, 成功率: %.1f%%\n", 
        stats.getTotalNodes(), stats.getSuccessRate());
} else {
    System.out.println("执行失败: " + result.getMessage());
}

// 关闭引擎
engine.shutdown();
```

## 脚本编程

脚本节点提供了丰富的上下文和工具函数：

```javascript
// 访问输入数据
var data = input;

// 访问工作流上下文
var config = context.get('config');
context.set('result', processedData);

// 使用日志记录
logger.info('处理了 ' + data.length + ' 条记录');

// 使用工具函数
var containsError = utils.containsIgnoreCase(logLine, 'error');
var number = utils.extractNumber('响应时间: 123ms');
var timestamp = utils.now();

// 返回处理结果
processedData;
```

## 诊断功能

系统提供多种诊断分析功能：

### 错误检测
检测日志中的错误、异常和失败模式。

### 性能分析
分析响应时间，识别慢请求和性能瓶颈。

### 模式分析
识别日志中的常见模式（时间戳、IP地址、HTTP状态码等）。

### 异常检测
检测日志量的异常波动和突发事件。

### 趋势分析
分析错误率趋势和系统健康状况。

## 扩展开发

### 自定义节点

继承`AbstractWorkflowNode`创建自定义节点：

```java
public class CustomNode extends AbstractWorkflowNode {
    
    public CustomNode(String id, String name) {
        super(id, name, NodeType.CUSTOM);
    }
    
    @Override
    protected NodeExecutionResult doExecute(WorkflowContext context) throws WorkflowException {
        // 实现自定义逻辑
        Object result = processData(context.getData("input"));
        return NodeExecutionResult.success(getId(), result);
    }
    
    @Override
    public ValidationResult validate() {
        // 实现配置验证
        return ValidationResult.success();
    }
}
```

### 自定义脚本引擎

可以通过添加新的脚本引擎支持更多编程语言。

## 项目结构

```
src/main/java/
├── com/logflow/
│   ├── core/           # 核心接口和类
│   ├── nodes/          # 节点实现
│   ├── engine/         # 工作流引擎
│   ├── builder/        # 工作流构建器
│   └── examples/       # 示例代码
```

## 依赖库

- **JGraphT**: 图论算法库，用于工作流图遍历
- **Jackson**: JSON处理
- **SLF4J**: 日志框架
- **Nashorn**: JavaScript脚本引擎

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request来改进这个项目。

## 联系方式

如有问题或建议，请通过项目的Issue页面联系我们。
