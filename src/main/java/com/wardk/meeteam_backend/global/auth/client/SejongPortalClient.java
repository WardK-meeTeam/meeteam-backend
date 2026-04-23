package com.wardk.meeteam_backend.global.auth.client;

import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * 세종대학교 포털 로그인 클라이언트
 * <p>
 * 세종대 포털(portal.sejong.ac.kr)에 HTTP 요청을 보내 학생 인증을 수행합니다.
 * Java 11+ HttpClient 사용 (Docker 환경 TLS 호환성 개선)
 */
@Slf4j
@Component
public class SejongPortalClient {

    private static final String LOGIN_URL = "https://portal.sejong.ac.kr/jsp/login/login_action.jsp";

    private final HttpClient httpClient;

    public SejongPortalClient() {
        this.httpClient = buildClient();
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
        String formData = "mainLogin=" + URLEncoder.encode("N", StandardCharsets.UTF_8)
                + "&id=" + URLEncoder.encode(studentId, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://portal.sejong.ac.kr")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("세종대 포털 응답 코드: {}", response.statusCode());
        String body = response.body();
        if (body != null) {
            log.info("세종대 포털 응답 길이: {}, 내용 미리보기: {}",
                    body.length(),
                    body.substring(0, Math.min(200, body.length())));
        }
        return body;
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

        log.warn("세종대 포털 로그인 실패: 알 수 없는 응답 - {}", response.substring(0, Math.min(500, response.length())));
        throw new CustomException(ErrorCode.SEJONG_LOGIN_FAILED);
    }

    /**
     * SSL 검증을 우회하는 HttpClient를 생성합니다.
     */
    private HttpClient buildClient() {
        try {
            X509TrustManager trustAllManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllManager}, new java.security.SecureRandom());

            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(30))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        } catch (Exception e) {
            log.error("HttpClient 생성 실패: {}", e.getMessage(), e);
            return HttpClient.newHttpClient();
        }
    }
}
