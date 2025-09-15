package org.example.agenta.core;

import org.example.agenta.model.AgentResult;
import org.example.agenta.model.WorkflowContext;

/**
 * Agent接口，定义Agent的基本行为
 */
public interface Agent {
    
    /**
     * 获取Agent ID
     */
    String getAgentId();
    
    /**
     * 获取Agent描述
     */
    String getDescription();
    
    /**
     * 执行Agent任务
     * @param context 工作流上下文
     * @return Agent执行结果
     */
    AgentResult execute(WorkflowContext context);
    
    /**
     * 检查Agent是否可以执行（前置条件检查）
     */
    boolean canExecute(WorkflowContext context);
}
