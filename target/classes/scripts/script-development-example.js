/**
 * LogFlow脚本开发示例
 * 
 * 这个文件展示了如何在独立的JavaScript文件中开发LogFlow脚本，
 * 然后复制到YAML配置中使用。
 * 
 * 使用方法：
 * 1. 在支持TypeScript的IDE中打开此文件
 * 2. 确保logflow.d.ts在同一目录或IDE能识别的路径中
 * 3. 享受智能提示和类型检查
 * 4. 开发完成后复制到YAML配置的script字段
 */

/// <reference path="logflow.d.ts" />

// ==================== 示例1：数据过滤和转换 ====================

/**
 * 日志数据过滤和转换脚本
 * 功能：过滤指定级别的日志，并添加处理时间戳
 */
function logFilterAndTransform() {
    // 获取配置参数
    const config = context.get('config') || {};
    const minLevel = config.minLevel || 'INFO';

    // 定义日志级别优先级
    const levelPriority = {
        'DEBUG': 1,
        'INFO': 2,
        'WARN': 3,
        'ERROR': 4,
        'FATAL': 5
    };

    const minPriority = levelPriority[minLevel] || 2;

    // 处理输入数据
    const logs = input;
    const processed = [];
    let filteredCount = 0;

    logger.info(`开始处理日志，总数: ${logs.length}，最小级别: ${minLevel}`);

    for (let i = 0; i < logs.length; i++) {
        const log = logs[i];

        // 检查日志级别
        const logPriority = levelPriority[log.level] || 1;
        if (logPriority < minPriority) {
            filteredCount++;
            continue;
        }

        // 转换日志格式
        const processedLog = {
            ...log,
            processedAt: utils.now(),
            processingIndex: processed.length + 1,
            originalIndex: i + 1,
            // 添加解析后的时间信息
            parsedTime: new Date(log.timestamp),
            hour: new Date(log.timestamp).getHours(),
            // 添加级别优先级
            levelPriority: logPriority
        };

        processed.push(processedLog);
    }

    // 保存统计信息到上下文
    const stats = {
        totalInput: logs.length,
        totalOutput: processed.length,
        filteredCount: filteredCount,
        filterRate: ((filteredCount / logs.length) * 100).toFixed(2) + '%',
        minLevel: minLevel,
        processedAt: utils.now()
    };

    context.set('filter_stats', stats);

    logger.info(`处理完成: 输入${logs.length}条，输出${processed.length}条，过滤${filteredCount}条`);

    return processed;
}

// ==================== 示例2：错误分析和统计 ====================

/**
 * 错误分析脚本
 * 功能：分析日志中的错误模式，生成错误报告
 */
function errorAnalysis() {
    const logs = input;
    const analysis = {
        summary: {
            totalLogs: logs.length,
            errorCount: 0,
            warningCount: 0,
            fatalCount: 0,
            errorRate: 0
        },
        patterns: {
            errorTypes: {},
            timeDistribution: {},
            frequentErrors: []
        },
        recommendations: []
    };

    logger.info(`开始错误分析，日志总数: ${logs.length}`);

    // 分析每条日志
    for (let i = 0; i < logs.length; i++) {
        const log = logs[i];

        // 统计错误级别
        switch (log.level) {
            case 'ERROR':
                analysis.summary.errorCount++;
                analyzeErrorPattern(log, analysis.patterns);
                break;
            case 'FATAL':
                analysis.summary.fatalCount++;
                analyzeErrorPattern(log, analysis.patterns);
                break;
            case 'WARN':
                analysis.summary.warningCount++;
                break;
        }

        // 时间分布分析
        const hour = new Date(log.timestamp).getHours();
        analysis.patterns.timeDistribution[hour] = (analysis.patterns.timeDistribution[hour] || 0) + 1;
    }

    // 计算错误率
    const totalErrors = analysis.summary.errorCount + analysis.summary.fatalCount;
    analysis.summary.errorRate = ((totalErrors / logs.length) * 100).toFixed(2);

    // 查找高频错误
    const sortedErrors = Object.entries(analysis.patterns.errorTypes)
        .sort(([, a], [, b]) => b - a)
        .slice(0, 5);

    analysis.patterns.frequentErrors = sortedErrors.map(([type, count]) => ({
        type: type,
        count: count,
        percentage: ((count / totalErrors) * 100).toFixed(2)
    }));

    // 生成建议
    generateRecommendations(analysis);

    logger.info(`错误分析完成: 发现${totalErrors}个错误，错误率${analysis.summary.errorRate}%`);

    return analysis;
}

/**
 * 分析单条错误日志的模式
 */
function analyzeErrorPattern(log, patterns) {
    // 分析错误类型
    let errorType = 'Unknown';

    if (log.message.includes('Exception')) {
        errorType = 'Exception';
    } else if (log.message.includes('Timeout')) {
        errorType = 'Timeout';
    } else if (log.message.includes('Connection')) {
        errorType = 'Connection';
    } else if (log.message.includes('Permission') || log.message.includes('Access')) {
        errorType = 'Permission';
    } else if (log.message.includes('Memory') || log.message.includes('OutOfMemory')) {
        errorType = 'Memory';
    } else if (log.message.includes('Database') || log.message.includes('SQL')) {
        errorType = 'Database';
    }

    patterns.errorTypes[errorType] = (patterns.errorTypes[errorType] || 0) + 1;
}

/**
 * 根据分析结果生成建议
 */
function generateRecommendations(analysis) {
    const { summary, patterns } = analysis;

    // 错误率建议
    if (parseFloat(summary.errorRate) > 10) {
        analysis.recommendations.push('错误率过高(' + summary.errorRate + '%)，建议立即检查系统状态');
    } else if (parseFloat(summary.errorRate) > 5) {
        analysis.recommendations.push('错误率较高(' + summary.errorRate + '%)，需要关注');
    }

    // 致命错误建议
    if (summary.fatalCount > 0) {
        analysis.recommendations.push('发现' + summary.fatalCount + '个致命错误，需要紧急处理');
    }

    // 高频错误建议
    if (patterns.frequentErrors.length > 0) {
        const topError = patterns.frequentErrors[0];
        analysis.recommendations.push('最频繁的错误类型是' + topError.type + '，占比' + topError.percentage + '%');
    }

    // 时间分布建议
    const peakHour = Object.entries(patterns.timeDistribution)
        .sort(([, a], [, b]) => b - a)[0];

    if (peakHour) {
        analysis.recommendations.push('错误高峰时段是' + peakHour[0] + '点，建议重点监控');
    }

    // 正常情况
    if (analysis.recommendations.length === 0) {
        analysis.recommendations.push('错误情况正常，继续监控');
    }
}

// ==================== 示例3：性能监控和分析 ====================

/**
 * 性能分析脚本
 * 功能：分析响应时间、吞吐量等性能指标
 */
function performanceAnalysis() {
    const data = input;
    const config = context.get('config') || {};
    const slowThreshold = config.slowThreshold || 1000; // 默认1秒

    const performance = {
        summary: {
            totalRequests: data.length,
            slowRequests: 0,
            fastRequests: 0,
            averageResponseTime: 0,
            maxResponseTime: 0,
            minResponseTime: Number.MAX_VALUE,
            throughput: 0
        },
        distribution: {
            fast: 0,      // < 100ms
            normal: 0,    // 100ms - 1000ms
            slow: 0,      // 1000ms - 5000ms
            verySlow: 0   // > 5000ms
        },
        percentiles: {},
        trends: {
            hourlyAverage: {},
            timeBasedPattern: []
        }
    };

    logger.info(`开始性能分析，请求总数: ${data.length}，慢请求阈值: ${slowThreshold}ms`);

    const responseTimes = [];
    let totalTime = 0;

    // 分析每个请求
    for (let i = 0; i < data.length; i++) {
        const item = data[i];
        const responseTime = item.responseTime || item.duration || item.executionTime || 0;

        responseTimes.push(responseTime);
        totalTime += responseTime;

        // 更新最大最小值
        performance.summary.maxResponseTime = Math.max(performance.summary.maxResponseTime, responseTime);
        performance.summary.minResponseTime = Math.min(performance.summary.minResponseTime, responseTime);

        // 统计快慢请求
        if (responseTime > slowThreshold) {
            performance.summary.slowRequests++;
        } else {
            performance.summary.fastRequests++;
        }

        // 响应时间分布
        if (responseTime < 100) {
            performance.distribution.fast++;
        } else if (responseTime < 1000) {
            performance.distribution.normal++;
        } else if (responseTime < 5000) {
            performance.distribution.slow++;
        } else {
            performance.distribution.verySlow++;
        }

        // 时间趋势分析
        const hour = new Date(item.timestamp).getHours();
        if (!performance.trends.hourlyAverage[hour]) {
            performance.trends.hourlyAverage[hour] = { total: 0, count: 0 };
        }
        performance.trends.hourlyAverage[hour].total += responseTime;
        performance.trends.hourlyAverage[hour].count++;
    }

    // 计算平均值和百分位数
    performance.summary.averageResponseTime = Math.round(totalTime / data.length);
    performance.summary.throughput = Math.round(data.length * 1000 / totalTime * 60); // 每分钟请求数

    // 计算百分位数
    responseTimes.sort((a, b) => a - b);
    performance.percentiles = {
        p50: getPercentile(responseTimes, 50),
        p75: getPercentile(responseTimes, 75),
        p90: getPercentile(responseTimes, 90),
        p95: getPercentile(responseTimes, 95),
        p99: getPercentile(responseTimes, 99)
    };

    // 计算小时平均值
    for (const hour in performance.trends.hourlyAverage) {
        const hourData = performance.trends.hourlyAverage[hour];
        performance.trends.hourlyAverage[hour] = Math.round(hourData.total / hourData.count);
    }

    logger.info(`性能分析完成: 平均响应时间${performance.summary.averageResponseTime}ms，慢请求${performance.summary.slowRequests}个`);

    return performance;
}

/**
 * 计算百分位数
 */
function getPercentile(sortedArray, percentile) {
    const index = Math.ceil((percentile / 100) * sortedArray.length) - 1;
    return sortedArray[Math.max(0, index)];
}

// ==================== 示例4：数据聚合和报告生成 ====================

/**
 * 综合报告生成脚本
 * 功能：汇总多个分析结果，生成综合报告
 */
function generateComprehensiveReport() {
    // 获取各种分析结果
    const filterStats = context.get('filter_stats');
    const errorAnalysis = context.get('error_analysis');
    const performanceAnalysis = context.get('performance_analysis');
    const config = context.get('config') || {};

    const report = {
        metadata: {
            generatedAt: utils.now(),
            workflowId: context.getWorkflowId(),
            executionId: context.getExecutionId(),
            reportVersion: '1.0.0',
            dataSource: config.dataSource || 'unknown'
        },

        summary: {
            riskLevel: 'LOW',
            riskScore: 0,
            totalIssues: 0,
            healthScore: 100,
            status: 'HEALTHY'
        },

        dataProcessing: filterStats,

        errorAnalysis: errorAnalysis,

        performanceAnalysis: performanceAnalysis,

        insights: [],

        recommendations: [],

        metrics: {
            reliability: 0,    // 可靠性评分 (0-100)
            performance: 0,    // 性能评分 (0-100)  
            availability: 0    // 可用性评分 (0-100)
        }
    };

    logger.info('开始生成综合报告');

    // 计算风险评分和健康度
    calculateRiskAndHealth(report);

    // 生成洞察
    generateInsights(report);

    // 合并所有建议
    mergeRecommendations(report);

    logger.info(`综合报告生成完成: 风险级别=${report.summary.riskLevel}, 健康评分=${report.summary.healthScore}`);

    return report;
}

/**
 * 计算风险评分和健康度
 */
function calculateRiskAndHealth(report) {
    let riskScore = 0;
    let totalIssues = 0;

    // 错误风险评分
    if (report.errorAnalysis) {
        const errorRate = parseFloat(report.errorAnalysis.summary.errorRate);
        totalIssues += report.errorAnalysis.summary.errorCount + report.errorAnalysis.summary.fatalCount;

        if (errorRate > 10) riskScore += 40;
        else if (errorRate > 5) riskScore += 25;
        else if (errorRate > 1) riskScore += 10;

        if (report.errorAnalysis.summary.fatalCount > 0) riskScore += 30;

        // 可靠性评分
        report.metrics.reliability = Math.max(0, 100 - errorRate * 5);
    }

    // 性能风险评分
    if (report.performanceAnalysis) {
        const slowRate = (report.performanceAnalysis.summary.slowRequests / report.performanceAnalysis.summary.totalRequests) * 100;

        if (slowRate > 20) riskScore += 35;
        else if (slowRate > 10) riskScore += 20;
        else if (slowRate > 5) riskScore += 10;

        // 性能评分
        const avgTime = report.performanceAnalysis.summary.averageResponseTime;
        if (avgTime < 100) report.metrics.performance = 100;
        else if (avgTime < 500) report.metrics.performance = 90;
        else if (avgTime < 1000) report.metrics.performance = 75;
        else if (avgTime < 2000) report.metrics.performance = 50;
        else report.metrics.performance = 25;
    }

    // 可用性评分（基于错误率和性能）
    report.metrics.availability = Math.round((report.metrics.reliability + report.metrics.performance) / 2);

    // 确定风险级别
    if (riskScore >= 60) report.summary.riskLevel = 'CRITICAL';
    else if (riskScore >= 40) report.summary.riskLevel = 'HIGH';
    else if (riskScore >= 20) report.summary.riskLevel = 'MEDIUM';
    else report.summary.riskLevel = 'LOW';

    // 健康评分
    report.summary.healthScore = Math.max(0, 100 - riskScore);
    report.summary.riskScore = riskScore;
    report.summary.totalIssues = totalIssues;

    // 系统状态
    if (report.summary.healthScore >= 90) report.summary.status = 'EXCELLENT';
    else if (report.summary.healthScore >= 75) report.summary.status = 'HEALTHY';
    else if (report.summary.healthScore >= 50) report.summary.status = 'CONCERNING';
    else report.summary.status = 'CRITICAL';
}

/**
 * 生成数据洞察
 */
function generateInsights(report) {
    const insights = [];

    // 数据处理洞察
    if (report.dataProcessing) {
        insights.push(`数据处理效率: 处理了${report.dataProcessing.totalInput}条记录，过滤率${report.dataProcessing.filterRate}`);
    }

    // 错误洞察
    if (report.errorAnalysis && report.errorAnalysis.patterns.frequentErrors.length > 0) {
        const topError = report.errorAnalysis.patterns.frequentErrors[0];
        insights.push(`主要错误类型: ${topError.type}，占比${topError.percentage}%`);
    }

    // 性能洞察
    if (report.performanceAnalysis) {
        const p95 = report.performanceAnalysis.percentiles.p95;
        insights.push(`性能表现: 95%的请求响应时间在${p95}ms以内`);

        if (report.performanceAnalysis.summary.slowRequests > 0) {
            const slowRate = ((report.performanceAnalysis.summary.slowRequests / report.performanceAnalysis.summary.totalRequests) * 100).toFixed(1);
            insights.push(`慢请求比例: ${slowRate}%，需要关注`);
        }
    }

    report.insights = insights;
}

/**
 * 合并所有建议
 */
function mergeRecommendations(report) {
    const recommendations = [];

    // 添加错误分析建议
    if (report.errorAnalysis && report.errorAnalysis.recommendations) {
        recommendations.push(...report.errorAnalysis.recommendations);
    }

    // 添加基于综合评分的建议
    if (report.summary.riskLevel === 'CRITICAL') {
        recommendations.unshift('系统风险级别为严重，建议立即采取措施');
    } else if (report.summary.riskLevel === 'HIGH') {
        recommendations.unshift('系统风险级别较高，建议尽快处理');
    }

    // 添加性能建议
    if (report.performanceAnalysis && report.performanceAnalysis.summary.slowRequests > 10) {
        recommendations.push('慢请求数量较多，建议进行性能优化');
    }

    // 如果没有问题
    if (recommendations.length === 0) {
        recommendations.push('系统运行状况良好，继续保持监控');
    }

    report.recommendations = recommendations;
}

// ==================== 主执行脚本选择器 ====================

/**
 * 根据配置选择执行不同的脚本功能
 * 这个函数展示了如何在一个脚本中实现多种功能
 */
function executeScript() {
    const config = context.get('config') || {};
    const scriptType = config.scriptType || 'filter';

    logger.info(`执行脚本类型: ${scriptType}`);

    switch (scriptType) {
        case 'filter':
            return logFilterAndTransform();

        case 'error_analysis':
            return errorAnalysis();

        case 'performance_analysis':
            return performanceAnalysis();

        case 'comprehensive_report':
            return generateComprehensiveReport();

        default:
            logger.warn(`未知的脚本类型: ${scriptType}，使用默认过滤功能`);
            return logFilterAndTransform();
    }
}

// 执行主脚本
// 注意：在实际的YAML配置中，您只需要复制需要的函数和这一行执行代码
const result = executeScript();
result;
