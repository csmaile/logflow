# LogFlow 插件概览

以下是所有可用的插件列表：

| 插件名称 | 插件ID | 版本 | 描述 | 作者 |
|----------|--------|------|------|------|
| File Data Source | `file` | 1.0.0 | 读取本地文件数据的数据源插件，支持JSON、CSV、文本等格式 | LogFlow Team |
| Mock Data Source | `mock` | 1.0.0 | 生成模拟测试数据的数据源插件，支持多种数据类型和场景 | LogFlow Team |

## 快速开始

### 1. 选择插件
从上表中选择你需要的插件，记录其插件ID。

### 2. 生成配置模板
```bash
# 生成特定插件的配置模板
mvn exec:java -Dexec.mainClass="com.logflow.tools.PluginConfigTool" \
    -Dexec.args="generate-template --plugin <plugin-id>"
```

### 3. 配置工作流
将生成的配置添加到你的 workflow.yml 文件中。

