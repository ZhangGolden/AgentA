package org.example.agenta.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.agenta.model.ApiRequest;
import org.example.agenta.model.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * API调用服务
 */
@Service
@Slf4j
public class ApiCallService {
    
    @Autowired
    private WebClient webClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 异步调用API
     */
    public CompletableFuture<ApiResponse> callApiAsync(ApiRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                log.info("开始调用API: {} {}", request.getMethod(), request.getUrl());
                
                ApiResponse response = callApiWithRetry(request);
                response.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                
                log.info("API调用完成: {} - 状态码: {}, 耗时: {}ms", 
                        request.getUrl(), response.getStatusCode(), response.getExecutionTimeMs());
                
                return response;
                
            } catch (Exception e) {
                log.error("API调用异常: {}", request.getUrl(), e);
                return ApiResponse.error("API调用异常: " + e.getMessage())
                        .setExecutionTimeMs(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    /**
     * 带重试的API调用
     */
    private ApiResponse callApiWithRetry(ApiRequest request) {
        int maxAttempts = request.getRetryCount() + 1;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    log.info("API调用重试 {}/{}: {}", attempt, maxAttempts, request.getUrl());
                    Thread.sleep(1000 * attempt); // 递增延迟
                }
                
                return executeApiCall(request);
                
            } catch (Exception e) {
                lastException = e;
                log.warn("API调用失败 {}/{}: {}", attempt, maxAttempts, e.getMessage());
                
                if (attempt == maxAttempts) {
                    break;
                }
            }
        }
        
        // 所有重试都失败
        String errorMessage = "API调用失败，已重试" + request.getRetryCount() + "次: " + 
                             (lastException != null ? lastException.getMessage() : "未知错误");
        return ApiResponse.error(errorMessage);
    }
    
    /**
     * 执行具体的API调用
     */
    private ApiResponse executeApiCall(ApiRequest request) {
        try {
            WebClient.RequestBodySpec requestSpec = webClient
                    .method(HttpMethod.valueOf(request.getMethod().toUpperCase()))
                    .uri(request.getUrl())
                    .headers(headers -> {
                        request.getHeaders().forEach(headers::add);
                    });
            
            // 添加请求体（如果有）
            WebClient.ResponseSpec responseSpec;
            if (request.getBody() != null && 
                ("POST".equals(request.getMethod().toUpperCase()) || 
                 "PUT".equals(request.getMethod().toUpperCase()))) {
                
                String jsonBody = convertToJson(request.getBody());
                responseSpec = requestSpec.bodyValue(jsonBody).retrieve();
            } else {
                responseSpec = requestSpec.retrieve();
            }
            
            // 执行请求并处理响应
            Mono<String> responseMono = responseSpec
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(request.getTimeoutSeconds()));
            
            String responseBody = responseMono.block();
            
            return ApiResponse.success(200, responseBody);
            
        } catch (WebClientResponseException e) {
            log.error("API响应异常: 状态码={}, 响应体={}", e.getStatusCode(), e.getResponseBodyAsString());
            return ApiResponse.failure(e.getStatusCode().value(), 
                    "HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
            
        } catch (Exception e) {
            log.error("API调用执行异常", e);
            return ApiResponse.error("执行异常: " + e.getMessage());
        }
    }
    
    /**
     * 将对象转换为JSON字符串
     */
    private String convertToJson(Object obj) {
        try {
            if (obj instanceof String) {
                return (String) obj;
            }
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return obj.toString();
        }
    }
    
    /**
     * 同步调用API（简化版）
     */
    public ApiResponse callApi(ApiRequest request) {
        try {
            return callApiAsync(request).get();
        } catch (Exception e) {
            log.error("同步API调用失败", e);
            return ApiResponse.error("同步调用异常: " + e.getMessage());
        }
    }
}
