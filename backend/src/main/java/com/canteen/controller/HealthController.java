package com.canteen.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查接口：验证后端服务是否正常启动
 */
@Tag(name = "健康检查")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Operation(summary = "健康检查")
    @GetMapping
    public java.util.Map<String, Object> health() {
        Map<String, Object> info = new HashMap<>();
        info.put("status", "UP");
        info.put("project", "canteen-system");
        info.put("time", LocalDateTime.now().toString());
        return info;
    }
}
