package com.wardk.meeteam_backend.characterization.application;

import com.wardk.meeteam_backend.domain.application.entity.ApplicationStatus;
import com.wardk.meeteam_backend.domain.application.entity.ProjectApplication;
import com.wardk.meeteam_backend.domain.application.repository.ProjectApplicationRepository;
import com.wardk.meeteam_backend.domain.application.service.ProjectApplicationService;
import com.wardk.meeteam_backend.domain.job.entity.JobField;
import com.wardk.meeteam_backend.domain.job.entity.JobFieldCode;
import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.job.entity.JobPositionCode;
import com.wardk.meeteam_backend.domain.job.repository.JobFieldRepository;
import com.wardk.meeteam_backend.domain.job.repository.JobPositionRepository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectmember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.domain.recruitment.entity.RecruitmentState;
import com.wardk.meeteam_backend.domain.recruitment.repository.RecruitmentStateRepository;
import com.wardk.meeteam_backend.fixture.MemberFixture;
import com.wardk.meeteam_backend.fixture.ProjectFixture;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.application.dto.request.ApplicationDecisionRequest;
import com.wardk.meeteam_backend.web.application.dto.request.ApplicationRequest;
import com.wardk.meeteam_backend.web.application.dto.response.ApplicationCancelResponse;
import com.wardk.meeteam_backend.web.application.dto.response.ApplicationDecisionResponse;
import com.wardk.meeteam_backend.web.application.dto.response.ApplicationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 프로젝트 지원(Application) 도메인 특성화 테스트 (Characterization Test).
 *
 * <p><b>목적:</b> DDD Week 2 리팩터링의 <b>안전망</b>. 코드를 고치기 <b>전에</b> 지금의 동작을 박제한다.
 * 여기 적힌 기대값은 "이렇게 동작하는 게 옳다"가 아니라 "지금 이렇게 동작한다"를 의미한다.
 * 리팩터링(상태 전이 메서드 분리, Aggregate 경계 분리, 규칙의 도메인 이전) 도중 이 테스트가 빨개지면,
 * 동작이 바뀐 것이므로 멈추고 원인부터 확인한다.
 *
 * <p>대상: {@link ProjectApplicationService}의 apply / decide / cancel 흐름.
 *
 * @see docs/ddd/week2.md Mission 2-0
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("[특성화] 프로젝트 지원 흐름")
class ProjectApplicationCharacterizationTest {

    @Autowired
    private ProjectApplicationService applicationService;

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private JobFieldRepository jobFieldRepository;
    @Autowired
    private JobPositionRepository jobPositionRepository;
    @Autowired
    private RecruitmentStateRepository recruitmentStateRepository;
    @Autowired
    private ProjectApplicationRepository applicationRepository;
    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    // 모집 중인 포지션
    private static final JobPositionCode RECRUITING_CODE = JobPositionCode.WEB_FRONTEND;
    // 프로젝트에 모집 등록되지 않은 포지션
    private static final JobPositionCode NOT_RECRUITING_CODE = JobPositionCode.IOS;
    private static final int RECRUITMENT_COUNT = 2;

    private Member creator;
    private Member applicant;
    private Project project;
    private JobPosition recruitingPosition;
    private Long recruitmentStateId;

    @BeforeEach
    void setUp() {
        // 직군/직무
        JobField jobField = jobFieldRepository.save(JobField.of(JobFieldCode.FRONTEND));
        recruitingPosition = jobPositionRepository.save(JobPosition.of(jobField, RECRUITING_CODE));
        jobPositionRepository.save(JobPosition.of(jobField, NOT_RECRUITING_CODE)); // 모집 미등록 포지션

        // 회원
        creator = memberRepository.save(MemberFixture.withEmail("creator@example.com", "프로젝트장"));
        applicant = memberRepository.save(MemberFixture.withEmail("applicant@example.com", "지원자"));

        // 프로젝트 + 모집현황(WEB_FRONTEND 2명)
        project = ProjectFixture.defaultProject(creator);
        RecruitmentState recruitmentState =
                RecruitmentState.createRecruitmentState(recruitingPosition, RECRUITMENT_COUNT);
        project.addRecruitment(recruitmentState);
        project = projectRepository.save(project); // cascade로 recruitmentState 함께 저장
        recruitmentStateId = project.getRecruitments().get(0).getId();
    }

    // ==================== apply ====================

    @Nested
    @DisplayName("apply 메서드는")
    class Apply {

        @Test
        @DisplayName("정상 지원하면 PENDING 상태의 지원서를 생성한다")
        void creates_pending_application_on_success() {
            // when
            ApplicationResponse response = applicationService.apply(
                    project.getId(), applicant.getId(), applyRequest(RECRUITING_CODE));

            // then
            assertThat(response.applicationId()).isNotNull();
            assertThat(response.projectId()).isEqualTo(project.getId());
            assertThat(response.applicantId()).isEqualTo(applicant.getId());
            assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);

            ProjectApplication saved = applicationRepository.findById(response.applicationId()).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        }

        @Test
        @DisplayName("프로젝트 생성자가 자기 프로젝트에 지원하면 예외를 던진다")
        void throws_when_creator_applies_to_own_project() {
            assertThatThrownBy(() -> applicationService.apply(
                    project.getId(), creator.getId(), applyRequest(RECRUITING_CODE)))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_SELF_PROJECT_FORBIDDEN);
        }

        @Test
        @DisplayName("이미 지원한 프로젝트에 또 지원하면 예외를 던진다")
        void throws_when_already_applied() {
            // given
            applicationService.apply(project.getId(), applicant.getId(), applyRequest(RECRUITING_CODE));

            // when & then
            assertThatThrownBy(() -> applicationService.apply(
                    project.getId(), applicant.getId(), applyRequest(RECRUITING_CODE)))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROJECT_APPLICATION_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("프로젝트가 모집하지 않는 포지션에 지원하면 예외를 던진다")
        void throws_when_position_not_recruiting() {
            assertThatThrownBy(() -> applicationService.apply(
                    project.getId(), applicant.getId(), applyRequest(NOT_RECRUITING_CODE)))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECRUITMENT_POSITION_NOT_RECRUITING);
        }

        @Test
        @DisplayName("모집이 마감된 포지션에 지원하면 예외를 던진다")
        void throws_when_position_closed() {
            // given - 정원을 모두 채워 마감 상태로 만든다
            RecruitmentState state = recruitmentStateRepository.findById(recruitmentStateId).orElseThrow();
            for (int i = 0; i < RECRUITMENT_COUNT; i++) {
                state.increaseCurrentCount();
            }

            // when & then
            assertThatThrownBy(() -> applicationService.apply(
                    project.getId(), applicant.getId(), applyRequest(RECRUITING_CODE)))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECRUITMENT_POSITION_CLOSED);
        }
    }

    // ==================== decide ====================

    @Nested
    @DisplayName("decide 메서드는")
    class Decide {

        @Test
        @DisplayName("수락하면 지원자가 프로젝트 멤버로 합류하고 모집 인원이 증가한다")
        void accept_joins_member_and_increases_count() {
            // given
            Long applicationId = apply();
            int before = recruitmentStateRepository.findById(recruitmentStateId).orElseThrow().getCurrentCount();

            // when
            ApplicationDecisionResponse response = applicationService.decide(
                    applicationId, decision(ApplicationStatus.ACCEPTED), creator.getEmail());

            // then
            assertThat(response.decision()).isEqualTo(ApplicationStatus.ACCEPTED);
            assertThat(applicationRepository.findById(applicationId).orElseThrow().getStatus())
                    .isEqualTo(ApplicationStatus.ACCEPTED);
            assertThat(projectMemberRepository.existsByProjectIdAndMemberId(project.getId(), applicant.getId()))
                    .isTrue();
            assertThat(recruitmentStateRepository.findById(recruitmentStateId).orElseThrow().getCurrentCount())
                    .isEqualTo(before + 1);
        }

        @Test
        @DisplayName("거절하면 지원서 상태가 REJECTED가 되고 멤버로 합류하지 않는다")
        void reject_marks_rejected_and_does_not_join() {
            // given
            Long applicationId = apply();

            // when
            ApplicationDecisionResponse response = applicationService.decide(
                    applicationId, decision(ApplicationStatus.REJECTED), creator.getEmail());

            // then
            assertThat(response.decision()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(applicationRepository.findById(applicationId).orElseThrow().getStatus())
                    .isEqualTo(ApplicationStatus.REJECTED);
            assertThat(projectMemberRepository.existsByProjectIdAndMemberId(project.getId(), applicant.getId()))
                    .isFalse();
        }

        @Test
        @DisplayName("프로젝트 생성자가 아닌 사람이 결정하면 예외를 던진다")
        void throws_when_requester_is_not_creator() {
            // given
            Long applicationId = apply();

            // when & then
            assertThatThrownBy(() -> applicationService.decide(
                    applicationId, decision(ApplicationStatus.ACCEPTED), applicant.getEmail()))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        @Test
        @DisplayName("이미 처리된 지원서를 다시 결정하면 예외를 던진다")
        void throws_when_already_decided() {
            // given
            Long applicationId = apply();
            applicationService.decide(applicationId, decision(ApplicationStatus.ACCEPTED), creator.getEmail());

            // when & then
            assertThatThrownBy(() -> applicationService.decide(
                    applicationId, decision(ApplicationStatus.REJECTED), creator.getEmail()))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_ALREADY_DECIDED);
        }
    }

    // ==================== cancel ====================

    @Nested
    @DisplayName("cancel 메서드는")
    class Cancel {

        @Test
        @DisplayName("본인이 PENDING 지원서를 취소하면 CANCELLED 상태가 된다")
        void cancels_pending_application_by_applicant() {
            // given
            Long applicationId = apply();

            // when
            ApplicationCancelResponse response = applicationService.cancel(applicationId, applicant.getId());

            // then
            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.status()).isEqualTo("CANCELLED");
            assertThat(applicationRepository.findById(applicationId).orElseThrow().getStatus())
                    .isEqualTo(ApplicationStatus.CANCELLED);
        }

        @Test
        @DisplayName("본인이 아닌 사람이 취소하면 예외를 던진다")
        void throws_when_canceller_is_not_applicant() {
            // given
            Long applicationId = apply();

            // when & then
            assertThatThrownBy(() -> applicationService.cancel(applicationId, creator.getId()))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_CANCEL_FORBIDDEN);
        }

        @Test
        @DisplayName("PENDING이 아닌 지원서를 취소하면 예외를 던진다")
        void throws_when_not_pending() {
            // given - 수락되어 PENDING이 아닌 상태
            Long applicationId = apply();
            applicationService.decide(applicationId, decision(ApplicationStatus.ACCEPTED), creator.getEmail());

            // when & then
            assertThatThrownBy(() -> applicationService.cancel(applicationId, applicant.getId()))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_CANCEL_NOT_ALLOWED);
        }
    }

    // ==================== 헬퍼 ====================

    private Long apply() {
        return applicationService.apply(
                project.getId(), applicant.getId(), applyRequest(RECRUITING_CODE)).applicationId();
    }

    private ApplicationRequest applyRequest(JobPositionCode code) {
        return new ApplicationRequest(code, "이 프로젝트에 꼭 참여하고 싶습니다.");
    }

    private ApplicationDecisionRequest decision(ApplicationStatus status) {
        return new ApplicationDecisionRequest(status);
    }
}
