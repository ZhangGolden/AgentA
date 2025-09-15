package org.example.agenta.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.agenta.core.WorkflowDAG;
import org.example.agenta.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Workflow REST控制器
 */
@RestController
@RequestMapping("/api/workflow")
@Slf4j
public class WorkflowController {
    
    @Autowired
    private WorkflowService workflowService;
    
    /**
     * 执行示例工作流 (AND逻辑)
     */
    @PostMapping("/execute/sample")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeSampleWorkflow(
            @RequestBody Map<String, Object> request) {
        
        log.info("收到示例工作流执行请求: {}", request);
        
        Object inputData = request.getOrDefault("input", "默认输入数据");
        
        WorkflowDAG workflow = workflowService.createSampleWorkflow();
        
        return workflowService.executeWorkflow(workflow, inputData)
                .thenApply(result -> {
                    workflow.shutdown(); // 清理资源
                    return ResponseEntity.ok(result);
                })
                .exceptionally(throwable -> {
                    workflow.shutdown(); // 清理资源
                    log.error("示例工作流执行失败", throwable);
                    return ResponseEntity.internalServerError()
                            .body(Map.of("error", throwable.getMessage()));
                });
    }
    
    /**
     * 执行复杂工作流 (OR逻辑)
     */
    @PostMapping("/execute/complex")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeComplexWorkflow(
            @RequestBody Map<String, Object> request) {
        
        log.info("收到复杂工作流执行请求: {}", request);
        
        Object inputData = request.getOrDefault("input", "默认输入数据");
        
        WorkflowDAG workflow = workflowService.createComplexWorkflow();
        
        return workflowService.executeWorkflow(workflow, inputData)
                .thenApply(result -> {
                    workflow.shutdown(); // 清理资源
                    return ResponseEntity.ok(result);
                })
                .exceptionally(throwable -> {
                    workflow.shutdown(); // 清理资源
                    log.error("复杂工作流执行失败", throwable);
                    return ResponseEntity.internalServerError()
                            .body(Map.of("error", throwable.getMessage()));
                });
    }
    
    /**
     * 获取工作流信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getWorkflowInfo() {
        Map<String, Object> info = Map.of(
                "title", "AgentA Workflow System",
                "description", "基于DAG的Agent编排系统",
                "version", "1.0.0",
                "features", Map.of(
                        "logicalOperators", new String[]{"AND", "OR", "NOT"},
                        "agents", new String[]{"DataProcessorAgent", "ValidationAgent", "ReportGeneratorAgent"},
                        "executionMode", "Parallel with dependency management"
                ),
                "endpoints", Map.of(
                        "sampleWorkflow", "/api/workflow/execute/sample",
                        "complexWorkflow", "/api/workflow/execute/complex"
                )
        );
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "Workflow system is running"
        ));
    }
}
