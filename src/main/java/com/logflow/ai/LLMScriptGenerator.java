package com.logflow.ai;

import com.logflow.engine.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LLM脚本生成器
 * 使用大语言模型根据用户需求生成LogFlow脚本
 */
public class LLMScriptGenerator {

    private static final Logger logger = LoggerFactory.getLogger(LLMScriptGenerator.class);
    private static final String PROMPT_TEMPLATE_PATH = "/prompts/logflow-script-generation-prompt.md";

    private final WorkflowContextAnalyzer contextAnalyzer;
    private final LLMProvider llmProvider;
    private String promptTemplate;

    public LLMScriptGenerator(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
        this.contextAnalyzer = new WorkflowContextAnalyzer();
        loadPromptTemplate();
    }

    /**
     * 根据用户需求生成脚本
     * 
     * @param workflow          工作流对象
     * @param scriptNodeId      脚本节点ID
     * @param userRequirement   用户需求描述
     * @param additionalContext 额外的上下文信息
     * @return 脚本生成结果
     */
    public ScriptGenerationResult generateScript(Workflow workflow, String scriptNodeId,
            String userRequirement, Map<String, Object> additionalContext) {
        logger.info("开始生成脚本: 节点={}, 需求长度={}", scriptNodeId, userRequirement.length());

        try {
            // 1. 分析工作流上下文
            WorkflowContextAnalyzer.ContextAnalysisResult contextResult = contextAnalyzer.analyzeScriptContext(workflow,
                    scriptNodeId);

            // 2. 构建LLM提示
            String llmPrompt = buildLLMPrompt(contextResult, userRequirement, additionalContext);

            // 3. 调用LLM生成脚本
            logger.debug("调用LLM生成脚本，提示长度: {}", llmPrompt.length());
            String generatedScript = llmProvider.generateText(llmPrompt);

            // 4. 解析和清理生成的脚本
            String cleanedScript = cleanupGeneratedScript(generatedScript);

            // 5. 验证脚本
            List<String> validationIssues = validateScript(cleanedScript, contextResult);

            // 6. 构建结果
            ScriptGenerationResult result = new ScriptGenerationResult();
            result.setSuccess(true);
            result.setGeneratedScript(cleanedScript);
            result.setOriginalResponse(generatedScript);
            result.setUserRequirement(userRequirement);
            result.setContextAnalysis(contextResult);
            result.setValidationIssues(validationIssues);
            result.setLlmPrompt(llmPrompt);

            // 7. 生成使用说明
            result.setUsageInstructions(generateUsageInstructions(contextResult, cleanedScript));

            logger.info("脚本生成完成: 代码长度={}, 验证问题数={}",
                    cleanedScript.length(), validationIssues.size());

            return result;

        } catch (Exception e) {
            logger.error("脚本生成失败", e);

            ScriptGenerationResult result = new ScriptGenerationResult();
            result.setSuccess(false);
            result.setErrorMessage("脚本生成失败: " + e.getMessage());
            result.setUserRequirement(userRequirement);

            return result;
        }
    }

    /**
     * 构建LLM提示
     */
    private String buildLLMPrompt(WorkflowContextAnalyzer.ContextAnalysisResult contextResult,
            String userRequirement, Map<String, Object> additionalContext) {
        StringBuilder prompt = new StringBuilder();

        // 1. 基础提示模板
        prompt.append(promptTemplate);
        prompt.append("\n\n");

        // 2. 工作流上下文信息
        prompt.append("## 当前工作流上下文\n\n");
        prompt.append("### 工作流信息\n");
        prompt.append("- 工作流ID: ").append(contextResult.getWorkflowId()).append("\n");
        prompt.append("- 工作流名称: ").append(contextResult.getWorkflowName()).append("\n");
        prompt.append("- 脚本节点ID: ").append(contextResult.getNodeId()).append("\n\n");

        // 3. 数据流信息
        prompt.append("### 数据流分析\n");
        prompt.append(contextResult.getDataFlowDescription()).append("\n");

        // 4. 输入数据信息
        if (!contextResult.getInputSources().isEmpty()) {
            prompt.append("### 输入数据详情\n");
            for (WorkflowContextAnalyzer.InputSourceInfo source : contextResult.getInputSources()) {
                prompt.append(String.format("- **%s** (%s): %s\n  数据类型: `%s`\n",
                        source.getNodeName(), source.getNodeType(),
                        source.getDescription(), source.getExpectedDataType()));
            }
            prompt.append("\n");
        }

        // 5. 可用上下文数据
        if (!contextResult.getContextData().isEmpty()) {
            prompt.append("### 可用的上下文数据\n");
            for (WorkflowContextAnalyzer.ContextDataInfo context : contextResult.getContextData()) {
                prompt.append(String.format("- `context.get('%s')`: %s (类型: %s)\n",
                        context.getKey(), context.getDescription(), context.getDataType()));
            }
            prompt.append("\n");
        }

        // 6. 输出要求
        if (!contextResult.getOutputTargets().isEmpty()) {
            prompt.append("### 输出要求\n");
            prompt.append("脚本输出将被以下节点使用:\n");
            for (WorkflowContextAnalyzer.OutputTargetInfo target : contextResult.getOutputTargets()) {
                prompt.append(String.format("- **%s** (%s): %s\n  期望数据类型: `%s`\n",
                        target.getNodeName(), target.getNodeType(),
                        target.getDescription(), target.getExpectedDataType()));
            }
            prompt.append("\n");
        }

        // 7. 脚本配置信息
        if (contextResult.getScriptConfig() != null) {
            prompt.append("### 脚本配置\n");
            WorkflowContextAnalyzer.ScriptConfigInfo config = contextResult.getScriptConfig();
            if (config.getInputKey() != null) {
                prompt.append("- 输入键: `").append(config.getInputKey()).append("`\n");
            }
            if (config.getOutputKey() != null) {
                prompt.append("- 输出键: `").append(config.getOutputKey()).append("`\n");
            }
            if (config.getParameters() != null && !config.getParameters().isEmpty()) {
                prompt.append("- 可用参数: ");
                config.getParameters().forEach((key, value) -> prompt.append("`").append(key).append("` "));
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        // 8. 额外上下文
        if (additionalContext != null && !additionalContext.isEmpty()) {
            prompt.append("### 额外上下文信息\n");
            additionalContext
                    .forEach((key, value) -> prompt.append("- ").append(key).append(": ").append(value).append("\n"));
            prompt.append("\n");
        }

        // 9. 用户需求
        prompt.append("## 用户需求\n\n");
        prompt.append(userRequirement);
        prompt.append("\n\n");

        // 10. 生成指令
        prompt.append("## 生成指令\n\n");
        prompt.append("请根据以上信息生成符合需求的JavaScript脚本。确保:\n");
        prompt.append("1. 脚本能正确处理输入数据\n");
        prompt.append("2. 合理使用可用的上下文数据\n");
        prompt.append("3. 输出格式符合后续节点的期望\n");
        prompt.append("4. 包含适当的错误处理和日志记录\n");
        prompt.append("5. 添加清晰的注释说明关键逻辑\n\n");

        return prompt.toString();
    }

    /**
     * 清理生成的脚本
     */
    private String cleanupGeneratedScript(String generatedScript) {
        // 移除可能的markdown代码块标记
        String cleaned = generatedScript;

        // 移除开头的```javascript或```js
        cleaned = cleaned.replaceAll("^```(?:javascript|js)?\\s*\n", "");

        // 移除结尾的```
        cleaned = cleaned.replaceAll("\n?```\\s*$", "");

        // 移除可能的解释性文本（保留注释）
        String[] lines = cleaned.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inScript = false;

        for (String line : lines) {
            // 如果行以//开头，总是保留（注释）
            if (line.trim().startsWith("//")) {
                result.append(line).append("\n");
                inScript = true;
                continue;
            }

            // 如果行包含JavaScript关键字，开始保留
            if (line.contains("var ") || line.contains("function ") ||
                    line.contains("for (") || line.contains("if (") ||
                    line.contains("context.") || line.contains("logger.") ||
                    line.contains("utils.") || line.contains("input")) {
                inScript = true;
            }

            // 如果已经在脚本中，保留所有行
            if (inScript) {
                result.append(line).append("\n");
            }
        }

        return result.toString().trim();
    }

    /**
     * 验证生成的脚本
     */
    private List<String> validateScript(String script, WorkflowContextAnalyzer.ContextAnalysisResult context) {
        List<String> issues = new ArrayList<>();

        // 检查是否包含基本的JavaScript语法
        if (!script.contains("var ") && !script.contains("function ")) {
            issues.add("脚本可能缺少变量声明或函数定义");
        }

        // 检查是否有返回值
        String[] lines = script.split("\n");
        String lastLine = "";
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (!line.isEmpty() && !line.startsWith("//")) {
                lastLine = line;
                break;
            }
        }

        if (lastLine.isEmpty() || lastLine.endsWith(";")) {
            issues.add("脚本可能缺少返回值表达式（最后一行应该是返回值，不以分号结尾）");
        }

        // 检查是否使用了不兼容的语法
        if (script.contains("const ") || script.contains("let ") || script.contains("=>")) {
            issues.add("检测到ES6+语法，建议使用var和function关键字以确保兼容性");
        }

        // 检查是否使用了input变量
        if (!script.contains("input")) {
            issues.add("脚本可能未使用input变量获取输入数据");
        }

        // 检查是否有日志记录
        if (!script.contains("logger.")) {
            issues.add("建议添加适当的日志记录");
        }

        return issues;
    }

    /**
     * 生成使用说明
     */
    private String generateUsageInstructions(WorkflowContextAnalyzer.ContextAnalysisResult context, String script) {
        StringBuilder instructions = new StringBuilder();

        instructions.append("## 脚本使用说明\n\n");

        instructions.append("### 1. 复制脚本到YAML配置\n");
        instructions.append("将生成的脚本复制到您的YAML配置文件中的script字段：\n\n");
        instructions.append("```yaml\n");
        instructions.append("- id: \"").append(context.getNodeId()).append("\"\n");
        instructions.append("  name: \"脚本节点\"\n");
        instructions.append("  type: \"script\"\n");
        instructions.append("  config:\n");
        instructions.append("    scriptEngine: \"javascript\"\n");
        if (context.getScriptConfig() != null) {
            if (context.getScriptConfig().getInputKey() != null) {
                instructions.append("    inputKey: \"").append(context.getScriptConfig().getInputKey()).append("\"\n");
            }
            if (context.getScriptConfig().getOutputKey() != null) {
                instructions.append("    outputKey: \"").append(context.getScriptConfig().getOutputKey())
                        .append("\"\n");
            }
        }
        instructions.append("    script: |\n");

        // 缩进脚本内容
        String[] lines = script.split("\n");
        for (String line : lines) {
            instructions.append("      ").append(line).append("\n");
        }
        instructions.append("```\n\n");

        instructions.append("### 2. 测试脚本\n");
        instructions.append("建议先在小数据集上测试脚本，确保功能正常。\n\n");

        instructions.append("### 3. 监控执行\n");
        instructions.append("运行工作流时注意观察日志输出，确认脚本执行结果。\n\n");

        return instructions.toString();
    }

    /**
     * 加载提示模板
     */
    private void loadPromptTemplate() {
        try (InputStream is = getClass().getResourceAsStream(PROMPT_TEMPLATE_PATH)) {
            if (is != null) {
                promptTemplate = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                logger.info("加载提示模板成功，长度: {}", promptTemplate.length());
            } else {
                logger.warn("提示模板文件不存在: {}", PROMPT_TEMPLATE_PATH);
                promptTemplate = "# 请根据需求生成LogFlow JavaScript脚本\n\n";
            }
        } catch (IOException e) {
            logger.error("加载提示模板失败", e);
            promptTemplate = "# 请根据需求生成LogFlow JavaScript脚本\n\n";
        }
    }

    /**
     * LLM提供者接口
     */
    public interface LLMProvider {
        /**
         * 生成文本
         * 
         * @param prompt 输入提示
         * @return 生成的文本
         * @throws Exception 生成失败时抛出异常
         */
        String generateText(String prompt) throws Exception;
    }

    /**
     * 脚本生成结果
     */
    public static class ScriptGenerationResult {
        private boolean success;
        private String generatedScript;
        private String originalResponse;
        private String userRequirement;
        private String errorMessage;
        private WorkflowContextAnalyzer.ContextAnalysisResult contextAnalysis;
        private List<String> validationIssues;
        private String llmPrompt;
        private String usageInstructions;

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getGeneratedScript() {
            return generatedScript;
        }

        public void setGeneratedScript(String generatedScript) {
            this.generatedScript = generatedScript;
        }

        public String getOriginalResponse() {
            return originalResponse;
        }

        public void setOriginalResponse(String originalResponse) {
            this.originalResponse = originalResponse;
        }

        public String getUserRequirement() {
            return userRequirement;
        }

        public void setUserRequirement(String userRequirement) {
            this.userRequirement = userRequirement;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public WorkflowContextAnalyzer.ContextAnalysisResult getContextAnalysis() {
            return contextAnalysis;
        }

        public void setContextAnalysis(WorkflowContextAnalyzer.ContextAnalysisResult contextAnalysis) {
            this.contextAnalysis = contextAnalysis;
        }

        public List<String> getValidationIssues() {
            return validationIssues;
        }

        public void setValidationIssues(List<String> validationIssues) {
            this.validationIssues = validationIssues;
        }

        public String getLlmPrompt() {
            return llmPrompt;
        }

        public void setLlmPrompt(String llmPrompt) {
            this.llmPrompt = llmPrompt;
        }

        public String getUsageInstructions() {
            return usageInstructions;
        }

        public void setUsageInstructions(String usageInstructions) {
            this.usageInstructions = usageInstructions;
        }
    }
}
