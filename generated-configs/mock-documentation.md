# Mock Data Source

**版本**: 1.0.0  
**插件ID**: `mock`  
**作者**: LogFlow Team  

## 描述

生成模拟测试数据的数据源插件，支持多种数据类型和场景

## 配置参数

| 参数名 | 类型 | 必需 | 默认值 | 描述 |
|--------|------|------|--------|------|
| `mockType` | ENUM | ✅ | `mixed_logs` | 要生成的模拟数据类型 |
| `recordCount` | INTEGER | ❌ | `1000` | 要生成的记录数量 |
| `errorRate` | INTEGER | ❌ | `10` | 错误日志的百分比(0-100) |
| `timeRange` | INTEGER | ❌ | `24` | 生成数据的时间跨度(小时) |
| `seed` | LONG | ❌ | - | 随机数生成种子，用于生成可重现的数据 |
| `customSchema` | JSON | ❌ | - | 自定义数据结构(JSON格式) |

## 配置示例

```yaml
nodes:
- id: "my_mock_node"
  name: "Mock Data Source"
  type: "plugin"
  config:
    pluginType: "mock"
```

## 程序化配置示例

```java
// 使用 WorkflowBuilder
WorkflowBuilder.create("my-workflow", "我的工作流")
    .addPluginNode("mock_node", "Mock Data Source", Map.of(
        "pluginType", "mock",
        "mockType", mixed_logs
    ))
    .build();
```

