package com.wardk.meeteam_backend.acceptance.cucumber.api;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * 파일 업로드 관련 API 호출 클래스
 * <p>
 * 실제 존재하는 API:
 * - PUT /api/members (프로필 수정, 인증 필요)
 * - POST /api/projects/{projectId} (프로젝트 수정, 인증 필요)
 */
@Component
public class FileApi {

    private static final String MEMBERS_PATH = "/api/members";
    private static final String PROJECTS_PATH = "/api/projects";

    /**
     * 프로필 수정 (프로필 사진 포함)
     * PUT /api/members - 인증 필요
     */
    public ExtractableResponse<Response> 프로필_사진_업로드(String accessToken, Map<String, Object> memberInfo, byte[] fileContent, String fileName, String contentType) {
        return given().log().all()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart("memberInfo", memberInfo, MediaType.APPLICATION_JSON_VALUE)
                .multiPart("profileImage", fileName, fileContent, contentType)
                .when()
                .put(MEMBERS_PATH)
                .then().log().all()
                .extract();
    }

    /**
     * 프로젝트 수정 (썸네일 포함)
     * POST /api/projects/{projectId} - 인증 필요
     */
    public ExtractableResponse<Response> 프로젝트_썸네일_업로드(String accessToken, Long projectId, Map<String, Object> projectInfo, byte[] fileContent, String fileName, String contentType) {
        return given().log().all()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart("projectUpdateRequest", projectInfo, MediaType.APPLICATION_JSON_VALUE)
                .multiPart("file", fileName, fileContent, contentType)
                .when()
                .post(PROJECTS_PATH + "/" + projectId)
                .then().log().all()
                .extract();
    }
}