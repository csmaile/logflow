/**
 * LogFlow脚本环境TypeScript定义文件
 * 提供LogFlow脚本节点中可用的API类型定义和智能提示
 * 
 * 使用方法：
 * 1. 将此文件复制到您的IDE工作区
 * 2. 在脚本编辑时引用此文件获得智能提示
 * 3. 在YAML文件中编写脚本时，IDE会识别这些全局对象
 */

/**
 * 工作流上下文对象，用于在节点间共享数据
 */
declare const context: {
    /**
     * 从上下文中获取数据
     * @param key 数据键名
     * @returns 存储的数据，如果不存在返回undefined
     * @example
     * const config = context.get('config');
     * const logData = context.get('log_data');
     */
    get<T = any>(key: string): T | undefined;

    /**
     * 将数据存储到上下文中
     * @param key 数据键名
     * @param value 要存储的数据
     * @example
     * context.set('processed_data', filteredLogs);
     * context.set('stats', { count: 100, errors: 5 });
     */
    set<T = any>(key: string, value: T): void;

    /**
     * 获取当前工作流的ID
     * @returns 工作流ID字符串
     * @example
     * const workflowId = context.getWorkflowId();
     */
    getWorkflowId(): string;

    /**
     * 获取当前执行的ID
     * @returns 执行ID字符串
     * @example
     * const executionId = context.getExecutionId();
     */
    getExecutionId(): string;
};

/**
 * 日志记录器对象，用于输出日志信息
 */
declare const logger: {
    /**
     * 输出调试级别日志
     * @param message 日志消息
     * @param args 额外参数
     * @example
     * logger.debug('处理开始', { count: logs.length });
     */
    debug(message: string, ...args: any[]): void;

    /**
     * 输出信息级别日志
     * @param message 日志消息
     * @param args 额外参数
     * @example
     * logger.info('处理完成，共处理 ' + result.length + ' 条记录');
     */
    info(message: string, ...args: any[]): void;

    /**
     * 输出警告级别日志
     * @param message 日志消息
     * @param args 额外参数
     * @example
     * logger.warn('发现异常数据', { anomalies: anomalyCount });
     */
    warn(message: string, ...args: any[]): void;

    /**
     * 输出错误级别日志
     * @param message 日志消息
     * @param args 额外参数
     * @example
     * logger.error('处理失败', error);
     */
    error(message: string, ...args: any[]): void;
};

/**
 * 实用工具对象，提供常用的辅助函数
 */
declare const utils: {
    /**
     * 获取当前时间戳（ISO格式字符串）
     * @returns 当前时间的ISO格式字符串
     * @example
     * const timestamp = utils.now(); // "2024-01-20T10:30:45.123Z"
     */
    now(): string;

    /**
     * 格式化时间戳
     * @param timestamp 时间戳或Date对象
     * @param format 格式化模式（可选）
     * @returns 格式化后的时间字符串
     * @example
     * const formatted = utils.formatDate(new Date(), 'yyyy-MM-dd HH:mm:ss');
     */
    formatDate(timestamp: Date | string | number, format?: string): string;

    /**
     * 深度克隆对象
     * @param obj 要克隆的对象
     * @returns 克隆后的对象
     * @example
     * const cloned = utils.deepClone(originalData);
     */
    deepClone<T>(obj: T): T;
};

/**
 * 脚本输入数据
 * 这是从上一个节点或初始输入传递过来的数据
 * 类型根据实际数据而变化
 */
declare const input: any;

/**
 * 脚本参数对象
 * 这是在节点配置中通过parameters字段传递的参数
 */
declare const params: Record<string, any>;

// ================ 常用数据类型定义 ================

/**
 * 日志条目接口
 */
interface LogEntry {
    id: number | string;
    level: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR' | 'FATAL';
    message: string;
    timestamp: string | number;
    value?: number;
    [key: string]: any;
}

/**
 * 诊断结果接口
 */
interface DiagnosisResult {
    type: string;
    issueCount: number;
    maxSeverity: string;
    issues: Issue[];
    timestamp: string;
}

/**
 * 问题/异常接口
 */
interface Issue {
    code: string;
    title: string;
    description: string;
    severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | 'INFO' | 'WARN' | 'ERROR' | 'FATAL';
    lineNumber?: number;
    [key: string]: any;
}

/**
 * 分析报告接口
 */
interface AnalysisReport {
    timestamp: string;
    summary: {
        riskScore: number;
        riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
        totalIssues: number;
        [key: string]: any;
    };
    diagnostics: {
        errors: DiagnosisResult;
        performance: DiagnosisResult;
        patterns: DiagnosisResult;
        anomalies: DiagnosisResult;
        [key: string]: any;
    };
    recommendations: string[];
    [key: string]: any;
}

/**
 * 统计信息接口
 */
interface ProcessingStats {
    totalInput: number;
    totalOutput: number;
    errorCount: number;
    filterRate: string;
    [key: string]: any;
}

// ================ 常用脚本模式 ================

/**
 * 数据过滤模式
 * @example
 * // 过滤日志级别
 * const filtered = input.filter(log => log.level !== 'DEBUG');
 *
 * // 按时间范围过滤
 * const recent = input.filter(log => {
 *   const logTime = new Date(log.timestamp);
 *   const hourAgo = new Date(Date.now() - 3600000);
 *   return logTime > hourAgo;
 * });
 */

/**
 * 数据转换模式
 * @example
 * // 添加处理时间戳
 * const processed = input.map(item => ({
 *   ...item,
 *   processedAt: utils.now(),
 *   processedBy: 'script_node_id'
 * }));
 *
 * // 数据聚合
 * const summary = input.reduce((acc, log) => {
 *   acc[log.level] = (acc[log.level] || 0) + 1;
 *   return acc;
 * }, {});
 */

/**
 * 上下文操作模式
 * @example
 * // 获取配置
 * const config = context.get('config') || {};
 *
 * // 设置处理结果
 * context.set('processing_result', {
 *   success: true,
 *   count: processedData.length,
 *   timestamp: utils.now()
 * });
 *
 * // 累积统计信息
 * const existingStats = context.get('stats') || { total: 0 };
 * context.set('stats', {
 *   ...existingStats,
 *   total: existingStats.total + newData.length
 * });
 */

/**
 * 日志记录模式
 * @example
 * // 记录处理进度
 * logger.info(`开始处理 ${input.length} 条记录`);
 *
 * // 记录关键指标
 * logger.info(`处理完成: 成功=${successCount}, 失败=${errorCount}`);
 *
 * // 记录异常情况
 * if (anomalies.length > 0) {
 *   logger.warn(`发现 ${anomalies.length} 个异常项`, { anomalies });
 * }
 */

// ================ 脚本返回值说明 ================

/**
 * 脚本的返回值
 * 
 * 脚本的最后一个表达式的值将作为节点的输出结果。
 * 这个值会被存储到配置的outputKey中，供后续节点使用。
 * 
 * @example
 * // 返回处理后的数据
 * const result = processData(input);
 * result; // 这个值会作为节点输出
 * 
 * // 返回复杂对象
 * ({
 *   data: processedData,
 *   metadata: {
 *     processedAt: utils.now(),
 *     count: processedData.length
 *   }
 * });
 */
