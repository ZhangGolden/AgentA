package org.example.agenta.service;

import lombok.extern.slf4j.Slf4j;
import org.example.agenta.agent.ApiCallAgent;
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
    
    @Autowired
    private ApiCallAgent apiCallAgent;
    
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
     * 创建包含API调用的工作流
     * Agent1(DataProcessor) -> Agent2(ApiCall) -> Agent3(ReportGenerator)
     */
    public WorkflowDAG createApiWorkflow() {
        String workflowId = "api-workflow-" + System.currentTimeMillis();
        WorkflowDAG workflow = new WorkflowDAG(workflowId);
        
        // 创建节点
        WorkflowNode node1 = new WorkflowNode("node-1", dataProcessorAgent);
        WorkflowNode node2 = new WorkflowNode("node-2", apiCallAgent)
                .addDependency("data-processor-agent")
                .setOperator(LogicalOperator.AND); // 数据处理完成后调用API
        WorkflowNode node3 = new WorkflowNode("node-3", reportGeneratorAgent)
                .addDependency("api-call-agent")
                .setOperator(LogicalOperator.AND); // API调用完成后生成报告
        
        // 添加节点到工作流
        workflow.addNode(node1)
                .addNode(node2)
                .addNode(node3);
        
        log.info("创建API工作流成功: {}", workflowId);
        return workflow;
    }
    
    /**
     * 创建并行API调用工作流
     * Agent1(DataProcessor) -> [Agent2(ApiCall) AND Agent3(Validation)] -> Agent4(ReportGenerator)
     */
    public WorkflowDAG createParallelApiWorkflow() {
        String workflowId = "parallel-api-workflow-" + System.currentTimeMillis();
        WorkflowDAG workflow = new WorkflowDAG(workflowId);
        
        // 创建节点
        WorkflowNode node1 = new WorkflowNode("node-1", dataProcessorAgent);
        WorkflowNode node2 = new WorkflowNode("node-2", apiCallAgent)
                .addDependency("data-processor-agent")
                .setOperator(LogicalOperator.AND);
        WorkflowNode node3 = new WorkflowNode("node-3", validationAgent)
                .addDependency("data-processor-agent")
                .setOperator(LogicalOperator.AND);
        WorkflowNode node4 = new WorkflowNode("node-4", reportGeneratorAgent)
                .addDependency("api-call-agent")
                .addDependency("validation-agent")
                .setOperator(LogicalOperator.AND); // API调用和验证都完成后生成报告
        
        // 添加节点到工作流
        workflow.addNode(node1)
                .addNode(node2)
                .addNode(node3)
                .addNode(node4);
        
        log.info("创建并行API工作流成功: {}", workflowId);
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
    
    /**
     * 执行工作流（带上下文）
     */
    public CompletableFuture<Map<String, Object>> executeWorkflow(WorkflowDAG workflow, WorkflowContext context) {
        if (context.getData("startTime") == null) {
            context.addData("startTime", System.currentTimeMillis());
        }
        
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
