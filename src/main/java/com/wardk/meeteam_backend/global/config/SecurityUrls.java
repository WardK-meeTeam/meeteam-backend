package com.wardk.meeteam_backend.global.config;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Component
public class SecurityUrls {
    // 인증이 필요 없는 화이트리스트 경로들
    public static final List<String> WHITELIST = List.of(
            "/webjars/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/**",
            "/docs/**",
            "/default-ui.css",
            "/favicon.ico",
            "/api/login",
            "/api/auth/**",
            "/",
            "/uploads/**",
            "/api/register",
            "/api/project/register",
            "/oauth2/**",
            "/login/oauth2/**",
            "/login/oauth2/code/**",
            "/api/auth/oauth2/success",
            "/api/auth/oauth2/failure",
            "/api/webhooks/github",
            "/api/v1/files/**"  // 파일 업로드 API 경로 추가
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 주어진 URI 가 화이트리스트에 포함되는지 확인
    public boolean isWhitelisted(String uri) {
        return WHITELIST.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}