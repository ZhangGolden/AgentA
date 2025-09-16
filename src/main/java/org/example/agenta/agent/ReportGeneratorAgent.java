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
            AgentResult apiCallResult = context.getAgentResult("api-call-agent");
            
            // 检查必需的前置条件 - 数据处理必须成功
            if (dataProcessorResult == null || !dataProcessorResult.isSuccess()) {
                throw new RuntimeException("DataProcessorAgent 未成功执行");
            }
            
            // 检查至少有一个其他Agent成功执行
            boolean hasValidation = validationResult != null && validationResult.isSuccess();
            boolean hasApiCall = apiCallResult != null && apiCallResult.isSuccess();
            
            if (!hasValidation && !hasApiCall) {
                throw new RuntimeException("ValidationAgent 或 ApiCallAgent 至少需要一个成功执行");
            }
            
            // 模拟LLM调用生成综合报告
            String report = generateReportWithLLM(dataProcessorResult, validationResult, apiCallResult, context);
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("finalReport", report);
            result.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            result.put("dataProcessorSummary", summarizeAgentResult(dataProcessorResult));
            
            if (validationResult != null) {
                result.put("validationSummary", summarizeAgentResult(validationResult));
            }
            
            if (apiCallResult != null) {
                result.put("apiCallSummary", summarizeAgentResult(apiCallResult));
            }
            
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
        // 检查是否有基础的数据处理结果
        boolean hasDataProcessor = context.isAgentCompleted("data-processor-agent");
        
        // 检查是否有其他Agent的结果（验证或API调用）
        boolean hasValidation = context.isAgentCompleted("validation-agent");
        boolean hasApiCall = context.isAgentCompleted("api-call-agent");
        
        // 至少需要数据处理完成，以及验证或API调用中的一个
        return hasDataProcessor && (hasValidation || hasApiCall);
    }
    
    /**
     * 模拟使用LLM生成综合报告
     */
    private String generateReportWithLLM(AgentResult dataResult, AgentResult validationResult, AgentResult apiCallResult, WorkflowContext context) {
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
        
        // 验证结果摘要（如果存在）
        if (validationResult != null && validationResult.isSuccess()) {
            report.append("## 验证结果\n");
            if (validationResult.getResult() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> validationMap = (Map<String, Object>) validationResult.getResult();
                report.append("- 验证状态: ").append(validationMap.get("isValid")).append("\n");
                report.append("- 验证评分: ").append(validationMap.get("validationScore")).append("\n");
                report.append("- 验证报告: ").append(validationMap.get("validationReport")).append("\n\n");
            }
        }
        
        // API调用结果摘要（如果存在）
        if (apiCallResult != null && apiCallResult.isSuccess()) {
            report.append("## API调用结果\n");
            if (apiCallResult.getResult() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> apiMap = (Map<String, Object>) apiCallResult.getResult();
                
                @SuppressWarnings("unchecked")
                Map<String, Object> apiRequest = (Map<String, Object>) apiMap.get("apiRequest");
                @SuppressWarnings("unchecked")
                Map<String, Object> apiResponse = (Map<String, Object>) apiMap.get("apiResponse");
                
                if (apiRequest != null) {
                    report.append("- 请求URL: ").append(apiRequest.get("url")).append("\n");
                    report.append("- 请求方法: ").append(apiRequest.get("method")).append("\n");
                }
                
                if (apiResponse != null) {
                    report.append("- 响应状态码: ").append(apiResponse.get("statusCode")).append("\n");
                    report.append("- 调用成功: ").append(apiResponse.get("success")).append("\n");
                    report.append("- 执行时间: ").append(apiResponse.get("executionTimeMs")).append("ms\n");
                    
                    Object body = apiResponse.get("body");
                    if (body != null) {
                        String bodyStr = body.toString();
                        if (bodyStr.length() > 100) {
                            bodyStr = bodyStr.substring(0, 100) + "...";
                        }
                        report.append("- 响应内容: ").append(bodyStr).append("\n");
                    }
                }
                report.append("\n");
            }
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
