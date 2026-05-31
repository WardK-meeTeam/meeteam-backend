package com.wardk.meeteam_backend.global.config;

import com.wardk.meeteam_backend.global.auth.client.SejongPortalClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 Warm-up 수행
 *
 * <p>JVM JIT 컴파일 최적화 및 커넥션 풀 초기화를 위해
 * 애플리케이션이 완전히 시작된 후 warm-up을 수행합니다.</p>
 *
 * <h3>설정</h3>
 * <pre>
 * # application.yml
 * app:
 *   warmup:
 *     enabled: true  # false로 설정하면 warm-up 비활성화
 * </pre>
 *
 * <h3>Warm-up 대상</h3>
 * <ul>
 *   <li>세종대 포털 커넥션 풀 (TCP + SSL 핸드셰이크)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarmUpRunner implements ApplicationRunner {

    private final SejongPortalClient sejongPortalClient;

    @Value("${app.warmup.enabled:true}")
    private boolean warmupEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!warmupEnabled) {
            log.info("=== Warm-up 비활성화됨 (app.warmup.enabled=false) ===");
            return;
        }

        log.info("=== 애플리케이션 Warm-up 시작 ===");
        long startTime = System.currentTimeMillis();

        try {
            // 세종대 포털 커넥션 풀 워밍업
            sejongPortalClient.warmUp();
            log.info("세종대 포털 커넥션 풀 Warm-up 완료");
        } catch (Exception e) {
            log.warn("세종대 포털 Warm-up 실패 (서비스는 정상 동작): {}", e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("=== 애플리케이션 Warm-up 완료 ({}ms) ===", elapsed);
    }
}
