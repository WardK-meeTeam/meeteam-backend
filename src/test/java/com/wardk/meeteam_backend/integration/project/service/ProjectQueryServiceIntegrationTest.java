package com.wardk.meeteam_backend.integration.project.service;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import com.wardk.meeteam_backend.domain.project.entity.Recruitment;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.project.service.ProjectQueryService;
import com.wardk.meeteam_backend.fixture.MemberFixture;
import com.wardk.meeteam_backend.fixture.ProjectFixture;
import com.wardk.meeteam_backend.web.mainpage.dto.response.ProjectCardResponse;
import com.wardk.meeteam_backend.web.project.dto.request.ProjectSearchRequest;
import com.wardk.meeteam_backend.web.project.dto.request.ProjectSortType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ProjectQueryService 통합 테스트")
class ProjectQueryServiceIntegrationTest {

    // ==================== 상수 ====================

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int TOTAL_PROJECTS_FOR_PAGINATION = 25;

    private static final String PROJECT_NAME_AI = "AI 개발 프로젝트";
    private static final String PROJECT_NAME_HEALTHCARE = "헬스케어 앱";
    private static final String PROJECT_NAME_RECRUITING = "모집중 프로젝트";
    private static final String PROJECT_NAME_CLOSED = "마감된 프로젝트";

    private static final String LEADER_NAME_HONG = "홍길동";
    private static final String LEADER_NAME_KIM = "김철수";

    private static final String KEYWORD_DEVELOP = "개발";
    private static final String KEYWORD_NOT_EXIST = "존재하지않는키워드";

    // ==================== 의존성 ====================

    @Autowired
    private ProjectQueryService projectQueryService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member leader;

    @BeforeEach
    void setUp() {
        leader = memberRepository.save(MemberFixture.defaultMember());
    }

    // ==================== 헬퍼 메서드 ====================

    private Pageable defaultPageable() {
        return PageRequest.of(DEFAULT_PAGE, DEFAULT_PAGE_SIZE);
    }

    private Pageable pageableOf(int page, int size) {
        return PageRequest.of(page, size);
    }

    private ProjectSearchRequest requestWithKeyword(String keyword) {
        return new ProjectSearchRequest(keyword, null, null, null, null, null, null);
    }

    private ProjectSearchRequest requestWithCategory(ProjectCategory category) {
        return new ProjectSearchRequest(null, category, null, null, null, null, null);
    }

    private ProjectSearchRequest requestWithRecruitment(Recruitment recruitment) {
        return new ProjectSearchRequest(null, null, recruitment, null, null, null, null);
    }

    private ProjectSearchRequest requestWithPlatform(PlatformCategory platform) {
        return new ProjectSearchRequest(null, null, null, platform, null, null, null);
    }

    private ProjectSearchRequest requestWithSort(ProjectSortType sort) {
        return new ProjectSearchRequest(null, null, null, null, null, null, sort);
    }

    private ProjectSearchRequest emptyRequest() {
        return new ProjectSearchRequest(null, null, null, null, null, null, null);
    }

    private Page<ProjectCardResponse> search(ProjectSearchRequest request) {
        return projectQueryService.searchV1(request, defaultPageable(), null);
    }

    private Page<ProjectCardResponse> search(ProjectSearchRequest request, Pageable pageable) {
        return projectQueryService.searchV1(request, pageable, null);
    }

    private ProjectCardResponse getFirstResult(Page<ProjectCardResponse> result) {
        return result.getContent().get(0);
    }

    private void createProjects(int count) {
        for (int i = 1; i <= count; i++) {
            projectRepository.save(ProjectFixture.withName("프로젝트 " + i, leader));
        }
    }

    // ==================== 테스트 ====================

    @Nested
    @DisplayName("searchV1 메서드는")
    class SearchV1 {

        @Nested
        @DisplayName("키워드 검색 시")
        class WithKeyword {

            @Test
            @DisplayName("프로젝트명에 키워드가 포함된 프로젝트의 DTO를 반환한다")
            void returns_dto_when_keyword_matches_project_name() {
                // given
                Project saved = projectRepository.save(ProjectFixture.withName(PROJECT_NAME_AI, leader));
                projectRepository.save(ProjectFixture.withName(PROJECT_NAME_HEALTHCARE, leader));

                // when
                Page<ProjectCardResponse> result = search(requestWithKeyword(KEYWORD_DEVELOP));

                // then
                assertThat(result.getContent()).hasSize(1);

                ProjectCardResponse dto = getFirstResult(result);
                assertThat(dto.getProjectId()).isEqualTo(saved.getId());
                assertThat(dto.getProjectName()).isEqualTo(PROJECT_NAME_AI);
                assertThat(dto.getCreatorName()).isEqualTo(leader.getRealName());
                assertThat(dto.isLiked()).isFalse();
            }

            @Test
            @DisplayName("리더 이름에 키워드가 포함된 프로젝트의 DTO를 반환한다")
            void returns_dto_when_keyword_matches_leader_name() {
                // given
                Member leader1 = memberRepository.save(MemberFixture.withName(LEADER_NAME_HONG));
                Member leader2 = memberRepository.save(MemberFixture.withName(LEADER_NAME_KIM));

                projectRepository.save(ProjectFixture.withName("프로젝트 A", leader1));
                projectRepository.save(ProjectFixture.withName("프로젝트 B", leader2));

                // when
                Page<ProjectCardResponse> result = search(requestWithKeyword(LEADER_NAME_HONG));

                // then
                assertThat(result.getContent()).hasSize(1);
                assertThat(getFirstResult(result).getCreatorName()).isEqualTo(LEADER_NAME_HONG);
            }

            @Test
            @DisplayName("검색 결과가 없으면 빈 Page를 반환한다")
            void returns_empty_page_when_no_results() {
                // given
                projectRepository.save(ProjectFixture.withName(PROJECT_NAME_AI, leader));

                // when
                Page<ProjectCardResponse> result = search(requestWithKeyword(KEYWORD_NOT_EXIST));

                // then
                assertThat(result.getContent()).isEmpty();
                assertThat(result.getTotalElements()).isZero();
            }
        }

        @Nested
        @DisplayName("카테고리 필터 시")
        class WithCategoryFilter {

            @Test
            @DisplayName("해당 카테고리의 프로젝트만 반환한다")
            void returns_only_matching_category_projects() {
                // given
                projectRepository.save(ProjectFixture.withCategory(ProjectCategory.AI_TECH, leader, "AI 프로젝트"));
                projectRepository.save(ProjectFixture.withCategory(ProjectCategory.HEALTHCARE, leader, "헬스케어 프로젝트"));

                // when
                Page<ProjectCardResponse> result = search(requestWithCategory(ProjectCategory.AI_TECH));

                // then
                assertThat(result.getContent()).hasSize(1);
                assertThat(getFirstResult(result).getCategoryCode()).isEqualTo(ProjectCategory.AI_TECH.name());
            }
        }

        @Nested
        @DisplayName("플랫폼 필터 시")
        class WithPlatformFilter {

            @Test
            @DisplayName("해당 플랫폼의 프로젝트만 반환한다")
            void returns_only_matching_platform_projects() {
                // given
                projectRepository.save(ProjectFixture.withPlatform(PlatformCategory.WEB, leader, "웹 프로젝트"));
                projectRepository.save(ProjectFixture.withPlatform(PlatformCategory.IOS, leader, "iOS 프로젝트"));

                // when
                Page<ProjectCardResponse> result = search(requestWithPlatform(PlatformCategory.IOS));

                // then
                assertThat(result.getContent()).hasSize(1);
                assertThat(getFirstResult(result).getPlatformName()).isEqualTo(PlatformCategory.IOS.name());
            }
        }

        @Nested
        @DisplayName("모집상태 필터 시")
        class WithRecruitmentFilter {

            @Test
            @DisplayName("해당 모집상태의 프로젝트만 반환한다")
            void returns_only_matching_recruitment_projects() {
                // given
                projectRepository.save(ProjectFixture.withRecruitment(Recruitment.RECRUITING, leader, PROJECT_NAME_RECRUITING));
                projectRepository.save(ProjectFixture.withRecruitment(Recruitment.CLOSED, leader, PROJECT_NAME_CLOSED));

                // when
                Page<ProjectCardResponse> result = search(requestWithRecruitment(Recruitment.RECRUITING));

                // then
                assertThat(result.getContent()).hasSize(1);
                assertThat(getFirstResult(result).getProjectName()).isEqualTo(PROJECT_NAME_RECRUITING);
            }
        }

        @Nested
        @DisplayName("정렬 시")
        class WithSort {

            @Test
            @DisplayName("LATEST 정렬 시 최신순으로 반환한다")
            void returns_dtos_sorted_by_latest() {
                // given
                projectRepository.save(ProjectFixture.withName("첫번째", leader));
                projectRepository.save(ProjectFixture.withName("두번째", leader));
                projectRepository.save(ProjectFixture.withName("세번째", leader));

                // when
                Page<ProjectCardResponse> result = search(requestWithSort(ProjectSortType.LATEST));

                // then
                assertThat(result.getContent())
                        .extracting(ProjectCardResponse::getProjectName)
                        .containsExactly("세번째", "두번째", "첫번째");
            }

            @Test
            @DisplayName("DEADLINE 정렬 시 마감임박순으로 반환한다")
            void returns_dtos_sorted_by_deadline_ascending() {
                // given
                projectRepository.save(ProjectFixture.withEndDate(LocalDate.now().plusMonths(2), leader, "먼 프로젝트"));
                projectRepository.save(ProjectFixture.withEndDate(LocalDate.now().plusDays(3), leader, "임박 프로젝트"));
                projectRepository.save(ProjectFixture.withEndDate(LocalDate.now().plusDays(10), leader, "중간 프로젝트"));

                // when
                Page<ProjectCardResponse> result = search(requestWithSort(ProjectSortType.DEADLINE));

                // then
                assertThat(result.getContent())
                        .extracting(ProjectCardResponse::getProjectName)
                        .containsExactly("임박 프로젝트", "중간 프로젝트", "먼 프로젝트");
            }
        }

        @Nested
        @DisplayName("페이징 시")
        class WithPagination {

            @Test
            @DisplayName("페이지 크기만큼 결과를 반환한다")
            void returns_page_size_results() {
                // given
                createProjects(TOTAL_PROJECTS_FOR_PAGINATION);

                // when
                Page<ProjectCardResponse> result = search(emptyRequest());

                // then
                assertThat(result.getContent()).hasSize(DEFAULT_PAGE_SIZE);
                assertThat(result.getTotalElements()).isEqualTo(TOTAL_PROJECTS_FOR_PAGINATION);
                assertThat(result.getTotalPages()).isEqualTo(2);
                assertThat(result.isLast()).isFalse();
            }

            @Test
            @DisplayName("두번째 페이지 조회 시 나머지 결과를 반환한다")
            void returns_remaining_results_on_second_page() {
                // given
                createProjects(TOTAL_PROJECTS_FOR_PAGINATION);

                // when
                Page<ProjectCardResponse> result = search(emptyRequest(), pageableOf(1, DEFAULT_PAGE_SIZE));

                // then
                int expectedRemaining = TOTAL_PROJECTS_FOR_PAGINATION - DEFAULT_PAGE_SIZE;
                assertThat(result.getContent()).hasSize(expectedRemaining);
                assertThat(result.isLast()).isTrue();
            }
        }

        @Nested
        @DisplayName("DTO 변환 시")
        class DtoConversion {

            @Test
            @DisplayName("비로그인 사용자는 isLiked가 false이다")
            void is_liked_false_for_anonymous_user() {
                // given
                projectRepository.save(ProjectFixture.defaultProject(leader));

                // when
                Page<ProjectCardResponse> result = search(emptyRequest());

                // then
                assertThat(getFirstResult(result).isLiked()).isFalse();
            }

            @Test
            @DisplayName("카테고리 displayName이 올바르게 변환된다")
            void category_display_name_is_converted() {
                // given
                projectRepository.save(ProjectFixture.withCategory(ProjectCategory.AI_TECH, leader, "AI 프로젝트"));

                // when
                Page<ProjectCardResponse> result = search(emptyRequest());

                // then
                ProjectCardResponse dto = getFirstResult(result);
                assertThat(dto.getCategoryName()).isEqualTo(ProjectCategory.AI_TECH.getDisplayName());
                assertThat(dto.getCategoryCode()).isEqualTo(ProjectCategory.AI_TECH.name());
            }
        }
    }
}
