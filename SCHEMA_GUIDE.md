# LogFlow YAML Schema使用指南

## 概述

LogFlow提供了完整的JSON Schema来支持YAML配置文件的IDE智能提示、语法验证和自动完成功能。通过使用Schema，您可以获得更好的开发体验和配置文件的可靠性。

## Schema特性

### 🎯 智能提示和自动完成
- **节点类型提示**：IDE会提示所有可用的节点类型（input, output, datasource, diagnosis, script）
- **配置选项提示**：根据节点类型自动提示相应的配置选项
- **枚举值提示**：对于有限值选项（如outputType、diagnosisType），提供下拉选择
- **类型检查**：确保配置值符合预期的数据类型

### ✅ 实时验证
- **语法验证**：实时检查YAML语法错误
- **结构验证**：验证配置文件结构是否符合LogFlow规范
- **必填字段检查**：提示缺失的必填配置项
- **值域检查**：验证数值是否在允许的范围内

### 📚 内置文档
- **配置说明**：每个配置项都有详细的描述
- **示例值**：提供实际的配置示例
- **最佳实践**：内置推荐的配置模式

## IDE支持

### VS Code
VS Code通过YAML Language Server自动支持JSON Schema：

1. **自动识别**：文件顶部的schema注释会自动被识别
2. **智能提示**：Ctrl+Space 触发自动完成
3. **实时验证**：语法错误会实时高亮显示
4. **悬停提示**：鼠标悬停显示配置项说明

### IntelliJ IDEA / WebStorm
JetBrains IDE同样支持JSON Schema：

1. **Schema关联**：IDE会自动关联schema文件
2. **代码补全**：Ctrl+Space 触发智能提示
3. **错误检查**：实时检查并高亮错误
4. **文档查看**：Ctrl+Q 查看配置项文档

### 其他编辑器
大多数现代编辑器都支持YAML Schema：
- **Vim/Neovim**：通过coc.nvim或LSP插件
- **Emacs**：通过lsp-mode
- **Sublime Text**：通过LSP插件

## 使用方法

### 1. 添加Schema引用

在YAML文件的第一行添加schema引用：

```yaml
# yaml-language-server: $schema=../schemas/logflow-workflow-schema.json
```

对于不同的文件位置，调整相对路径：
```yaml
# 在workflows目录下的文件
# yaml-language-server: $schema=../schemas/logflow-workflow-schema.json

# 在根目录下的文件
# yaml-language-server: $schema=src/main/resources/schemas/logflow-workflow-schema.json

# 使用在线schema（如果可用）
# yaml-language-server: $schema=https://logflow.com/schemas/workflow.schema.json
```

### 2. 基本配置结构

Schema定义了以下主要结构：

```yaml
workflow:          # 工作流基本信息（必需）
  id: string       # 工作流ID（必需）
  name: string     # 工作流名称（必需）
  description: string
  version: string
  author: string
  metadata: object

globalConfig:      # 全局配置（可选）
  timeout: integer
  retryCount: integer
  logLevel: enum
  # ... 更多选项

nodes:            # 节点列表（必需）
  - id: string    # 节点ID（必需）
    name: string  # 节点名称（必需）
    type: enum    # 节点类型（必需）
    enabled: boolean
    position:
      x: integer
      y: integer
    config: object  # 根据节点类型变化

connections:      # 连接关系（可选）
  - from: string  # 源节点ID（必需）
    to: string    # 目标节点ID（必需）
    enabled: boolean
    condition: string
```

### 3. 节点类型特定配置

#### Input节点
```yaml
- id: "input_node"
  name: "输入节点"
  type: "input"
  config:
    inputKey: "user_input"     # 输入键
    outputKey: "processed_input" # 输出键
    dataType: "string"         # 数据类型：string, integer, boolean, object
    defaultValue: "默认值"      # 默认值（可选）
```

#### Output节点
```yaml
- id: "output_node"
  name: "输出节点"
  type: "output"
  config:
    inputKey: "result"         # 输入键
    outputType: "console"      # 输出类型：console, file, json, context
    format: "json"             # 格式：text, json, xml, csv
    filePath: "output.txt"     # 文件路径（文件输出时）
    contextKey: "result_key"   # 上下文键（上下文输出时）
```

#### DataSource节点
```yaml
- id: "data_source"
  name: "数据源节点"
  type: "datasource"
  config:
    sourceType: "file"         # 数据源类型：file, url, database, log, mock
    filePath: "/path/to/file"  # 文件路径
    format: "json"             # 数据格式：text, json, lines, csv
    outputKey: "data"          # 输出键
    # 其他特定配置...
```

#### Diagnosis节点
```yaml
- id: "diagnosis_node"
  name: "诊断节点"
  type: "diagnosis"
  config:
    diagnosisType: "error_detection"  # 诊断类型
    inputKey: "log_data"              # 输入键
    outputKey: "diagnosis_result"     # 输出键
    errorPatterns: ["ERROR", "FATAL"] # 错误模式（错误检测时）
    slowThreshold: 1000.0             # 慢响应阈值（性能分析时）
```

#### Script节点
```yaml
- id: "script_node"
  name: "脚本节点"
  type: "script"
  config:
    scriptEngine: "javascript"  # 脚本引擎：javascript, groovy, python
    inputKey: "input_data"      # 输入键
    outputKey: "processed_data" # 输出键
    parameters:                 # 脚本参数
      param1: "value1"
    script: |                   # 脚本内容
      var result = input.map(x => x * 2);
      logger.info('Processed ' + result.length + ' items');
      result;
```

## 最佳实践

### 1. 文件组织
```
project/
├── src/main/resources/
│   ├── schemas/
│   │   └── logflow-workflow-schema.json
│   └── workflows/
│       ├── production/
│       │   ├── error-monitoring.yaml
│       │   └── performance-analysis.yaml
│       ├── development/
│       │   └── test-workflow.yaml
│       └── templates/
│           └── basic-template.yaml
```

### 2. Schema引用
```yaml
# 推荐：使用相对路径
# yaml-language-server: $schema=../schemas/logflow-workflow-schema.json

# 对于模板文件，可以使用绝对路径
# yaml-language-server: $schema=file:///path/to/schemas/logflow-workflow-schema.json
```

### 3. 配置验证
定期验证配置文件：
```bash
# 使用在线工具验证
curl -X POST https://www.jsonschemavalidator.net/api/validate \
  -H "Content-Type: application/json" \
  -d '{"schema": {...}, "data": {...}}'

# 使用本地工具
npm install -g ajv-cli
ajv validate -s schema.json -d config.yaml
```

### 4. 团队协作
- 共享schema文件确保团队使用相同的配置标准
- 在版本控制中包含schema文件
- 定期更新schema以支持新功能

## 故障排除

### Schema不生效
1. **检查文件路径**：确保schema引用路径正确
2. **IDE设置**：确认IDE已启用YAML Language Server
3. **文件格式**：确保文件以.yaml或.yml结尾
4. **语法错误**：先修复基本的YAML语法错误

### 智能提示不工作
1. **重启IDE**：重新加载language server
2. **清除缓存**：清除IDE的YAML缓存
3. **检查插件**：确保YAML支持插件已安装并启用

### 验证错误
1. **查看错误详情**：鼠标悬停在错误标记上查看详细信息
2. **检查必填字段**：确保所有必填配置项都已提供
3. **验证数据类型**：确保配置值的类型正确
4. **参考示例**：查看schema-example.yaml文件

## 高级用法

### 条件配置
某些配置项只在特定条件下有效：

```yaml
# 文件数据源特定配置
- type: "datasource"
  config:
    sourceType: "file"
    filePath: "/path/to/file"  # 只在sourceType为file时需要
    format: "json"

# 诊断节点特定配置
- type: "diagnosis"
  config:
    diagnosisType: "error_detection"
    errorPatterns: ["ERROR"]   # 只在diagnosisType为error_detection时有效
```

### 自定义验证
您可以扩展schema以支持自定义验证规则：

```json
{
  "properties": {
    "customConfig": {
      "type": "object",
      "patternProperties": {
        "^[a-zA-Z][a-zA-Z0-9_]*$": {
          "type": "string"
        }
      }
    }
  }
}
```

## 示例文件

项目提供了几个示例配置文件：

1. **basic-error-detection.yaml** - 基础错误检测工作流
2. **complex-log-analysis.yaml** - 复杂多步分析工作流
3. **simple-test.yaml** - 简单测试配置
4. **schema-example.yaml** - 完整的schema功能展示

建议从这些示例开始，根据需要进行修改。

## 总结

通过使用LogFlow的JSON Schema，您可以：
- 快速编写正确的YAML配置
- 减少配置错误
- 提高开发效率
- 获得更好的IDE支持

Schema是LogFlow配置系统的重要组成部分，建议在所有YAML配置文件中使用。
