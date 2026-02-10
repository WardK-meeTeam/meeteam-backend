package com.wardk.meeteam_backend.acceptance.cucumber.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProjectManagementApi {

    private final ObjectMapper objectMapper;

    public ProjectManagementApi(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Response getProjectMembers(Long projectId) {
        return RestAssured.given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/api/project-members/{projectId}", projectId)
                .then()
                .extract()
                .response();
    }

    public Response deleteProjectMember(Long projectId, Long memberId, String accessToken) {
        var spec = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of(
                        "projectId", projectId,
                        "memberId", memberId
                ));

        if (accessToken != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        return spec.when()
                .post("/api/project-members")
                .then()
                .extract()
                .response();
    }

    public Response applyProject(Long projectId, String jobPosition, String motivation, String accessToken) {
        var spec = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .formParam("projectId", projectId)
                .formParam("jobPosition", jobPosition)
                .formParam("motivation", motivation)
                .formParam("availableHoursPerWeek", 10)
                .formParam("availableDays", "MONDAY,FRIDAY")
                .formParam("offlineAvailable", true);

        if (accessToken != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        return spec.when()
                .post("/api/projects-application")
                .then()
                .extract()
                .response();
    }

    public Response getApplications(Long projectId, String accessToken) {
        var spec = RestAssured.given().accept(MediaType.APPLICATION_JSON_VALUE);
        if (accessToken != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return spec.when()
                .get("/api/projects-application/{projectId}", projectId)
                .then()
                .extract()
                .response();
    }

    public Response getApplicationDetail(Long projectId, Long applicationId, String accessToken) {
        var spec = RestAssured.given().accept(MediaType.APPLICATION_JSON_VALUE);
        if (accessToken != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return spec.when()
                .get("/api/projects-application/{projectId}/{applicationId}", projectId, applicationId)
                .then()
                .extract()
                .response();
    }

    public Response updateProject(Long projectId, Map<String, Object> updateRequest, String accessToken) {
        var spec = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .multiPart("projectUpdateRequest", toJson(updateRequest), MediaType.APPLICATION_JSON_VALUE);

        if (accessToken != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        return spec.when()
                .post("/api/projects/{projectId}", projectId)
                .then()
                .extract()
                .response();
    }

    public Response completeProject(Long projectId, String accessToken) {
        var spec = RestAssured.given().accept(MediaType.APPLICATION_JSON_VALUE);
        if (accessToken != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return spec.when()
                .post("/api/projects/{projectId}/complete", projectId)
                .then()
                .extract()
                .response();
    }

    public Response deleteProject(Long projectId, String accessToken) {
        var spec = RestAssured.given().accept(MediaType.APPLICATION_JSON_VALUE);
        if (accessToken != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return spec.when()
                .delete("/api/projects/{projectId}", projectId)
                .then()
                .extract()
                .response();
    }

    public Response getProjectDetail(Long projectId) {
        return RestAssured.given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/api/projects/V2/{projectId}", projectId)
                .then()
                .extract()
                .response();
    }

    public Response getProjectList() {
        return RestAssured.given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/api/projects")
                .then()
                .extract()
                .response();
    }

    private String toJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("요청 바디 직렬화에 실패했습니다.", e);
        }
    }
}
