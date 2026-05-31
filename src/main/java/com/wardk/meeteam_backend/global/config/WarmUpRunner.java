package com.wardk.meeteam_backend.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 애플리케이션 시작 시 Warm-up 수행.
 *
 * <p>실제 로그인 API를 호출하여 전체 요청 플로우를 워밍업합니다:</p>
 * <ul>
 *   <li>Spring Security 필터 체인</li>
 *   <li>Controller, Service 클래스 로딩</li>
 *   <li>세종대 포털 커넥션 풀 (TCP + SSL)</li>
 *   <li>DB 커넥션 풀 (HikariCP)</li>
 * </ul>
 */
@Slf4j
@Component
public class WarmUpRunner implements ApplicationRunner {

    @Value("${app.warmup.enabled:true}")
    private boolean warmupEnabled;

    @Value("${server.port:8080}")
    private int serverPort;

    @Override
    public void run(ApplicationArguments args) {
        if (!warmupEnabled) {
            log.info("=== Warm-up 비활성화됨 (app.warmup.enabled=false) ===");
            return;
        }

        log.info("=== 애플리케이션 Warm-up 시작 ===");
        long startTime = System.currentTimeMillis();

        performWarmupCall();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("=== 애플리케이션 Warm-up 완료 ({}ms) ===", elapsed);
    }

    private void performWarmupCall() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:" + serverPort + "/api/v1/auth/login/sejong";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestJson = "{\"studentId\": \"_warmup_\", \"password\": \"_warmup_\"}";

        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            log.info("Warm-up 요청 성공");
        } catch (Exception e) {
            // 인증 실패해도 워밍업은 완료됨
            log.info("Warm-up 요청 완료 (클래스 로딩 완료)");
        }
    }
}