package org.example.agenta.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Workflow执行上下文，用于在Agent之间传递数据
 */
@Data
@Accessors(chain = true)
public class WorkflowContext {
    
    private String workflowId;
    private Map<String, Object> data;
    private Map<String, AgentResult> agentResults;
    private String currentStep;
    
    public WorkflowContext() {
        this.workflowId = UUID.randomUUID().toString();
        this.data = new HashMap<>();
        this.agentResults = new HashMap<>();
    }
    
    public WorkflowContext(String workflowId) {
        this.workflowId = workflowId;
        this.data = new HashMap<>();
        this.agentResults = new HashMap<>();
    }
    
    /**
     * 添加数据到上下文
     */
    public WorkflowContext addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
    
    /**
     * 获取数据
     */
    public Object getData(String key) {
        return this.data.get(key);
    }
    
    /**
     * 添加Agent执行结果
     */
    public WorkflowContext addAgentResult(String agentId, AgentResult result) {
        this.agentResults.put(agentId, result);
        return this;
    }
    
    /**
     * 获取Agent执行结果
     */
    public AgentResult getAgentResult(String agentId) {
        return this.agentResults.get(agentId);
    }
    
    /**
     * 检查Agent是否已完成
     */
    public boolean isAgentCompleted(String agentId) {
        AgentResult result = this.agentResults.get(agentId);
        return result != null && result.isSuccess();
    }
}
