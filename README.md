# AgentA - Workflow托管管理系统

基于Java和Spring Boot的Agent编排系统，支持DAG工作流和逻辑表达式。

## 系统架构

### 核心组件

1. **WorkflowDAG** - 工作流有向无环图管理器
2. **Agent接口** - 统一的Agent行为定义
3. **WorkflowNode** - DAG节点，包含Agent和依赖关系
4. **LogicalOperator** - 逻辑操作符（AND、OR、NOT）
5. **WorkflowContext** - 工作流执行上下文

### 已实现的Agent

1. **DataProcessorAgent** - 数据处理Agent
   - 负责数据清洗和转换
   - 模拟LLM调用进行数据处理

2. **ValidationAgent** - 验证Agent
   - 负责数据验证和完整性检查
   - 提供验证评分和报告

3. **ReportGeneratorAgent** - 报告生成Agent
   - 依赖前两个Agent完成后执行
   - 生成综合报告

## 使用方式

### 1. 启动应用

```bash
./mvnw spring-boot:run
```

应用启动后会自动运行演示，展示AND和OR逻辑的工作流执行。

### 2. REST API调用

#### 执行示例工作流（AND逻辑）

```bash
curl -X POST http://localhost:8080/api/workflow/execute/sample \
  -H "Content-Type: application/json" \
  -d '{"input": "您的输入数据"}'
```

#### 执行复杂工作流（OR逻辑）

```bash
curl -X POST http://localhost:8080/api/workflow/execute/complex \
  -H "Content-Type: application/json" \
  -d '{"input": "您的输入数据"}'
```

#### 获取系统信息

```bash
curl http://localhost:8080/api/workflow/info
```

#### 健康检查

```bash
curl http://localhost:8080/api/workflow/health
```

### 3. 工作流设计示例

#### AND逻辑工作流
```
Agent1(DataProcessor) ─┐
                       ├─ AND ─> Agent3(ReportGenerator)
Agent2(Validation)   ─┘
```

#### OR逻辑工作流
```
Agent1(DataProcessor) ─┐
                       ├─ OR ─> Agent3(ReportGenerator)  
Agent2(Validation)   ─┘
```

## 核心特性

### 1. DAG支持
- 支持复杂的依赖关系定义
- 自动检测和避免循环依赖
- 并行执行独立的节点

### 2. 逻辑表达式
- **AND**: 所有依赖Agent必须完成
- **OR**: 任一依赖Agent完成即可
- **NOT**: 所有依赖Agent都未完成（可扩展）

### 3. 上下文管理
- Agent间数据传递
- 执行状态跟踪
- 结果聚合

### 4. 并发执行
- 基于CompletableFuture的异步执行
- 线程池管理
- 错误处理和恢复

## 扩展指南

### 添加新的Agent

1. 实现`Agent`接口：

```java
@Component
public class MyCustomAgent implements Agent {
    @Override
    public String getAgentId() {
        return "my-custom-agent";
    }
    
    @Override
    public AgentResult execute(WorkflowContext context) {
        // 实现Agent逻辑
        return AgentResult.success(getAgentId(), result);
    }
    
    @Override
    public boolean canExecute(WorkflowContext context) {
        // 检查前置条件
        return true;
    }
}
```

2. 在工作流中使用：

```java
WorkflowNode node = new WorkflowNode("node-id", myCustomAgent)
    .addDependency("previous-agent-id")
    .setOperator(LogicalOperator.AND);
```

### 集成LangChain4j

项目已包含LangChain4j依赖，可以在Agent中集成真实的LLM调用：

```java
// 在Agent中添加LLM调用
@Autowired
private ChatLanguageModel chatModel;

public AgentResult execute(WorkflowContext context) {
    String prompt = "处理数据: " + context.getData("input");
    String response = chatModel.generate(prompt);
    
    return AgentResult.success(getAgentId(), response);
}
```

## 测试

运行集成测试：

```bash
./mvnw test
```

## 技术栈

- Java 17
- Spring Boot 3.5.5
- LangChain4j 0.34.0
- Lombok
- JUnit 5

## 日志

系统提供详细的执行日志，包括：
- 工作流创建和执行状态
- Agent执行时间和结果
- 错误信息和堆栈跟踪

查看日志以了解系统运行状态和调试问题。
