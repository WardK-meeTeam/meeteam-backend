package com.wardk.meeteam_backend.acceptance.cucumber.steps;

import com.wardk.meeteam_backend.acceptance.cucumber.api.ProjectLikeApi;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestContext;
import com.wardk.meeteam_backend.domain.applicant.entity.RecruitmentState;
import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import com.wardk.meeteam_backend.domain.project.entity.RecruitmentDeadlineType;
import com.wardk.meeteam_backend.domain.projectlike.entity.ProjectLike;
import com.wardk.meeteam_backend.domain.projectlike.repository.ProjectLikeRepository;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class ProjectLikeSteps {

    @Autowired
    private ProjectLikeApi projectLikeApi;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectLikeRepository projectLikeRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TestContext context;

    @PersistenceContext
    private EntityManager entityManager;

    private Long currentProjectId;
    private String currentProjectName;
    private String currentMemberName;
    private Integer currentExpectedLikeCount;
    private Integer baseLikeCount;
    private Boolean currentMemberLiked;
    private List<Member> concurrentMembers = new ArrayList<>();
    private List<Response> concurrentResponses = new ArrayList<>();
    private ConcurrentLinkedQueue<Throwable> concurrentErrors = new ConcurrentLinkedQueue<>();

    @Before("@like")
    public void setUpLikeScenario() {
        currentProjectId = null;
        currentProjectName = null;
        currentMemberName = null;
        currentExpectedLikeCount = null;
        baseLikeCount = null;
        currentMemberLiked = null;
        concurrentMembers = new ArrayList<>();
        concurrentResponses = new ArrayList<>();
        concurrentErrors = new ConcurrentLinkedQueue<>();
    }

    @Given("{string} 프로젝트의 좋아요 수가 {int}개이다")
    public void 프로젝트의_좋아요_수가_n개이다(String projectName, int likeCount) {
        currentProjectName = projectName;
        currentExpectedLikeCount = likeCount;
        baseLikeCount = likeCount;
        Project project = createOrReplaceProjectWithLikes(projectName, likeCount);
        if (currentMemberName != null && Boolean.TRUE.equals(currentMemberLiked) && likeCount > 0) {
            Member member = createOrFindMember(currentMemberName);
            resetProjectLikes(project, likeCount, member.getId());
        }
        currentProjectId = project.getId();
    }

    @Given("{string} 회원이 {string} 프로젝트에 좋아요를 누른 상태이다")
    public void 회원이_프로젝트에_좋아요를_누른_상태이다(String memberName, String projectName) {
        currentMemberName = memberName;
        currentProjectName = projectName;
        if (currentProjectId == null) {
            currentProjectId = createOrReplaceProjectWithLikes(projectName, 0).getId();
        }
        Member member = createOrFindMember(memberName);
        Project project = projectRepository.findById(currentProjectId)
                .orElseThrow(() -> new AssertionError("프로젝트를 찾을 수 없습니다."));
        int target = currentExpectedLikeCount != null ? currentExpectedLikeCount : Math.max(project.getLikeCount(), 1);
        resetProjectLikes(project, target, member.getId());
        currentMemberLiked = true;

        String token = jwtUtil.createAccessToken(member);
        context.setAccessToken(token);
    }

    @Given("좋아요를 누르지 않은 회원 {int}명이 존재한다")
    public void 좋아요를_누르지_않은_회원_n명이_존재한다(int count) {
        concurrentMembers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String name = "동시회원" + i;
            concurrentMembers.add(createOrFindMember(name));
        }
    }

    @And("{string}의 좋아요 버튼 상태가 {string}이다")
    public void 회원의_좋아요_버튼_상태가이다(String memberName, String expectedStatus) {
        currentMemberName = memberName;
        ensureProjectExists();

        Member member = createOrFindMember(memberName);
        String token = jwtUtil.createAccessToken(member);
        context.setAccessToken(token);

        Response status = projectLikeApi.likeStatus(currentProjectId, token);

        assertEquals(200, status.statusCode());
        boolean isLiked = extractLikeStatus(status);
        if ("선택".equals(expectedStatus)) {
            assertTrue(isLiked);
            currentMemberLiked = true;
        } else if ("미선택".equals(expectedStatus)) {
            assertFalse(isLiked);
            currentMemberLiked = false;
        } else {
            fail("지원하지 않는 좋아요 버튼 상태: " + expectedStatus);
        }
    }

    @When("{string}가 해당 프로젝트에 좋아요를 누르면")
    public void 회원이_해당_프로젝트에_좋아요를_누르면(String memberName) {
        currentMemberName = memberName;
        ensureProjectExists();
        String token = context.getAccessToken();
        if (token == null) {
            Member member = createOrFindMember(memberName);
            token = jwtUtil.createAccessToken(member);
            context.setAccessToken(token);
        }

        Response response = projectLikeApi.toggleLike(currentProjectId, token);
        context.setLastResponse(response);
        Boolean liked = extractToggleLiked(response);
        currentMemberLiked = liked;
    }

    @When("{string}가 같은 좋아요 버튼을 다시 누르면")
    public void 회원이_같은_좋아요_버튼을_다시_누르면(String memberName) {
        회원이_해당_프로젝트에_좋아요를_누르면(memberName);
    }

    @When("프로젝트에 좋아요를 누르면")
    public void 프로젝트에_좋아요를_누르면() {
        ensureProjectExists();
        Response response = projectLikeApi.toggleLike(currentProjectId, context.getAccessToken());
        context.setLastResponse(response);
    }

    @When("{string}가 같은 프로젝트의 좋아요 버튼을 동시에 {int}번 누르면")
    public void 회원이_같은_프로젝트의_좋아요_버튼을_동시에_누르면(String memberName, int times) throws Exception {
        currentMemberName = memberName;
        ensureProjectExists();
        Member member = createOrFindMember(memberName);
        String token = jwtUtil.createAccessToken(member);
        context.setAccessToken(token);

        concurrentResponses = new ArrayList<>();
        concurrentErrors = new ConcurrentLinkedQueue<>();

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(times, 8));
        try {
            List<Future<Response>> futures = new ArrayList<>();
            for (int i = 0; i < times; i++) {
                futures.add(pool.submit(() -> {
                    try {
                        return projectLikeApi.toggleLike(currentProjectId, token);
                    } catch (Throwable t) {
                        concurrentErrors.add(t);
                        throw t;
                    }
                }));
            }
            for (Future<Response> future : futures) {
                concurrentResponses.add(future.get(10, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        if (!concurrentResponses.isEmpty()) {
            context.setLastResponse(concurrentResponses.get(concurrentResponses.size() - 1));
        }
    }

    @When("{int}명의 회원이 동시에 좋아요 버튼을 누르면")
    public void 여러_회원이_동시에_좋아요_버튼을_누르면(int count) throws Exception {
        ensureProjectExists();
        assertEquals(count, concurrentMembers.size(), "준비된 회원 수와 요청 수가 일치해야 합니다.");

        concurrentResponses = new ArrayList<>();
        concurrentErrors = new ConcurrentLinkedQueue<>();

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(count, 24));
        try {
            List<Future<Response>> futures = new ArrayList<>();
            for (Member member : concurrentMembers) {
                String token = jwtUtil.createAccessToken(member);
                futures.add(pool.submit(() -> {
                    try {
                        return projectLikeApi.toggleLike(currentProjectId, token);
                    } catch (Throwable t) {
                        concurrentErrors.add(t);
                        throw t;
                    }
                }));
            }
            for (Future<Response> future : futures) {
                concurrentResponses.add(future.get(15, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        if (!concurrentResponses.isEmpty()) {
            context.setLastResponse(concurrentResponses.get(concurrentResponses.size() - 1));
        }
    }

    @Then("좋아요 등록에 성공한다")
    public void 좋아요_등록에_성공한다() {
        Response response = context.getLastResponse();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertEquals("COMMON200", response.jsonPath().getString("code"));
        Boolean liked = extractToggleLiked(response);
        assertTrue(liked);
    }

    @Then("좋아요 취소에 성공한다")
    public void 좋아요_취소에_성공한다() {
        Response response = context.getLastResponse();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertEquals("COMMON200", response.jsonPath().getString("code"));
        Boolean liked = extractToggleLiked(response);
        assertFalse(liked);
    }

    @And("{string}의 좋아요 버튼 상태가 {string}으로 변경된다")
    public void 회원의_좋아요_버튼_상태가_변경된다(String memberName, String expectedStatus) {
        회원의_좋아요_버튼_상태가이다(memberName, expectedStatus);
    }

    @And("좋아요 수가 {int}개로 증가한다")
    public void 좋아요_수가_n개로_증가한다(int expectedLikeCount) {
        Response response = context.getLastResponse();
        assertNotNull(response);
        assertEquals(expectedLikeCount, response.jsonPath().getInt("result.likeCount"));
        assertEquals(expectedLikeCount, currentLikeCountFromDetail());
    }

    @And("좋아요 수가 {int}개로 감소한다")
    public void 좋아요_수가_n개로_감소한다(int expectedLikeCount) {
        좋아요_수가_n개로_증가한다(expectedLikeCount);
    }

    @Then("{string}의 최종 좋아요 상태는 1회 토글 결과와 동일하다")
    public void 회원의_최종_좋아요_상태는_1회_토글_결과와_동일하다(String memberName) {
        ensureProjectExists();
        Member member = createOrFindMember(memberName);
        String token = jwtUtil.createAccessToken(member);
        Response status = projectLikeApi.likeStatus(currentProjectId, token);
        assertEquals(200, status.statusCode());
        boolean liked = extractLikeStatus(status);

        int likeCount = currentLikeCountFromDetail();
        int baseline = baseLikeCount != null ? baseLikeCount : 10;
        if (liked) {
            assertEquals(baseline + 1, likeCount);
        } else {
            assertEquals(baseline, likeCount);
        }
    }

    @And("좋아요 수는 {int}개 또는 {int}개 중 하나의 일관된 값이어야 한다")
    public void 좋아요_수는_두_값중_하나의_일관된_값이어야_한다(int first, int second) {
        int likeCount = currentLikeCountFromDetail();
        assertTrue(likeCount == first || likeCount == second, "likeCount=" + likeCount);
    }

    @And("중복 좋아요 데이터가 생성되지 않는다")
    public void 중복_좋아요_데이터가_생성되지_않는다() {
        ensureProjectExists();
        String memberName = currentMemberName != null ? currentMemberName : "김철수";
        Member member = createOrFindMember(memberName);
        Long count = ((Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM project_like WHERE member_id = :memberId AND project_id = :projectId")
                .setParameter("memberId", member.getId())
                .setParameter("projectId", currentProjectId)
                .getSingleResult()).longValue();
        assertTrue(count <= 1, "동일 회원/프로젝트 좋아요는 1개를 초과하면 안 됩니다.");
    }

    @Then("좋아요 수는 {int}개가 된다")
    public void 좋아요_수는_n개가_된다(int expected) {
        assertEquals(expected, currentLikeCountFromDetail());
    }

    @And("각 회원의 좋아요 상태가 {string}으로 저장된다")
    public void 각_회원의_좋아요_상태가_저장된다(String expectedStatus) {
        assertEquals("선택", expectedStatus);
        for (Member member : concurrentMembers) {
            String token = jwtUtil.createAccessToken(member);
            Response status = projectLikeApi.likeStatus(currentProjectId, token);
            assertEquals(200, status.statusCode());
            assertTrue(status.jsonPath().getBoolean("result.status"),
                    "회원 " + member.getEmail() + " 의 좋아요 상태가 선택이어야 합니다.");
        }
    }

    @And("요청 처리 중 데이터 정합성 오류가 발생하지 않는다")
    public void 요청_처리_중_데이터_정합성_오류가_발생하지_않는다() {
        assertTrue(concurrentErrors.isEmpty(), "동시성 처리 중 예외 발생: " + concurrentErrors);
        boolean hasUnexpected = concurrentResponses.stream()
                .map(Response::statusCode)
                .anyMatch(code -> code >= 500);
        assertFalse(hasUnexpected, "서버 오류 응답이 없어야 합니다.");
    }

    private void ensureProjectExists() {
        if (currentProjectId == null) {
            Project project = createOrReplaceProjectWithLikes("AI 뉴스 요약 서비스", 10);
            currentProjectId = project.getId();
            currentProjectName = project.getName();
        }
    }

    private Project createOrReplaceProjectWithLikes(String projectName, int likeCount) {
        Project existing = projectRepository.findAll().stream()
                .filter(p -> projectName.equals(p.getName()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            resetProjectLikes(existing, likeCount, null);
            return existing;
        }

        Member creator = createOrFindMember("홍길동");
        Project project = Project.createProject(
                creator,
                projectName,
                "좋아요 테스트 프로젝트",
                ProjectCategory.AI_TECH,
                PlatformCategory.WEB,
                null,
                RecruitmentDeadlineType.END_DATE,
                LocalDate.now().plusDays(30)
        );
        project.addRecruitment(RecruitmentState.createRecruitmentState(JobPosition.WEB_FRONTEND, 2));
        project = projectRepository.save(project);

        resetProjectLikes(project, likeCount, null);
        return project;
    }

    private void resetProjectLikes(Project project, int targetCount, Long mustIncludeMemberId) {
        entityManager.createNativeQuery("DELETE FROM project_like WHERE project_id = :projectId")
                .setParameter("projectId", project.getId())
                .executeUpdate();

        int created = 0;
        if (mustIncludeMemberId != null && targetCount > 0) {
            Member mustMember = memberRepository.findById(mustIncludeMemberId)
                    .orElseThrow(() -> new AssertionError("필수 멤버를 찾을 수 없습니다."));
            projectLikeRepository.save(ProjectLike.create(mustMember, project));
            created++;
        }

        for (int i = 1; created < targetCount; i++) {
            Member likeMember = createOrFindMember("좋아요회원" + i);
            if (mustIncludeMemberId != null && likeMember.getId().equals(mustIncludeMemberId)) {
                continue;
            }
            projectLikeRepository.save(ProjectLike.create(likeMember, project));
            created++;
        }

        project.setLikeCount(targetCount);
        projectRepository.save(project);
        entityManager.flush();
    }

    private Integer currentLikeCountFromDetail() {
        Response detail = projectLikeApi.getProjectDetail(currentProjectId);
        assertEquals(200, detail.statusCode());
        return detail.jsonPath().getInt("result.likeCount");
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
            default -> "user" + Math.abs(memberName.toLowerCase(Locale.ROOT).hashCode()) + "@example.com";
        };
    }

    private boolean extractLikeStatus(Response response) {
        Boolean status = response.jsonPath().get("result.status");
        assertNotNull(status, "좋아요 상태 응답이 비어 있습니다: " + response.asString());
        return status;
    }

    private Boolean extractToggleLiked(Response response) {
        try {
            Boolean liked = response.jsonPath().get("result.liked");
            assertNotNull(liked, "좋아요 토글 응답이 비어 있습니다: " + response.asString());
            return liked;
        } catch (RuntimeException e) {
            throw new AssertionError("좋아요 토글 응답 파싱 실패: " + response.asString(), e);
        }
    }

}
