package org.example.agenta.demo;

import lombok.extern.slf4j.Slf4j;
import org.example.agenta.agent.DataProcessorAgent;
import org.example.agenta.agent.ReportGeneratorAgent;
import org.example.agenta.agent.ValidationAgent;
import org.example.agenta.core.LogicalOperator;
import org.example.agenta.core.WorkflowDAG;
import org.example.agenta.core.WorkflowNode;
import org.example.agenta.model.WorkflowContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Workflow演示类，在应用启动时运行示例
 */
@Component
@Slf4j
public class WorkflowDemo implements CommandLineRunner {
    
    @Override
    public void run(String... args) throws Exception {
        log.info("=== 开始Workflow系统演示 ===");
        
        // 演示1: AND逻辑工作流
        demonstrateANDWorkflow();
        
        Thread.sleep(2000); // 等待一段时间
        
        // 演示2: OR逻辑工作流
        demonstrateORWorkflow();
        
        log.info("=== Workflow系统演示完成 ===");
    }
    
    /**
     * 演示AND逻辑工作流
     */
    private void demonstrateANDWorkflow() throws Exception {
        log.info("\n--- 演示AND逻辑工作流 ---");
        log.info("场景: Agent1(数据处理) AND Agent2(验证) 完成后 -> Agent3(报告生成)");
        
        // 创建Agent实例
        DataProcessorAgent dataAgent = new DataProcessorAgent();
        ValidationAgent validationAgent = new ValidationAgent();
        ReportGeneratorAgent reportAgent = new ReportGeneratorAgent();
        
        // 构建工作流DAG
        WorkflowDAG workflow = new WorkflowDAG("and-demo-workflow");
        
        WorkflowNode node1 = new WorkflowNode("node-1", dataAgent);
        WorkflowNode node2 = new WorkflowNode("node-2", validationAgent);
        WorkflowNode node3 = new WorkflowNode("node-3", reportAgent)
                .addDependency("data-processor-agent")
                .addDependency("validation-agent")
                .setOperator(LogicalOperator.AND);
        
        workflow.addNode(node1).addNode(node2).addNode(node3);
        
        // 创建执行上下文
        WorkflowContext context = new WorkflowContext()
                .addData("input", "这是AND逻辑演示的输入数据")
                .addData("demo_type", "AND_LOGIC");
        
        // 执行工作流
        WorkflowContext result = workflow.execute(context).get();
        
        // 输出结果
        printWorkflowResults("AND逻辑工作流", workflow, result);
        
        workflow.shutdown();
    }
    
    /**
     * 演示OR逻辑工作流
     */
    private void demonstrateORWorkflow() throws Exception {
        log.info("\n--- 演示OR逻辑工作流 ---");
        log.info("场景: Agent1(数据处理) OR Agent2(验证) 任一完成后 -> Agent3(报告生成)");
        
        // 创建Agent实例
        DataProcessorAgent dataAgent = new DataProcessorAgent();
        ValidationAgent validationAgent = new ValidationAgent();
        ReportGeneratorAgent reportAgent = new ReportGeneratorAgent();
        
        // 构建工作流DAG
        WorkflowDAG workflow = new WorkflowDAG("or-demo-workflow");
        
        WorkflowNode node1 = new WorkflowNode("node-1", dataAgent);
        WorkflowNode node2 = new WorkflowNode("node-2", validationAgent);
        WorkflowNode node3 = new WorkflowNode("node-3", reportAgent)
                .addDependency("data-processor-agent")
                .addDependency("validation-agent")
                .setOperator(LogicalOperator.OR);
        
        workflow.addNode(node1).addNode(node2).addNode(node3);
        
        // 创建执行上下文
        WorkflowContext context = new WorkflowContext()
                .addData("input", "这是OR逻辑演示的输入数据")
                .addData("demo_type", "OR_LOGIC");
        
        // 执行工作流
        WorkflowContext result = workflow.execute(context).get();
        
        // 输出结果
        printWorkflowResults("OR逻辑工作流", workflow, result);
        
        workflow.shutdown();
    }
    
    /**
     * 打印工作流执行结果
     */
    private void printWorkflowResults(String workflowType, WorkflowDAG workflow, WorkflowContext result) {
        log.info("{}执行完成!", workflowType);
        
        // 打印执行摘要
        Map<String, Object> summary = workflow.getExecutionSummary(result);
        log.info("执行摘要: {}", summary);
        
        // 打印各Agent执行结果
        result.getAgentResults().forEach((agentId, agentResult) -> {
            log.info("Agent[{}] - 成功: {}, 执行时间: {}", 
                    agentId, agentResult.isSuccess(), agentResult.getExecutionTime());
            
            if (agentResult.isSuccess() && agentResult.getResult() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultData = (Map<String, Object>) agentResult.getResult();
                
                if ("report-generator-agent".equals(agentId)) {
                    log.info("最终报告:\n{}", resultData.get("finalReport"));
                }
            }
        });
        
        log.info("--- {} 演示结束 ---\n", workflowType);
    }
}
