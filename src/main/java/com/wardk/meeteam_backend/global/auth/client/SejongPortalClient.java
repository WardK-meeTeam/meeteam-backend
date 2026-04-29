package com.wardk.meeteam_backend.global.auth.client;

import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 세종대학교 포털 로그인 클라이언트.
 * <p>
 * Apache HttpClient 5를 사용하여 커넥션 풀을 관리합니다.
 * - 최대 커넥션: 5개 (DDoS 방지)
 * - Idle 커넥션: 120초 후 제거 (세종대 Keep-Alive 300초 대비 안전 마진)
 */
@Slf4j
@Component
public class SejongPortalClient {

    private static final String LOGIN_URL = "https://portal.sejong.ac.kr/jsp/login/login_action.jsp";

    private static final int MAX_CONNECTIONS = 5;
    private static final int IDLE_TIMEOUT_SECONDS = 120;
    private static final int CONNECTION_TTL_SECONDS = 300;  // 커넥션 최대 수명 (서버 max 요청 제한 대비)
    private static final int CONNECTION_TIMEOUT_SECONDS = 30;
    private static final int RESPONSE_TIMEOUT_SECONDS = 30;

    private final CloseableHttpClient httpClient;
    private final PoolingHttpClientConnectionManager connectionManager;

    public SejongPortalClient() {
        this.connectionManager = createConnectionManager();
        this.httpClient = createHttpClient();
        log.info("SejongPortalClient 초기화 완료 - maxConnections: {}, idleTimeout: {}초, TTL: {}초",
                MAX_CONNECTIONS, IDLE_TIMEOUT_SECONDS, CONNECTION_TTL_SECONDS);
    }

    /**
     * 애플리케이션 시작 시 커넥션 워밍업.
     * TCP + SSL 핸드셰이크를 미리 수행하여 첫 요청 지연을 방지합니다.
     * 병렬 실행으로 여러 커넥션을 동시에 생성합니다.
     */
    // @PostConstruct
    public void warmUp() {
        // 워밍업 임시 비활성화
        /*
        int warmUpCount = 2;
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < warmUpCount; i++) {
            final int index = i + 1;
            Thread t = new Thread(() -> {
                try {
                    HttpGet warmUpRequest = new HttpGet("https://portal.sejong.ac.kr");
                    httpClient.execute(warmUpRequest, response -> {
                        EntityUtils.consume(response.getEntity());
                        return null;
                    });
                    log.info("커넥션 워밍업 ({}/{}) 완료", index, warmUpCount);
                } catch (Exception e) {
                    log.warn("커넥션 워밍업 ({}/{}) 실패: {}", index, warmUpCount, e.getMessage());
                }
            });
            threads.add(t);
            t.start();
        }

        // 모든 워밍업 완료 대기
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("SejongPortalClient 워밍업 완료 - available: {}",
                connectionManager.getTotalStats().getAvailable());
        */
    }

    /**
     * 세종대 포털 로그인을 시도합니다.
     *
     * @param studentId 학번
     * @param password  비밀번호
     * @return 로그인 성공 여부
     * @throws CustomException 로그인 실패 시
     */
    public boolean authenticate(String studentId, String password) {
        try {
            String response = performLogin(studentId, password);
            return validateLoginResponse(response);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("세종대 포털 로그인 중 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.SEJONG_PORTAL_ERROR);
        }
    }

    /**
     * 세종대 포털에 로그인 POST 요청을 보냅니다.
     */
    private String performLogin(String studentId, String password) throws Exception {
        // 요청 전 커넥션 풀 상태
        log.info("[요청 전] 커넥션 풀 - leased: {}, available: {}, pending: {}",
                connectionManager.getTotalStats().getLeased(),
                connectionManager.getTotalStats().getAvailable(),
                connectionManager.getTotalStats().getPending());

        String formData = "mainLogin=" + URLEncoder.encode("N", StandardCharsets.UTF_8)
                + "&id=" + URLEncoder.encode(studentId, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

        HttpPost httpPost = new HttpPost(LOGIN_URL);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.setHeader("User-Agent", "Mozilla/5.0");
        httpPost.setHeader("Referer", "https://portal.sejong.ac.kr");
        httpPost.setEntity(new StringEntity(formData, ContentType.APPLICATION_FORM_URLENCODED));

        String result = httpClient.execute(httpPost, response -> {
            int statusCode = response.getCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            log.info("세종대 포털 응답 코드: {}", statusCode);
            if (body != null) {
                log.info("세종대 포털 응답 길이: {}, 내용 미리보기: {}",
                        body.length(),
                        body.substring(0, Math.min(200, body.length())));
            }
            return body;
        });

        // 요청 후 커넥션 풀 상태
        log.info("[요청 후] 커넥션 풀 - leased: {}, available: {}, pending: {}",
                connectionManager.getTotalStats().getLeased(),
                connectionManager.getTotalStats().getAvailable(),
                connectionManager.getTotalStats().getPending());

        return result;
    }

    /**
     * 로그인 응답을 검증합니다.
     */
    private boolean validateLoginResponse(String response) {
        if (response == null) {
            throw new CustomException(ErrorCode.SEJONG_PORTAL_ERROR);
        }

        if (response.contains("var result = 'OK'")) {
            log.info("세종대 포털 로그인 성공");
            return true;
        }

        if (response.contains("erridpwd")) {
            log.warn("세종대 포털 로그인 실패: 아이디 또는 비밀번호 불일치");
            throw new CustomException(ErrorCode.SEJONG_LOGIN_FAILED);
        }

        if (response.contains("pwdNeedChg")) {
            log.warn("세종대 포털 로그인 실패: 비밀번호 재설정 필요");
            throw new CustomException(ErrorCode.SEJONG_PASSWORD_RESET_REQUIRED);
        }

        if (response.contains("noaccess")) {
            log.warn("세종대 포털 로그인 실패: 접근 차단");
            throw new CustomException(ErrorCode.SEJONG_PORTAL_ERROR);
        }

        log.warn("세종대 포털 로그인 실패: 알 수 없는 응답 - {}",
                response.substring(0, Math.min(500, response.length())));
        throw new CustomException(ErrorCode.SEJONG_LOGIN_FAILED);
    }

    /**
     * 커넥션 풀 매니저를 생성합니다.
     */
    private PoolingHttpClientConnectionManager createConnectionManager() {
        try {
            return PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                            .setSslContext(SSLContextBuilder.create()
                                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                                    .build())
                            .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                            .build())
                    .setMaxConnTotal(MAX_CONNECTIONS)
                    .setMaxConnPerRoute(MAX_CONNECTIONS)
                    .setDefaultConnectionConfig(ConnectionConfig.custom()
                            .setConnectTimeout(Timeout.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                            .setSocketTimeout(Timeout.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
                            .setTimeToLive(TimeValue.ofSeconds(CONNECTION_TTL_SECONDS))
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("ConnectionManager 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("ConnectionManager 생성 실패", e);
        }
    }

    /**
     * HttpClient를 생성합니다.
     */
    private CloseableHttpClient createHttpClient() {
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                        .setResponseTimeout(Timeout.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
                        .build())
                .evictIdleConnections(TimeValue.of(IDLE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .evictExpiredConnections()
                .build();
    }

    /**
     * 애플리케이션 종료 시 리소스 정리.
     */
    @PreDestroy
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
                log.info("SejongPortalClient HttpClient 종료");
            }
            if (connectionManager != null) {
                connectionManager.close();
                log.info("SejongPortalClient ConnectionManager 종료");
            }
        } catch (Exception e) {
            log.error("SejongPortalClient 종료 중 오류: {}", e.getMessage(), e);
        }
    }
}