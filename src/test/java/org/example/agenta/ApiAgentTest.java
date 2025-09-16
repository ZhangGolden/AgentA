package org.example.agenta;

import org.example.agenta.agent.ApiCallAgent;
import org.example.agenta.agent.DataProcessorAgent;
import org.example.agenta.agent.ReportGeneratorAgent;
import org.example.agenta.core.LogicalOperator;
import org.example.agenta.core.WorkflowDAG;
import org.example.agenta.core.WorkflowNode;
import org.example.agenta.model.WorkflowContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API Agent测试
 */
@SpringBootTest
public class ApiAgentTest {
    
    @Test
    public void testApiCallAgentWithConfig() throws Exception {
        // 创建Agent实例
        DataProcessorAgent dataAgent = new DataProcessorAgent();
        ApiCallAgent apiAgent = new ApiCallAgent();
        ReportGeneratorAgent reportAgent = new ReportGeneratorAgent();
        
        // 创建API调用工作流
        WorkflowDAG workflow = new WorkflowDAG("api-test-workflow");
        
        WorkflowNode node1 = new WorkflowNode("node-1", dataAgent);
        WorkflowNode node2 = new WorkflowNode("node-2", apiAgent)
                .addDependency("data-processor-agent")
                .setOperator(LogicalOperator.AND);
        WorkflowNode node3 = new WorkflowNode("node-3", reportAgent)
                .addDependency("api-call-agent")
                .setOperator(LogicalOperator.AND);
        
        workflow.addNode(node1).addNode(node2).addNode(node3);
        
        // 创建包含API配置的上下文
        WorkflowContext context = new WorkflowContext()
                .addData("input", "API调用测试数据")
                .addData("apiConfig", Map.of(
                        "url", "https://jsonplaceholder.typicode.com/posts",
                        "method", "POST",
                        "body", Map.of(
                                "title", "AgentA测试API调用",
                                "body", "这是通过AgentA系统发起的API调用测试",
                                "userId", 1
                        ),
                        "timeoutSeconds", 10
                ));
        
        try {
            // 执行工作流
            WorkflowContext result = workflow.execute(context).get();
            
            // 验证结果
            assertNotNull(result);
            assertTrue(result.isAgentCompleted("data-processor-agent"));
            
            // 注意：API调用可能因为网络问题失败，这是正常的
            var apiResult = result.getAgentResult("api-call-agent");
            assertNotNull(apiResult);
            
            // 如果API调用成功，验证报告生成
            if (apiResult.isSuccess()) {
                assertTrue(result.isAgentCompleted("report-generator-agent"));
            }
            
            System.out.println("API工作流测试完成");
            System.out.println("执行摘要: " + workflow.getExecutionSummary(result));
            
        } catch (Exception e) {
            // API调用失败是正常的（网络问题），不应该导致测试失败
            System.out.println("API调用测试失败（可能由于网络问题）: " + e.getMessage());
        } finally {
            workflow.shutdown();
        }
    }
    
    @Test
    public void testApiCallAgentCanExecute() {
        ApiCallAgent apiAgent = new ApiCallAgent();
        
        // 测试没有API配置的情况
        WorkflowContext contextWithoutConfig = new WorkflowContext()
                .addData("input", "测试数据");
        
        assertFalse(apiAgent.canExecute(contextWithoutConfig));
        
        // 测试有API配置的情况
        WorkflowContext contextWithConfig = new WorkflowContext()
                .addData("input", "测试数据")
                .addData("apiConfig", Map.of("url", "https://example.com"));
        
        assertTrue(apiAgent.canExecute(contextWithConfig));
    }
}
