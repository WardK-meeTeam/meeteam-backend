package com.wardk.meeteam_backend.acceptance.cucumber.api;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * 프로젝트 관련 API 호출 클래스
 */
@Component
public class ProjectApi {

    private static final String MAIN_PROJECTS_PATH = "/api/main/projects";
    private static final String PROJECTS_CONDITION_PATH = "/api/projects/condition";
    private static final String PROJECTS_PATH = "/api/projects";

    /**
     * 메인 페이지 프로젝트 목록 조회
     */
    public ExtractableResponse<Response> 메인_프로젝트_목록_조회() {
        return given().log().all()
                .when()
                .get(MAIN_PROJECTS_PATH)
                .then().log().all()
                .extract();
    }

    /**
     * 메인 페이지 프로젝트 목록 조회 (파라미터 포함)
     */
    public ExtractableResponse<Response> 메인_프로젝트_목록_조회(Map<String, Object> params) {
        return given().log().all()
                .queryParams(params)
                .when()
                .get(MAIN_PROJECTS_PATH)
                .then().log().all()
                .extract();
    }

    /**
     * 조건별 프로젝트 검색
     */
    public ExtractableResponse<Response> 프로젝트_조건_검색(Map<String, Object> params) {
        return given().log().all()
                .queryParams(params)
                .when()
                .get(PROJECTS_CONDITION_PATH)
                .then().log().all()
                .extract();
    }

    /**
     * 카테고리로 프로젝트 필터링
     */
    public ExtractableResponse<Response> 카테고리로_필터링(String category) {
        Map<String, Object> params = new HashMap<>();
        params.put("projectCategory", category);
        return 메인_프로젝트_목록_조회(params);
    }

    /**
     * 기술 스택으로 프로젝트 필터링
     */
    public ExtractableResponse<Response> 기술스택으로_필터링(String techStack) {
        Map<String, Object> params = new HashMap<>();
        params.put("techStack", techStack);
        return 프로젝트_조건_검색(params);
    }

    /**
     * 복합 조건으로 프로젝트 필터링
     */
    public ExtractableResponse<Response> 복합_조건_필터링(String category, String techStack) {
        Map<String, Object> params = new HashMap<>();
        params.put("projectCategory", category);
        params.put("techStack", techStack);
        return 프로젝트_조건_검색(params);
    }

    /**
     * 모집 상태로 프로젝트 필터링
     */
    public ExtractableResponse<Response> 모집상태로_필터링(String recruitmentStatus) {
        Map<String, Object> params = new HashMap<>();
        params.put("recruitmentStatus", recruitmentStatus);
        return 메인_프로젝트_목록_조회(params);
    }

    /**
     * 플랫폼으로 프로젝트 필터링
     */
    public ExtractableResponse<Response> 플랫폼으로_필터링(String platform) {
        Map<String, Object> params = new HashMap<>();
        params.put("platformCategory", platform);
        return 메인_프로젝트_목록_조회(params);
    }

    /**
     * 키워드로 프로젝트 검색
     */
    public ExtractableResponse<Response> 키워드_검색(String keyword) {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        return 메인_프로젝트_목록_조회(params);
    }

    /**
     * 페이지네이션 조회
     */
    public ExtractableResponse<Response> 페이지_조회(int page, int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("page", page);
        params.put("size", size);
        return 메인_프로젝트_목록_조회(params);
    }

    /**
     * 정렬하여 조회
     */
    public ExtractableResponse<Response> 정렬_조회(String sortBy) {
        Map<String, Object> params = new HashMap<>();
        params.put("sort", sortBy);
        return 메인_프로젝트_목록_조회(params);
    }

    /**
     * 프로젝트 상세 조회
     */
    public ExtractableResponse<Response> 프로젝트_상세_조회(Long projectId) {
        return given().log().all()
                .when()
                .get(PROJECTS_PATH + "/" + projectId)
                .then().log().all()
                .extract();
    }

    /**
     * 프로젝트 생성 (인증 필요)
     */
    public ExtractableResponse<Response> 프로젝트_생성(String accessToken, Map<String, Object> projectData) {
        return given().log().all()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(projectData)
                .when()
                .post(PROJECTS_PATH)
                .then().log().all()
                .extract();
    }
}
