package com.wardk.meeteam_backend.acceptance.auth;

import com.wardk.meeteam_backend.acceptance.common.AcceptanceTest;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import com.wardk.meeteam_backend.web.auth.dto.login.LoginRequest;
import io.restassured.http.Cookie;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("인증 관련 인수 테스트")
public class AuthAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member member;
    private String email = "test@example.com";
    private String password = "password123";

    @BeforeEach
    void setUpTestData() {
        member = memberRepository.save(Member.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .realName("테스터")
                .role(UserRole.USER)
                .isParticipating(true)
                .build());
    }

    @Test
    @DisplayName("로그인 후 토큰 재발급(Silent Refresh) 시나리오 테스트")
    void loginAndRefreshFlow() {
        // 1. 로그인 요청
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(email);
        loginRequest.setPassword(password);

        ExtractableResponse<Response> loginResponse = given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then().log().all()
                .extract();

        assertThat(loginResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

        // 2. 로그인 응답에서 토큰 추출
        String accessToken = loginResponse.header("Authorization");
        String refreshTokenCookieValue = loginResponse.cookie(JwtUtil.REFRESH_COOKIE_NAME);

        assertThat(accessToken).isNotNull();
        assertThat(refreshTokenCookieValue).isNotNull();

        // AccessToken은 'Bearer ' 접두사를 포함할 수 있으므로 제거 (헤더에서 직접 읽으면 포함될 수 있음)
        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        // 3. 토큰 재발급 요청 (Access Token 없이 Refresh Token 쿠키만 전송)
        // 시나리오: AccessToken 만료 상황 가정 (클라이언트가 401 받고 재발급 요청)
        ExtractableResponse<Response> refreshResponse = given().log().all()
                .cookie(JwtUtil.REFRESH_COOKIE_NAME, refreshTokenCookieValue)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/api/auth/refresh")
                .then().log().all()
                .extract();

        assertThat(refreshResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

        // 4. 재발급된 Access Token 확인
        String newAccessToken = refreshResponse.jsonPath().getString("result");
        assertThat(newAccessToken).isNotNull();
        assertThat(newAccessToken).isNotEqualTo(accessToken); // 새로 발급되었으므로 다를 수 있음 (JTI 등)

        // 5. 새로 발급받은 토큰으로 보호된 리소스 접근 (예: 내 정보 조회 등, 여기서는 간단히 검증만)
        // 실제로는 보호된 API 호출 테스트가 필요하지만, 재발급 API 성공 여부가 핵심이므로 생략 가능
    }
}
