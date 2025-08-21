# LogFlow 工作流配置文件示例

本目录包含了 LogFlow 系统的示例配置文件，展示了从简单到复杂的各种工作流场景。

## 📁 文件清单

### 1. `simple-workflow.yml` - 简单工作流
**适用场景**: 日志处理入门、学习基础概念
- **节点数量**: 5个
- **连接数量**: 4个
- **复杂度**: 简单

**工作流程**:
```
输入 → 文件读取 → 日志过滤 → 错误检测 → 控制台输出
```

**包含的节点类型**:
- ✅ `input` - 数据输入
- ✅ `plugin` - 文件读取插件
- ✅ `script` - JavaScript日志过滤
- ✅ `diagnosis` - 错误检测
- ✅ `notification` - 控制台通知

### 2. `comprehensive-workflow.yml` - 综合工作流
**适用场景**: 企业级日志分析、完整解决方案
- **节点数量**: 11个
- **连接数量**: 14个
- **复杂度**: 复杂 (评分: 28)

**工作流程**:
```
数据输入 → 文件读取 → 数据预处理
                    ├─ 错误检测 ────┐
                    ├─ 性能分析 ────┼─ 深度分析 ─┐
                    └─ 业务分析 ────┘           │
                                              ↓
                    结果聚合 ←──────────────────┘
                       ├─ 控制台通知
                       ├─ 文件报告
                       └─ 上下文输出
```

**包含的节点类型**:
- ✅ `input` - 工作流输入
- ✅ `plugin` - 文件数据源（可扩展为数据库、消息队列等）
- ✅ `script` - 多个JavaScript处理脚本
  - 数据预处理器
  - 业务逻辑分析器
  - 结果聚合器
- ✅ `diagnosis` - 双重诊断分析
  - 错误检测分析器
  - 性能分析器
- ✅ `reference` - 深度分析子工作流调用
- ✅ `notification` - 多渠道通知
  - 控制台通知
  - 文件输出
  - 上下文传递

### 3. `sample-logs.json` - 测试数据
模拟的日志数据，包含不同级别的日志条目用于测试。

## 🚀 使用方法

### 快速开始
```bash
# 编译项目
mvn clean compile

# 运行配置演示
mvn exec:java -Dexec.mainClass="com.logflow.examples.WorkflowConfigDemo"
```

### 自定义配置
1. **复制基础模板**: 从 `simple-workflow.yml` 开始
2. **添加节点**: 根据需要添加新的节点类型
3. **配置连接**: 定义节点间的数据流向
4. **测试验证**: 使用演示程序验证配置

## 📋 配置文件结构

### 基本结构
```yaml
workflow:
  id: "workflow_id"
  name: "工作流名称"
  description: "工作流描述"
  version: "1.0.0"
  author: "作者"

globalConfig:
  timeout: 60000
  retryCount: 2
  logLevel: "INFO"

nodes:
  - id: node1
    name: "节点名称"
    type: "节点类型"
    enabled: true
    config:
      # 节点特定配置

connections:
  - from: node1
    to: node2
    enabled: true
```

### 支持的节点类型
| 节点类型 | 描述 | 配置要点 |
|---------|------|----------|
| `input` | 数据输入 | `inputKey`, `outputKey`, `dataType` |
| `plugin` | 插件节点 | `pluginType`, `outputKey`, 插件特定配置 |
| `script` | 脚本执行 | `scriptEngine`, `script`, `inputKey`, `outputKey` |
| `diagnosis` | 诊断分析 | `diagnosisType`, `inputKey`, `outputKey` |
| `reference` | 工作流引用 | `workflowId`, `executionMode`, `inputMapping` |
| `notification` | 通知输出 | `providerType`, `messageTemplate`, `priority` |

## 💡 最佳实践

### 1. 命名规范
- **节点ID**: 使用下划线命名 (如: `data_processor`)
- **节点名称**: 使用中文描述 (如: "数据处理器")
- **工作流ID**: 使用下划线或连字符 (如: `log_analysis_workflow`)

### 2. 性能优化
- **并行执行**: 启用 `parallelExecution: true`
- **合理超时**: 设置适当的 `timeout` 值
- **资源管理**: 使用 `BaseDemo` 确保资源正确释放

### 3. 错误处理
- **重试机制**: 配置 `retryCount`
- **条件连接**: 使用连接条件进行流程控制
- **验证配置**: 确保所有必需配置项都已设置

### 4. 可维护性
- **模块化设计**: 将复杂逻辑拆分为多个节点
- **清晰注释**: 在配置中添加描述和说明
- **版本控制**: 使用语义化版本号

## 🔧 故障排除

### 常见问题
1. **文件路径错误**: 确保文件路径存在且可访问
2. **插件配置**: 检查插件类型和相关配置参数
3. **脚本语法**: 验证JavaScript脚本语法正确性
4. **连接关系**: 确保节点ID匹配且连接逻辑正确

### 调试技巧
- 使用 `WorkflowConfigDemo` 验证配置
- 检查日志输出中的错误信息
- 逐步增加节点复杂度
- 使用简单数据源测试连通性

## 📚 参考资源

- **JSON Schema**: `src/main/resources/schemas/logflow-workflow-schema.json`
- **插件配置**: 使用 `PluginConfigTool` 生成插件配置模板
- **更多示例**: 查看 `src/main/java/com/logflow/examples/` 目录
