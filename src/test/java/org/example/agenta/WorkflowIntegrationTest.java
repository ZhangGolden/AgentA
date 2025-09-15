package org.example.agenta;

import org.example.agenta.agent.DataProcessorAgent;
import org.example.agenta.agent.ReportGeneratorAgent;
import org.example.agenta.agent.ValidationAgent;
import org.example.agenta.core.LogicalOperator;
import org.example.agenta.core.WorkflowDAG;
import org.example.agenta.core.WorkflowNode;
import org.example.agenta.model.WorkflowContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Workflow集成测试
 */
@SpringBootTest
public class WorkflowIntegrationTest {
    
    @Test
    public void testSimpleWorkflowExecution() throws Exception {
        // 创建Agent实例
        DataProcessorAgent dataAgent = new DataProcessorAgent();
        ValidationAgent validationAgent = new ValidationAgent();
        ReportGeneratorAgent reportAgent = new ReportGeneratorAgent();
        
        // 创建工作流
        WorkflowDAG workflow = new WorkflowDAG("test-workflow");
        
        WorkflowNode node1 = new WorkflowNode("node-1", dataAgent);
        WorkflowNode node2 = new WorkflowNode("node-2", validationAgent);
        WorkflowNode node3 = new WorkflowNode("node-3", reportAgent)
                .addDependency("data-processor-agent")
                .addDependency("validation-agent")
                .setOperator(LogicalOperator.AND);
        
        workflow.addNode(node1).addNode(node2).addNode(node3);
        
        // 创建上下文
        WorkflowContext context = new WorkflowContext()
                .addData("input", "测试数据输入");
        
        // 执行工作流
        WorkflowContext result = workflow.execute(context).get();
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isAgentCompleted("data-processor-agent"));
        assertTrue(result.isAgentCompleted("validation-agent"));
        assertTrue(result.isAgentCompleted("report-generator-agent"));
        
        // 验证报告生成结果
        var reportResult = result.getAgentResult("report-generator-agent");
        assertNotNull(reportResult);
        assertTrue(reportResult.isSuccess());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> reportData = (Map<String, Object>) reportResult.getResult();
        assertNotNull(reportData.get("finalReport"));
        
        workflow.shutdown();
        
        System.out.println("工作流执行摘要:");
        System.out.println(workflow.getExecutionSummary(result));
    }
    
    @Test
    public void testORLogicWorkflow() throws Exception {
        // 创建Agent实例
        DataProcessorAgent dataAgent = new DataProcessorAgent();
        ValidationAgent validationAgent = new ValidationAgent();
        ReportGeneratorAgent reportAgent = new ReportGeneratorAgent();
        
        // 创建使用OR逻辑的工作流
        WorkflowDAG workflow = new WorkflowDAG("or-logic-workflow");
        
        WorkflowNode node1 = new WorkflowNode("node-1", dataAgent);
        WorkflowNode node2 = new WorkflowNode("node-2", validationAgent);
        WorkflowNode node3 = new WorkflowNode("node-3", reportAgent)
                .addDependency("data-processor-agent")
                .addDependency("validation-agent")
                .setOperator(LogicalOperator.OR); // 任一完成即可
        
        workflow.addNode(node1).addNode(node2).addNode(node3);
        
        // 创建上下文
        WorkflowContext context = new WorkflowContext()
                .addData("input", "OR逻辑测试数据");
        
        // 执行工作流
        WorkflowContext result = workflow.execute(context).get();
        
        // 验证结果 - OR逻辑下，只要一个Agent完成，第三个Agent就会执行
        assertNotNull(result);
        assertTrue(result.isAgentCompleted("report-generator-agent"));
        
        workflow.shutdown();
        
        System.out.println("OR逻辑工作流执行摘要:");
        System.out.println(workflow.getExecutionSummary(result));
    }
    
    @Test
    public void testWorkflowContextDataFlow() throws Exception {
        // 测试上下文数据在Agent间的流转
        DataProcessorAgent dataAgent = new DataProcessorAgent();
        
        WorkflowDAG workflow = new WorkflowDAG("context-test-workflow");
        WorkflowNode node1 = new WorkflowNode("node-1", dataAgent);
        workflow.addNode(node1);
        
        WorkflowContext context = new WorkflowContext()
                .addData("input", "上下文测试数据")
                .addData("customParam", "自定义参数");
        
        WorkflowContext result = workflow.execute(context).get();
        
        // 验证上下文数据保持
        assertEquals("上下文测试数据", result.getData("input"));
        assertEquals("自定义参数", result.getData("customParam"));
        
        // 验证Agent结果存储
        assertNotNull(result.getAgentResult("data-processor-agent"));
        
        workflow.shutdown();
    }
}
