package com.wardk.meeteam_backend.acceptance.cucumber.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardk.meeteam_backend.domain.job.JobPosition;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 인증 관련 API 호출 클래스
 */
@Component
public class AuthAPI {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 일반 회원가입 요청
     */
    public ExtractableResponse<Response> 일반회원가입_요청(
            String email, String password, String name, String birthDate,
            String gender, List<JobPosition> jobPositions) {

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("email", email);
        params.put("password", password);
        params.put("name", name);
        params.put("birthDate", birthDate);
        params.put("gender", toGender(gender));
        params.put("jobPositions", jobPositions.stream().map(Enum::name).toList());

        String jsonBody = toJson(params);

        return RestAssured.given().log().all()
                .contentType("multipart/form-data")
                .multiPart("request", "request.json", jsonBody.getBytes(), "application/json")
                .when()
                .post("/api/auth/register")
                .then().log().all()
                .extract();
    }

    /**
     * OAuth2 회원가입 요청
     */
    public ExtractableResponse<Response> OAuth회원가입_요청(
            String oauthCode, String name, String birthDate,
            String gender, List<JobPosition> jobPositions, List<String> skills) {

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("code", oauthCode);
        params.put("name", name);
        params.put("birthDate", birthDate);
        params.put("gender", toGender(gender));
        params.put("jobPositions", jobPositions.stream().map(Enum::name).toList());
        params.put("skills", skills != null ? skills : List.of());

        String jsonBody = toJson(params);

        return RestAssured.given().log().all()
                .contentType("multipart/form-data")
                .multiPart("request", "request.json", jsonBody.getBytes(), "application/json")
                .when()
                .post("/api/auth/register/oauth2")
                .then().log().all()
                .extract();
    }

    /**
     * 로그인 요청
     */
    public ExtractableResponse<Response> 로그인_요청(String email, String password) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("email", email);
        params.put("password", password);

        return RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(params)
                .when()
                .post("/api/auth/login")
                .then().log().all()
                .extract();
    }

    /**
     * 이메일 중복 확인 요청
     */
    public ExtractableResponse<Response> 이메일중복확인_요청(String email) {
        return RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .queryParam("email", email)
                .when()
                .post("/api/auth/email")
                .then().log().all()
                .extract();
    }

    /**
     * 토큰 재발급 요청
     */
    public ExtractableResponse<Response> 토큰재발급_요청(String refreshToken) {
        var spec = RestAssured.given().log().all()
                .contentType(ContentType.JSON);

        if (refreshToken != null) {
            spec.cookie("refreshToken", refreshToken);
        }

        return spec
                .when()
                .post("/api/auth/refresh")
                .then().log().all()
                .extract();
    }

    /**
     * 로그아웃 요청
     */
    public ExtractableResponse<Response> 로그아웃_요청(String accessToken) {
        var spec = RestAssured.given().log().all()
                .contentType(ContentType.JSON);

        if (accessToken != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        return spec
                .when()
                .post("/api/auth/logout")
                .then().log().all()
                .extract();
    }

    /**
     * OAuth 토큰 교환 요청 (기존 회원 로그인)
     */
    public ExtractableResponse<Response> 토큰교환_요청(String code) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", code);

        return RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(params)
                .when()
                .post("/api/auth/token/exchange")
                .then().log().all()
                .extract();
    }

    /**
     * 인증이 필요한 API 접근 테스트용
     */
    public ExtractableResponse<Response> 회원전용API접근_요청(String accessToken) {
        var spec = RestAssured.given().log().all();

        if (accessToken != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        return spec
                .when()
                .get("/api/members")
                .then().log().all()
                .extract();
    }

    // === 내부 변환 헬퍼 ===

    private String toGender(String gender) {
        if (gender == null) return "MALE";
        return switch (gender) {
            case "남성" -> "MALE";
            case "여성" -> "FEMALE";
            default -> gender;
        };
    }

    private String toJson(Map<String, Object> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }
}
