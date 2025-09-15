package org.example.agenta.core;

import lombok.Data;
import lombok.experimental.Accessors;
import org.example.agenta.model.WorkflowContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow节点，表示DAG中的一个节点
 */
@Data
@Accessors(chain = true)
public class WorkflowNode {
    
    private String nodeId;
    private Agent agent;
    private List<String> dependencies;  // 依赖的节点ID列表
    private LogicalOperator operator;   // 依赖关系的逻辑操作符
    private boolean executed;           // 是否已执行
    private boolean canExecute;         // 是否可以执行
    
    public WorkflowNode(String nodeId, Agent agent) {
        this.nodeId = nodeId;
        this.agent = agent;
        this.dependencies = new ArrayList<>();
        this.operator = LogicalOperator.AND; // 默认为AND
        this.executed = false;
        this.canExecute = false;
    }
    
    /**
     * 添加依赖节点
     */
    public WorkflowNode addDependency(String dependencyNodeId) {
        this.dependencies.add(dependencyNodeId);
        return this;
    }
    
    /**
     * 检查节点是否可以执行
     */
    public boolean checkCanExecute(WorkflowContext context) {
        if (dependencies.isEmpty()) {
            this.canExecute = true;
            return true;
        }
        
        boolean result = evaluateLogicalExpression(context);
        this.canExecute = result;
        return result;
    }
    
    /**
     * 评估逻辑表达式
     */
    private boolean evaluateLogicalExpression(WorkflowContext context) {
        switch (operator) {
            case AND:
                return dependencies.stream()
                        .allMatch(context::isAgentCompleted);
            case OR:
                return dependencies.stream()
                        .anyMatch(context::isAgentCompleted);
            case NOT:
                // NOT操作符：所有依赖都未完成
                return dependencies.stream()
                        .noneMatch(context::isAgentCompleted);
            default:
                return false;
        }
    }
}
