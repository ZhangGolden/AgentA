# AgentA API调用功能使用示例

## 概述

AgentA系统现已支持API调用功能，通过`ApiCallAgent`可以在工作流中集成外部API调用。

## 新增功能

### 1. ApiCallAgent
- 支持HTTP GET/POST/PUT/DELETE方法
- 支持自定义请求头和请求体
- 支持超时设置和重试机制
- 支持异步调用

### 2. 新的工作流类型
- **API工作流**: DataProcessor → ApiCall → ReportGenerator
- **并行API工作流**: DataProcessor → [ApiCall + Validation] → ReportGenerator

## REST API端点

### 1. 执行API调用工作流
```bash
curl -X POST http://localhost:8080/api/workflow/execute/api \
  -H "Content-Type: application/json" \
  -d '{
    "input": "需要处理的数据",
    "apiConfig": {
      "url": "https://jsonplaceholder.typicode.com/posts",
      "method": "POST",
      "headers": {
        "Content-Type": "application/json",
        "Authorization": "Bearer your-token"
      },
      "body": {
        "title": "测试标题",
        "body": "测试内容",
        "userId": 1
      },
      "timeoutSeconds": 30,
      "retryCount": 2
    }
  }'
```

### 2. 执行并行API调用工作流
```bash
curl -X POST http://localhost:8080/api/workflow/execute/parallel-api \
  -H "Content-Type: application/json" \
  -d '{
    "input": "需要处理的数据",
    "apiConfig": {
      "url": "https://api.example.com/data",
      "method": "GET",
      "headers": {
        "Accept": "application/json"
      },
      "timeoutSeconds": 15
    }
  }'
```

## API配置参数

### 必需参数
- `url`: API端点URL

### 可选参数
- `method`: HTTP方法（默认: GET）
- `headers`: 自定义请求头（Map<String, String>）
- `body`: 请求体（用于POST/PUT请求）
- `timeoutSeconds`: 超时时间（默认: 30秒）
- `retryCount`: 重试次数（默认: 0）

## 工作流示例

### 1. 简单API调用工作流
```
DataProcessorAgent → ApiCallAgent → ReportGeneratorAgent
```

### 2. 并行处理工作流
```
DataProcessorAgent
       ↓
   ┌─────────┬─────────┐
   ↓         ↓         ↓
ApiCallAgent ValidationAgent
   ↓         ↓         ↓
   └─────────┴─────────┘
           ↓
  ReportGeneratorAgent
```

## 预定义API配置示例

### 天气API调用
```java
Map<String, Object> weatherConfig = Map.of(
    "url", "https://api.openweathermap.org/data/2.5/weather",
    "method", "GET",
    "headers", Map.of("Accept", "application/json"),
    "queryParams", Map.of(
        "q", "Beijing",
        "appid", "your_api_key",
        "units", "metric"
    )
);
```

### LLM API调用
```java
Map<String, Object> llmConfig = Map.of(
    "url", "https://api.openai.com/v1/chat/completions",
    "method", "POST",
    "headers", Map.of(
        "Content-Type", "application/json",
        "Authorization", "Bearer your_openai_api_key"
    ),
    "body", Map.of(
        "model", "gpt-3.5-turbo",
        "messages", new Object[]{
            Map.of("role", "user", "content", "你好，请帮我分析这些数据")
        }
    ),
    "timeoutSeconds", 60,
    "retryCount", 2
);
```

## 错误处理

ApiCallAgent具备完善的错误处理机制：

1. **网络异常**: 自动重试指定次数
2. **超时处理**: 支持自定义超时时间
3. **HTTP错误**: 返回详细的状态码和错误信息
4. **序列化错误**: 自动处理JSON序列化问题

## 响应格式

API调用成功后，结果包含：
```json
{
  "apiRequest": {
    "url": "https://api.example.com/data",
    "method": "POST",
    "headers": {...}
  },
  "apiResponse": {
    "statusCode": 200,
    "success": true,
    "body": "响应内容",
    "executionTimeMs": 1250
  },
  "callSuccess": true
}
```

## 最佳实践

1. **设置合理的超时时间**: 根据API响应时间设置合适的超时值
2. **使用重试机制**: 对于可能不稳定的API，设置适当的重试次数
3. **错误处理**: 在工作流中处理API调用失败的情况
4. **安全性**: 不要在配置中硬编码API密钥，使用环境变量或配置文件
5. **监控**: 监控API调用的成功率和响应时间

## 系统信息

查看系统支持的所有功能：
```bash
curl http://localhost:8080/api/workflow/info
```

响应示例：
```json
{
  "title": "AgentA Workflow System",
  "description": "基于DAG的Agent编排系统",
  "version": "1.0.0",
  "features": {
    "logicalOperators": ["AND", "OR", "NOT"],
    "agents": ["DataProcessorAgent", "ValidationAgent", "ReportGeneratorAgent", "ApiCallAgent"],
    "executionMode": "Parallel with dependency management",
    "apiSupport": "HTTP GET/POST/PUT/DELETE with retry and timeout"
  },
  "endpoints": {
    "sampleWorkflow": "/api/workflow/execute/sample",
    "complexWorkflow": "/api/workflow/execute/complex",
    "apiWorkflow": "/api/workflow/execute/api",
    "parallelApiWorkflow": "/api/workflow/execute/parallel-api"
  }
}
```
