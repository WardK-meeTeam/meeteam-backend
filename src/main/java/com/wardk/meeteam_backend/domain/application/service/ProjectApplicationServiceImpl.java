package com.wardk.meeteam_backend.domain.application.service;

import com.wardk.meeteam_backend.domain.application.entity.ApplicationAnswer;
import com.wardk.meeteam_backend.domain.application.entity.ApplicationQuestion;
import com.wardk.meeteam_backend.domain.application.entity.ApplicationStatus;
import com.wardk.meeteam_backend.domain.application.entity.ProjectApplication;
import com.wardk.meeteam_backend.domain.application.entity.QuestionInputType;
import com.wardk.meeteam_backend.domain.application.repository.ApplicationAnswerRepository;
import com.wardk.meeteam_backend.domain.application.repository.ApplicationQuestionRepository;
import com.wardk.meeteam_backend.domain.application.repository.ApplicationSearchRepository;
import com.wardk.meeteam_backend.domain.application.repository.ProjectApplicationRepository;
import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.job.repository.JobPositionRepository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.notification.NotificationEvent;
import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import com.wardk.meeteam_backend.domain.notification.service.NotificationSaveService;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectmember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.domain.projectmember.service.ProjectMemberService;
import com.wardk.meeteam_backend.domain.recruitment.entity.RecruitmentState;
import com.wardk.meeteam_backend.domain.recruitment.repository.RecruitmentStateRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.application.dto.request.*;
import com.wardk.meeteam_backend.web.application.dto.response.*;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 프로젝트 지원 서비스 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectApplicationServiceImpl implements ProjectApplicationService {

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectApplicationRepository applicationRepository;
    private final ApplicationQuestionRepository applicationQuestionRepository;
    private final ApplicationAnswerRepository applicationAnswerRepository;
    private final ApplicationSearchRepository applicationSearchRepository;
    private final ProjectMemberService projectMemberService;
    private final JobPositionRepository jobPositionRepository;
    private final RecruitmentStateRepository recruitmentStateRepository;
    private final NotificationSaveService notificationSaveService;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    @Counted("project.apply")
    @Override
    public ApplicationResponse apply(Long projectId, Long memberId, ApplicationRequest request) {
        // 1단계: 엔티티 조회
        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        JobPosition jobPosition = jobPositionRepository.findByCode(request.jobPositionCode())
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_POSITION_NOT_FOUND));

        // 2단계: 권한 검증
        validateNotSelfApplication(project, memberId);

        // 3단계: 비즈니스 규칙 검증
        validateApplyPrecondition(project, member, projectId, memberId);
        validateRecruitmentPosition(projectId, jobPosition);

        // 누적 지원 수 +1 — 지원서 INSERT(FK 검증이 project 행 S-lock을 잡음)보다 먼저
        // X-lock을 획득해 S→X 승격 데드락을 차단한다. 검증 단계는 락 밖, INSERT~커밋은 락 안.
        long updateStartNanos = System.nanoTime();
        projectRepository.increaseApplicationCount(projectId);
        recordLockTiming(projectId, updateStartNanos, System.nanoTime());

        // 4단계: 지원서 생성 및 저장
        ProjectApplication savedApplication = createAndSaveApplication(project, member, jobPosition, request.motivation());

        // 5단계: 동적 지원서 답변 저장 (EAV)
        saveAnswers(project, savedApplication, request.answers());

        // 6단계: 알림 발행 및 응답
        publishApplyNotifications(project, member, savedApplication);

        log.info("프로젝트 지원 완료 - projectId: {}, applicantId: {}, jobPositionCode: {}",
                projectId, memberId, request.jobPositionCode());

        return ApplicationResponse.from(savedApplication);
    }

    /**
     * applicationCount UPDATE가 잡은 project 행 X-lock의 소유 시간을 측정합니다.
     * <p>
     * InnoDB는 커밋 시점에 행 락을 해제하므로, UPDATE 반환 직후부터 트랜잭션 완료까지가 락 소유 구간입니다.
     * <ul>
     *   <li>{@code project.apply.lock.acquire} — UPDATE 문 수행 시간 (다른 트랜잭션의 락 대기 포함)</li>
     *   <li>{@code project.apply.lock.held} — 락 획득 후 커밋 완료까지 소유 시간</li>
     * </ul>
     */
    private void recordLockTiming(Long projectId, long updateStartNanos, long lockAcquiredNanos) {
        meterRegistry.timer("project.apply.lock.acquire")
                .record(lockAcquiredNanos - updateStartNanos, TimeUnit.NANOSECONDS);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                long heldNanos = System.nanoTime() - lockAcquiredNanos;
                meterRegistry.timer("project.apply.lock.held")
                        .record(heldNanos, TimeUnit.NANOSECONDS);
                log.info("project 행 락 소유 시간 - projectId: {}, held: {}ms, committed: {}",
                        projectId, heldNanos / 1_000_000.0, status == STATUS_COMMITTED);
            }
        });
    }

    private void validateNotSelfApplication(Project project, Long memberId) {
        if (project.getCreator().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.APPLICATION_SELF_PROJECT_FORBIDDEN);
        }
    }

    private void validateApplyPrecondition(Project project, Member member, Long projectId, Long memberId) {
        if (project.isCompleted()) {
            throw new CustomException(ErrorCode.PROJECT_RECRUITMENT_SUSPENDED);
        }

        if (applicationRepository.existsByProjectAndApplicant(project, member)) {
            throw new CustomException(ErrorCode.PROJECT_APPLICATION_ALREADY_EXISTS);
        }

        if (projectMemberRepository.existsByProjectIdAndMemberId(projectId, memberId)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
        }
    }

    private void validateRecruitmentPosition(Long projectId, JobPosition jobPosition) {
        RecruitmentState recruitmentState = recruitmentStateRepository.findByProjectIdAndJobPosition(projectId, jobPosition)
                .orElseThrow(() -> new CustomException(ErrorCode.RECRUITMENT_POSITION_NOT_RECRUITING));

        if (recruitmentState.isClosed()) {
            throw new CustomException(ErrorCode.RECRUITMENT_POSITION_CLOSED);
        }
    }

    private ProjectApplication createAndSaveApplication(Project project, Member member, JobPosition jobPosition, String motivation) {
        ProjectApplication application = ProjectApplication.create(
                project,
                member,
                jobPosition,
                motivation
        );

        return applicationRepository.save(application);
    }

    /**
     * 동적 지원서 답변을 EAV(application_answer)로 저장합니다.
     * 질문마다 1행씩 생성되며, 필수 질문에 답이 없으면 예외를 던집니다.
     */
    private void saveAnswers(Project project, ProjectApplication application, Map<Long, String> answers) {
        List<ApplicationQuestion> questions =
                applicationQuestionRepository.findByProjectIdOrderByDisplayOrderAsc(project.getId());

        if (questions.isEmpty()) {
            return;
        }

        Map<Long, String> given = (answers == null) ? Map.of() : answers;
        List<ApplicationAnswer> toSave = new ArrayList<>();

        for (ApplicationQuestion question : questions) {
            String value = given.get(question.getId());

            if (value == null || value.isBlank()) {
                if (question.isRequired()) {
                    throw new CustomException(ErrorCode.APPLICATION_ANSWER_REQUIRED);
                }
                continue;
            }

            // value 가 VARCHAR 라 DB 가 타입을 막아주지 못하므로 애플리케이션에서 검증한다. (EAV 비용)
            validateAnswerType(question, value);

            toSave.add(ApplicationAnswer.of(application, question, value));
        }

        applicationAnswerRepository.saveAll(toSave);
    }

    /**
     * 질문 타입에 맞는 답변인지 검증합니다.
     * EAV 는 모든 값을 VARCHAR 로 저장하므로 타입 안전성을 애플리케이션이 전담합니다.
     */
    private void validateAnswerType(ApplicationQuestion question, String value) {
        switch (question.getInputType()) {
            case NUMBER -> {
                // 숫자 컬럼(valInt, INT)에 저장되므로 int 로 파싱 가능한 값만 허용 (범위 초과도 차단)
                try {
                    Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new CustomException(ErrorCode.APPLICATION_ANSWER_INVALID_TYPE);
                }
            }
            case SELECT -> {
                if (!question.getOptions().contains(value)) {
                    throw new CustomException(ErrorCode.APPLICATION_ANSWER_INVALID_OPTION);
                }
            }
            case TEXT, TEXTAREA -> {
                // 자유 입력 - 별도 형식 검증 없음
            }
        }
    }

    private void publishApplyNotifications(Project project, Member member, ProjectApplication savedApplication) {
        List<Notification> notifications = notificationSaveService.saveForApply(
                project.getCreator(), member, project, savedApplication.getId()
        );

        eventPublisher.publishEvent(NotificationEvent.of(notifications));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectApplicationListResponse> getApplicationList(Long projectId, String requesterEmail) {
        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        List<RecruitmentState> recruitmentStates = recruitmentStateRepository.findByProjectIdWithJobPosition(projectId);

        return applicationRepository.findPendingByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(application -> {
                    RecruitmentState recruitmentState = recruitmentStates.stream()
                            .filter(rs -> rs.getJobPosition().getId().equals(application.getJobPosition().getId()))
                            .findFirst()
                            .orElseThrow(() -> new CustomException(ErrorCode.RECRUITMENT_NOT_FOUND));
                    return ProjectApplicationListResponse.from(application, recruitmentState);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationSearchResponse> searchApplications(Long projectId, String requesterEmail,
                                                              ApplicationSearchRequest request) {
        // 1단계: 프로젝트 조회 및 리더 권한 검증
        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        List<ApplicationSearchRequest.FilterCondition> filters =
                (request.filters() == null) ? List.of() : request.filters();

        // 2단계: 필터 조건이 리더의 질문/타입과 맞는지 검증
        Map<Long, ApplicationQuestion> questionMap = applicationQuestionRepository
                .findByProjectIdOrderByDisplayOrderAsc(projectId).stream()
                .collect(Collectors.toMap(ApplicationQuestion::getId, question -> question));


        validateFilters(filters, questionMap);

        // 3단계: EAV 동적 쿼리로 조건을 만족하는 지원서 조회
        List<ProjectApplication> matched = applicationSearchRepository.search(projectId, filters);

        // 4단계: 각 지원서의 답변을 함께 담아 응답
        return matched.stream()
                .map(application -> ApplicationSearchResponse.from(
                        application,
                        applicationAnswerRepository.findByApplicationIdWithQuestion(application.getId())
                ))
                .toList();
    }

    private void validateFilters(List<ApplicationSearchRequest.FilterCondition> filters,
                                 Map<Long, ApplicationQuestion> questionMap) {
        for (ApplicationSearchRequest.FilterCondition filter : filters) {
            ApplicationQuestion question = questionMap.get(filter.questionId());

            if (question == null) {
                throw new CustomException(ErrorCode.APPLICATION_FILTER_INVALID);
            }

            switch (filter.operator()) {
                case GTE, LTE -> {
                    // 숫자 비교는 NUMBER 질문 + int 범위 숫자일 때만 허용
                    if (question.getInputType() != QuestionInputType.NUMBER) {
                        throw new CustomException(ErrorCode.APPLICATION_FILTER_INVALID);
                    }
                    try {
                        Integer.parseInt(filter.value());
                    } catch (NumberFormatException e) {
                        throw new CustomException(ErrorCode.APPLICATION_FILTER_INVALID);
                    }
                }
                case EQ, CONTAINS -> {
                    // 숫자 질문에는 등가/포함 필터를 적용하지 않음
                    if (question.getInputType() == QuestionInputType.NUMBER) {
                        throw new CustomException(ErrorCode.APPLICATION_FILTER_INVALID);
                    }
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDetailResponse getApplicationDetail(Long projectId, Long applicationId, String requesterEmail) {
        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        ProjectApplication application = applicationRepository.findByIdWithApplicantAndJobPosition(projectId, applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        // 동적 지원서 답변(EAV)을 질문과 함께 조회
        List<ApplicationAnswer> answers = applicationAnswerRepository.findByApplicationIdWithQuestion(applicationId);

        return ApplicationDetailResponse.from(application, answers);
    }

    @Override
    public ApplicationDecisionResponse decide(Long applicationId, ApplicationDecisionRequest request, String requesterEmail) {
        // 1단계: 엔티티 조회
        ProjectApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        // 2단계: 권한 검증
        validateProjectCreator(application, requesterEmail);

        // 3단계: 비즈니스 규칙 검증
        validateDecisionPrecondition(application);

        // 4단계: 결정 처리
        return processDecision(application, request.decision());
    }

    private void validateProjectCreator(ProjectApplication application, String requesterEmail) {
        if (!application.getProject().getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }
    }

    private void validateDecisionPrecondition(ProjectApplication application) {
        if (application.getProject().isCompleted()) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new CustomException(ErrorCode.APPLICATION_ALREADY_DECIDED);
        }
    }

    private ApplicationDecisionResponse processDecision(ProjectApplication application, ApplicationStatus decision) {
        application.updateStatus(decision);

        if (decision == ApplicationStatus.ACCEPTED) {
            return processAcceptance(application);
        }
        return processRejection(application);
    }

    private ApplicationDecisionResponse processAcceptance(ProjectApplication application) {
        Long projectId = application.getProject().getId();
        Long applicantId = application.getApplicant().getId();
        Long projectCreatorId = application.getProject().getCreator().getId();

        projectMemberService.addMember(
                projectId,
                applicantId,
                application.getJobPosition()
        );

        Notification notification = notificationSaveService.saveForApprove(
                application.getApplicant(),
                application.getProject(),
                projectCreatorId,
                application.getId()
        );

        eventPublisher.publishEvent(NotificationEvent.of(notification));

        return ApplicationDecisionResponse.acceptResponseDto(
                application.getId(),
                application.getProject().getId(),
                application.getApplicant().getId(),
                application.getStatus()
        );
    }

    private ApplicationDecisionResponse processRejection(ProjectApplication application) {
        Long projectCreatorId = application.getProject().getCreator().getId();

        Notification notification = notificationSaveService.saveForReject(
                application.getApplicant(),
                application.getProject(),
                projectCreatorId,
                application.getId()
        );

        eventPublisher.publishEvent(NotificationEvent.of(notification));

        return ApplicationDecisionResponse.rejectResponseDto(
                application.getId(),
                application.getStatus()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppliedProjectResponse> getAppliedProjects(CustomSecurityUserDetails userDetails) {
        List<ProjectApplication> applications = applicationRepository.findAllByApplicantId(userDetails.getMemberId());

        return applications.stream()
                .map(AppliedProjectResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationPageResponse getApplicationPage(Long projectId, Long memberId) {
        // 1단계: 엔티티 조회
        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 2단계: 프로젝트 리더 접근 차단
        if (project.getCreator().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.APPLICATION_SELF_PROJECT_FORBIDDEN);
        }

        // 3단계: 이미 프로젝트 멤버인 경우 접근 차단
        if (projectMemberRepository.existsByProjectIdAndMemberId(projectId, memberId)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
        }

        // 4단계: 모집 중인 포지션만 조회 (마감된 포지션 제외)
        List<RecruitmentState> recruitments = recruitmentStateRepository.findByProjectIdWithJobPosition(projectId)
                .stream()
                .filter(r -> !r.isClosed())
                .toList();

        return ApplicationPageResponse.of(member, recruitments);
    }

    @Override
    public ApplicationCancelResponse cancel(Long applicationId, Long memberId) {
        // 1단계: 지원서 조회
        ProjectApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        // 2단계: 본인 확인
        if (!application.isApplicant(memberId)) {
            throw new CustomException(ErrorCode.APPLICATION_CANCEL_FORBIDDEN);
        }

        // 3단계: PENDING 상태 확인
        if (!application.isPending()) {
            throw new CustomException(ErrorCode.APPLICATION_CANCEL_NOT_ALLOWED);
        }

        // 4단계: 취소 처리
        application.cancel();

        log.info("지원 취소 완료 - applicationId: {}, memberId: {}", applicationId, memberId);

        return ApplicationCancelResponse.of(
                application.getId(),
                application.getProject().getId(),
                application.getProject().getName()
        );
    }
}
