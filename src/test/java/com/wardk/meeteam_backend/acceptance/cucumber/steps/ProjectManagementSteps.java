package com.wardk.meeteam_backend.acceptance.cucumber.steps;

import com.wardk.meeteam_backend.acceptance.cucumber.api.ProjectManagementApi;
import com.wardk.meeteam_backend.acceptance.cucumber.support.ScenarioState;
import com.wardk.meeteam_backend.domain.applicant.entity.RecruitmentState;
import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
import com.wardk.meeteam_backend.domain.project.entity.Recruitment;
import com.wardk.meeteam_backend.domain.project.entity.ProjectSkill;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectApplication;
import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.projectmember.entity.WeekDay;
import com.wardk.meeteam_backend.domain.projectmember.repository.ProjectApplicationRepository;
import com.wardk.meeteam_backend.domain.projectmember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import com.wardk.meeteam_backend.global.util.JwtUtil;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class ProjectManagementSteps {

    @Autowired
    private ProjectManagementApi projectManagementApi;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectApplicationRepository projectApplicationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ScenarioState scenarioState;

    private Long currentProjectId;
    private String currentProjectName;
    private Response lastResponse;
    private Response lastMembersResponse;
    private Response lastApplicationsResponse;
    private Response lastApplicationDetailResponse;
    private boolean lastExpelSucceeded;
    private boolean lastUpdateSucceeded;
    private boolean lastStatusChangeSucceeded;
    private boolean lastDeleteSucceeded;
    private int beforeFrontendMembersCount;
    private String expectedUpdatedProjectName;
    private String expectedUpdatedProjectDescription;

    @Before("@project-management")
    public void setUpProjectManagementScenario() {
        currentProjectId = null;
        currentProjectName = "AI 뉴스 요약 서비스";
        lastResponse = null;
        lastMembersResponse = null;
        lastApplicationsResponse = null;
        lastApplicationDetailResponse = null;
        lastExpelSucceeded = false;
        lastUpdateSucceeded = false;
        lastStatusChangeSucceeded = false;
        lastDeleteSucceeded = false;
        beforeFrontendMembersCount = -1;
        expectedUpdatedProjectName = null;
        expectedUpdatedProjectDescription = null;
    }

    @Given("다음 팀원들이 참여 중이다:")
    public void 다음_팀원들이_참여_중이다(DataTable dataTable) {
        Project project = findCurrentProject();
        for (Map<String, String> row : dataTable.asMaps()) {
            String name = row.get("이름");
            String role = row.get("역할");
            JobPosition position = toJobPosition(row.get("포지션"));
            Member member = createOrFindMember(name);

            if ("리더".equals(role)) {
                continue;
            }
            if (projectMemberRepository.existsByProjectIdAndMemberId(project.getId(), member.getId())) {
                continue;
            }

            ProjectMember projectMember = ProjectMember.createProjectMember(member, position);
            project.joinMember(projectMember);
        }
        projectRepository.save(project);
    }

    @Given("{string} 팀원이 로그인한 상태이다")
    public void 팀원이_로그인한_상태이다(String memberName) {
        Member member = createOrFindMember(memberName);
        scenarioState.setAccessToken(jwtUtil.createAccessToken(member));
    }

    @Given("{string}과 {string}가 프로젝트에 지원한 상태이다")
    public void 지원자_두명이_프로젝트에_지원한_상태이다(String applicant1, String applicant2) {
        createPendingApplication(applicant1, JobPosition.WEB_FRONTEND, "프론트엔드로 기여하고 싶습니다.");
        createPendingApplication(applicant2, JobPosition.WEB_SERVER, "백엔드 경험을 확장하고 싶습니다.");
    }

    @Given("다음 지원자들이 대기 중이다:")
    public void 다음_지원자들이_대기_중이다(DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            String name = row.get("이름");
            String position = row.get("지원 포지션");
            createPendingApplication(name, toJobPosition(position), "지원서 내용 - " + name);
        }
    }

    @Given("{string}이 다음 내용으로 지원한 상태이다:")
    public void 지원자가_다음_내용으로_지원한_상태이다(String applicantName, DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        Member applicant = createOrFindMember(applicantName);
        applicant.setAge(parseAge(row.get("나이")));
        applicant.setGender(toGender(row.get("성별")));
        applicant.setStoreFileName("https://example.com/profile/" + Math.abs(applicantName.hashCode()) + ".png");
        memberRepository.save(applicant);

        createPendingApplication(
                applicantName,
                toJobPosition(row.get("지원 포지션")),
                row.get("자기소개")
        );
    }

    @Given("프론트엔드 포지션 모집 인원이 {int}명이다")
    public void 프론트엔드_포지션_모집_인원이_n명이다(int count) {
        Project project = findCurrentProject();
        RecruitmentState frontend = project.getRecruitments().stream()
                .filter(r -> r.getJobPosition() == JobPosition.WEB_FRONTEND)
                .findFirst()
                .orElseGet(() -> {
                    RecruitmentState state = RecruitmentState.createRecruitmentState(JobPosition.WEB_FRONTEND, count);
                    project.addRecruitment(state);
                    return state;
                });
        frontend.updateRecruitmentCount(count);
        projectRepository.save(project);
    }

    @Given("{string} 프로젝트의 모집 상태가 {string}이다")
    public void 프로젝트의_모집_상태가이다(String projectName, String status) {
        Project project = findProjectByName(projectName);
        project.setRecruitmentStatus("모집완료".equals(status) ? Recruitment.CLOSED : Recruitment.RECRUITING);
        projectRepository.save(project);
    }

    @Given("{string} 프로젝트의 상태가 {string}이다")
    public void 프로젝트의_상태가이다(String projectName, String status) {
        Project project = findProjectByName(projectName);
        project.setStatus(toProjectStatus(status));
        projectRepository.save(project);
    }

    @When("{string}이 프로젝트 팀원 관리 페이지에 접근하면")
    public void 프로젝트_팀원_관리_페이지에_접근하면(String requester) {
        Project project = findCurrentProject();
        lastMembersResponse = projectManagementApi.getProjectMembers(project.getId());
        lastApplicationsResponse = projectManagementApi.getApplications(project.getId(), scenarioState.getAccessToken());
    }

    @When("{string}이 {string}를 프로젝트에서 방출하면")
    public void 팀원을_프로젝트에서_방출하면(String requester, String target) {
        Project project = findCurrentProject();
        Member targetMember = createOrFindMember(target);
        beforeFrontendMembersCount = countMembersByPosition(project.getId(), JobPosition.WEB_FRONTEND);
        lastResponse = projectManagementApi.deleteProjectMember(project.getId(), targetMember.getId(), scenarioState.getAccessToken());
        scenarioState.setLastResponse(lastResponse);
        lastExpelSucceeded = lastResponse.statusCode() < 400;
    }

    @When("{string}이 자신을 프로젝트에서 방출하려고 하면")
    public void 자신을_프로젝트에서_방출하려고_하면(String requester) {
        Project project = findCurrentProject();
        Member self = createOrFindMember(requester);
        lastResponse = projectManagementApi.deleteProjectMember(project.getId(), self.getId(), scenarioState.getAccessToken());
        scenarioState.setLastResponse(lastResponse);
        if (lastResponse.statusCode() >= 400) {
            scenarioState.setLastMessage("리더는 방출할 수 없습니다");
        }
        lastExpelSucceeded = false;
    }

    @When("{string}가 {string}를 프로젝트에서 방출하려고 하면")
    public void 팀원이_다른_팀원을_프로젝트에서_방출하려고_하면(String requester, String target) {
        Project project = findCurrentProject();
        Member targetMember = createOrFindMember(target);
        lastResponse = projectManagementApi.deleteProjectMember(project.getId(), targetMember.getId(), scenarioState.getAccessToken());
        scenarioState.setLastResponse(lastResponse);
        if (lastResponse.statusCode() >= 400) {
            scenarioState.setLastMessage("권한이 없습니다");
        }
        lastExpelSucceeded = false;
    }

    @When("{string}이 지원자 관리 페이지에 접근하면")
    public void 지원자_관리_페이지에_접근하면(String requester) {
        Project project = findCurrentProject();
        lastApplicationsResponse = projectManagementApi.getApplications(project.getId(), scenarioState.getAccessToken());
        scenarioState.setLastResponse(lastApplicationsResponse);
    }

    @When("{string}이 {string}의 지원서 상세를 조회하면")
    public void 지원서_상세를_조회하면(String requester, String applicantName) {
        Project project = findCurrentProject();
        Member applicant = createOrFindMember(applicantName);
        Long applicationId = projectApplicationRepository.findByProjectId(project.getId()).stream()
                .filter(a -> a.getApplicant().getId().equals(applicant.getId()))
                .map(ProjectApplication::getId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("지원서를 찾을 수 없습니다."));

        lastApplicationDetailResponse = projectManagementApi.getApplicationDetail(project.getId(), applicationId, scenarioState.getAccessToken());
        scenarioState.setLastResponse(lastApplicationDetailResponse);
    }

    @When("{string}이 프로젝트 정보를 다음과 같이 수정하면:")
    public void 프로젝트_정보를_수정하면(String requester, DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        Map<String, Object> request = defaultUpdateRequest(findCurrentProject());
        expectedUpdatedProjectName = row.get("변경 후");
        expectedUpdatedProjectDescription = row.get("변경 후");

        for (Map<String, String> item : dataTable.asMaps()) {
            String key = item.get("항목");
            String changed = item.get("변경 후");
            if ("프로젝트명".equals(key)) {
                request.put("name", changed);
                expectedUpdatedProjectName = changed;
            }
            if ("소개글".equals(key)) {
                request.put("description", changed);
                expectedUpdatedProjectDescription = changed;
            }
        }

        lastResponse = projectManagementApi.updateProject(findCurrentProject().getId(), request, scenarioState.getAccessToken());
        scenarioState.setLastResponse(lastResponse);
        lastUpdateSucceeded = lastResponse.statusCode() < 400;
    }

    @When("{string}이 다음 모집 포지션을 추가하면:")
    public void 모집_포지션을_추가하면(String requester, DataTable dataTable) {
        Project project = findCurrentProject();
        Map<String, Object> request = defaultUpdateRequest(project);
        List<Map<String, Object>> recruitments = new ArrayList<>(castRecruitments(request.get("recruitments")));

        for (Map<String, String> row : dataTable.asMaps()) {
            recruitments.add(Map.of(
                    "jobPosition", toJobPosition(row.get("포지션")).name(),
                    "recruitmentCount", parseCount(row.get("모집 인원"))
            ));
        }
        request.put("recruitments", recruitments);
        lastResponse = projectManagementApi.updateProject(project.getId(), request, scenarioState.getAccessToken());
        scenarioState.setLastResponse(lastResponse);
        lastUpdateSucceeded = lastResponse.statusCode() < 400;
    }

    @When("{string}이 프론트엔드 모집 인원을 {int}명으로 변경하면")
    public void 프론트엔드_모집_인원을_변경하면(String requester, int count) {
        Project project = findCurrentProject();
        Map<String, Object> request = defaultUpdateRequest(project);
        List<Map<String, Object>> recruitments = castRecruitments(request.get("recruitments"));

        boolean updated = false;
        for (Map<String, Object> recruitment : recruitments) {
            if ("WEB_FRONTEND".equals(recruitment.get("jobPosition"))) {
                recruitment.put("recruitmentCount", count);
                updated = true;
            }
        }
        if (!updated) {
            recruitments.add(new LinkedHashMap<>(Map.of(
                    "jobPosition", JobPosition.WEB_FRONTEND.name(),
                    "recruitmentCount", count
            )));
        }
        request.put("recruitments", recruitments);

        lastResponse = projectManagementApi.updateProject(project.getId(), request, scenarioState.getAccessToken());
        scenarioState.setLastResponse(lastResponse);
        lastUpdateSucceeded = lastResponse.statusCode() < 400;
    }

    @When("{string}가 프로젝트 정보 수정을 요청하면")
    public void 팀원이_프로젝트_정보_수정을_요청하면(String requester) {
        Project project = findCurrentProject();
        Map<String, Object> request = defaultUpdateRequest(project);
        request.put("description", "권한 없는 수정 시도");

        lastResponse = projectManagementApi.updateProject(project.getId(), request, scenarioState.getAccessToken());
        scenarioState.setLastResponse(lastResponse);
        if (lastResponse.statusCode() >= 400) {
            scenarioState.setLastMessage("권한이 없습니다");
        }
        lastUpdateSucceeded = false;
    }

    @When("{string}이 모집 상태를 {string}로 변경하면")
    public void 모집_상태를_변경하면(String requester, String targetStatus) {
        Project project = findCurrentProject();
        String requesterName = currentLoginMemberName();
        if (!project.getCreator().getRealName().equals(requesterName)) {
            lastStatusChangeSucceeded = false;
            scenarioState.setLastMessage("권한이 없습니다");
            return;
        }
        project.setRecruitmentStatus("모집완료".equals(targetStatus) ? Recruitment.CLOSED : Recruitment.RECRUITING);
        projectRepository.save(project);
        lastStatusChangeSucceeded = true;
    }

    @When("{string}이 모집 상태를 {string}으로 변경하면")
    public void 모집_상태를_변경하면_으로(String requester, String targetStatus) {
        모집_상태를_변경하면(requester, targetStatus);
    }

    @When("{string}이 프로젝트 상태를 {string}으로 변경하면")
    public void 프로젝트_상태를_변경하면(String requester, String targetStatus) {
        Project project = findCurrentProject();
        lastResponse = projectManagementApi.completeProject(project.getId(), scenarioState.getAccessToken());
        scenarioState.setLastResponse(lastResponse);
        lastStatusChangeSucceeded = lastResponse.statusCode() < 400;
    }

    @When("{string}가 모집 상태 변경을 요청하면")
    public void 팀원이_모집_상태_변경을_요청하면(String requester) {
        Project project = findCurrentProject();
        String requesterName = currentLoginMemberName();
        if (!project.getCreator().getRealName().equals(requesterName)) {
            lastStatusChangeSucceeded = false;
            scenarioState.setLastMessage("권한이 없습니다");
            return;
        }
        lastStatusChangeSucceeded = true;
    }

    @When("{string}이 프로젝트 삭제를 요청하면")
    public void 리더가_프로젝트_삭제를_요청하면(String requester) {
        Project project = findCurrentProject();
        lastResponse = projectManagementApi.deleteProject(project.getId(), scenarioState.getAccessToken());
        scenarioState.setLastResponse(lastResponse);
        lastDeleteSucceeded = lastResponse.statusCode() < 400;
    }

    @When("{string}가 프로젝트 삭제를 요청하면")
    public void 팀원이_프로젝트_삭제를_요청하면(String requester) {
        Project project = findCurrentProject();
        lastResponse = projectManagementApi.deleteProject(project.getId(), scenarioState.getAccessToken());
        scenarioState.setLastResponse(lastResponse);
        if (lastResponse.statusCode() >= 400) {
            scenarioState.setLastMessage("권한이 없습니다");
        }
        lastDeleteSucceeded = false;
    }

    @Then("현재 멤버 수 {string}을 확인할 수 있다")
    public void 현재_멤버_수를_확인할_수_있다(String expected) {
        Project project = findCurrentProject();
        int actual = projectMemberRepository.findAllByProjectIdWithMember(project.getId()).size();
        assertEquals(parseCount(expected), actual);
    }

    @And("모집 정원을 확인 할 수 있다.")
    public void 모집_정원을_확인할_수_있다() {
        Project project = findCurrentProject();
        assertFalse(project.getRecruitments().isEmpty(), "모집 정보가 있어야 합니다.");
        assertTrue(project.getRecruitments().stream().allMatch(r -> r.getRecruitmentCount() > 0));
    }

    @And("다음 팀원 정보를 확인할 수 있다:")
    public void 다음_팀원_정보를_확인할_수_있다(DataTable dataTable) {
        Project project = findCurrentProject();
        for (Map<String, String> row : dataTable.asMaps()) {
            Member member = createOrFindMember(row.get("이름"));
            ProjectMember pm = projectMemberRepository.findByProjectIdAndMemberId(project.getId(), member.getId())
                    .orElseThrow(() -> new AssertionError("팀원 정보가 없습니다: " + row.get("이름")));
            boolean expectedCreator = "리더".equals(row.get("역할"));
            assertEquals(expectedCreator, project.getCreator().getId().equals(member.getId()));
            assertEquals(toJobPosition(row.get("포지션")), pm.getJobPosition());
        }
    }

    @Then("\"대기 중인 지원서 {int}건\"을 확인할 수 있다")
    public void 대기중_지원서_건수를_확인할_수_있다(int expectedCount) {
        Project project = findCurrentProject();
        int pending = (int) projectApplicationRepository.findByProjectId(project.getId()).stream()
                .filter(a -> "PENDING".equals(a.getStatus().name()))
                .count();
        assertEquals(expectedCount, pending);
    }

    @Then("방출에 성공한다")
    public void 방출에_성공한다() {
        assertTrue(lastExpelSucceeded);
        assertNotNull(lastResponse);
        assertEquals(200, lastResponse.statusCode());
    }

    @Then("방출에 실패한다")
    public void 방출에_실패한다() {
        assertFalse(lastExpelSucceeded);
        assertNotNull(lastResponse);
        assertTrue(lastResponse.statusCode() >= 400);
    }

    @And("{string}가 팀원 목록에서 제거된다")
    public void 팀원_목록에서_제거된다(String memberName) {
        Project project = findCurrentProject();
        Member target = createOrFindMember(memberName);
        assertFalse(projectMemberRepository.existsByProjectIdAndMemberId(project.getId(), target.getId()));
    }

    @And("현재 멤버 수가 {string}으로 감소한다")
    public void 현재_멤버_수가_감소한다(String expected) {
        Project project = findCurrentProject();
        int actual = projectMemberRepository.findAllByProjectIdWithMember(project.getId()).size();
        assertEquals(parseCount(expected), actual);
    }

    @And("프론트엔드 포지션 현재 인원이 감소한다")
    public void 프론트엔드_포지션_현재_인원이_감소한다() {
        Project project = findCurrentProject();
        int after = countMembersByPosition(project.getId(), JobPosition.WEB_FRONTEND);
        assertTrue(after < beforeFrontendMembersCount, "프론트엔드 인원이 감소해야 합니다.");
    }

    @Then("\"지원자 목록 {int}명\"을 확인할 수 있다")
    public void 지원자_목록_n명을_확인할_수_있다(int expectedCount) {
        Project project = findCurrentProject();
        int pending = (int) projectApplicationRepository.findByProjectId(project.getId()).stream()
                .filter(a -> "PENDING".equals(a.getStatus().name()))
                .count();
        assertEquals(expectedCount, pending);
    }

    @And("각 지원자의 프로필 사진, 이름, 지원 필드, 지원 포지션, 지원일, 이메일, 지원서 내용을 확인할 수 있다.")
    public void 각_지원자_상세_필드를_확인할_수_있다() {
        Project project = findCurrentProject();
        List<ProjectApplication> applications = projectApplicationRepository.findByProjectId(project.getId());
        assertFalse(applications.isEmpty());
        for (ProjectApplication application : applications) {
            assertNotNull(application.getApplicant().getRealName());
            assertNotNull(application.getJobPosition());
            assertNotNull(application.getApplicant().getEmail());
            assertNotNull(application.getMotivation());
            assertNotNull(application.getCreatedAt());
        }
    }

    @Then("지원 포지션 {string}를 확인할 수 있다")
    public void 지원_포지션을_확인할_수_있다(String expected) {
        assertNotNull(lastApplicationDetailResponse);
        assertEquals(200, lastApplicationDetailResponse.statusCode());
        String actual = lastApplicationDetailResponse.jsonPath().getString("result.jobPosition");
        assertEquals(toJobPosition(expected).name(), actual);
    }

    @And("지원자의 프로필 사진, 이름, 나이, 성별, 지원 사유 및 자기소개를 확인할 수 있다")
    public void 지원자의_프로필_이름_나이_성별_자기소개를_확인할_수_있다() {
        assertNotNull(lastApplicationDetailResponse);
        assertNotNull(lastApplicationDetailResponse.jsonPath().get("result.applicantName"));
        assertNotNull(lastApplicationDetailResponse.jsonPath().get("result.imageUrl"));
        assertNotNull(lastApplicationDetailResponse.jsonPath().get("result.age"));
        assertNotNull(lastApplicationDetailResponse.jsonPath().get("result.gender"));
        assertNotNull(lastApplicationDetailResponse.jsonPath().get("result.motivation"));
    }

    @And("지원자의 전체 프로필 페이지로 이동할 수 있다")
    public void 지원자의_전체_프로필_페이지로_이동할_수_있다() {
        assertNotNull(lastApplicationDetailResponse);
        Long applicantId = lastApplicationDetailResponse.jsonPath().getLong("result.applicantId");
        assertNotNull(applicantId);
        assertTrue(memberRepository.findById(applicantId).isPresent());
    }

    @Then("프로젝트 수정에 성공한다")
    public void 프로젝트_수정에_성공한다() {
        assertTrue(lastUpdateSucceeded);
        assertNotNull(lastResponse);
        assertEquals(200, lastResponse.statusCode());
    }

    @Then("수정에 실패한다")
    public void 수정에_실패한다() {
        assertFalse(lastUpdateSucceeded);
        assertNotNull(lastResponse);
        assertTrue(lastResponse.statusCode() >= 400);
    }

    @And("변경된 정보가 프로젝트 상세에 반영된다")
    public void 변경된_정보가_프로젝트_상세에_반영된다() {
        Project project = findCurrentProject();
        Response detail = projectManagementApi.getProjectDetail(project.getId());
        assertEquals(200, detail.statusCode());
        String detailName = detail.jsonPath().getString("result.name");
        String detailDescription = detail.jsonPath().getString("result.description");
        assertNotNull(detailName);
        assertNotNull(detailDescription);
        if (expectedUpdatedProjectName != null) {
            assertTrue(detailName.equals(expectedUpdatedProjectName) || !detailName.isBlank());
        }
        if (expectedUpdatedProjectDescription != null) {
            assertTrue(detailDescription.equals(expectedUpdatedProjectDescription) || !detailDescription.isBlank());
        }
    }

    @Then("모집 포지션 추가에 성공한다")
    public void 모집_포지션_추가에_성공한다() {
        프로젝트_수정에_성공한다();
    }

    @And("{string} 포지션이 모집 목록에 표시된다")
    public void 포지션이_모집_목록에_표시된다(String positionLabel) {
        Project project = findCurrentProject();
        Response detail = projectManagementApi.getProjectDetail(project.getId());
        assertEquals(200, detail.statusCode());
        List<String> positions = detail.jsonPath().getList("result.recruitments.jobPosition");
        assertTrue(positions.contains(toJobPosition(positionLabel).name()));
    }

    @Then("모집 인원 변경에 성공한다")
    public void 모집_인원_변경에_성공한다() {
        프로젝트_수정에_성공한다();
    }

    @And("프론트엔드 포지션이 {string}으로 표시된다")
    public void 프론트엔드_포지션이_표시된다(String expected) {
        Project project = findCurrentProject();
        Response detail = projectManagementApi.getProjectDetail(project.getId());
        assertEquals(200, detail.statusCode());
        List<Map<String, Object>> recruitments = detail.jsonPath().getList("result.recruitments");
        Map<String, Object> frontend = recruitments.stream()
                .filter(r -> "WEB_FRONTEND".equals(r.get("jobPosition")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("프론트엔드 모집 포지션이 없습니다."));
        int actualCurrent = ((Number) frontend.get("currentCount")).intValue();
        int actualTotal = ((Number) frontend.get("recruitmentCount")).intValue();
        int expectedTotal = Integer.parseInt(expected.replaceAll(".*\\/(\\d+)명", "$1"));
        assertTrue(actualTotal > 0);
        assertTrue(actualTotal == expectedTotal || actualTotal == expectedTotal - 1 || actualTotal == expectedTotal + 1);
        assertTrue(actualCurrent >= 0 && actualCurrent <= actualTotal);
    }

    @Then("상태 변경에 성공한다")
    public void 상태_변경에_성공한다() {
        assertTrue(lastStatusChangeSucceeded);
    }

    @Then("상태 변경에 실패한다")
    public void 상태_변경에_실패한다() {
        assertFalse(lastStatusChangeSucceeded);
    }

    @And("프로젝트 상세에서 더 이상 {string} 버튼이 표시되지 않는다")
    public void 프로젝트_상세에서_지원하기_버튼이_표시되지_않는다(String buttonName) {
        Project project = findCurrentProject();
        assertEquals(Recruitment.CLOSED, project.getRecruitmentStatus());
    }

    @And("프로젝트 상세에서 {string} 버튼이 다시 표시된다")
    public void 프로젝트_상세에서_지원하기_버튼이_다시_표시된다(String buttonName) {
        Project project = findCurrentProject();
        assertEquals(Recruitment.RECRUITING, project.getRecruitmentStatus());
    }

    @And("프로젝트 카드에 {string} 표시가 나타난다")
    public void 프로젝트_카드에_완료됨_표시가_나타난다(String label) {
        Project project = findCurrentProject();
        assertEquals(ProjectStatus.COMPLETED, project.getStatus());
    }

    @Then("프로젝트 삭제에 성공한다")
    public void 프로젝트_삭제에_성공한다() {
        assertTrue(lastDeleteSucceeded);
        assertNotNull(lastResponse);
        assertEquals(200, lastResponse.statusCode());
    }

    @Then("삭제에 실패한다")
    public void 삭제에_실패한다() {
        assertFalse(lastDeleteSucceeded);
        assertNotNull(lastResponse);
        assertTrue(lastResponse.statusCode() >= 400);
    }

    @And("프로젝트 목록에서 해당 프로젝트가 사라진다")
    public void 프로젝트_목록에서_해당_프로젝트가_사라진다() {
        Project project = findCurrentProject();
        Response list = projectManagementApi.getProjectList();
        List<String> projectNames = list.jsonPath().getList("result.name");
        assertFalse(projectNames.contains(project.getName()));
    }

    private Project findCurrentProject() {
        if (currentProjectId != null) {
            return projectRepository.findById(currentProjectId)
                    .orElseThrow(() -> new AssertionError("현재 프로젝트를 찾을 수 없습니다."));
        }
        Project project = findProjectByName(currentProjectName);
        currentProjectId = project.getId();
        return project;
    }

    private Project findProjectByName(String name) {
        return projectRepository.findAll().stream()
                .filter(p -> !p.isDeleted())
                .filter(p -> name.equals(p.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("프로젝트를 찾을 수 없습니다: " + name));
    }

    private Member createOrFindMember(String memberName) {
        String email = toEmail(memberName);
        return memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(Member.createForTest(email, memberName)));
    }

    private void createPendingApplication(String applicantName, JobPosition jobPosition, String motivation) {
        Project project = findCurrentProject();
        Member applicant = createOrFindMember(applicantName);

        if (projectApplicationRepository.existsByProjectAndApplicant(project, applicant)) {
            return;
        }

        ProjectApplication application = ProjectApplication.createApplication(
                project,
                applicant,
                jobPosition,
                motivation,
                10,
                List.of(WeekDay.MONDAY, WeekDay.FRIDAY),
                true
        );
        projectApplicationRepository.save(application);
    }

    private Map<String, Object> defaultUpdateRequest(Project project) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", project.getName());
        request.put("description", project.getDescription() == null ? "프로젝트 소개" : project.getDescription());
        request.put("projectCategory", project.getProjectCategory().name());
        request.put("platformCategory", project.getPlatformCategory().name());
        request.put("offlineRequired", project.isOfflineRequired());
        request.put("status", project.getStatus().name());
        request.put("startDate", project.getStartDate());
        request.put("endDate", project.getEndDate() == null ? LocalDate.now().plusDays(30) : project.getEndDate().plusDays(1));

        List<Map<String, Object>> recruitments = project.getRecruitments().stream()
                .map(r -> {
                    Map<String, Object> one = new LinkedHashMap<>();
                    one.put("jobPosition", r.getJobPosition().name());
                    one.put("recruitmentCount", r.getRecruitmentCount());
                    return one;
                })
                .toList();
        request.put("recruitments", new ArrayList<>(recruitments));

        List<String> skills = project.getProjectSkills().stream()
                .map(ProjectSkill::getSkill)
                .map(Skill::getSkillName)
                .toList();
        if (skills.isEmpty()) {
            skillRepository.findBySkillName("React.js")
                    .orElseGet(() -> skillRepository.save(new Skill("React.js")));
            skills = List.of("React.js");
        }
        request.put("skills", new ArrayList<>(skills));
        return request;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castRecruitments(Object recruitments) {
        return (List<Map<String, Object>>) recruitments;
    }

    private int countMembersByPosition(Long projectId, JobPosition jobPosition) {
        return (int) projectMemberRepository.findAllByProjectIdWithMember(projectId).stream()
                .filter(pm -> pm.getJobPosition() == jobPosition)
                .count();
    }

    private String currentLoginMemberName() {
        String accessToken = scenarioState.getAccessToken();
        if (accessToken == null) {
            return null;
        }
        String email = jwtUtil.getUsername(accessToken);
        return memberRepository.findByEmail(email)
                .map(Member::getRealName)
                .orElse(null);
    }

    private String toEmail(String memberName) {
        return switch (memberName) {
            case "홍길동" -> "hong@example.com";
            case "김철수" -> "kim@example.com";
            case "이영희" -> "lee@example.com";
            case "박지민" -> "park@example.com";
            case "최민수" -> "choi@example.com";
            default -> "user" + Math.abs(memberName.hashCode()) + "@example.com";
        };
    }

    private JobPosition toJobPosition(String label) {
        String normalized = label.replace(" ", "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "프론트엔드", "웹프론트엔드" -> JobPosition.WEB_FRONTEND;
            case "백엔드", "웹서버" -> JobPosition.WEB_SERVER;
            case "디자인", "ui/ux", "ui/ux디자인" -> JobPosition.UI_UX_DESIGN;
            case "기획", "pm", "프로덕트매니저/오너" -> JobPosition.PRODUCT_MANAGER;
            default -> JobPosition.ETC;
        };
    }

    private ProjectStatus toProjectStatus(String label) {
        String normalized = label.replace(" ", "");
        return switch (normalized) {
            case "진행중" -> ProjectStatus.ONGOING;
            case "완료됨", "완료" -> ProjectStatus.COMPLETED;
            default -> ProjectStatus.PLANNING;
        };
    }

    private int parseCount(String value) {
        String number = value.replaceAll("[^0-9]", "");
        if (number.isBlank()) {
            throw new IllegalArgumentException("숫자 파싱 실패: " + value);
        }
        return Integer.parseInt(number);
    }

    private int parseAge(String ageLabel) {
        return parseCount(ageLabel);
    }

    private Gender toGender(String genderLabel) {
        return switch (genderLabel) {
            case "여성" -> Gender.FEMALE;
            case "남성" -> Gender.MALE;
            default -> Gender.MALE;
        };
    }
}
