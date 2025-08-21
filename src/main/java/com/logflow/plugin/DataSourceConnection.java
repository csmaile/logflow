package com.logflow.plugin;

import com.logflow.core.WorkflowContext;

import java.util.List;
import java.util.Map;

/**
 * 数据源连接接口
 * 
 * 定义了数据源连接的基本操作，由具体插件实现
 */
public interface DataSourceConnection extends AutoCloseable {

    /**
     * 读取数据
     * 
     * @param context 工作流上下文
     * @return 读取的数据，通常是List或单个对象
     * @throws PluginException 读取失败时抛出
     */
    Object readData(WorkflowContext context) throws PluginException;

    /**
     * 分页读取数据
     * 
     * @param context    工作流上下文
     * @param pageSize   每页大小
     * @param pageNumber 页码（从1开始）
     * @return 分页数据结果
     * @throws PluginException 读取失败时抛出
     */
    default PagedResult readDataPaged(WorkflowContext context, int pageSize, int pageNumber) throws PluginException {
        // 默认实现：如果不支持分页，则返回全部数据的第一页
        Object data = readData(context);
        List<?> dataList = convertToList(data);

        int total = dataList.size();
        int start = (pageNumber - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        List<?> pageData = dataList.subList(start, end);
        return new PagedResult(pageData, pageNumber, pageSize, total);
    }

    /**
     * 流式读取数据
     * 
     * @param context  工作流上下文
     * @param callback 数据回调处理器
     * @throws PluginException 读取失败时抛出
     */
    default void readDataStream(WorkflowContext context, DataCallback callback) throws PluginException {
        // 默认实现：将批量读取转换为流式处理
        Object data = readData(context);
        List<?> dataList = convertToList(data);

        for (Object item : dataList) {
            callback.onData(item);
        }
        callback.onComplete();
    }

    /**
     * 写入数据（可选）
     * 
     * @param data    要写入的数据
     * @param context 工作流上下文
     * @throws PluginException 写入失败时抛出
     */
    default void writeData(Object data, WorkflowContext context) throws PluginException {
        throw new PluginException("Write operation not supported by this data source");
    }

    /**
     * 测试连接是否有效
     * 
     * @return 是否连接有效
     */
    boolean isConnected();

    /**
     * 获取连接状态信息
     * 
     * @return 连接状态信息
     */
    default Map<String, Object> getConnectionInfo() {
        return Map.of(
                "connected", isConnected(),
                "connectionTime", System.currentTimeMillis());
    }

    /**
     * 获取数据统计信息（可选）
     * 
     * @return 数据统计信息，如记录数量等
     */
    default Map<String, Object> getDataStatistics() {
        return Map.of();
    }

    /**
     * 关闭连接
     * 实现AutoCloseable接口
     */
    @Override
    void close() throws Exception;

    /**
     * 将数据转换为List
     * 辅助方法，用于处理不同类型的数据
     */
    @SuppressWarnings("unchecked")
    default List<?> convertToList(Object data) {
        if (data instanceof List) {
            return (List<?>) data;
        } else if (data instanceof Object[]) {
            return List.of((Object[]) data);
        } else if (data != null) {
            return List.of(data);
        } else {
            return List.of();
        }
    }

    /**
     * 分页结果封装
     */
    class PagedResult {
        private final List<?> data;
        private final int pageNumber;
        private final int pageSize;
        private final long totalCount;
        private final int totalPages;

        public PagedResult(List<?> data, int pageNumber, int pageSize, long totalCount) {
            this.data = data;
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.totalCount = totalCount;
            this.totalPages = (int) Math.ceil((double) totalCount / pageSize);
        }

        public List<?> getData() {
            return data;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public int getPageSize() {
            return pageSize;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public boolean hasNext() {
            return pageNumber < totalPages;
        }

        public boolean hasPrevious() {
            return pageNumber > 1;
        }
    }

    /**
     * 数据回调接口
     * 用于流式处理
     */
    interface DataCallback {
        /**
         * 处理单条数据
         * 
         * @param data 数据项
         */
        void onData(Object data);

        /**
         * 数据处理完成
         */
        default void onComplete() {
        }

        /**
         * 处理错误
         * 
         * @param error 错误信息
         */
        default void onError(Exception error) {
        }
    }
}
