package org.example.agenta.agent;

import lombok.extern.slf4j.Slf4j;
import org.example.agenta.core.Agent;
import org.example.agenta.model.AgentResult;
import org.example.agenta.model.ApiRequest;
import org.example.agenta.model.ApiResponse;
import org.example.agenta.model.WorkflowContext;
import org.example.agenta.service.ApiCallService;
import org.example.agenta.service.SimpleApiCallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * API调用Agent - 具备调用外部API的能力
 * 可以根据上下文中的配置调用不同的API接口
 */
@Component
@Slf4j
public class ApiCallAgent implements Agent {
    
    private static final String AGENT_ID = "api-call-agent";
    
    @Autowired(required = false)
    private ApiCallService apiCallService;
    
    @Autowired
    private SimpleApiCallService simpleApiCallService;
    
    @Override
    public String getAgentId() {
        return AGENT_ID;
    }
    
    @Override
    public String getDescription() {
        return "API调用Agent - 具备调用外部API的能力，支持GET/POST/PUT/DELETE等HTTP方法";
    }
    
    @Override
    public AgentResult execute(WorkflowContext context) {
        try {
            log.info("ApiCallAgent 开始执行");
            
            // 从上下文获取API配置
            ApiRequest apiRequest = buildApiRequest(context);
            
            // 调用API - 优先使用WebFlux实现，如果不可用则使用简单实现
            CompletableFuture<ApiResponse> apiCallFuture;
            if (apiCallService != null) {
                log.debug("使用WebFlux HTTP客户端");
                apiCallFuture = apiCallService.callApiAsync(apiRequest);
            } else {
                log.debug("使用标准Java HTTP客户端");
                apiCallFuture = simpleApiCallService.callApiAsync(apiRequest);
            }
            ApiResponse apiResponse = apiCallFuture.get(); // 等待API调用完成
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("apiRequest", Map.of(
                    "url", apiRequest.getUrl(),
                    "method", apiRequest.getMethod(),
                    "headers", apiRequest.getHeaders()
            ));
            result.put("apiResponse", Map.of(
                    "statusCode", apiResponse.getStatusCode(),
                    "success", apiResponse.isSuccess(),
                    "body", apiResponse.getBody(),
                    "executionTimeMs", apiResponse.getExecutionTimeMs()
            ));
            result.put("callSuccess", apiResponse.isSuccess());
            
            if (!apiResponse.isSuccess()) {
                result.put("errorMessage", apiResponse.getErrorMessage());
                log.warn("API调用失败: {}", apiResponse.getErrorMessage());
            }
            
            log.info("ApiCallAgent 执行完成，API调用成功: {}", apiResponse.isSuccess());
            return AgentResult.success(AGENT_ID, result);
            
        } catch (Exception e) {
            log.error("ApiCallAgent 执行失败", e);
            return AgentResult.failure(AGENT_ID, "API调用异常: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canExecute(WorkflowContext context) {
        // 检查是否有API配置
        Object apiConfig = context.getData("apiConfig");
        return apiConfig != null;
    }
    
    /**
     * 从上下文构建API请求
     */
    private ApiRequest buildApiRequest(WorkflowContext context) {
        // 从上下文获取API配置
        Object apiConfigObj = context.getData("apiConfig");
        
        if (apiConfigObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> apiConfig = (Map<String, Object>) apiConfigObj;
            
            String url = (String) apiConfig.get("url");
            String method = (String) apiConfig.getOrDefault("method", "GET");
            
            ApiRequest apiRequest = new ApiRequest(url).setMethod(method);
            
            // 设置请求头
            if (apiConfig.containsKey("headers") && apiConfig.get("headers") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> headers = (Map<String, String>) apiConfig.get("headers");
                headers.forEach(apiRequest::addHeader);
            }
            
            // 设置请求体
            if (apiConfig.containsKey("body")) {
                apiRequest.setBody(apiConfig.get("body"));
            }
            
            // 设置超时时间
            if (apiConfig.containsKey("timeoutSeconds")) {
                apiRequest.setTimeoutSeconds((Integer) apiConfig.get("timeoutSeconds"));
            }
            
            // 设置重试次数
            if (apiConfig.containsKey("retryCount")) {
                apiRequest.setRetryCount((Integer) apiConfig.get("retryCount"));
            }
            
            return apiRequest;
        }
        
        // 默认配置：调用一个示例API
        return createDefaultApiRequest(context);
    }
    
    /**
     * 创建默认的API请求（示例）
     */
    private ApiRequest createDefaultApiRequest(WorkflowContext context) {
        // 使用JSONPlaceholder作为示例API
        String url = "https://jsonplaceholder.typicode.com/posts/1";
        
        // 如果上下文中有输入数据，可以作为查询参数或请求体
        Object inputData = context.getData("input");
        if (inputData != null) {
            // 对于POST请求，可以将输入数据作为请求体
            url = "https://jsonplaceholder.typicode.com/posts";
            return ApiRequest.post(url)
                    .setBody(Map.of(
                            "title", "来自AgentA的请求",
                            "body", inputData.toString(),
                            "userId", 1
                    ));
        }
        
        return ApiRequest.get(url);
    }
    
    /**
     * 创建调用天气API的示例
     */
    public static Map<String, Object> createWeatherApiConfig(String city) {
        Map<String, Object> config = new HashMap<>();
        config.put("url", "https://api.openweathermap.org/data/2.5/weather");
        config.put("method", "GET");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        config.put("headers", headers);
        
        // 注意：实际使用时需要替换为真实的API Key
        config.put("queryParams", Map.of(
                "q", city,
                "appid", "your_api_key_here",
                "units", "metric"
        ));
        
        return config;
    }
    
    /**
     * 创建调用LLM API的示例配置
     */
    public static Map<String, Object> createLLMApiConfig(String prompt) {
        Map<String, Object> config = new HashMap<>();
        config.put("url", "https://api.openai.com/v1/chat/completions");
        config.put("method", "POST");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer your_openai_api_key_here");
        config.put("headers", headers);
        
        config.put("body", Map.of(
                "model", "gpt-3.5-turbo",
                "messages", new Object[]{
                        Map.of("role", "user", "content", prompt)
                }
        ));
        
        config.put("timeoutSeconds", 60);
        config.put("retryCount", 2);
        
        return config;
    }
}
