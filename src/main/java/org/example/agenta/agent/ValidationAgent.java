package org.example.agenta.agent;

import lombok.extern.slf4j.Slf4j;
import org.example.agenta.core.Agent;
import org.example.agenta.model.AgentResult;
import org.example.agenta.model.WorkflowContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent2 - 验证Agent
 * 负责验证数据的有效性和完整性
 */
@Component
@Slf4j
public class ValidationAgent implements Agent {
    
    private static final String AGENT_ID = "validation-agent";
    
    @Override
    public String getAgentId() {
        return AGENT_ID;
    }
    
    @Override
    public String getDescription() {
        return "验证Agent - 负责数据验证和完整性检查";
    }
    
    @Override
    public AgentResult execute(WorkflowContext context) {
        try {
            log.info("ValidationAgent 开始执行");
            
            // 获取输入数据进行验证
            Object inputData = context.getData("input");
            if (inputData == null) {
                inputData = "默认验证数据";
            }
            
            // 模拟LLM调用进行数据验证
            ValidationResult validationResult = validateDataWithLLM(inputData.toString());
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("inputData", inputData);
            result.put("isValid", validationResult.isValid());
            result.put("validationScore", validationResult.getScore());
            result.put("validationReport", validationResult.getReport());
            
            // 模拟执行时间
            Thread.sleep(800);
            
            log.info("ValidationAgent 执行完成，验证结果: {}, 评分: {}", 
                    validationResult.isValid(), validationResult.getScore());
            
            return AgentResult.success(AGENT_ID, result);
            
        } catch (Exception e) {
            log.error("ValidationAgent 执行失败", e);
            return AgentResult.failure(AGENT_ID, e.getMessage());
        }
    }
    
    @Override
    public boolean canExecute(WorkflowContext context) {
        // 该Agent也可以作为起始节点，无前置条件
        return true;
    }
    
    /**
     * 模拟使用LLM进行数据验证
     */
    private ValidationResult validateDataWithLLM(String input) {
        // 这里模拟LLM调用，实际应用中可以集成LangChain4j进行智能验证
        boolean isValid = input != null && input.trim().length() > 0;
        double score = isValid ? 0.85 + Math.random() * 0.15 : 0.3; // 随机生成0.85-1.0的分数
        String report = isValid ? "数据验证通过，格式正确，内容完整" : "数据验证失败，内容为空或格式错误";
        
        return new ValidationResult(isValid, score, report);
    }
    
    /**
     * 验证结果内部类
     */
    private static class ValidationResult {
        private final boolean valid;
        private final double score;
        private final String report;
        
        public ValidationResult(boolean valid, double score, String report) {
            this.valid = valid;
            this.score = score;
            this.report = report;
        }
        
        public boolean isValid() { return valid; }
        public double getScore() { return score; }
        public String getReport() { return report; }
    }
}
