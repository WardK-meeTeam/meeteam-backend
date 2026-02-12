package com.wardk.meeteam_backend.acceptance.cucumber.support;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * 시나리오 내 Step 클래스 간 데이터 공유를 위한 중앙 저장소
 * @ScenarioScope로 시나리오마다 새로운 인스턴스 생성
 */
@Component
@ScenarioScope
public class TestContext {

    // === 공통 필드 ===
    private String accessToken;
    private ExtractableResponse<Response> response;
    private io.restassured.response.Response lastResponse; // 기존 코드 호환용
    private String lastMessage;

    // === 도메인별 데이터 클래스 ===
    @Getter @Setter
    public static class MemberData {
        private Long id;
        private String email;
        private String name;
        private String password;
        private String oauthCode;  // OAuth 회원가입용 코드
    }

    @Getter @Setter
    public static class ProjectData {
        private Long id;
        private String name;
    }

    // === 도메인 데이터 인스턴스 ===
    private final MemberData memberData = new MemberData();
    private final ProjectData projectData = new ProjectData();

    // === 공통 필드 Getter/Setter ===
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public ExtractableResponse<Response> getResponse() {
        return response;
    }

    public void setResponse(ExtractableResponse<Response> response) {
        this.response = response;
    }

    // 기존 코드 호환용 (Response 타입)
    public io.restassured.response.Response getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(io.restassured.response.Response lastResponse) {
        this.lastResponse = lastResponse;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    // === 도메인 데이터 접근 메서드 ===
    public MemberData member() {
        return memberData;
    }

    public ProjectData project() {
        return projectData;
    }

    // === 초기화 ===
    public void clear() {
        this.accessToken = null;
        this.response = null;
        this.lastResponse = null;
        this.lastMessage = null;
    }
}