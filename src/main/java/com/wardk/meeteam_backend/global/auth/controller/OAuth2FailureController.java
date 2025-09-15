package com.wardk.meeteam_backend.global.auth.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth/oauth2")
public class OAuth2FailureController {

    @GetMapping("/failure")
    public ResponseEntity<Map<String, Object>> failure(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description) {

        log.error("=== OAuth2 로그인 실패 ===");
        log.error("error: {}", error);
        log.error("error_description: {}", error_description);
        log.error("========================");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OAuth2_LOGIN_FAILED");
        response.put("message", "소셜 로그인 실패");
        response.put("error", error != null ? error : "unknown");
        response.put("description", error_description != null ? error_description : "중복 데이터 문제 추정");

        return ResponseEntity.badRequest().body(response);
    }
}
