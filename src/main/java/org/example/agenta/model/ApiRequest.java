package org.example.agenta.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * API请求模型
 */
@Data
@Accessors(chain = true)
public class ApiRequest {
    
    private String url;
    private String method = "GET";
    private Map<String, String> headers;
    private Object body;
    private int timeoutSeconds = 30;
    private int retryCount = 0;
    
    public ApiRequest() {
        this.headers = new HashMap<>();
    }
    
    public ApiRequest(String url) {
        this();
        this.url = url;
    }
    
    /**
     * 添加请求头
     */
    public ApiRequest addHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }
    
    /**
     * 创建GET请求
     */
    public static ApiRequest get(String url) {
        return new ApiRequest(url).setMethod("GET");
    }
    
    /**
     * 创建POST请求
     */
    public static ApiRequest post(String url) {
        return new ApiRequest(url).setMethod("POST")
                .addHeader("Content-Type", "application/json");
    }
    
    /**
     * 创建PUT请求
     */
    public static ApiRequest put(String url) {
        return new ApiRequest(url).setMethod("PUT")
                .addHeader("Content-Type", "application/json");
    }
    
    /**
     * 创建DELETE请求
     */
    public static ApiRequest delete(String url) {
        return new ApiRequest(url).setMethod("DELETE");
    }
}
