package org.example.agenta.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent执行结果
 */
@Data
@Accessors(chain = true)
public class AgentResult {
    
    private String agentId;
    private boolean success;
    private Object result;
    private String errorMessage;
    private LocalDateTime executionTime;
    private Map<String, Object> metadata;
    
    public AgentResult() {
        this.executionTime = LocalDateTime.now();
    }
    
    public static AgentResult success(String agentId, Object result) {
        return new AgentResult()
                .setAgentId(agentId)
                .setSuccess(true)
                .setResult(result);
    }
    
    public static AgentResult failure(String agentId, String errorMessage) {
        return new AgentResult()
                .setAgentId(agentId)
                .setSuccess(false)
                .setErrorMessage(errorMessage);
    }
}
