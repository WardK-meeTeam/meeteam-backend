package com.wardk.meeteam_backend.global.auth.client;

import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 세종대학교 포털 로그인 클라이언트
 * <p>
 * 세종대 포털(portal.sejong.ac.kr)에 HTTP 요청을 보내 학생 인증을 수행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SejongPortalClient {

    private static final String PORTAL_BASE_URL = "https://portal.sejong.ac.kr";
    private static final String LOGIN_PAGE_PATH = "/jsp/login/loginSSL.jsp";
    private static final String LOGIN_ACTION_PATH = "/jsp/login/login_action.jsp";

    private final WebClient webClient;

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
            // Step 1: 로그인 페이지 접근하여 세션 쿠키 획득
            String sessionCookie = getSessionCookie();

            // Step 2: 로그인 POST 요청
            String response = performLogin(studentId, password, sessionCookie);

            // Step 3: 로그인 결과 확인
            return validateLoginResponse(response);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("세종대 포털 로그인 중 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.SEJONG_PORTAL_ERROR);
        }
    }

    /**
     * 로그인 페이지에 접근하여 세션 쿠키를 획득합니다.
     */
    private String getSessionCookie() {
        return webClient.get()
                .uri(PORTAL_BASE_URL + LOGIN_PAGE_PATH)
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .exchangeToMono(response -> {
                    List<String> cookies = response.headers().header(HttpHeaders.SET_COOKIE);
                    String cookieHeader = cookies.stream()
                            .map(c -> c.split(";")[0])
                            .reduce((a, b) -> a + "; " + b)
                            .orElse("");
                    return Mono.just(cookieHeader);
                })
                .block();
    }

    /**
     * 세종대 포털에 로그인 POST 요청을 보냅니다.
     */
    private String performLogin(String studentId, String password, String sessionCookie) {
        return webClient.post()
                .uri(PORTAL_BASE_URL + LOGIN_ACTION_PATH)
                .header(HttpHeaders.COOKIE, sessionCookie)
                .header(HttpHeaders.REFERER, PORTAL_BASE_URL + LOGIN_PAGE_PATH)
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", studentId)
                        .with("password", password)
                        .with("mainLogin", "Y")
                        .with("rtUrl", ""))
                .retrieve()
                .bodyToMono(String.class)
                .block();
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

        log.warn("세종대 포털 로그인 실패: 알 수 없는 응답");
        throw new CustomException(ErrorCode.SEJONG_LOGIN_FAILED);
    }
}