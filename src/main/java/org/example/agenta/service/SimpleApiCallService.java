package org.example.agenta.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.agenta.model.ApiRequest;
import org.example.agenta.model.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * 简化版API调用服务 - 使用Java 11+ 标准HTTP客户端
 * 作为WebFlux不可用时的备选方案
 */
@Service("simpleApiCallService")
@Slf4j
public class SimpleApiCallService {
    
    private final HttpClient httpClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public SimpleApiCallService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
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
    private ApiResponse executeApiCall(ApiRequest request) throws IOException, InterruptedException {
        try {
            // 构建HTTP请求
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(request.getUrl()))
                    .timeout(Duration.ofSeconds(request.getTimeoutSeconds()));
            
            // 添加请求头
            request.getHeaders().forEach(requestBuilder::header);
            
            // 设置HTTP方法和请求体
            switch (request.getMethod().toUpperCase()) {
                case "GET":
                    requestBuilder.GET();
                    break;
                case "POST":
                    String postBody = convertToJson(request.getBody());
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(postBody));
                    break;
                case "PUT":
                    String putBody = convertToJson(request.getBody());
                    requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(putBody));
                    break;
                case "DELETE":
                    requestBuilder.DELETE();
                    break;
                default:
                    throw new IllegalArgumentException("不支持的HTTP方法: " + request.getMethod());
            }
            
            HttpRequest httpRequest = requestBuilder.build();
            
            // 执行请求
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            // 构建响应
            boolean isSuccess = response.statusCode() >= 200 && response.statusCode() < 300;
            
            if (isSuccess) {
                return ApiResponse.success(response.statusCode(), response.body());
            } else {
                return ApiResponse.failure(response.statusCode(), 
                        "HTTP " + response.statusCode() + ": " + response.body());
            }
            
        } catch (IOException | InterruptedException e) {
            log.error("API调用执行异常", e);
            return ApiResponse.error("执行异常: " + e.getMessage());
        }
    }
    
    /**
     * 将对象转换为JSON字符串
     */
    private String convertToJson(Object obj) {
        if (obj == null) {
            return "";
        }
        
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
     * 同步调用API
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
