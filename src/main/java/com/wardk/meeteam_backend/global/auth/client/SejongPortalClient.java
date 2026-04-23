package com.wardk.meeteam_backend.global.auth.client;

import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import okhttp3.JavaNetCookieJar;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.security.cert.X509Certificate;

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

    private final OkHttpClient httpClient;

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
        RequestBody formBody = new FormBody.Builder()
                .add("id", studentId)
                .add("password", password)
                .add("mainLogin", "N")
                .build();

        Request request = new Request.Builder()
                .url(PORTAL_BASE_URL + LOGIN_ACTION_PATH)
                .post(formBody)
                .header("Host", "portal.sejong.ac.kr")
                .header("Referer", PORTAL_BASE_URL)
                .header("User-Agent", "Mozilla/5.0")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
            return null;
        }
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

    /**
     * SSL 검증을 우회하는 OkHttpClient를 생성합니다.
     */
    private OkHttpClient buildClient() {
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

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{trustAllManager}, new java.security.SecureRandom());

            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), trustAllManager)
                    .hostnameVerifier((hostname, session) -> true)
                    .cookieJar(new JavaNetCookieJar(cookieManager))
                    .build();
        } catch (Exception e) {
            log.error("OkHttpClient 생성 실패: {}", e.getMessage(), e);
            return new OkHttpClient();
        }
    }
}
