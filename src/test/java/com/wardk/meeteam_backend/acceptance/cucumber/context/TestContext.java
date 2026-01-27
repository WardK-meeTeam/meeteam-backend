package com.wardk.meeteam_backend.acceptance.cucumber.context;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 시나리오 간 데이터 공유를 위한 컨텍스트
 * ScenarioScope로 각 시나리오마다 새로운 인스턴스가 생성됩니다.
 */
@Getter
@Component
@ScenarioScope
public class TestContext {

    // 현재 시나리오의 회원 정보
    private final MemberContext currentMember = new MemberContext();

    // 현재 시나리오의 프로젝트 정보
    private final ProjectContext currentProject = new ProjectContext();

    // 이름으로 회원 조회용 맵
    private final Map<String, MemberContext> members = new HashMap<>();

    // 이름으로 프로젝트 조회용 맵
    private final Map<String, ProjectContext> projects = new HashMap<>();

    // API 응답
    @Setter
    private ExtractableResponse<Response> response;

    // 현재 로그인한 사용자의 Access Token
    @Setter
    private String accessToken;

    // 현재 로그인한 사용자의 Refresh Token
    @Setter
    private String refreshToken;

    /**
     * 회원 컨텍스트 저장
     */
    public void addMember(String name, MemberContext memberContext) {
        members.put(name, memberContext);
    }

    /**
     * 회원 컨텍스트 조회
     */
    public MemberContext getMember(String name) {
        MemberContext member = members.get(name);
        if (member == null) {
            throw new IllegalStateException("'" + name + "' 회원이 존재하지 않습니다. 먼저 회원을 생성해주세요.");
        }
        return member;
    }

    /**
     * 프로젝트 컨텍스트 저장
     */
    public void addProject(String name, ProjectContext projectContext) {
        projects.put(name, projectContext);
    }

    /**
     * 프로젝트 컨텍스트 조회
     */
    public ProjectContext getProject(String name) {
        ProjectContext project = projects.get(name);
        if (project == null) {
            throw new IllegalStateException("'" + name + "' 프로젝트가 존재하지 않습니다. 먼저 프로젝트를 생성해주세요.");
        }
        return project;
    }

    /**
     * 응답 상태 코드 확인
     */
    public int getStatusCode() {
        return response.statusCode();
    }

    /**
     * 응답 본문에서 메시지 추출
     */
    public String getResponseMessage() {
        return response.jsonPath().getString("message");
    }

    /**
     * 응답 본문에서 결과 추출
     */
    public <T> T getResult(Class<T> clazz) {
        return response.jsonPath().getObject("result", clazz);
    }

    /**
     * 회원 컨텍스트 내부 클래스
     */
    @Getter
    @Setter
    public static class MemberContext {
        private Long id;
        private String name;
        private String email;
        private String password;
        private String accessToken;
        private String refreshToken;
        private String provider; // OAuth provider (google, github)
        private String oauthCode; // OAuth 인증 코드
    }

    /**
     * 프로젝트 컨텍스트 내부 클래스
     */
    @Getter
    @Setter
    public static class ProjectContext {
        private Long id;
        private String name;
        private String category;
        private String status;
        private Long leaderId; // 팀장 ID
    }
}
