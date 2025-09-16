package org.example.agenta.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API响应模型
 */
@Data
@Accessors(chain = true)
public class ApiResponse {
    
    private int statusCode;
    private boolean success;
    private Object body;
    private Map<String, String> headers;
    private String errorMessage;
    private long executionTimeMs;
    private LocalDateTime responseTime;
    
    public ApiResponse() {
        this.responseTime = LocalDateTime.now();
    }
    
    /**
     * 创建成功响应
     */
    public static ApiResponse success(int statusCode, Object body) {
        return new ApiResponse()
                .setStatusCode(statusCode)
                .setSuccess(true)
                .setBody(body);
    }
    
    /**
     * 创建失败响应
     */
    public static ApiResponse failure(int statusCode, String errorMessage) {
        return new ApiResponse()
                .setStatusCode(statusCode)
                .setSuccess(false)
                .setErrorMessage(errorMessage);
    }
    
    /**
     * 创建异常响应
     */
    public static ApiResponse error(String errorMessage) {
        return new ApiResponse()
                .setStatusCode(0)
                .setSuccess(false)
                .setErrorMessage(errorMessage);
    }
}
