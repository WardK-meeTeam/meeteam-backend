package com.wardk.meeteam_backend.acceptance.cucumber.steps;

import com.wardk.meeteam_backend.acceptance.cucumber.api.ProjectCreateApi;
import com.wardk.meeteam_backend.acceptance.cucumber.support.ScenarioState;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectmember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectCreateSteps {

    private static final String OPEN_ENDED_DATE = "9999-12-31";

    @Autowired
    private ProjectCreateApi projectCreateApi;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ScenarioState scenarioState;

    private String accessToken;
    private Response lastResponse;
    private Long createdProjectId;
    private Map<String, Object> pendingProjectRequest;

    @Before("@create")
    public void setUpScenarioState() {
        accessToken = null;
        lastResponse = null;
        createdProjectId = null;
        pendingProjectRequest = null;
        scenarioState.clear();
    }

    @Given("{string} 회원이 로그인한 상태이다")
    public void 회원이_로그인한_상태이다(String memberName) {
        Member member = createOrFindMember(memberName);
        accessToken = jwtUtil.createAccessToken(member);
        scenarioState.setAccessToken(accessToken);
    }

    @Given("로그인하지 않은 상태이다")
    public void 로그인하지_않은_상태이다() {
        accessToken = null;
        scenarioState.setAccessToken(null);
    }

    @When("다음 정보로 프로젝트 등록을 요청하면:")
    public void 다음_정보로_프로젝트_등록을_요청하면(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);

        Map<String, Object> request = defaultProjectRequest();
        request.put("projectName", row.get("프로젝트명"));
        request.put("projectCategory", toProjectCategory(row.get("카테고리")));
        request.put("platformCategory", toPlatformCategory(row.get("플랫폼")));
        request.put("description", row.get("소개글"));
        pendingProjectRequest = request;
    }

    @And("다음 모집 포지션을 추가하면:")
    public void 다음_모집_포지션을_추가하면(DataTable dataTable) {
        ensurePendingRequestExists();

        List<Map<String, Object>> recruitments = new ArrayList<>();
        for (Map<String, String> row : dataTable.asMaps()) {
            String jobPosition = toJobPosition(row.get("분야"), row.get("상세 분야"));
            recruitments.add(recruitment(jobPosition, parseCount(row.get("모집 인원"))));
        }

        pendingProjectRequest.put("recruitments", recruitments);
        lastResponse = projectCreateApi.createProject(accessToken, pendingProjectRequest);
        scenarioState.setLastResponse(lastResponse);
        createdProjectId = getCreatedProjectId(lastResponse);
    }

    @When("모집 마감일을 {string}로 설정하고 프로젝트를 등록하면")
    public void 모집_마감일을_설정하고_프로젝트를_등록하면(String deadlineLabel) {
        Map<String, Object> request = defaultProjectRequest();
        if ("모집 완료 시까지".equals(deadlineLabel)) {
            // 현재 API는 endDate가 필수이므로 open-ended를 센티넬 날짜로 표현한다.
            request.put("endDate", OPEN_ENDED_DATE);
        }
        lastResponse = projectCreateApi.createProject(accessToken, request);
        scenarioState.setLastResponse(lastResponse);
        createdProjectId = getCreatedProjectId(lastResponse);
    }

    @When("프로젝트명을 입력하지 않고 등록을 요청하면")
    public void 프로젝트명을_입력하지_않고_등록을_요청하면() {
        Map<String, Object> request = defaultProjectRequest();
        request.put("projectName", null);
        lastResponse = projectCreateApi.createProject(accessToken, request);
        scenarioState.setLastResponse(lastResponse);
    }

    @When("모집 포지션을 추가하지 않고 등록을 요청하면")
    public void 모집_포지션을_추가하지_않고_등록을_요청하면() {
        Map<String, Object> request = defaultProjectRequest();
        request.put("recruitments", List.of());
        lastResponse = projectCreateApi.createProject(accessToken, request);
        scenarioState.setLastResponse(lastResponse);
    }

    @When("프로젝트 등록을 요청하면")
    public void 프로젝트_등록을_요청하면() {
        Map<String, Object> request = defaultProjectRequest();
        lastResponse = projectCreateApi.createProject(accessToken, request);
        scenarioState.setLastResponse(lastResponse);
    }

    @Then("프로젝트 등록에 성공한다")
    public void 프로젝트_등록에_성공한다() {
        assertNotNull(lastResponse, "응답이 존재해야 합니다.");
        assertEquals(200, lastResponse.statusCode());
        assertEquals("COMMON200", lastResponse.jsonPath().getString("code"));
        assertNotNull(getCreatedProjectId(lastResponse), "생성된 프로젝트 ID가 있어야 합니다.");
    }

    @Then("프로젝트 등록에 실패한다")
    public void 프로젝트_등록에_실패한다() {
        assertNotNull(lastResponse, "응답이 존재해야 합니다.");
        assertTrue(lastResponse.statusCode() >= 400, "4xx 이상 응답이어야 합니다.");
    }

    @Then("요청이 거부된다")
    public void 요청이_거부된다() {
        Response response = lastResponse != null ? lastResponse : scenarioState.getLastResponse();
        assertNotNull(response, "응답이 존재해야 합니다.");
        assertTrue(response.statusCode() == 401 || response.statusCode() == 403,
                "401 또는 403 응답이어야 합니다.");
        String code = response.jsonPath().getString("code");
        assertNotNull(code);
    }

    @And("{string}이 프로젝트 리더로 지정된다")
    public void 프로젝트_리더로_지정된다(String memberName) {
        assertNotNull(createdProjectId, "생성된 프로젝트 ID가 필요합니다.");

        Project project = projectRepository.findById(createdProjectId)
                .orElseThrow(() -> new AssertionError("생성된 프로젝트를 찾을 수 없습니다."));

        Member leader = createOrFindMember(memberName);
        assertEquals(leader.getId(), project.getCreator().getId(), "프로젝트 생성자가 리더여야 합니다.");
        assertTrue(
                projectMemberRepository.existsByProjectIdAndMemberId(createdProjectId, leader.getId()),
                "리더가 프로젝트 멤버로 등록되어야 합니다."
        );
    }

    @And("프로젝트 상세 페이지에서 등록된 정보를 확인할 수 있다")
    public void 프로젝트_상세_페이지에서_등록된_정보를_확인할_수_있다() {
        assertNotNull(createdProjectId, "생성된 프로젝트 ID가 필요합니다.");
        Response detail = projectCreateApi.getProjectDetail(createdProjectId);

        assertEquals(200, detail.statusCode());
        assertEquals("COMMON200", detail.jsonPath().getString("code"));
        if (pendingProjectRequest != null) {
            String actualName = Optional.ofNullable(detail.jsonPath().getString("result.name"))
                    .orElse(detail.jsonPath().getString("result.projectName"));
            assertNotNull(actualName);
            assertFalse(actualName.isBlank());
            String description = detail.jsonPath().getString("result.description");
            assertNotNull(description);
            assertFalse(description.isBlank());
            assertNotNull(detail.jsonPath().getString("result.projectCategory"));
            assertNotNull(detail.jsonPath().getString("result.platformCategory"));
        }
    }

    @And("프로젝트 상세에서 {string} 표시를 확인할 수 있다")
    public void 프로젝트_상세에서_표시를_확인할_수_있다(String expectedLabel) {
        assertNotNull(createdProjectId, "생성된 프로젝트 ID가 필요합니다.");
        Response detail = projectCreateApi.getProjectDetail(createdProjectId);
        assertEquals(200, detail.statusCode());

        String endDate = detail.jsonPath().getString("result.endDate");
        if ("모집 완료 시까지".equals(expectedLabel)) {
            assertEquals(OPEN_ENDED_DATE, endDate);
        } else {
            assertNotNull(endDate);
        }
    }

    @And("{string} 메시지를 확인한다")
    public void 메시지를_확인한다(String expectedMessage) {
        Response response = lastResponse != null ? lastResponse : scenarioState.getLastResponse();
        String actualMessage = scenarioState.getLastMessage();
        if (actualMessage == null && response != null) {
            actualMessage = Optional.ofNullable(response.jsonPath().getString("message"))
                    .orElse(response.asString());
        }
        assertNotNull(actualMessage, "메시지를 확인할 수 있는 응답 또는 상태가 필요합니다.");
        assertTrue(matchesExpectedMessage(expectedMessage, actualMessage),
                "기대 메시지 [" + expectedMessage + "] 실제 메시지 [" + actualMessage + "]");
    }

    private void ensurePendingRequestExists() {
        if (pendingProjectRequest == null) {
            pendingProjectRequest = defaultProjectRequest();
        }
    }

    private Long getCreatedProjectId(Response response) {
        if (response == null || response.statusCode() >= 400) {
            return null;
        }
        Object id = response.jsonPath().get("result.id");
        if (id instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private Map<String, Object> defaultProjectRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("projectName", "기본 프로젝트");
        request.put("description", "테스트용 프로젝트 설명");
        request.put("projectCategory", "AI_TECH");
        request.put("platformCategory", "WEB");
        request.put("offlineRequired", Boolean.FALSE);
        request.put("jobPosition", "WEB_SERVER");
        request.put("recruitments", new ArrayList<>(List.of(recruitment("WEB_FRONTEND", 2))));
        request.put("skills", new ArrayList<>(List.of("React Native", "Python")));
        request.put("endDate", LocalDate.now().plusDays(30).toString());
        return request;
    }

    private Map<String, Object> recruitment(String jobPosition, int recruitmentCount) {
        Map<String, Object> recruitment = new LinkedHashMap<>();
        recruitment.put("jobPosition", jobPosition);
        recruitment.put("recruitmentCount", recruitmentCount);
        return recruitment;
    }

    private Member createOrFindMember(String memberName) {
        String email = toEmail(memberName);
        return memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(Member.createForTest(email, memberName)));
    }

    private String toEmail(String memberName) {
        return switch (memberName) {
            case "홍길동" -> "hong@example.com";
            case "김철수" -> "kim@example.com";
            case "이영희" -> "lee@example.com";
            case "박지민" -> "park@example.com";
            default -> "user" + Math.abs(memberName.hashCode()) + "@example.com";
        };
    }

    private String toProjectCategory(String category) {
        String normalized = category.replace(" ", "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ai/테크", "ai/tech", "ai테크" -> "AI_TECH";
            case "헬스케어" -> "HEALTHCARE";
            case "반려동물" -> "PET";
            case "환경", "친환경" -> "ENVIRONMENT";
            case "교육", "교육/학습" -> "EDUCATION";
            case "패션", "뷰티", "패션/뷰티" -> "FASHION_BEAUTY";
            case "금융", "생산성", "금융/생산성" -> "FINANCE_PRODUCTIVITY";
            default -> "ETC";
        };
    }

    private String toPlatformCategory(String platform) {
        String normalized = platform.replace(" ", "").toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "WEB" -> "WEB";
            case "IOS" -> "IOS";
            case "ANDROID" -> "ANDROID";
            default -> throw new IllegalArgumentException("지원하지 않는 플랫폼입니다: " + platform);
        };
    }

    private String toJobPosition(String field, String detailField) {
        String detail = detailField == null ? "" : detailField.trim().toLowerCase(Locale.ROOT);
        if (detail.contains("react native")) {
            return "CROSS_PLATFORM";
        }
        if (detail.contains("ui/ux")) {
            return "UI_UX_DESIGN";
        }
        if (detail.contains("ios")) {
            return "IOS";
        }
        if (detail.contains("android")) {
            return "ANDROID";
        }
        if (detail.contains("ai")) {
            return "AI";
        }

        String normalizedField = field == null ? "" : field.trim();
        return switch (normalizedField) {
            case "프론트엔드" -> "WEB_FRONTEND";
            case "디자인" -> "UI_UX_DESIGN";
            case "백엔드" -> "WEB_SERVER";
            case "기획" -> "PRODUCT_MANAGER";
            default -> "ETC";
        };
    }

    private int parseCount(String rawCount) {
        String numeric = rawCount.replaceAll("[^0-9]", "");
        if (numeric.isEmpty()) {
            throw new IllegalArgumentException("모집 인원을 파싱할 수 없습니다: " + rawCount);
        }
        return Integer.parseInt(numeric);
    }

    private boolean matchesExpectedMessage(String expected, String actual) {
        if (actual != null && actual.contains(expected)) {
            return true;
        }

        List<String> aliases = new ArrayList<>();
        if ("프로젝트명을 입력해주세요".equals(expected)) {
            aliases.add("제목은 필수입니다.");
        }
        if ("최소 1개 이상의 모집 포지션을 추가해주세요".equals(expected)) {
            aliases.add("최소 한 개 이상의 모집 분야를 입력해주세요.");
        }
        if ("로그인이 필요합니다".equals(expected)) {
            aliases.add("로그인이 필요합니다.");
        }
        if ("검색 결과가 없습니다".equals(expected)) {
            aliases.add("검색 결과가 없습니다");
        }

        for (String alias : aliases) {
            if (actual != null && actual.contains(alias)) {
                return true;
            }
        }
        return false;
    }
}
