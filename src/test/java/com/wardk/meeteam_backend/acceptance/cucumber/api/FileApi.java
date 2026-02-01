package com.wardk.meeteam_backend.acceptance.cucumber.api;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * 파일 업로드 관련 API 호출 클래스
 */
@Component
public class FileApi {

    private static final String MEMBERS_PATH = "/api/members";
    private static final String PROJECTS_PATH = "/api/projects";

    /**
     * 프로필 사진 업로드 (인증된 사용자)
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
     * 프로필 사진 업로드 (비인증)
     */
    public ExtractableResponse<Response> 비인증_프로필_사진_업로드(Map<String, Object> memberInfo, byte[] fileContent, String fileName, String contentType) {
        return given().log().all()
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart("memberInfo", memberInfo, MediaType.APPLICATION_JSON_VALUE)
                .multiPart("profileImage", fileName, fileContent, contentType)
                .when()
                .put(MEMBERS_PATH)
                .then().log().all()
                .extract();
    }

    /**
     * 타인 프로필 사진 변경 시도
     */
    public ExtractableResponse<Response> 타인_프로필_사진_변경(String accessToken, Long targetMemberId, Map<String, Object> memberInfo, byte[] fileContent, String fileName, String contentType) {
        return given().log().all()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart("memberInfo", memberInfo, MediaType.APPLICATION_JSON_VALUE)
                .multiPart("profileImage", fileName, fileContent, contentType)
                .when()
                .put(MEMBERS_PATH + "/" + targetMemberId)
                .then().log().all()
                .extract();
    }

    /**
     * 프로젝트 썸네일 업로드
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