package org.example.agenta.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.agenta.model.AgentResult;
import org.example.agenta.model.WorkflowContext;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Workflow DAG管理器，负责管理和执行工作流
 */
@Data
@Slf4j
public class WorkflowDAG {
    
    private String workflowId;
    private Map<String, WorkflowNode> nodes;
    private ExecutorService executorService;
    
    public WorkflowDAG(String workflowId) {
        this.workflowId = workflowId;
        this.nodes = new LinkedHashMap<>();
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * 添加节点
     */
    public WorkflowDAG addNode(WorkflowNode node) {
        this.nodes.put(node.getNodeId(), node);
        return this;
    }
    
    /**
     * 获取节点
     */
    public WorkflowNode getNode(String nodeId) {
        return this.nodes.get(nodeId);
    }
    
    /**
     * 执行工作流
     */
    public CompletableFuture<WorkflowContext> execute(WorkflowContext context) {
        log.info("开始执行工作流: {}", workflowId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                while (hasExecutableNodes(context)) {
                    List<WorkflowNode> executableNodes = getExecutableNodes(context);
                    
                    if (executableNodes.isEmpty()) {
                        log.warn("没有可执行的节点，可能存在循环依赖");
                        break;
                    }
                    
                    // 并行执行可执行的节点
                    List<CompletableFuture<Void>> futures = executableNodes.stream()
                            .map(node -> executeNode(node, context))
                            .toList();
                    
                    // 等待所有节点执行完成
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                }
                
                log.info("工作流执行完成: {}", workflowId);
                return context;
                
            } catch (Exception e) {
                log.error("工作流执行失败: {}", workflowId, e);
                throw new RuntimeException("工作流执行失败", e);
            }
        }, executorService);
    }
    
    /**
     * 执行单个节点
     */
    private CompletableFuture<Void> executeNode(WorkflowNode node, WorkflowContext context) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("开始执行节点: {}", node.getNodeId());
                
                AgentResult result = node.getAgent().execute(context);
                context.addAgentResult(node.getAgent().getAgentId(), result);
                node.setExecuted(true);
                
                log.info("节点执行完成: {}, 结果: {}", node.getNodeId(), result.isSuccess());
                
            } catch (Exception e) {
                log.error("节点执行失败: {}", node.getNodeId(), e);
                AgentResult failureResult = AgentResult.failure(node.getAgent().getAgentId(), e.getMessage());
                context.addAgentResult(node.getAgent().getAgentId(), failureResult);
                node.setExecuted(true);
            }
        }, executorService);
    }
    
    /**
     * 检查是否还有可执行的节点
     */
    private boolean hasExecutableNodes(WorkflowContext context) {
        return nodes.values().stream()
                .anyMatch(node -> !node.isExecuted() && node.checkCanExecute(context));
    }
    
    /**
     * 获取当前可执行的节点列表
     */
    private List<WorkflowNode> getExecutableNodes(WorkflowContext context) {
        return nodes.values().stream()
                .filter(node -> !node.isExecuted() && node.checkCanExecute(context))
                .toList();
    }
    
    /**
     * 获取执行结果统计
     */
    public Map<String, Object> getExecutionSummary(WorkflowContext context) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("workflowId", workflowId);
        summary.put("totalNodes", nodes.size());
        
        long completedNodes = nodes.values().stream()
                .mapToLong(node -> node.isExecuted() ? 1 : 0)
                .sum();
        
        long successfulNodes = context.getAgentResults().values().stream()
                .mapToLong(result -> result.isSuccess() ? 1 : 0)
                .sum();
        
        summary.put("completedNodes", completedNodes);
        summary.put("successfulNodes", successfulNodes);
        summary.put("failedNodes", completedNodes - successfulNodes);
        
        return summary;
    }
    
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
