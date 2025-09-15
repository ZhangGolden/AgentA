package org.example.agenta.agent;

import lombok.extern.slf4j.Slf4j;
import org.example.agenta.core.Agent;
import org.example.agenta.model.AgentResult;
import org.example.agenta.model.WorkflowContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent3 - 报告生成Agent
 * 依赖Agent1和Agent2完成后执行，生成综合报告
 */
@Component
@Slf4j
public class ReportGeneratorAgent implements Agent {
    
    private static final String AGENT_ID = "report-generator-agent";
    
    @Override
    public String getAgentId() {
        return AGENT_ID;
    }
    
    @Override
    public String getDescription() {
        return "报告生成Agent - 基于前置Agent结果生成综合报告";
    }
    
    @Override
    public AgentResult execute(WorkflowContext context) {
        try {
            log.info("ReportGeneratorAgent 开始执行");
            
            // 获取前置Agent的执行结果
            AgentResult dataProcessorResult = context.getAgentResult("data-processor-agent");
            AgentResult validationResult = context.getAgentResult("validation-agent");
            
            // 检查前置条件
            if (dataProcessorResult == null || !dataProcessorResult.isSuccess()) {
                throw new RuntimeException("DataProcessorAgent 未成功执行");
            }
            
            if (validationResult == null || !validationResult.isSuccess()) {
                throw new RuntimeException("ValidationAgent 未成功执行");
            }
            
            // 模拟LLM调用生成综合报告
            String report = generateReportWithLLM(dataProcessorResult, validationResult, context);
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("finalReport", report);
            result.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            result.put("dataProcessorSummary", summarizeAgentResult(dataProcessorResult));
            result.put("validationSummary", summarizeAgentResult(validationResult));
            
            // 模拟执行时间
            Thread.sleep(1200);
            
            log.info("ReportGeneratorAgent 执行完成，生成报告长度: {}", report.length());
            return AgentResult.success(AGENT_ID, result);
            
        } catch (Exception e) {
            log.error("ReportGeneratorAgent 执行失败", e);
            return AgentResult.failure(AGENT_ID, e.getMessage());
        }
    }
    
    @Override
    public boolean canExecute(WorkflowContext context) {
        // 必须等待前置Agent完成
        return context.isAgentCompleted("data-processor-agent") && 
               context.isAgentCompleted("validation-agent");
    }
    
    /**
     * 模拟使用LLM生成综合报告
     */
    private String generateReportWithLLM(AgentResult dataResult, AgentResult validationResult, WorkflowContext context) {
        StringBuilder report = new StringBuilder();
        
        report.append("=== 工作流执行综合报告 ===\n");
        report.append("工作流ID: ").append(context.getWorkflowId()).append("\n");
        report.append("生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        // 数据处理结果摘要
        report.append("## 数据处理结果\n");
        if (dataResult.getResult() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) dataResult.getResult();
            report.append("- 原始数据: ").append(dataMap.get("originalData")).append("\n");
            report.append("- 处理后数据: ").append(dataMap.get("processedData")).append("\n");
            report.append("- 数据大小: ").append(dataMap.get("dataSize")).append(" 字符\n\n");
        }
        
        // 验证结果摘要
        report.append("## 验证结果\n");
        if (validationResult.getResult() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> validationMap = (Map<String, Object>) validationResult.getResult();
            report.append("- 验证状态: ").append(validationMap.get("isValid")).append("\n");
            report.append("- 验证评分: ").append(validationMap.get("validationScore")).append("\n");
            report.append("- 验证报告: ").append(validationMap.get("validationReport")).append("\n\n");
        }
        
        // 使用LLM生成智能总结
        report.append("## AI 智能总结\n");
        report.append("基于以上数据处理和验证结果，系统运行状态良好。");
        report.append("数据处理流程正常，验证机制有效，整体工作流执行成功。");
        report.append("建议继续监控后续数据流转情况。\n\n");
        
        report.append("=== 报告结束 ===");
        
        return report.toString();
    }
    
    /**
     * 汇总Agent执行结果
     */
    private Map<String, Object> summarizeAgentResult(AgentResult result) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("agentId", result.getAgentId());
        summary.put("success", result.isSuccess());
        summary.put("executionTime", result.getExecutionTime());
        
        if (!result.isSuccess()) {
            summary.put("errorMessage", result.getErrorMessage());
        }
        
        return summary;
    }
}
