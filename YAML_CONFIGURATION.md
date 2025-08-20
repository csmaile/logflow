# LogFlow YAML配置指南

## 概述

LogFlow现在支持通过YAML配置文件来定义工作流，这比纯Java代码更加直观和易于维护。YAML配置方式提供了以下优势：

- **声明式配置**：描述工作流结构而非执行步骤
- **易于理解**：清晰的层次结构和可读性
- **版本控制友好**：文本格式便于跟踪变更
- **动态加载**：无需重新编译即可修改工作流
- **团队协作**：非程序员也能参与工作流设计

## YAML配置结构

### 基本结构

```yaml
# 工作流基本信息
workflow:
  id: "工作流唯一标识"
  name: "工作流显示名称"
  description: "工作流描述"
  version: "版本号"
  author: "作者"
  metadata:
    key: value

# 全局配置（可选）
globalConfig:
  timeout: 30000
  logLevel: "INFO"

# 节点定义
nodes:
  - id: "节点ID"
    name: "节点名称"
    type: "节点类型"
    enabled: true
    position:
      x: 100
      y: 100
    config:
      # 节点特定配置

# 连接关系
connections:
  - from: "源节点ID"
    to: "目标节点ID"
    enabled: true
    condition: "连接条件（可选）"
```

### 支持的节点类型

| 节点类型 | 描述 | 主要配置项 |
|---------|------|----------|
| `input` | 输入节点 | `inputKey`, `outputKey`, `dataType`, `defaultValue` |
| `output` | 输出节点 | `inputKey`, `outputType`, `format`, `filePath` |
| `datasource` | 数据源节点 | `sourceType`, `filePath`, `count`, `outputKey` |
| `diagnosis` | 诊断节点 | `diagnosisType`, `inputKey`, `outputKey` |
| `script` | 脚本节点 | `scriptEngine`, `inputKey`, `outputKey`, `script` |

## 配置示例

### 1. 简单错误检测工作流

```yaml
workflow:
  id: "simple_error_detection"
  name: "简单错误检测"
  description: "检测日志中的错误信息"
  version: "1.0.0"

nodes:
  - id: "log_source"
    name: "日志数据源"
    type: "datasource"
    config:
      sourceType: "mock"
      count: 100
      outputKey: "logs"

  - id: "error_detector"
    name: "错误检测器"
    type: "diagnosis"
    config:
      diagnosisType: "error_detection"
      inputKey: "logs"
      outputKey: "errors"

  - id: "console_out"
    name: "控制台输出"
    type: "output"
    config:
      inputKey: "errors"
      outputType: "console"

connections:
  - from: "log_source"
    to: "error_detector"
  - from: "error_detector"
    to: "console_out"
```

### 2. 复杂数据处理工作流

```yaml
workflow:
  id: "data_processing"
  name: "数据处理流水线"
  description: "完整的数据处理和分析流程"
  version: "2.0.0"

globalConfig:
  timeout: 60000
  retryCount: 3

nodes:
  - id: "config_input"
    name: "配置输入"
    type: "input"
    config:
      inputKey: "processing_config"
      outputKey: "config"

  - id: "data_loader"
    name: "数据加载器"
    type: "datasource"
    config:
      sourceType: "file"
      filePath: "data/input.log"
      outputKey: "raw_data"

  - id: "data_processor"
    name: "数据处理器"
    type: "script"
    config:
      scriptEngine: "javascript"
      inputKey: "raw_data"
      outputKey: "processed_data"
      script: |
        var config = context.get('config');
        var data = input;
        var result = [];
        
        for (var i = 0; i < data.length; i++) {
          var item = data[i];
          if (item.level !== 'DEBUG' || config.includeDebug) {
            result.push({
              id: item.id,
              level: item.level,
              message: item.message,
              processed_time: utils.now()
            });
          }
        }
        
        logger.info('处理了 ' + result.length + ' 条记录');
        result;

  - id: "analyzer"
    name: "数据分析器"
    type: "diagnosis"
    config:
      diagnosisType: "pattern_analysis"
      inputKey: "processed_data"
      outputKey: "analysis_result"

  - id: "report_generator"
    name: "报告生成器"
    type: "output"
    config:
      inputKey: "analysis_result"
      outputType: "json"
      filePath: "reports/analysis_report.json"

connections:
  - from: "config_input"
    to: "data_loader"
  - from: "data_loader"
    to: "data_processor"
  - from: "data_processor"
    to: "analyzer"
  - from: "analyzer"
    to: "report_generator"
```

## Java代码使用

### 基本用法

```java
import com.logflow.config.WorkflowConfigLoader;
import com.logflow.engine.Workflow;
import com.logflow.engine.WorkflowEngine;

// 创建配置加载器
WorkflowConfigLoader loader = new WorkflowConfigLoader();

// 从文件加载
Workflow workflow = loader.loadFromFile("path/to/workflow.yaml");

// 从资源加载
Workflow workflow = loader.loadFromResource("workflows/my-workflow.yaml");

// 从字符串加载
String yamlContent = "..."; // YAML内容
Workflow workflow = loader.loadFromYamlString(yamlContent);

// 执行工作流
WorkflowEngine engine = new WorkflowEngine();
WorkflowExecutionResult result = engine.execute(workflow, initialData);
```

### 错误处理

```java
try {
    Workflow workflow = loader.loadFromFile("workflow.yaml");
    WorkflowExecutionResult result = engine.execute(workflow, null);
    
    if (result.isSuccess()) {
        System.out.println("工作流执行成功");
    } else {
        System.out.println("执行失败: " + result.getMessage());
    }
} catch (WorkflowConfigLoader.WorkflowConfigException e) {
    System.err.println("配置错误: " + e.getMessage());
} catch (IOException e) {
    System.err.println("文件读取错误: " + e.getMessage());
}
```

## 最佳实践

### 1. 文件组织

```
src/main/resources/workflows/
├── production/
│   ├── log-analysis.yaml
│   └── monitoring.yaml
├── development/
│   ├── test-workflow.yaml
│   └── debug-workflow.yaml
└── templates/
    ├── basic-template.yaml
    └── complex-template.yaml
```

### 2. 配置验证

- 确保所有必需字段都已配置
- 使用描述性的节点ID和名称
- 为复杂脚本添加注释
- 定期验证YAML格式

### 3. 版本管理

```yaml
workflow:
  id: "log_analysis"
  name: "日志分析工作流"
  version: "1.2.3"  # 使用语义化版本
  metadata:
    created: "2024-01-15"
    last_modified: "2024-01-20"
    changelog: "添加了性能分析节点"
```

### 4. 环境配置

```yaml
workflow:
  id: "multi_env_workflow"
  name: "多环境工作流"
  metadata:
    environment: "${ENV:development}"  # 环境变量
    
globalConfig:
  logLevel: "${LOG_LEVEL:INFO}"
  timeout: "${TIMEOUT:30000}"
```

## 调试和故障排除

### 常见问题

1. **节点ID重复**
   ```
   错误: 节点ID 'data_source' 已存在
   解决: 确保每个节点的ID在工作流中唯一
   ```

2. **连接引用不存在的节点**
   ```
   错误: 连接中的目标节点不存在: unknown_node
   解决: 检查connections中引用的节点ID是否正确
   ```

3. **配置项缺失**
   ```
   错误: 节点配置验证失败: 缺少必需的配置项: inputKey
   解决: 为相应节点添加必需的配置项
   ```

### 调试技巧

1. **启用详细日志**
   ```yaml
   globalConfig:
     logLevel: "DEBUG"
   ```

2. **使用简化版本**
   - 先创建最小可工作版本
   - 逐步添加复杂功能

3. **验证配置**
   ```java
   WorkflowValidationResult validation = workflow.validate();
   if (!validation.isValid()) {
       validation.getErrors().forEach(System.err::println);
   }
   ```

## 总结

YAML配置使LogFlow工作流的定义更加直观和易于维护。通过声明式的配置方式，用户可以专注于业务逻辑而不是实现细节。结合Java代码的灵活性，YAML配置为LogFlow提供了强大的工作流定义能力。
