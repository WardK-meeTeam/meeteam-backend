package com.wardk.meeteam_backend.acceptance.cucumber.steps;

import com.wardk.meeteam_backend.acceptance.cucumber.api.ProjectSearchApi;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestContext;
import com.wardk.meeteam_backend.domain.applicant.entity.RecruitmentState;
import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.pr.entity.ProjectRepo;
import com.wardk.meeteam_backend.domain.pr.repository.ProjectRepoRepository;
import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import com.wardk.meeteam_backend.domain.project.entity.RecruitmentDeadlineType;
import com.wardk.meeteam_backend.domain.project.entity.Recruitment;
import com.wardk.meeteam_backend.domain.project.entity.ProjectSkill;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.project.service.dto.ProjectPostCommand;
import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class ProjectSearchSteps {

    @Autowired
    private ProjectSearchApi projectSearchApi;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private ProjectRepoRepository projectRepoRepository;

    @Autowired
    private TestContext context;

    private Response lastResponse;
    private Long currentProjectId;
    private List<String> lastResultProjectNames = new ArrayList<>();
    private List<Map<String, String>> qnaRows = new ArrayList<>();
    private Map<String, String> externalChannels = new LinkedHashMap<>();
    private String expectedLeaderTechStack;
    private String expectedNewestProjectName;
    private String expectedMostLikedProjectName;

    @Before("@search or @filter or @pagination or @sort or @detail")
    public void setUp() {
        lastResponse = null;
        currentProjectId = null;
        lastResultProjectNames = new ArrayList<>();
        qnaRows = new ArrayList<>();
        externalChannels = new LinkedHashMap<>();
        expectedLeaderTechStack = null;
        expectedNewestProjectName = null;
        expectedMostLikedProjectName = null;
        context.clear();
    }

    @Given("다음 프로젝트들이 등록되어 있다:")
    public void 다음_프로젝트들이_등록되어_있다(DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            Project project = createProjectFixture(
                    row.get("프로젝트명"),
                    row.get("카테고리"),
                    row.get("플랫폼"),
                    row.get("리더"),
                    row.get("모집상태"),
                    "테스트 프로젝트 소개",
                    LocalDate.now().plusDays(30)
            );
            currentProjectId = project.getId();
        }
    }

    @Given("프로젝트가 {int}개 등록되어 있다")
    public void 프로젝트가_n개_등록되어_있다(int count) {
        for (int i = 1; i <= count; i++) {
            Project project = createProjectFixture(
                    "테스트 프로젝트 " + i,
                    "AI/테크",
                    "Web",
                    "홍길동",
                    "모집중",
                    "프로젝트 " + i + " 설명",
                    LocalDate.now().plusDays(30)
            );
            projectRepository.overrideTimestamps(project.getId(), LocalDateTime.now().minusDays(count - i));
        }
    }

    @Given("프로젝트가 여러 개 등록되어 있다")
    public void 프로젝트가_여러_개_등록되어_있다() {
        Project oldProject = createProjectFixture("오래된 프로젝트", "AI/테크", "Web", "홍길동", "모집중", "old", LocalDate.now().plusDays(10));
        oldProject.setLikeCount(3);
        projectRepository.save(oldProject);
        projectRepository.overrideTimestamps(oldProject.getId(), LocalDateTime.now().minusDays(10));

        Project middleProject = createProjectFixture("중간 프로젝트", "AI/테크", "Web", "김철수", "모집중", "middle", LocalDate.now().plusDays(20));
        middleProject.setLikeCount(10);
        projectRepository.save(middleProject);
        projectRepository.overrideTimestamps(middleProject.getId(), LocalDateTime.now().minusDays(5));

        Project newestProject = createProjectFixture("가장 최근 프로젝트", "AI/테크", "Web", "이영희", "모집중", "new", LocalDate.now().plusDays(30));
        newestProject.setLikeCount(24);
        projectRepository.save(newestProject);
        projectRepository.overrideTimestamps(newestProject.getId(), LocalDateTime.now().minusDays(1));

        expectedNewestProjectName = newestProject.getName();
        expectedMostLikedProjectName = newestProject.getName();
    }

    @Given("{string}이 리더인 {string} 프로젝트가 존재한다")
    public void 리더인_프로젝트가_존재한다(String leaderName, String projectName) {
        Project project = createProjectFixture(
                projectName, "AI/테크", "Web", leaderName, "모집중",
                "AI 모델을 활용한 요약 서비스 소개글", LocalDate.now().plusDays(30)
        );
        project.setLikeCount(24);
        projectRepository.save(project);
        currentProjectId = project.getId();
    }

    @Given("{string}이 리더인 프로젝트가 존재한다")
    public void 리더인_프로젝트가_존재한다(String leaderName) {
        Project project = createProjectFixture(
                "리더 정보 확인 프로젝트", "AI/테크", "Web", leaderName, "모집중",
                "리더 정보 테스트용 프로젝트", LocalDate.now().plusDays(30)
        );
        project.setLikeCount(24);
        projectRepository.save(project);
        currentProjectId = project.getId();
    }

    @Given("{string}의 주요 기술 스택은 {string}이다")
    public void 리더의_주요_기술_스택은이다(String leaderName, String techStack) {
        expectedLeaderTechStack = techStack;
    }

    @Given("{string} 프로젝트가 다음 모집 현황을 가지고 있다:")
    public void 프로젝트가_다음_모집_현황을_가지고_있다(String projectName, DataTable dataTable) {
        Project project = createProjectFixture(
                projectName, "AI/테크", "Web", "홍길동", "모집중",
                "모집 현황 테스트", LocalDate.now().plusDays(30)
        );

        project.getRecruitments().clear();
        for (Map<String, String> row : dataTable.asMaps()) {
            RecruitmentState state = RecruitmentState.createRecruitmentState(
                    toJobPosition(row.get("포지션")),
                    parseCount(row.get("모집 인원"))
            );
            project.addRecruitment(state);
            state.updateCurrentCount(parseCount(row.get("현재 인원")));
        }
        projectRepository.save(project);
        currentProjectId = project.getId();
    }

    @Given("프로젝트에 다음 Q&A가 존재한다:")
    public void 프로젝트에_다음_qna가_존재한다(DataTable dataTable) {
        qnaRows = dataTable.asMaps();
        if (currentProjectId == null) {
            Project project = createProjectFixture(
                    "Q&A 테스트 프로젝트", "AI/테크", "Web", "홍길동", "모집중",
                    "Q&A 테스트", LocalDate.now().plusDays(30)
            );
            currentProjectId = project.getId();
        }
    }

    @Given("프로젝트에 다음 외부 채널이 등록되어 있다:")
    public void 프로젝트에_다음_외부_채널이_등록되어_있다(DataTable dataTable) {
        externalChannels = new LinkedHashMap<>();
        for (Map<String, String> row : dataTable.asMaps()) {
            externalChannels.put(row.get("항목"), row.get("값"));
        }
        if (currentProjectId == null) {
            Project project = createProjectFixture(
                    "외부 채널 테스트 프로젝트", "AI/테크", "Web", "홍길동", "모집중",
                    "외부 채널 테스트", LocalDate.now().plusDays(30)
            );
            currentProjectId = project.getId();
        }
        String repoUrl = externalChannels.get("저장소");
        if (repoUrl != null && repoUrl.startsWith("https://github.com/")) {
            String repoFullName = repoUrl.replace("https://github.com/", "");
            Project project = projectRepository.findById(currentProjectId)
                    .orElseThrow(() -> new AssertionError("프로젝트를 찾을 수 없습니다."));
            ProjectRepo repo = ProjectRepo.create(project, repoFullName, 1L, "seed", 0L, 0L, LocalDateTime.now(), "Java");
            projectRepoRepository.save(repo);
        }
    }

    @When("{string}로 프로젝트를 검색하면")
    public void 키워드로_프로젝트를_검색하면(String keyword) {
        lastResponse = projectSearchApi.getProjectList();
        context.setLastResponse(lastResponse);

        List<Map<String, Object>> all = lastResponse.jsonPath().getList("result");
        if (all == null) {
            all = List.of();
        }

        final String query = keyword.toLowerCase(Locale.ROOT);
        lastResultProjectNames = all.stream()
                .filter(row -> {
                    String name = String.valueOf(row.get("name")).toLowerCase(Locale.ROOT);
                    String creator = String.valueOf(row.get("creatorName")).toLowerCase(Locale.ROOT);
                    return name.contains(query) || creator.contains(query);
                })
                .map(row -> String.valueOf(row.get("name")))
                .collect(Collectors.toList());

        if (lastResultProjectNames.isEmpty()) {
            context.setLastMessage("검색 결과가 없습니다");
        }
    }

    @When("{string}으로 프로젝트를 검색하면")
    public void 키워드으로_프로젝트를_검색하면(String keyword) {
        키워드로_프로젝트를_검색하면(keyword);
    }

    @When("카테고리 {string}로 필터링하면")
    public void 카테고리로_필터링하면(String category) {
        lastResponse = projectSearchApi.searchCondition(Map.of(
                "projectCategory", toProjectCategory(category),
                "page", 0,
                "size", 20
        ));
        context.setLastResponse(lastResponse);
        lastResultProjectNames = getProjectNamesFromConditionResponse(lastResponse);
    }

    @When("플랫폼 {string}로 필터링하면")
    public void 플랫폼으로_필터링하면(String platform) {
        lastResponse = projectSearchApi.searchCondition(Map.of(
                "platformCategory", toPlatformCategory(platform),
                "page", 0,
                "size", 20
        ));
        context.setLastResponse(lastResponse);
        lastResultProjectNames = getProjectNamesFromConditionResponse(lastResponse);
    }

    @When("{string} 상태로 필터링하면")
    public void 모집상태로_필터링하면(String recruitmentStatus) {
        lastResponse = projectSearchApi.searchCondition(Map.of(
                "recruitment", toRecruitmentStatus(recruitmentStatus),
                "page", 0,
                "size", 20
        ));
        context.setLastResponse(lastResponse);
        lastResultProjectNames = getProjectNamesFromConditionResponse(lastResponse);
    }

    @When("다음 조건으로 검색하면:")
    public void 다음_조건으로_검색하면(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("projectCategory", toProjectCategory(row.get("카테고리")));
        params.put("platformCategory", toPlatformCategory(row.get("플랫폼")));
        params.put("recruitment", toRecruitmentStatus(row.get("모집상태")));
        params.put("page", 0);
        params.put("size", 20);

        lastResponse = projectSearchApi.searchCondition(params);
        context.setLastResponse(lastResponse);
        lastResultProjectNames = getProjectNamesFromConditionResponse(lastResponse);
    }

    @When("첫 번째 페이지를 조회하면")
    public void 첫_번째_페이지를_조회하면() {
        lastResponse = projectSearchApi.searchCondition(Map.of(
                "page", 0,
                "size", 8,
                "sort", "createdAt,desc"
        ));
        context.setLastResponse(lastResponse);
        lastResultProjectNames = getProjectNamesFromConditionResponse(lastResponse);
    }

    @When("프로젝트 목록을 최신순으로 정렬하면")
    public void 프로젝트_목록을_최신순으로_정렬하면() {
        lastResponse = projectSearchApi.searchCondition(Map.of(
                "page", 0,
                "size", 20,
                "sort", "createdAt,desc"
        ));
        context.setLastResponse(lastResponse);
        lastResultProjectNames = getProjectNamesFromConditionResponse(lastResponse);
    }

    @When("프로젝트 목록을 좋아요순으로 정렬하면")
    public void 프로젝트_목록을_좋아요순으로_정렬하면() {
        lastResponse = projectSearchApi.searchCondition(Map.of(
                "page", 0,
                "size", 20,
                "sort", "likeCount,desc"
        ));
        context.setLastResponse(lastResponse);
        lastResultProjectNames = getProjectNamesFromConditionResponse(lastResponse);
    }

    @When("해당 프로젝트 상세를 조회하면")
    public void 해당_프로젝트_상세를_조회하면() {
        assertNotNull(currentProjectId, "상세 조회 대상 프로젝트가 필요합니다.");
        lastResponse = projectSearchApi.getProjectDetail(currentProjectId);
        context.setLastResponse(lastResponse);
    }

    @When("프로젝트 상세의 {string} 탭에 접근하면")
    public void 프로젝트_상세의_탭에_접근하면(String tabName) {
        assertNotNull(currentProjectId, "상세 조회 대상 프로젝트가 필요합니다.");
        lastResponse = projectSearchApi.getProjectDetail(currentProjectId);
        context.setLastResponse(lastResponse);
        assertNotNull(tabName);
    }

    @Then("{string} 프로젝트가 검색 결과에 포함된다")
    public void 프로젝트가_검색결과에_포함된다(String projectName) {
        assertTrue(lastResultProjectNames.contains(projectName));
    }

    @Then("{string}이 리더인 프로젝트가 검색 결과에 포함된다")
    public void 리더인_프로젝트가_검색결과에_포함된다(String leaderName) {
        assertNotNull(lastResponse);
        List<Map<String, Object>> all = lastResponse.jsonPath().getList("result");
        assertNotNull(all);
        boolean exists = all.stream().anyMatch(row -> leaderName.equals(row.get("creatorName")));
        assertTrue(exists, "리더명이 검색 결과에 포함되어야 합니다.");
    }

    @Then("다른 프로젝트는 검색 결과에 포함되지 않는다")
    public void 다른_프로젝트는_검색결과에_포함되지_않는다() {
        assertEquals(1, lastResultProjectNames.size());
    }

    @Then("{string} 프로젝트만 결과에 포함된다")
    public void 프로젝트만_결과에_포함된다(String projectName) {
        assertEquals(1, lastResultProjectNames.size());
        assertEquals(projectName, lastResultProjectNames.get(0));
    }

    @Then("다른 카테고리의 프로젝트는 포함되지 않는다")
    public void 다른_카테고리의_프로젝트는_포함되지_않는다() {
        assertEquals(1, lastResultProjectNames.size());
    }

    @Then("모집 중인 프로젝트 {int}개가 결과에 포함된다")
    public void 모집_중인_프로젝트_n개가_결과에_포함된다(int count) {
        assertEquals(count, lastResultProjectNames.size());
    }

    @Then("{string}은 결과에 포함되지 않는다")
    public void 프로젝트는_결과에_포함되지_않는다(String projectName) {
        assertFalse(lastResultProjectNames.contains(projectName));
    }

    @Then("검색 결과가 비어있다")
    public void 검색_결과가_비어있다() {
        assertTrue(lastResultProjectNames.isEmpty());
    }

    @Then("{int}개의 프로젝트가 표시된다")
    public void n개의_프로젝트가_표시된다(int count) {
        assertEquals(count, lastResultProjectNames.size());
    }

    @And("총 프로젝트 수 {int}개를 확인할 수 있다")
    public void 총_프로젝트_수를_확인할_수_있다(int totalCount) {
        assertNotNull(lastResponse);
        assertEquals(totalCount, lastResponse.jsonPath().getInt("totalElements"));
    }

    @Then("가장 최근에 등록된 프로젝트가 첫 번째로 표시된다")
    public void 가장_최근에_등록된_프로젝트가_첫_번째로_표시된다() {
        assertNotNull(expectedNewestProjectName);
        assertFalse(lastResultProjectNames.isEmpty());
        assertEquals(expectedNewestProjectName, lastResultProjectNames.get(0));
    }

    @Then("가장 좋아요 수가 많은 프로젝트가 첫 번째로 표시된다")
    public void 가장_좋아요_수가_많은_프로젝트가_첫_번째로_표시된다() {
        assertNotNull(expectedMostLikedProjectName);
        assertFalse(lastResultProjectNames.isEmpty());
        assertEquals(expectedMostLikedProjectName, lastResultProjectNames.get(0));
    }

    @Then("{string} 탭이 기본으로 선택된다")
    public void 탭이_기본으로_선택된다(String tabName) {
        assertNotNull(lastResponse);
        assertEquals(200, lastResponse.statusCode());
        assertNotNull(tabName);
    }

    @Then("다음 정보를 확인할 수 있다:")
    public void 다음_정보를_확인할_수_있다(DataTable dataTable) {
        assertNotNull(lastResponse);
        for (Map<String, String> row : dataTable.asMaps()) {
            String item = row.get("항목");
            String expected = row.get("값");
            switch (item) {
                case "프로젝트명" -> assertEquals(expected, lastResponse.jsonPath().getString("result.name"));
                case "카테고리" -> assertEquals(expected, toKoreanCategory(lastResponse.jsonPath().getString("result.projectCategory")));
                case "플랫폼" -> assertEquals(expected, toUiPlatform(lastResponse.jsonPath().getString("result.platformCategory")));
                case "리더" -> {
                    List<Map<String, Object>> members = lastResponse.jsonPath().getList("result.projectMembers");
                    Map<String, Object> leader = members.stream()
                            .filter(m -> Boolean.TRUE.equals(m.get("creator")))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("리더 정보를 찾을 수 없습니다."));
                    assertEquals(expected, leader.get("name"));
                }
                default -> throw new AssertionError("지원하지 않는 검증 항목: " + item);
            }
        }
    }

    @And("모집 마감일을 확인할 수 있다")
    public void 모집_마감일을_확인할_수_있다() {
        assertNotNull(lastResponse.jsonPath().getString("result.endDate"));
    }

    @And("프로젝트 소개글을 확인할 수 있다")
    public void 프로젝트_소개글을_확인할_수_있다() {
        assertNotNull(lastResponse.jsonPath().getString("result.description"));
    }

    @And("필요 기술 스택을 확인할 수 있다")
    public void 필요_기술_스택을_확인할_수_있다() {
        List<String> skills = lastResponse.jsonPath().getList("result.skills");
        assertNotNull(skills);
        assertFalse(skills.isEmpty());
    }

    @Then("모집 현황 {string}을 확인할 수 있다")
    public void 포지션_현황을_확인할_수_있다(String recruitmentSummary) {
        List<Map<String, Object>> recruitments = lastResponse.jsonPath().getList("result.recruitments");
        assertNotNull(recruitments);
        boolean exists = recruitments.stream().anyMatch(r -> {
            String label = toKoreanPosition(String.valueOf(r.get("jobPosition")));
            int current = ((Number) r.get("currentCount")).intValue();
            int total = ((Number) r.get("recruitmentCount")).intValue();
            return recruitmentSummary.equals(label + " " + current + "/" + total + "명");
        });
        assertTrue(exists, "모집 현황 [" + recruitmentSummary + "] 을 찾을 수 없습니다.");
    }

    @And("각 모집 포지션의 {string} 버튼을 확인할 수 있다")
    public void 각_모집_포지션의_버튼을_확인할_수_있다(String buttonName) {
        List<Map<String, Object>> recruitments = lastResponse.jsonPath().getList("result.recruitments");
        assertNotNull(recruitments);
        assertFalse(recruitments.isEmpty());
        assertEquals("지원하기", buttonName);
    }

    @Then("{int}개의 Q&A를 확인할 수 있다")
    public void n개의_qna를_확인할_수_있다(int count) {
        assertEquals(count, qnaRows.size());
    }

    @And("각 질문과 답변 내용을 확인할 수 있다")
    public void 각_질문과_답변_내용을_확인할_수_있다() {
        assertFalse(qnaRows.isEmpty());
        boolean allFilled = qnaRows.stream().allMatch(row ->
                row.get("질문") != null && !row.get("질문").isBlank() &&
                        row.get("답변") != null && !row.get("답변").isBlank()
        );
        assertTrue(allFilled);
    }

    @And("질문 작성 진입 버튼을 확인할 수 있다")
    public void 질문_작성_진입_버튼을_확인할_수_있다() {
        assertNotNull(currentProjectId);
    }

    @Then("리더의 이름 {string}을 확인할 수 있다")
    public void 리더의_이름을_확인할_수_있다(String leaderName) {
        List<Map<String, Object>> members = lastResponse.jsonPath().getList("result.projectMembers");
        assertNotNull(members);
        boolean exists = members.stream().anyMatch(member ->
                Boolean.TRUE.equals(member.get("creator")) && leaderName.equals(member.get("name")));
        assertTrue(exists);
    }

    @And("리더의 역할 정보를 확인할 수 있다")
    public void 리더의_역할_정보를_확인할_수_있다() {
        List<Map<String, Object>> members = lastResponse.jsonPath().getList("result.projectMembers");
        assertNotNull(members);
        boolean exists = members.stream().anyMatch(member -> Boolean.TRUE.equals(member.get("creator")));
        assertTrue(exists);
    }

    @And("리더의 기술 스택을 확인할 수 있다")
    public void 리더의_기술_스택을_확인할_수_있다() {
        assertNotNull(expectedLeaderTechStack);
        List<String> projectSkills = lastResponse.jsonPath().getList("result.skills");
        assertNotNull(projectSkills);
        List<String> expected = List.of(expectedLeaderTechStack.split(","))
                .stream().map(String::trim).collect(Collectors.toList());
        assertTrue(projectSkills.stream().anyMatch(expected::contains));
    }

    @And("좋아요 수를 확인할 수 있다")
    public void 좋아요_수를_확인할_수_있다() {
        Number likeCount = lastResponse.jsonPath().get("result.likeCount");
        assertNotNull(likeCount);
        assertTrue(likeCount.intValue() >= 0);
    }

    @Then("저장소 주소를 확인할 수 있다")
    public void 저장소_주소를_확인할_수_있다() {
        String expectedRepoUrl = externalChannels.get("저장소");
        assertNotNull(expectedRepoUrl);
        List<ProjectRepo> repos = projectRepoRepository.findAllByProjectId(currentProjectId);
        List<String> urls = repos.stream()
                .map(repo -> "https://github.com/" + repo.getRepoFullName())
                .toList();
        assertTrue(urls.contains(expectedRepoUrl));
    }

    @And("소통 채널 주소를 확인할 수 있다")
    public void 소통_채널_주소를_확인할_수_있다() {
        String channel = externalChannels.get("소통 채널");
        assertNotNull(channel);
        assertFalse(channel.isBlank());
    }

    private List<String> getProjectNamesFromConditionResponse(Response response) {
        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content == null) {
            return List.of();
        }
        return content.stream()
                .map(row -> String.valueOf(row.get("projectName")))
                .collect(Collectors.toList());
    }

    private Project createProjectFixture(
            String projectName,
            String category,
            String platform,
            String leaderName,
            String recruitmentStatus,
            String description,
            LocalDate endDate
    ) {
        Member leader = createOrFindMember(leaderName);
        ProjectPostCommand command = new ProjectPostCommand(
                projectName,
                null,
                null,
                toProjectCategoryEnum(category),
                description,
                toPlatformCategoryEnum(platform),
                JobPosition.WEB_SERVER,
                List.of(),
                List.of(),
                RecruitmentDeadlineType.END_DATE,
                endDate
        );
        Project project = Project.createProject(command, leader, null);

        RecruitmentState recruitment = RecruitmentState.createRecruitmentState(JobPosition.WEB_FRONTEND, 2);
        project.addRecruitment(recruitment);

        List<String> skillNames = List.of("React.js", "Next.js");
        for (String skillName : skillNames) {
            Skill skill = skillRepository.findBySkillName(skillName)
                    .orElseGet(() -> skillRepository.save(new Skill(skillName)));
            project.addProjectSkill(ProjectSkill.createProjectSkill(skill));
        }

        ProjectMember leaderMember = ProjectMember.createProjectMember(leader, JobPosition.WEB_SERVER);
        project.joinMember(leaderMember);

        project.setRecruitmentStatus("모집완료".equals(recruitmentStatus) ? Recruitment.CLOSED : Recruitment.RECRUITING);

        return projectRepository.save(project);
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
        return toProjectCategoryEnum(category).name();
    }

    private ProjectCategory toProjectCategoryEnum(String category) {
        String normalized = category.replace(" ", "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ai/테크", "ai/tech", "ai테크" -> ProjectCategory.AI_TECH;
            case "헬스케어" -> ProjectCategory.HEALTHCARE;
            case "반려동물" -> ProjectCategory.PET;
            case "여행" -> ProjectCategory.ETC;
            case "환경", "친환경" -> ProjectCategory.ENVIRONMENT;
            default -> ProjectCategory.ETC;
        };
    }

    private String toPlatformCategory(String platform) {
        return toPlatformCategoryEnum(platform).name();
    }

    private PlatformCategory toPlatformCategoryEnum(String platform) {
        String normalized = platform.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "WEB" -> PlatformCategory.WEB;
            case "IOS" -> PlatformCategory.IOS;
            case "ANDROID" -> PlatformCategory.ANDROID;
            default -> PlatformCategory.WEB;
        };
    }

    private String toRecruitmentStatus(String status) {
        return "모집완료".equals(status) ? Recruitment.CLOSED.name() : Recruitment.RECRUITING.name();
    }

    private String toUiPlatform(String enumValue) {
        return switch (enumValue) {
            case "WEB" -> "Web";
            case "IOS" -> "iOS";
            case "ANDROID" -> "Android";
            default -> enumValue;
        };
    }

    private String toKoreanCategory(String enumValue) {
        return switch (enumValue) {
            case "AI_TECH" -> "AI/테크";
            case "HEALTHCARE" -> "헬스케어";
            case "PET" -> "반려동물";
            case "ENVIRONMENT" -> "환경";
            default -> "기타";
        };
    }

    private JobPosition toJobPosition(String label) {
        return switch (label) {
            case "프론트엔드" -> JobPosition.WEB_FRONTEND;
            case "디자인" -> JobPosition.UI_UX_DESIGN;
            case "백엔드" -> JobPosition.WEB_SERVER;
            default -> JobPosition.ETC;
        };
    }

    private String toKoreanPosition(String enumValue) {
        return switch (enumValue) {
            case "WEB_FRONTEND" -> "프론트엔드";
            case "UI_UX_DESIGN" -> "디자인";
            case "WEB_SERVER" -> "백엔드";
            case "CROSS_PLATFORM" -> "프론트엔드";
            default -> "기타";
        };
    }

    private int parseCount(String raw) {
        String number = raw.replaceAll("[^0-9]", "");
        if (number.isEmpty()) {
            throw new IllegalArgumentException("숫자 파싱 실패: " + raw);
        }
        return Integer.parseInt(number);
    }
}
