package com.wardk.meeteam_backend.acceptance.cucumber.api;

import com.wardk.meeteam_backend.global.util.JwtUtil;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * 인증 관련 API 호출 클래스
 */
@Component
public class AuthApi {

    private static final String BASE_PATH = "/api/auth";

    /**
     * 로그인
     */
    public ExtractableResponse<Response> 로그인(String email, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        return given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .when()
                .post(BASE_PATH + "/login")
                .then().log().all()
                .extract();
    }

    /**
     * 토큰 재발급
     */
    public ExtractableResponse<Response> 토큰_재발급(String refreshToken) {
        return given().log().all()
                .cookie(JwtUtil.REFRESH_COOKIE_NAME, refreshToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post(BASE_PATH + "/refresh")
                .then().log().all()
                .extract();
    }

    /**
     * 토큰 재발급 (Access Token 포함)
     */
    public ExtractableResponse<Response> 토큰_재발급(String accessToken, String refreshToken) {
        return given().log().all()
                .header("Authorization", "Bearer " + accessToken)
                .cookie(JwtUtil.REFRESH_COOKIE_NAME, refreshToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post(BASE_PATH + "/refresh")
                .then().log().all()
                .extract();
    }

    /**
     * 로그아웃
     */
    public ExtractableResponse<Response> 로그아웃(String accessToken) {
        return given().log().all()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post(BASE_PATH + "/logout")
                .then().log().all()
                .extract();
    }

    /**
     * 회원가입
     */
    public ExtractableResponse<Response> 회원가입(String name, String email, String password, Integer age, String gender) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("email", email);
        request.put("password", password);
        request.put("age", age);
        request.put("gender", gender);
        request.put("subCategories", java.util.List.of());
        request.put("skills", java.util.List.of());

        return given().log().all()
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart("request", request, MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post(BASE_PATH + "/register")
                .then().log().all()
                .extract();
    }

    /**
     * OAuth2 토큰 교환
     */
    public ExtractableResponse<Response> 토큰_교환(String code) {
        Map<String, String> body = new HashMap<>();
        body.put("code", code);

        return given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .when()
                .post(BASE_PATH + "/token/exchange")
                .then().log().all()
                .extract();
    }

    /**
     * OAuth2 회원가입
     */
    public ExtractableResponse<Response> oauth2_회원가입(String code, String name, Integer age, String gender, java.util.List<Map<String, String>> subCategories, java.util.List<Map<String, String>> skills) {
        Map<String, Object> request = new HashMap<>();
        request.put("code", code);
        request.put("name", name);
        request.put("age", age);
        request.put("gender", gender);
        request.put("subCategories", subCategories);
        request.put("skills", skills);

        return given().log().all()
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart("request", request, MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post(BASE_PATH + "/register/oauth2")
                .then().log().all()
                .extract();
    }

    /**
     * 이메일 중복 체크
     */
    public ExtractableResponse<Response> 이메일_중복_체크(String email) {
        return given().log().all()
                .param("email", email)
                .when()
                .post(BASE_PATH + "/email")
                .then().log().all()
                .extract();
    }

    /**
     * 인증이 필요한 API 테스트용 (임의의 보호된 엔드포인트 호출)
     */
    public ExtractableResponse<Response> 인증_테스트(String accessToken) {
        return given().log().all()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/main/projects")
                .then().log().all()
                .extract();
    }
}
