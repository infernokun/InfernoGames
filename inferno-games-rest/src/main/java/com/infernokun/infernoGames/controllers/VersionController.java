package com.infernokun.infernoGames.controllers;

import com.infernokun.infernoGames.models.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/version")
public class VersionController extends BaseController {

    @Value("${app.version:unknown}")
    private String appVersion;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVersion() {
        Map<String, Object> versionInfo = new HashMap<>();
        versionInfo.put("version", appVersion);
        versionInfo.put("application", "Inferno Games REST API");
        versionInfo.put("timestamp", LocalDateTime.now().toString());
        versionInfo.put("java", System.getProperty("java.version"));
        
        return createSuccessResponse(versionInfo);
    }

    @GetMapping("/simple")
    public ResponseEntity<String> getSimpleVersion() {
        return ResponseEntity.ok(appVersion);
    }
}
