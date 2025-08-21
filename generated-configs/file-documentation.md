# File Data Source

**版本**: 1.0.0  
**插件ID**: `file`  
**作者**: LogFlow Team  

## 描述

读取本地文件数据的数据源插件，支持JSON、CSV、文本等格式

## 配置参数

| 参数名 | 类型 | 必需 | 默认值 | 描述 |
|--------|------|------|--------|------|
| `filePath` | FILE_PATH | ✅ | - | 要读取的文件路径，支持相对路径和绝对路径 |
| `format` | ENUM | ✅ | `json` | 文件数据格式 |
| `encoding` | ENUM | ❌ | `UTF-8` | 文件字符编码 |
| `csvDelimiter` | STRING | ❌ | `,` | CSV文件的字段分隔符 |
| `csvHeader` | BOOLEAN | ❌ | `true` | CSV文件第一行是否为表头 |
| `jsonArrayPath` | STRING | ❌ | - | JSON文件中数组数据的路径(JSONPath格式) |
| `maxLines` | INTEGER | ❌ | `0` | 最多读取的行数，0表示读取全部 |
| `skipLines` | INTEGER | ❌ | `0` | 从文件开头跳过的行数 |

## 配置示例

```yaml
nodes:
- id: "my_file_node"
  name: "File Data Source"
  type: "plugin"
  config:
    pluginType: "file"
```

## 程序化配置示例

```java
// 使用 WorkflowBuilder
WorkflowBuilder.create("my-workflow", "我的工作流")
    .addPluginNode("file_node", "File Data Source", Map.of(
        "pluginType", "file",
        "filePath", "your_value_here",
        "format", json
    ))
    .build();
```

