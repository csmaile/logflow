# LogFlow LLM脚本生成指南

## 概述

LogFlow LLM脚本生成功能允许用户通过自然语言描述需求，自动生成符合LogFlow规范的JavaScript脚本。这大大降低了脚本编写的门槛，即使不熟悉编程的用户也能快速创建复杂的数据处理逻辑。

## 🎯 核心优势

### 1. **智能上下文分析**
- 自动分析当前节点在工作流中的位置
- 识别可用的输入数据源和格式
- 检测可从上下文获取的数据
- 确定输出目标和期望格式

### 2. **自然语言理解**
- 支持中文和英文需求描述
- 理解复杂的业务逻辑要求
- 自动选择合适的脚本模式
- 生成符合最佳实践的代码

### 3. **代码质量保证**
- 自动添加错误处理逻辑
- 包含适当的日志记录
- 遵循LogFlow API规范
- 提供脚本验证和建议

## 🚀 快速开始

### 基本使用流程

```java
// 1. 创建LLM提供者（需要配置真实的API）
LLMProvider provider = new OpenAIProvider("your-api-key");

// 2. 创建脚本生成器
LLMScriptGenerator generator = new LLMScriptGenerator(provider);

// 3. 加载工作流
Workflow workflow = configLoader.loadFromResource("your-workflow.yaml");

// 4. 生成脚本
String userRequirement = "过滤掉DEBUG级别的日志，只保留ERROR和WARN";
ScriptGenerationResult result = generator.generateScript(
    workflow, "script_node_id", userRequirement, null);

// 5. 使用生成的脚本
if (result.isSuccess()) {
    String script = result.getGeneratedScript();
    // 复制到YAML配置中使用
}
```

## 🔧 LLM提供者集成

### OpenAI API集成

```java
public class OpenAIProvider implements LLMScriptGenerator.LLMProvider {
    private final String apiKey;
    private final String model;
    
    public OpenAIProvider(String apiKey) {
        this.apiKey = apiKey;
        this.model = "gpt-4"; // 或 gpt-3.5-turbo
    }
    
    @Override
    public String generateText(String prompt) throws Exception {
        // 调用OpenAI API的实现
        return callOpenAIAPI(prompt);
    }
    
    private String callOpenAIAPI(String prompt) throws Exception {
        // 实际的API调用实现
        // 使用HTTP客户端调用OpenAI API
        // 处理响应和错误
    }
}
```

### Azure OpenAI集成

```java
public class AzureOpenAIProvider implements LLMScriptGenerator.LLMProvider {
    private final String endpoint;
    private final String apiKey;
    private final String deploymentName;
    
    public AzureOpenAIProvider(String endpoint, String apiKey, String deploymentName) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.deploymentName = deploymentName;
    }
    
    @Override
    public String generateText(String prompt) throws Exception {
        // Azure OpenAI API调用实现
        return callAzureOpenAI(prompt);
    }
}
```

### 本地LLM集成（Ollama）

```java
public class OllamaProvider implements LLMScriptGenerator.LLMProvider {
    private final String baseUrl;
    private final String model;
    
    public OllamaProvider(String baseUrl, String model) {
        this.baseUrl = baseUrl; // 如: http://localhost:11434
        this.model = model;     // 如: codellama, llama2
    }
    
    @Override
    public String generateText(String prompt) throws Exception {
        // Ollama API调用实现
        return callOllamaAPI(prompt);
    }
}
```

## 📝 需求描述最佳实践

### 1. **清晰的功能描述**

```text
✅ 好的需求描述：
"创建一个脚本过滤日志数据，只保留ERROR和FATAL级别的日志，
统计过滤前后的数量，并将统计信息保存到上下文的stats键中。"

❌ 不好的需求描述：
"处理数据"
```

### 2. **包含输入输出要求**

```text
✅ 好的需求描述：
"输入是日志数组，需要按照时间分组（按小时），
输出每小时的错误数量统计，格式为{hour: count}的对象。"

❌ 不够具体：
"按时间统计错误"
```

### 3. **指定业务规则**

```text
✅ 好的需求描述：
"对于响应时间超过1000ms的请求标记为慢请求，
计算慢请求比例，如果超过20%则添加告警标记。"

❌ 缺少具体规则：
"检查性能问题"
```

### 4. **包含错误处理要求**

```text
✅ 好的需求描述：
"如果输入数据为空或格式不正确，返回空数组并记录警告日志。
处理过程中如果遇到无效数据项，跳过该项但继续处理其他数据。"
```

## 🎨 高级用法示例

### 复杂数据分析脚本

```java
String requirement = """
    创建一个综合日志分析脚本：
    1. 分析错误模式：统计不同类型的错误（Exception、Timeout、Connection等）
    2. 时间趋势分析：按小时统计错误分布
    3. 严重程度评估：计算风险评分（错误率>10%为高风险）
    4. 生成建议：根据分析结果提供运维建议
    5. 输出格式：结构化的分析报告包含summary、details、recommendations
    """;

Map<String, Object> context = Map.of(
    "分析深度", "详细分析",
    "报告格式", "JSON结构",
    "性能要求", "处理大量数据"
);

ScriptGenerationResult result = generator.generateScript(
    workflow, nodeId, requirement, context);
```

### 实时数据处理脚本

```java
String requirement = """
    开发实时数据处理脚本：
    1. 接收流式日志数据，每批处理100-1000条记录
    2. 实时检测异常模式（错误率突增、响应时间异常等）
    3. 使用滑动窗口计算近5分钟的指标
    4. 当发现异常时立即设置告警标记到上下文
    5. 保持高性能，处理延迟控制在100ms以内
    """;
```

### 数据清洗和标准化脚本

```java
String requirement = """
    构建数据清洗脚本：
    1. 标准化日志格式：统一时间戳格式、级别名称
    2. 数据验证：检查必填字段，标记无效记录
    3. 数据增强：添加地理位置、用户分类等信息
    4. 去重处理：基于message和timestamp去除重复日志
    5. 输出清洗报告：记录处理统计和质量指标
    """;
```

## 🔍 上下文分析详解

### 自动检测的上下文信息

LLM脚本生成器会自动分析以下信息：

1. **输入数据源**
   - 前置节点类型和输出格式
   - 数据结构和字段信息
   - 数据量和特征

2. **可用上下文数据**
   - 之前节点设置的上下文键
   - 配置参数和全局设置
   - 中间处理结果

3. **输出目标要求**
   - 后续节点的输入期望
   - 数据格式和结构要求
   - 处理性能要求

### 手动指定上下文

```java
Map<String, Object> additionalContext = Map.of(
    "数据来源", "生产环境日志",
    "处理频率", "每分钟执行一次",
    "数据量级", "每批1000-5000条记录",
    "性能要求", "延迟<200ms",
    "错误容忍", "跳过无效数据继续处理",
    "输出格式", "JSON结构化数据"
);
```

## 🧪 脚本验证和调试

### 自动验证检查

生成的脚本会自动进行以下验证：

1. **语法检查**
   - JavaScript基本语法
   - LogFlow API使用规范
   - 变量声明和作用域

2. **兼容性检查**
   - Nashorn引擎兼容性
   - ES5语法要求
   - 避免现代JavaScript特性

3. **逻辑检查**
   - 输入数据验证
   - 返回值检查
   - 错误处理完整性

### 手动验证建议

```javascript
// 1. 输入验证模板
if (!input || !Array.isArray(input)) {
  logger.warn('输入数据无效或为空');
  return [];
}

// 2. 错误处理模板
try {
  // 处理逻辑
} catch (error) {
  logger.error('处理失败: ' + error.message);
  return null;
}

// 3. 性能监控模板
var startTime = Date.now();
// ... 处理逻辑
var duration = Date.now() - startTime;
logger.info('处理完成，耗时: ' + duration + 'ms');
```

## 📊 性能优化建议

### 1. **提示工程优化**

```java
// 针对特定领域优化提示
String domainSpecificPrompt = """
    你是一个专业的日志分析专家，精通LogFlow工作流系统。
    请根据以下日志分析场景生成高效的JavaScript脚本...
    """;
```

### 2. **上下文信息精简**

```java
// 只传递相关的上下文信息
Map<String, Object> focusedContext = Map.of(
    "核心需求", "错误检测和分类",
    "关键指标", "错误率、响应时间、异常模式"
);
```

### 3. **分批处理大型需求**

```java
// 复杂需求分解为多个简单脚本
String[] subRequirements = {
    "数据预处理和清洗",
    "错误检测和分类", 
    "统计分析和汇总",
    "报告生成和格式化"
};
```

## 🔧 集成到现有系统

### 1. **工作流配置工具集成**

```java
public class WorkflowConfigTool {
    private final LLMScriptGenerator scriptGenerator;
    
    public void generateScriptForNode(String workflowFile, String nodeId, String requirement) {
        // 加载工作流
        Workflow workflow = loadWorkflow(workflowFile);
        
        // 生成脚本
        ScriptGenerationResult result = scriptGenerator.generateScript(
            workflow, nodeId, requirement, null);
        
        // 更新YAML配置
        if (result.isSuccess()) {
            updateYamlConfig(workflowFile, nodeId, result.getGeneratedScript());
        }
    }
}
```

### 2. **Web界面集成**

```java
@RestController
public class ScriptGenerationController {
    
    @PostMapping("/api/generate-script")
    public ResponseEntity<ScriptGenerationResult> generateScript(
            @RequestBody ScriptGenerationRequest request) {
        
        try {
            LLMScriptGenerator.ScriptGenerationResult result = 
                scriptGenerator.generateScript(
                    request.getWorkflow(),
                    request.getNodeId(),
                    request.getRequirement(),
                    request.getAdditionalContext()
                );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
```

### 3. **命令行工具集成**

```java
public class CLIScriptGenerator {
    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);
        
        String workflowFile = cmd.getOptionValue("workflow");
        String nodeId = cmd.getOptionValue("node");
        String requirement = cmd.getOptionValue("requirement");
        
        // 生成脚本
        LLMScriptGenerator generator = createGenerator();
        ScriptGenerationResult result = generator.generateScript(
            loadWorkflow(workflowFile), nodeId, requirement, null);
        
        // 输出结果
        if (result.isSuccess()) {
            System.out.println(result.getGeneratedScript());
        } else {
            System.err.println("生成失败: " + result.getErrorMessage());
        }
    }
}
```

## 🛡️ 安全和最佳实践

### 1. **API密钥管理**

```java
// 使用环境变量存储敏感信息
String apiKey = System.getenv("OPENAI_API_KEY");
if (apiKey == null) {
    throw new IllegalStateException("API密钥未配置");
}
```

### 2. **输入验证**

```java
public void validateRequirement(String requirement) {
    if (requirement == null || requirement.trim().isEmpty()) {
        throw new IllegalArgumentException("需求描述不能为空");
    }
    if (requirement.length() > 10000) {
        throw new IllegalArgumentException("需求描述过长");
    }
    // 检查是否包含敏感信息
    if (containsSensitiveInfo(requirement)) {
        throw new SecurityException("需求描述包含敏感信息");
    }
}
```

### 3. **生成结果审查**

```java
public void reviewGeneratedScript(String script) {
    // 检查是否包含危险操作
    String[] dangerousPatterns = {
        "eval(", "Function(", "setTimeout(", "setInterval("
    };
    
    for (String pattern : dangerousPatterns) {
        if (script.contains(pattern)) {
            logger.warn("生成的脚本包含潜在危险操作: " + pattern);
        }
    }
}
```

## 📈 未来扩展

### 1. **多模型支持**
- 集成多个LLM提供者
- 根据需求类型选择最佳模型
- 模型性能和成本优化

### 2. **学习优化**
- 收集用户反馈改进生成质量
- 基于历史成功案例优化提示
- 自动化脚本性能调优

### 3. **可视化集成**
- 图形化需求描述界面
- 脚本生成过程可视化
- 实时预览和调试功能

## 总结

LogFlow LLM脚本生成功能为用户提供了强大的自动化脚本开发能力。通过智能的上下文分析和自然语言理解，即使是非编程人员也能快速创建复杂的数据处理逻辑。

关键成功因素：
1. **清晰的需求描述** - 详细、具体的业务需求
2. **合适的LLM选择** - 根据需求复杂度选择模型
3. **充分的测试验证** - 在生产环境使用前充分测试
4. **持续的优化改进** - 根据使用反馈不断优化

通过合理使用这一功能，可以显著提升LogFlow工作流的开发效率和质量。
