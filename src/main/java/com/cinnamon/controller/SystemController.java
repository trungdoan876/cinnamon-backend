package com.cinnamon.controller;

import com.cinnamon.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/version")
public class SystemController {

    private final Environment environment;
    private final String appName;

    public SystemController(
            Environment environment,
            @Value("${spring.application.name:cinnamon-be}") String appName) {
        this.environment = environment;
        this.appName = appName;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVersionInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("app", appName);
        info.put("version", "1.0.0");
        info.put("status", "UP");
        info.put("timestamp", Instant.now().toString());
        info.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        return ResponseEntity.ok(ApiResponse.success("Thông tin phiên bản hệ thống", info));
    }
}
