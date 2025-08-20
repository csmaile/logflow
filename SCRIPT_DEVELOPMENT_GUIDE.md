# LogFlow脚本开发指南

## 问题描述

在YAML配置文件中编写JavaScript脚本时，常见的问题包括：

- ❌ **缺乏智能提示**：IDE无法识别LogFlow特有的API（`context`, `logger`, `utils`等）
- ❌ **编译警告**：IDE将脚本视为独立JavaScript，提示未定义变量错误
- ❌ **没有类型检查**：容易出现运行时错误
- ❌ **缺少代码补全**：需要手动编写常用的脚本模式

## 解决方案

### 🎯 方案1：使用TypeScript定义文件（推荐）

#### 1.1 设置TypeScript定义

将LogFlow的TypeScript定义文件复制到您的工作区：

```bash
# 复制定义文件到工作区根目录
cp src/main/resources/scripts/logflow.d.ts ./

# 或者复制到IDE能识别的类型定义目录
mkdir -p @types/logflow
cp src/main/resources/scripts/logflow.d.ts @types/logflow/index.d.ts
```

#### 1.2 在VS Code中配置

在VS Code工作区根目录创建 `.vscode/settings.json`：

```json
{
  "typescript.preferences.includePackageJsonAutoImports": "auto",
  "typescript.suggest.autoImports": true,
  "javascript.suggest.autoImports": true,
  "typescript.preferences.enableProjectWideIntelliSense": true,
  "files.associations": {
    "*.yaml": "yaml"
  }
}
```

#### 1.3 智能提示效果

配置完成后，您将获得：

```javascript
// ✅ 自动提示context的所有方法
context.get('config');        // 自动补全get方法
context.set('result', data);  // 自动补全set方法
context.getWorkflowId();      // 方法提示和参数说明

// ✅ 自动提示logger的所有方法
logger.info('处理完成');      // 自动补全，显示参数类型
logger.warn('发现异常');      // 方法说明和使用示例

// ✅ 自动提示utils的所有方法
utils.now();                  // 返回类型提示：string
```

### 🚀 方案2：使用代码片段

#### 2.1 VS Code代码片段设置

1. 打开VS Code设置：`Ctrl/Cmd + Shift + P` → "Preferences: Configure User Snippets"
2. 选择 "yaml" 语言
3. 将 `src/main/resources/scripts/vscode-snippets.json` 的内容添加到配置中

#### 2.2 使用代码片段

在YAML的脚本字段中输入片段前缀：

```yaml
# 输入 "lf-filter" 自动生成数据过滤脚本模板
script: |
  // 数据过滤脚本
  var config = context.get('config') || {};
  var data = input;
  var filtered = [];
  
  for (var i = 0; i < data.length; i++) {
    var item = data[i];
    
    // 在这里添加过滤条件
    if (item.level !== 'DEBUG') {
      filtered.push(item);
    }
  }
  
  logger.info('过滤完成: 输入' + data.length + '条，输出' + filtered.length + '条');
  filtered;
```

#### 2.3 可用的代码片段

| 前缀 | 描述 | 用途 |
|------|------|------|
| `lf-filter` | 数据过滤脚本 | 过滤输入数据 |
| `lf-transform` | 数据转换脚本 | 转换数据格式 |
| `lf-aggregate` | 数据聚合脚本 | 统计和聚合 |
| `lf-error-analysis` | 错误分析脚本 | 分析日志错误 |
| `lf-performance` | 性能分析脚本 | 性能统计分析 |
| `lf-context` | 上下文操作脚本 | 操作工作流上下文 |
| `lf-report` | 报告生成脚本 | 生成分析报告 |

### 💡 方案3：外部脚本开发

#### 3.1 独立JavaScript文件开发

创建独立的JavaScript文件进行开发：

```javascript
// scripts/data-processor.js

// 引入类型定义（如果使用TypeScript）
/// <reference path="../logflow.d.ts" />

/**
 * 数据处理脚本
 * @param {any[]} input 输入数据
 * @returns {any[]} 处理后的数据
 */
function processData(input) {
  const config = context.get('config') || {};
  const data = input;
  const filtered = [];
  
  for (let i = 0; i < data.length; i++) {
    const item = data[i];
    
    // 处理逻辑
    if (item.level !== 'DEBUG') {
      filtered.push({
        ...item,
        processedAt: utils.now()
      });
    }
  }
  
  logger.info(`处理完成: 输入${data.length}条，输出${filtered.length}条`);
  return filtered;
}

// 执行处理
const result = processData(input);
result;
```

#### 3.2 复制到YAML配置

开发完成后，将脚本内容复制到YAML配置的script字段中。

### 🔧 方案4：IDE特定配置

#### 4.1 IntelliJ IDEA / WebStorm

1. 在项目中添加 `logflow.d.ts` 文件
2. 在Settings中启用TypeScript支持：
   - `File` → `Settings` → `Languages & Frameworks` → `TypeScript`
   - 启用 "TypeScript Language Service"

#### 4.2 其他编辑器

- **Vim/Neovim**：使用LSP客户端配置TypeScript语言服务器
- **Emacs**：通过 `lsp-mode` 配置TypeScript支持
- **Sublime Text**：安装LSP插件并配置TypeScript

## 最佳实践

### 📝 脚本编写规范

#### 1. 错误处理

```javascript
// ❌ 不好的做法
var data = context.get('input_data');
data.forEach(item => { /* 处理 */ });

// ✅ 好的做法
var data = context.get('input_data');
if (!data || !Array.isArray(data)) {
  logger.warn('输入数据无效或为空');
  return [];
}

data.forEach(item => {
  try {
    // 处理逻辑
  } catch (error) {
    logger.error('处理项目失败: ' + error.message, { item: item });
  }
});
```

#### 2. 性能优化

```javascript
// ❌ 不好的做法 - 频繁操作上下文
for (var i = 0; i < data.length; i++) {
  var result = context.get('results') || [];
  result.push(processItem(data[i]));
  context.set('results', result);
}

// ✅ 好的做法 - 批量操作
var results = [];
for (var i = 0; i < data.length; i++) {
  results.push(processItem(data[i]));
}
context.set('results', results);
```

#### 3. 日志记录

```javascript
// ✅ 合理的日志级别
logger.debug('开始处理数据', { count: input.length });
logger.info('处理完成: 成功' + successCount + '个，失败' + errorCount + '个');
logger.warn('发现异常数据: ' + anomalies.length + '条');
logger.error('处理失败', error);
```

### 🧪 脚本测试

#### 1. 单元测试模拟

```javascript
// 测试脚本时可以模拟LogFlow环境
var mockContext = {
  data: {},
  get: function(key) { return this.data[key]; },
  set: function(key, value) { this.data[key] = value; },
  getWorkflowId: function() { return 'test-workflow'; },
  getExecutionId: function() { return 'test-execution'; }
};

var mockLogger = {
  info: console.log,
  warn: console.warn,
  error: console.error,
  debug: console.log
};

var mockUtils = {
  now: function() { return new Date().toISOString(); }
};

// 在浏览器控制台或Node.js中测试
var context = mockContext;
var logger = mockLogger;
var utils = mockUtils;
var input = [ /* 测试数据 */ ];

// 执行脚本代码
```

#### 2. 分步调试

```javascript
// 添加调试输出
logger.debug('输入数据:', { count: input.length, sample: input[0] });

var filtered = input.filter(item => item.level !== 'DEBUG');
logger.debug('过滤后数据:', { count: filtered.length });

var transformed = filtered.map(item => ({
  ...item,
  processedAt: utils.now()
}));
logger.debug('转换后数据:', { count: transformed.length, sample: transformed[0] });

return transformed;
```

### 📊 性能监控

```javascript
// 性能监控模板
var startTime = Date.now();

// 执行处理逻辑
var result = processData(input);

var endTime = Date.now();
var executionTime = endTime - startTime;

logger.info('脚本执行完成', {
  executionTime: executionTime + 'ms',
  inputCount: input.length,
  outputCount: result.length,
  throughput: Math.round(input.length * 1000 / executionTime) + ' items/sec'
});

context.set('performance_metrics', {
  executionTime: executionTime,
  throughput: Math.round(input.length * 1000 / executionTime)
});

result;
```

## 故障排除

### 常见问题

#### 1. TypeScript定义不生效

**症状**：仍然没有智能提示

**解决方案**：
- 确保 `logflow.d.ts` 文件在IDE能识别的路径中
- 重启IDE或重新加载TypeScript服务
- 检查IDE的TypeScript配置是否启用

#### 2. 代码片段不工作

**症状**：输入前缀没有自动补全

**解决方案**：
- 确保代码片段配置在正确的语言范围（yaml）
- 检查片段语法是否正确
- 重启IDE

#### 3. 脚本运行时错误

**症状**：配置验证通过但执行失败

**解决方案**：
- 检查脚本中使用的变量是否正确初始化
- 确保返回值类型正确
- 添加错误处理和日志输出

### 调试技巧

1. **使用logger输出调试信息**
2. **分步验证数据处理过程**
3. **在浏览器控制台中模拟测试**
4. **使用简化的测试数据**
5. **逐步增加脚本复杂度**

## 示例集合

完整的脚本示例请参考：
- `src/main/resources/workflows/` - 工作流配置示例
- `src/main/resources/scripts/` - 脚本模板和定义文件

通过使用这些工具和最佳实践，您可以显著改善LogFlow脚本的开发体验，减少错误，提高开发效率！
