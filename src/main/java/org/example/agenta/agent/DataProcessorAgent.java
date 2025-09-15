package org.example.agenta.agent;

import lombok.extern.slf4j.Slf4j;
import org.example.agenta.core.Agent;
import org.example.agenta.model.AgentResult;
import org.example.agenta.model.WorkflowContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent1 - 数据处理Agent
 * 负责处理输入数据，进行基础的数据清洗和转换
 */
@Component
@Slf4j
public class DataProcessorAgent implements Agent {
    
    private static final String AGENT_ID = "data-processor-agent";
    
    @Override
    public String getAgentId() {
        return AGENT_ID;
    }
    
    @Override
    public String getDescription() {
        return "数据处理Agent - 负责数据清洗和转换";
    }
    
    @Override
    public AgentResult execute(WorkflowContext context) {
        try {
            log.info("DataProcessorAgent 开始执行");
            
            // 模拟数据处理逻辑
            Object inputData = context.getData("input");
            if (inputData == null) {
                inputData = "默认输入数据";
            }
            
            // 模拟LLM调用进行数据处理
            String processedData = processDataWithLLM(inputData.toString());
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("originalData", inputData);
            result.put("processedData", processedData);
            result.put("dataSize", processedData.length());
            
            // 模拟执行时间
            Thread.sleep(1000);
            
            log.info("DataProcessorAgent 执行完成，处理数据: {}", processedData);
            return AgentResult.success(AGENT_ID, result);
            
        } catch (Exception e) {
            log.error("DataProcessorAgent 执行失败", e);
            return AgentResult.failure(AGENT_ID, e.getMessage());
        }
    }
    
    @Override
    public boolean canExecute(WorkflowContext context) {
        // 该Agent可以作为起始节点，无前置条件
        return true;
    }
    
    /**
     * 模拟使用LLM进行数据处理
     */
    private String processDataWithLLM(String input) {
        // 这里模拟LLM调用，实际应用中可以集成LangChain4j
        return String.format("已处理的数据: [%s] -> 清洗后的结构化数据", input);
    }
}
