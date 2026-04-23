package com.wardk.meeteam_backend.global.auth.client;

import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 세종대학교 포털 로그인 클라이언트
 * <p>
 * 세종대 포털(portal.sejong.ac.kr)에 HTTP 요청을 보내 학생 인증을 수행합니다.
 */
@Slf4j
@Component
public class SejongPortalClient {

    private static final String PORTAL_BASE_URL = "https://portal.sejong.ac.kr";
    private static final String LOGIN_ACTION_PATH = "/jsp/login/login_action.jsp";

    private final WebClient webClient;

    public SejongPortalClient(@Qualifier("sejongWebClient") WebClient webClient) {
        this.webClient = webClient;
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
    private String performLogin(String studentId, String password) {
        return webClient.post()
                .uri(PORTAL_BASE_URL + LOGIN_ACTION_PATH)
                .header(HttpHeaders.HOST, "portal.sejong.ac.kr")
                .header(HttpHeaders.REFERER, PORTAL_BASE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", studentId)
                        .with("password", password)
                        .with("mainLogin", "N"))
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