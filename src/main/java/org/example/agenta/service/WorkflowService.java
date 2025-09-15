package org.example.agenta.service;

import lombok.extern.slf4j.Slf4j;
import org.example.agenta.agent.DataProcessorAgent;
import org.example.agenta.agent.ReportGeneratorAgent;
import org.example.agenta.agent.ValidationAgent;
import org.example.agenta.core.LogicalOperator;
import org.example.agenta.core.WorkflowDAG;
import org.example.agenta.core.WorkflowNode;
import org.example.agenta.model.WorkflowContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Workflow服务，负责创建和执行工作流
 */
@Service
@Slf4j
public class WorkflowService {
    
    @Autowired
    private DataProcessorAgent dataProcessorAgent;
    
    @Autowired
    private ValidationAgent validationAgent;
    
    @Autowired
    private ReportGeneratorAgent reportGeneratorAgent;
    
    /**
     * 创建示例工作流
     * Agent1(DataProcessor) AND Agent2(Validation) -> Agent3(ReportGenerator)
     */
    public WorkflowDAG createSampleWorkflow() {
        String workflowId = "sample-workflow-" + System.currentTimeMillis();
        WorkflowDAG workflow = new WorkflowDAG(workflowId);
        
        // 创建节点
        WorkflowNode node1 = new WorkflowNode("node-1", dataProcessorAgent);
        WorkflowNode node2 = new WorkflowNode("node-2", validationAgent);
        WorkflowNode node3 = new WorkflowNode("node-3", reportGeneratorAgent)
                .addDependency("data-processor-agent")
                .addDependency("validation-agent")
                .setOperator(LogicalOperator.AND); // Agent1 AND Agent2 完成后才能执行Agent3
        
        // 添加节点到工作流
        workflow.addNode(node1)
                .addNode(node2)
                .addNode(node3);
        
        log.info("创建工作流成功: {}", workflowId);
        return workflow;
    }
    
    /**
     * 创建复杂的工作流示例（展示OR逻辑）
     */
    public WorkflowDAG createComplexWorkflow() {
        String workflowId = "complex-workflow-" + System.currentTimeMillis();
        WorkflowDAG workflow = new WorkflowDAG(workflowId);
        
        // 创建节点
        WorkflowNode node1 = new WorkflowNode("node-1", dataProcessorAgent);
        WorkflowNode node2 = new WorkflowNode("node-2", validationAgent);
        
        // 创建一个OR逻辑的节点：Agent1 OR Agent2 完成后就能执行Agent3
        WorkflowNode node3 = new WorkflowNode("node-3", reportGeneratorAgent)
                .addDependency("data-processor-agent")
                .addDependency("validation-agent")
                .setOperator(LogicalOperator.OR); // Agent1 OR Agent2 任一完成即可执行Agent3
        
        // 添加节点到工作流
        workflow.addNode(node1)
                .addNode(node2)
                .addNode(node3);
        
        log.info("创建复杂工作流成功: {}", workflowId);
        return workflow;
    }
    
    /**
     * 执行工作流
     */
    public CompletableFuture<Map<String, Object>> executeWorkflow(WorkflowDAG workflow, Object inputData) {
        // 创建工作流上下文
        WorkflowContext context = new WorkflowContext()
                .addData("input", inputData)
                .addData("startTime", System.currentTimeMillis());
        
        log.info("开始执行工作流: {}", workflow.getWorkflowId());
        
        return workflow.execute(context)
                .thenApply(completedContext -> {
                    // 生成执行摘要
                    Map<String, Object> summary = workflow.getExecutionSummary(completedContext);
                    summary.put("context", completedContext);
                    summary.put("executionTime", System.currentTimeMillis() - (Long) context.getData("startTime"));
                    
                    log.info("工作流执行完成: {}, 摘要: {}", workflow.getWorkflowId(), summary);
                    return summary;
                })
                .exceptionally(throwable -> {
                    log.error("工作流执行失败: {}", workflow.getWorkflowId(), throwable);
                    throw new RuntimeException("工作流执行失败", throwable);
                });
    }
}
