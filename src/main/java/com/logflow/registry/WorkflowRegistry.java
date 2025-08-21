package com.logflow.registry;

import com.logflow.engine.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流注册中心
 * 负责工作流的注册、管理和查找
 */
public class WorkflowRegistry {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowRegistry.class);

    private static volatile WorkflowRegistry instance;

    private final Map<String, WorkflowInfo> registeredWorkflows = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> workflowDependencies = new ConcurrentHashMap<>();
    private final Map<String, Long> workflowAccessTimes = new ConcurrentHashMap<>();

    // 工作流状态枚举
    public enum WorkflowStatus {
        ACTIVE, // 激活状态
        INACTIVE, // 非激活状态
        DEPRECATED, // 已废弃
        DRAFT // 草稿状态
    }

    private WorkflowRegistry() {
        logger.info("工作流注册中心已初始化");
    }

    /**
     * 获取单例实例
     */
    public static WorkflowRegistry getInstance() {
        if (instance == null) {
            synchronized (WorkflowRegistry.class) {
                if (instance == null) {
                    instance = new WorkflowRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * 注册工作流
     */
    public void registerWorkflow(Workflow workflow) {
        registerWorkflow(workflow, WorkflowStatus.ACTIVE, null, null);
    }

    /**
     * 注册工作流（完整版本）
     */
    public void registerWorkflow(Workflow workflow, WorkflowStatus status,
            String description, String version) {
        if (workflow == null || workflow.getId() == null) {
            throw new IllegalArgumentException("工作流和工作流ID不能为空");
        }

        String workflowId = workflow.getId();

        WorkflowInfo workflowInfo = new WorkflowInfo(
                workflow,
                status,
                description != null ? description : workflow.getDescription(),
                version != null ? version : "1.0.0",
                System.currentTimeMillis());

        registeredWorkflows.put(workflowId, workflowInfo);
        workflowAccessTimes.put(workflowId, System.currentTimeMillis());

        logger.info("工作流已注册: {} (版本: {}, 状态: {})",
                workflowId, workflowInfo.getVersion(), status);
    }

    /**
     * 获取工作流
     */
    public Workflow getWorkflow(String workflowId) {
        if (workflowId == null) {
            return null;
        }

        WorkflowInfo info = registeredWorkflows.get(workflowId);
        if (info == null) {
            logger.debug("工作流不存在: {}", workflowId);
            return null;
        }

        if (info.getStatus() != WorkflowStatus.ACTIVE) {
            logger.warn("工作流状态不是激活状态: {} (状态: {})", workflowId, info.getStatus());
            return null;
        }

        // 更新访问时间
        workflowAccessTimes.put(workflowId, System.currentTimeMillis());

        return info.getWorkflow();
    }

    /**
     * 获取工作流信息
     */
    public WorkflowInfo getWorkflowInfo(String workflowId) {
        return registeredWorkflows.get(workflowId);
    }

    /**
     * 检查工作流是否存在
     */
    public boolean hasWorkflow(String workflowId) {
        WorkflowInfo info = registeredWorkflows.get(workflowId);
        return info != null && info.getStatus() == WorkflowStatus.ACTIVE;
    }

    /**
     * 获取所有已注册的工作流ID
     */
    public Set<String> getRegisteredWorkflowIds() {
        return new HashSet<>(registeredWorkflows.keySet());
    }

    /**
     * 获取所有激活状态的工作流ID
     */
    public Set<String> getActiveWorkflowIds() {
        Set<String> activeIds = new HashSet<>();
        for (Map.Entry<String, WorkflowInfo> entry : registeredWorkflows.entrySet()) {
            if (entry.getValue().getStatus() == WorkflowStatus.ACTIVE) {
                activeIds.add(entry.getKey());
            }
        }
        return activeIds;
    }

    /**
     * 获取所有工作流信息
     */
    public Collection<WorkflowInfo> getAllWorkflowInfos() {
        return new ArrayList<>(registeredWorkflows.values());
    }

    /**
     * 更新工作流状态
     */
    public boolean updateWorkflowStatus(String workflowId, WorkflowStatus status) {
        WorkflowInfo info = registeredWorkflows.get(workflowId);
        if (info == null) {
            return false;
        }

        info.setStatus(status);
        logger.info("工作流状态已更新: {} -> {}", workflowId, status);
        return true;
    }

    /**
     * 注销工作流
     */
    public boolean unregisterWorkflow(String workflowId) {
        WorkflowInfo removed = registeredWorkflows.remove(workflowId);
        workflowAccessTimes.remove(workflowId);
        workflowDependencies.remove(workflowId);

        // 清理其他工作流对此工作流的依赖
        workflowDependencies.values().forEach(deps -> deps.remove(workflowId));

        if (removed != null) {
            logger.info("工作流已注销: {}", workflowId);
            return true;
        }

        return false;
    }

    /**
     * 添加工作流依赖关系
     */
    public void addWorkflowDependency(String workflowId, String dependentWorkflowId) {
        workflowDependencies.computeIfAbsent(workflowId, k -> new HashSet<>())
                .add(dependentWorkflowId);

        logger.debug("添加工作流依赖: {} -> {}", workflowId, dependentWorkflowId);
    }

    /**
     * 获取工作流依赖
     */
    public Set<String> getWorkflowDependencies(String workflowId) {
        return new HashSet<>(workflowDependencies.getOrDefault(workflowId, Collections.emptySet()));
    }

    /**
     * 检查循环依赖
     */
    public boolean hasCircularDependency(String workflowId) {
        return hasCircularDependency(workflowId, new HashSet<>());
    }

    private boolean hasCircularDependency(String workflowId, Set<String> visited) {
        if (visited.contains(workflowId)) {
            return true;
        }

        visited.add(workflowId);

        Set<String> dependencies = workflowDependencies.get(workflowId);
        if (dependencies != null) {
            for (String dependency : dependencies) {
                if (hasCircularDependency(dependency, new HashSet<>(visited))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 获取依赖此工作流的其他工作流
     */
    public Set<String> getWorkflowDependents(String workflowId) {
        Set<String> dependents = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : workflowDependencies.entrySet()) {
            if (entry.getValue().contains(workflowId)) {
                dependents.add(entry.getKey());
            }
        }

        return dependents;
    }

    /**
     * 按访问时间排序的工作流列表
     */
    public List<String> getWorkflowsByAccessTime(boolean ascending) {
        return workflowAccessTimes.entrySet().stream()
                .sorted(ascending ? Map.Entry.comparingByValue()
                        : Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 获取最近访问的工作流
     */
    public List<String> getRecentlyAccessedWorkflows(int limit) {
        return getWorkflowsByAccessTime(false).stream()
                .limit(limit)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 搜索工作流
     */
    public List<WorkflowInfo> searchWorkflows(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(registeredWorkflows.values());
        }

        String lowerKeyword = keyword.toLowerCase();

        return registeredWorkflows.values().stream()
                .filter(info -> info.getWorkflow().getId().toLowerCase().contains(lowerKeyword) ||
                        info.getWorkflow().getName().toLowerCase().contains(lowerKeyword) ||
                        (info.getDescription() != null &&
                                info.getDescription().toLowerCase().contains(lowerKeyword)))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 获取注册中心统计信息
     */
    public RegistryStatistics getStatistics() {
        Map<WorkflowStatus, Integer> statusCounts = new HashMap<>();
        for (WorkflowStatus status : WorkflowStatus.values()) {
            statusCounts.put(status, 0);
        }

        for (WorkflowInfo info : registeredWorkflows.values()) {
            statusCounts.merge(info.getStatus(), 1, Integer::sum);
        }

        int totalDependencies = workflowDependencies.values().stream()
                .mapToInt(Set::size)
                .sum();

        return new RegistryStatistics(
                registeredWorkflows.size(),
                statusCounts,
                totalDependencies,
                workflowAccessTimes.size());
    }

    /**
     * 清空注册中心
     */
    public void clear() {
        registeredWorkflows.clear();
        workflowDependencies.clear();
        workflowAccessTimes.clear();
        logger.info("工作流注册中心已清空");
    }

    /**
     * 工作流信息类
     */
    public static class WorkflowInfo {
        private final Workflow workflow;
        private WorkflowStatus status;
        private final String description;
        private final String version;
        private final long registrationTime;

        public WorkflowInfo(Workflow workflow, WorkflowStatus status,
                String description, String version, long registrationTime) {
            this.workflow = workflow;
            this.status = status;
            this.description = description;
            this.version = version;
            this.registrationTime = registrationTime;
        }

        // Getters
        public Workflow getWorkflow() {
            return workflow;
        }

        public WorkflowStatus getStatus() {
            return status;
        }

        public String getDescription() {
            return description;
        }

        public String getVersion() {
            return version;
        }

        public long getRegistrationTime() {
            return registrationTime;
        }

        // Setter
        public void setStatus(WorkflowStatus status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return String.format("WorkflowInfo{id='%s', name='%s', status=%s, version='%s'}",
                    workflow.getId(), workflow.getName(), status, version);
        }
    }

    /**
     * 注册中心统计信息类
     */
    public static class RegistryStatistics {
        private final int totalWorkflows;
        private final Map<WorkflowStatus, Integer> statusCounts;
        private final int totalDependencies;
        private final int trackedWorkflows;

        public RegistryStatistics(int totalWorkflows, Map<WorkflowStatus, Integer> statusCounts,
                int totalDependencies, int trackedWorkflows) {
            this.totalWorkflows = totalWorkflows;
            this.statusCounts = new HashMap<>(statusCounts);
            this.totalDependencies = totalDependencies;
            this.trackedWorkflows = trackedWorkflows;
        }

        // Getters
        public int getTotalWorkflows() {
            return totalWorkflows;
        }

        public Map<WorkflowStatus, Integer> getStatusCounts() {
            return new HashMap<>(statusCounts);
        }

        public int getTotalDependencies() {
            return totalDependencies;
        }

        public int getTrackedWorkflows() {
            return trackedWorkflows;
        }

        @Override
        public String toString() {
            return String.format("RegistryStatistics{total=%d, active=%d, dependencies=%d}",
                    totalWorkflows, statusCounts.getOrDefault(WorkflowStatus.ACTIVE, 0), totalDependencies);
        }
    }
}
