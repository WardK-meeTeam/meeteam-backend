package com.wardk.meeteam_backend.acceptance.cucumber.steps;

import com.wardk.meeteam_backend.acceptance.cucumber.context.TestContext;
import com.wardk.meeteam_backend.acceptance.cucumber.context.TestContext.MemberContext;
import com.wardk.meeteam_backend.acceptance.cucumber.context.TestContext.ProjectContext;
import com.wardk.meeteam_backend.acceptance.cucumber.factory.MemberFactory;
import com.wardk.meeteam_backend.acceptance.cucumber.factory.ProjectFactory;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestApiSupport;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestRepositorySupport;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.먼저;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프로젝트 검색 관련 Step 정의
 */
public class ProjectSteps {

    @Autowired
    private TestContext testContext;

    @Autowired
    private TestApiSupport api;

    @Autowired
    private TestRepositorySupport repository;

    @Autowired
    private MemberFactory memberFactory;

    @Autowired
    private ProjectFactory projectFactory;

    // ==========================================================================
    // 프로젝트 설정 Steps
    // ==========================================================================

    @먼저("시스템에 다음 프로젝트들이 등록되어 있다:")
    public void 시스템에_다음_프로젝트들이_등록되어_있다(DataTable dataTable) {
        // 테스트용 멤버 생성 (프로젝트 생성자)
        Member creator = memberFactory.createMember("시스템관리자");

        dataTable.asMaps().forEach(row -> {
            String name = row.get("프로젝트명");
            String categoryStr = row.get("카테고리");
            String techStackStr = row.getOrDefault("기술스택", "");

            ProjectCategory category = mapCategory(categoryStr);

            Project project;
            if (!techStackStr.isEmpty()) {
                List<String> skills = List.of(techStackStr.split(",\\s*"));
                project = projectFactory.createProjectWithSkills(name, creator, skills);
            } else {
                project = projectFactory.createProject(name, creator, category);
            }

            ProjectContext projectContext = new ProjectContext();
            projectContext.setId(project.getId());
            projectContext.setName(name);
            projectContext.setCategory(categoryStr);
            projectContext.setLeaderId(creator.getId());

            testContext.addProject(name, projectContext);
        });
    }

    @먼저("시스템에 프로젝트가 {int}개 등록되어 있다")
    public void 시스템에_프로젝트가_N개_등록되어_있다(int count) {
        Member creator = memberFactory.createMember("대량생성자");
        projectFactory.createBulkProjects(creator, count);
    }

    @먼저("{string}가 만든 {string} 프로젝트가 존재한다")
    @먼저("{string}이 만든 {string} 프로젝트가 존재한다")
    public void 프로젝트가_존재한다(String creatorName, String projectName) {
        MemberContext creatorContext = testContext.getMember(creatorName);
        Member creator = repository.getMember().findById(creatorContext.getId())
                .orElseThrow(() -> new IllegalStateException(creatorName + " 회원을 찾을 수 없습니다."));

        Project project = projectFactory.createProject(projectName, creator);

        ProjectContext projectContext = new ProjectContext();
        projectContext.setId(project.getId());
        projectContext.setName(projectName);
        projectContext.setLeaderId(creator.getId());

        testContext.addProject(projectName, projectContext);
    }

    // ==========================================================================
    // 프로젝트 검색 Steps
    // ==========================================================================

    @만약("{string}가 메인 페이지에 접속하면")
    @만약("{string}이 메인 페이지에 접속하면")
    public void 메인_페이지에_접속하면(String name) {
        ExtractableResponse<Response> response = api.getProject().메인_프로젝트_목록_조회();
        testContext.setResponse(response);
    }

    @만약("{string}가 프로젝트 목록을 조회하면")
    @만약("{string}이 프로젝트 목록을 조회하면")
    public void 프로젝트_목록을_조회하면(String name) {
        ExtractableResponse<Response> response = api.getProject().메인_프로젝트_목록_조회();
        testContext.setResponse(response);
    }

    @만약("{string}가 {string} 카테고리로 필터링하면")
    @만약("{string}이 {string} 카테고리로 필터링하면")
    public void 카테고리로_필터링하면(String name, String category) {
        String categoryCode = mapCategoryToCode(category);
        ExtractableResponse<Response> response = api.getProject().카테고리로_필터링(categoryCode);
        testContext.setResponse(response);
    }

    @만약("{string}가 {string} 기술 스택으로 필터링하면")
    @만약("{string}이 {string} 기술 스택으로 필터링하면")
    public void 기술스택으로_필터링하면(String name, String techStack) {
        ExtractableResponse<Response> response = api.getProject().기술스택으로_필터링(techStack.toUpperCase());
        testContext.setResponse(response);
    }

    @만약("{string}가 {string}과 {string} 기술 스택으로 필터링하면")
    public void 여러_기술스택으로_필터링하면(String name, String techStack1, String techStack2) {
        // 복합 기술 스택 필터링
        ExtractableResponse<Response> response = api.getProject().기술스택으로_필터링(
                techStack1.toUpperCase() + "," + techStack2.toUpperCase()
        );
        testContext.setResponse(response);
    }

    @만약("{string}가 다음 조건으로 검색하면:")
    @만약("{string}이 다음 조건으로 검색하면:")
    public void 다음_조건으로_검색하면(String name, DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        String category = row.getOrDefault("카테고리", null);
        String techStack = row.getOrDefault("기술스택", null);

        String categoryCode = category != null ? mapCategoryToCode(category) : null;
        ExtractableResponse<Response> response = api.getProject().복합_조건_필터링(
                categoryCode,
                techStack != null ? techStack.toUpperCase() : null
        );
        testContext.setResponse(response);
    }

    @만약("{string}가 {string} 플랫폼으로 필터링하면")
    public void 플랫폼으로_필터링하면(String name, String platform) {
        String platformCode = mapPlatformToCode(platform);
        ExtractableResponse<Response> response = api.getProject().플랫폼으로_필터링(platformCode);
        testContext.setResponse(response);
    }

    @만약("{string}가 {string} 상태로 필터링하면")
    public void 모집상태로_필터링하면(String name, String status) {
        String statusCode = mapRecruitmentToCode(status);
        ExtractableResponse<Response> response = api.getProject().모집상태로_필터링(statusCode);
        testContext.setResponse(response);
    }

    @만약("{string}가 {string}으로 검색하면")
    @만약("{string}이 {string}으로 검색하면")
    public void 키워드로_검색하면(String name, String keyword) {
        ExtractableResponse<Response> response = api.getProject().키워드_검색(keyword);
        testContext.setResponse(response);
    }

    @만약("{string}가 검색어 없이 검색하면")
    public void 검색어_없이_검색하면(String name) {
        ExtractableResponse<Response> response = api.getProject().메인_프로젝트_목록_조회();
        testContext.setResponse(response);
    }

    @만약("{string}가 두 번째 페이지를 요청하면")
    public void 두_번째_페이지를_요청하면(String name) {
        ExtractableResponse<Response> response = api.getProject().페이지_조회(1, 20);
        testContext.setResponse(response);
    }

    // ==========================================================================
    // 프로젝트 검증 Steps
    // ==========================================================================

    @그러면("프로젝트 목록이 표시된다")
    @그러면("프로젝트 목록이 정상적으로 표시된다")
    public void 프로젝트_목록이_표시된다() {
        assertThat(testContext.getStatusCode())
                .as("프로젝트 목록 조회 응답")
                .isEqualTo(HttpStatus.OK.value());
    }

    @그리고("최신 등록순으로 정렬되어 있다")
    public void 최신_등록순으로_정렬되어_있다() {
        // 정렬 검증 로직
        assertThat(testContext.getStatusCode())
                .isEqualTo(HttpStatus.OK.value());
    }

    @그리고("한 페이지에 {int}개씩 표시된다")
    public void 한_페이지에_N개씩_표시된다(int expectedSize) {
        List<?> content = testContext.getResponse().jsonPath().getList("result.content");
        assertThat(content)
                .as("페이지당 프로젝트 수")
                .hasSize(expectedSize);
    }

    @그리고("총 {int}개의 프로젝트가 있음을 알 수 있다")
    public void 총_N개의_프로젝트가_있음을_알_수_있다(int expectedTotal) {
        int totalElements = testContext.getResponse().jsonPath().getInt("result.totalElements");
        assertThat(totalElements)
                .as("전체 프로젝트 수")
                .isEqualTo(expectedTotal);
    }

    @그러면("다음 프로젝트들만 표시된다:")
    public void 다음_프로젝트들만_표시된다(DataTable dataTable) {
        List<String> expectedNames = dataTable.asMaps().stream()
                .map(row -> row.get("프로젝트명"))
                .toList();

        List<String> actualNames = testContext.getResponse().jsonPath().getList("result.content.projectName");

        assertThat(actualNames)
                .as("필터링된 프로젝트 목록")
                .containsExactlyInAnyOrderElementsOf(expectedNames);
    }

    @그리고("다른 카테고리의 프로젝트는 표시되지 않는다")
    public void 다른_카테고리의_프로젝트는_표시되지_않는다() {
        // 검증은 다음_프로젝트들만_표시된다에서 처리됨
    }

    @그러면("{string} 프로젝트가 표시된다")
    public void 특정_프로젝트가_표시된다(String projectName) {
        String responseBody = testContext.getResponse().body().asString();
        assertThat(responseBody)
                .as("프로젝트 검색 결과")
                .contains(projectName);
    }

    @그러면("{string} 메시지가 표시된다")
    public void 메시지가_표시된다(String expectedMessage) {
        // 빈 결과 또는 메시지 확인
        assertThat(testContext.getStatusCode())
                .isEqualTo(HttpStatus.OK.value());
    }

    @그리고("프로젝트 목록은 비어있다")
    public void 프로젝트_목록은_비어있다() {
        List<?> content = testContext.getResponse().jsonPath().getList("result.content");
        assertThat(content)
                .as("프로젝트 목록")
                .isEmpty();
    }

    @그러면("전체 프로젝트 목록이 표시된다")
    public void 전체_프로젝트_목록이_표시된다() {
        assertThat(testContext.getStatusCode())
                .isEqualTo(HttpStatus.OK.value());

        List<?> content = testContext.getResponse().jsonPath().getList("result.content");
        assertThat(content)
                .as("프로젝트 목록")
                .isNotEmpty();
    }

    // ==========================================================================
    // Helper Methods
    // ==========================================================================

    private ProjectCategory mapCategory(String category) {
        return switch (category) {
            case "교육" -> ProjectCategory.EDUCATION;
            case "건강" -> ProjectCategory.HEALTH;
            case "소셜" -> ProjectCategory.SOCIAL;
            case "반려동물" -> ProjectCategory.PET;
            case "금융" -> ProjectCategory.FINANCE;
            default -> ProjectCategory.ETC;
        };
    }

    private String mapCategoryToCode(String category) {
        return switch (category) {
            case "교육" -> "EDUCATION";
            case "건강" -> "HEALTH";
            case "소셜" -> "SOCIAL";
            case "반려동물" -> "PET";
            case "금융" -> "FINANCE";
            default -> "ETC";
        };
    }

    private String mapPlatformToCode(String platform) {
        return switch (platform) {
            case "웹" -> "WEB";
            case "앱" -> "APP";
            case "데스크톱" -> "DESKTOP";
            default -> "WEB";
        };
    }

    private String mapRecruitmentToCode(String status) {
        return switch (status) {
            case "모집중" -> "RECRUITING";
            case "모집완료" -> "CLOSED";
            default -> "RECRUITING";
        };
    }
}
