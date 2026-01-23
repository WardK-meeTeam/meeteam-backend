package com.wardk.meeteam_backend.acceptance.project;

import com.wardk.meeteam_backend.acceptance.common.AcceptanceTest;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.project.entity.*;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프로젝트 검색 인수 테스트
 *
 * 관련 인수 조건:
 * - 프로젝트 100개 상태에서 검색 결과가 정상적으로 표시되어야 한다
 */
@SuppressWarnings("NonAsciiCharacters")
@DisplayName("프로젝트 검색 인수 테스트")
public class ProjectSearchAcceptanceTest extends AcceptanceTest {

    private static final String API_MAIN_PROJECTS = "/api/main/projects";
    private static final String API_PROJECTS_CONDITION = "/api/projects/condition";

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SkillRepository skillRepository;

    private Member testMember;

    @BeforeEach
    void setUpTestData() {
        // 테스트용 멤버 생성
        testMember = memberRepository.save(Member.builder()
                .email("test@meeteam.com")
                .password("password1234")
                .realName("테스터")
                .role(UserRole.USER)
                .build());
    }

    @Nested
    @DisplayName("프로젝트 목록 조회")
    class 프로젝트_목록_조회 {

        /**
         * 시나리오: 사용자가 메인 페이지에서 프로젝트 목록을 조회한다.
         * given: 프로젝트 100개가 등록되어 있고,
         * when: 사용자가 프로젝트 목록을 요청하면,
         * then: 프로젝트 목록이 정상적으로 반환된다.
         */
        @Test
        @DisplayName("프로젝트_100개_상태에서_정상적으로_조회된다")
        void 프로젝트_100개_상태에서_정상적으로_조회된다() {
            // given - 프로젝트 100개 생성
            for (int i = 0; i < 100; i++) {
                프로젝트_생성("프로젝트 " + i, ProjectCategory.ETC, null);
            }

            // when - 메인 페이지 프로젝트 목록 요청
            var response = 메인_페이지_프로젝트_목록_조회를_요청한다();

            // then - 정상 응답 확인 (페이지 사이즈 기본 20개)
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.jsonPath().getString("code")).isEqualTo("COMMON200");
            assertThat(response.jsonPath().getList("result.content")).hasSize(20);
            assertThat(response.jsonPath().getInt("result.totalElements")).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("프로젝트 필터 검색")
    class 프로젝트_필터_검색 {

        /**
         * 시나리오: 사용자가 카테고리 필터로 프로젝트를 검색한다.
         * given: 다양한 카테고리의 프로젝트가 등록되어 있고,
         * when: 사용자가 특정 카테고리로 검색하면,
         * then: 해당 카테고리의 프로젝트만 반환된다.
         */
        @Test
        @DisplayName("카테고리_필터로_검색하면_해당_카테고리_프로젝트만_반환된다")
        void 카테고리_필터로_검색하면_해당_카테고리_프로젝트만_반환된다() {
            // given - 다양한 카테고리의 프로젝트 생성
            프로젝트_생성("웹 프로젝트 1", ProjectCategory.ETC, null); // ETC가 아닌 명확한 카테고리 사용 권장, 여기서는 테스트를 위해 ETC/EDUCATION 구분
            프로젝트_생성("웹 프로젝트 2", ProjectCategory.EDUCATION, null);
            프로젝트_생성("앱 프로젝트", ProjectCategory.PET, null);

            // when - 카테고리 필터로 검색 (EDUCATION)
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("projectCategory", "EDUCATION");
            ExtractableResponse<Response> response = 메인_페이지_프로젝트_목록_조회를_요청한다(searchParams);

            // then - EDUCATION 카테고리 프로젝트만 반환
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.jsonPath().getList("result.content")).hasSize(1);
            assertThat(response.jsonPath().getString("result.content[0].projectName")).isEqualTo("웹 프로젝트 2");
        }
    }

    @Nested
    @DisplayName("복합 필터 검색")
    class 복합_필터_검색 {

        /**
         * 시나리오: 사용자가 카테고리와 기술 스택으로 프로젝트를 검색한다.
         * given: 다양한 조건의 프로젝트가 등록되어 있고,
         * when: 사용자가 카테고리와 기술 스택을 모두 지정하여 검색하면,
         * then: 두 조건을 모두 만족하는 프로젝트만 반환된다.
         */
        @Test
        @DisplayName("카테고리와_기술스택으로_검색하면_조건을_만족하는_프로젝트만_반환된다")
        void 카테고리와_기술스택으로_검색하면_조건을_만족하는_프로젝트만_반환된다() {
            // given - 스킬 준비
            Skill springSkill = skillRepository.save(new Skill(TechStack.SPRING.getTechName()));
            Skill reactSkill = skillRepository.save(new Skill(TechStack.REACT.getTechName()));

            // given - 프로젝트 생성
            // 1. EDUCATION + Spring (Target)
            Project p1 = 프로젝트_생성("타겟 프로젝트", ProjectCategory.EDUCATION, null);
            p1.addProjectSkill(ProjectSkill.create(p1, springSkill));
            projectRepository.save(p1);

            // 2. EDUCATION + React
            Project p2 = 프로젝트_생성("다른 스택", ProjectCategory.EDUCATION, null);
            p2.addProjectSkill(ProjectSkill.create(p2, reactSkill));
            projectRepository.save(p2);

            // 3. PET + Spring
            Project p3 = 프로젝트_생성("다른 카테고리", ProjectCategory.PET, null);
            p3.addProjectSkill(ProjectSkill.create(p3, springSkill));
            projectRepository.save(p3);

            // when - 복합 조건 검색
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("projectCategory", "EDUCATION");
            searchParams.put("techStack", "SPRING");

            var response = 프로젝트_조건_검색을_요청한다(searchParams);

            // then - 타겟 프로젝트만 반환
            // ProjectQueryController returns Page<ProjectConditionRequest> directly (not wrapped in SuccessResponse based on controller code inspection)
            // Controller signature: public Page<ProjectConditionRequest> searchCondition(...)
            
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.jsonPath().getList("content")).hasSize(1);
            assertThat(response.jsonPath().getString("content[0].projectName")).isEqualTo("타겟 프로젝트");
        }
    }

    // --- Helper Methods ---

    private ExtractableResponse<Response> 메인_페이지_프로젝트_목록_조회를_요청한다() {
        return 메인_페이지_프로젝트_목록_조회를_요청한다(new HashMap<>());
    }

    private ExtractableResponse<Response> 메인_페이지_프로젝트_목록_조회를_요청한다(Map<String, Object> params) {
        return given().log().all()
                .queryParams(params)
                .when()
                .get(API_MAIN_PROJECTS)
                .then().log().all()
                .extract();
    }

    private ExtractableResponse<Response> 프로젝트_조건_검색을_요청한다(Map<String, Object> params) {
        return given().log().all()
                .queryParams(params)
                .when()
                .get(API_PROJECTS_CONDITION)
                .then().log().all()
                .extract();
    }

    private Project 프로젝트_생성(String name, ProjectCategory category, PlatformCategory platform) {
        return projectRepository.save(Project.builder()
                .creator(testMember)
                .name(name)
                .description(name + " 설명입니다.")
                .projectCategory(category)
                .platformCategory(platform != null ? platform : PlatformCategory.WEB)
                .imageUrl("http://example.com/image.jpg")
                .offlineRequired(false)
                .status(ProjectStatus.PLANNING)
                .recruitmentStatus(Recruitment.RECRUITING)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .isDeleted(false)
                .build());
    }
}
