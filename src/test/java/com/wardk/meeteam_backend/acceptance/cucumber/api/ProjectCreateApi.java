package com.wardk.meeteam_backend.acceptance.cucumber.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProjectCreateApi {

    private final ObjectMapper objectMapper;

    public Response createProject(String accessToken, Map<String, Object> requestBody) {
        var spec = RestAssured.given()
                .accept("application/json")
                .multiPart("projectPostRequest", toJson(requestBody), "application/json");

        if (accessToken != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        return spec
                .when()
                .post("/api/projects")
                .then()
                .extract()
                .response();
    }

    public Response getProjectDetail(Long projectId) {
        return RestAssured.given()
                .accept("application/json")
                .when()
                .get("/api/projects/V2/{projectId}", projectId)
                .then()
                .extract()
                .response();
    }

    private String toJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("프로젝트 요청 본문 직렬화에 실패했습니다.", e);
        }
    }
}
