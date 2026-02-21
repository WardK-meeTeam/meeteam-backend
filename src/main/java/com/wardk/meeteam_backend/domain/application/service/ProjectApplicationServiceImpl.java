package com.wardk.meeteam_backend.domain.application.service;

import com.wardk.meeteam_backend.domain.application.entity.ApplicationStatus;
import com.wardk.meeteam_backend.domain.application.entity.ProjectApplication;
import com.wardk.meeteam_backend.domain.application.repository.ProjectApplicationRepository;
import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.job.repository.JobPositionRepository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.notification.NotificationEvent;
import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.domain.notification.repository.NotificationRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final ProjectMemberService projectMemberService;
    private final JobPositionRepository jobPositionRepository;
    private final RecruitmentStateRepository recruitmentStateRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Counted("project.apply")
    @Override
    public ApplicationResponse apply(Long projectId, Long memberId, ApplicationRequest request) {
        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        validateProjectNotCompleted(project);

        // 프로젝트 리더는 자신의 프로젝트에 지원 불가
        if (project.getCreator().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.APPLICATION_SELF_PROJECT_FORBIDDEN);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (applicationRepository.existsByProjectAndApplicant(project, member)) {
            throw new CustomException(ErrorCode.PROJECT_APPLICATION_ALREADY_EXISTS);
        }

        if (projectMemberRepository.existsByProjectIdAndMemberId(projectId, memberId)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
        }

        JobPosition jobPosition = jobPositionRepository.findByCode(request.jobPositionCode())
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_POSITION_NOT_FOUND));

        // 해당 프로젝트에서 해당 포지션으로 모집 중인지 확인
        recruitmentStateRepository.findAvailableByProjectIdAndJobPosition(projectId, jobPosition)
                .orElseThrow(() -> new CustomException(ErrorCode.RECRUITMENT_POSITION_NOT_AVAILABLE));

        ProjectApplication application = ProjectApplication.create(
                project,
                member,
                jobPosition,
                request.motivation()
        );

        ProjectApplication savedApplication = applicationRepository.save(application);

        Long receiverId = project.getCreator().getId();
        Long actorId = member.getId();

        // 1. 팀장에게 지원 알림 저장
        Notification applyNotification = createNotification(
                project.getCreator(), project, actorId, NotificationType.PROJECT_APPLY, savedApplication.getId()
        );
        notificationRepository.save(applyNotification);

        // 2. 지원자에게 지원 완료 알림 저장
        Notification myApplyNotification = createNotification(
                member, project, actorId, NotificationType.PROJECT_MY_APPLY, savedApplication.getId()
        );
        notificationRepository.save(myApplyNotification);

        // 프로젝트 리더 알림 발행 (SSE)
        eventPublisher.publishEvent(new NotificationEvent(
                receiverId,
                project.getId(),
                actorId,
                NotificationType.PROJECT_APPLY,
                savedApplication.getId()
        ));

        // 지원자 알림 발행 (SSE)
        eventPublisher.publishEvent(new NotificationEvent(
                actorId,
                project.getId(),
                actorId,
                NotificationType.PROJECT_MY_APPLY
        ));

        log.info("프로젝트 지원 완료 - projectId: {}, applicantId: {}, jobPositionCode: {}",
                projectId, memberId, request.jobPositionCode());

        return ApplicationResponse.from(savedApplication);
    }

    private static void validateProjectNotCompleted(Project project) {
        if (project.isCompleted()) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }
    }

    private Notification createNotification(Member receiver, Project project, Long actorId, NotificationType type, Long applicationId) {
        return Notification.builder()
                .receiver(receiver)
                .project(project)
                .actorId(actorId)
                .type(type)
                .applicationId(applicationId)
                .isRead(false)
                .build();
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
    public ApplicationDetailResponse getApplicationDetail(Long projectId, Long applicationId, String requesterEmail) {
        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        ProjectApplication application = applicationRepository.findByIdWithApplicantAndJobPosition(projectId, applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        return ApplicationDetailResponse.from(application);
    }

    @Override
    public ApplicationDecisionResponse decide(ApplicationDecisionRequest request, String requesterEmail) {
        ProjectApplication application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        if (application.getProject().isCompleted()) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }

        if (!application.getProject().getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new CustomException(ErrorCode.APPLICATION_ALREADY_DECIDED);
        }

        application.updateStatus(request.getDecision());

        Long projectId = application.getProject().getId();
        Long applicantId = application.getApplicant().getId();
        Long projectCreatorId = application.getProject().getCreator().getId();

        // decision이 ACCEPTED인 경우 프로젝트 멤버로 등록 후 리턴
        if (application.getStatus() == ApplicationStatus.ACCEPTED) {
            projectMemberService.addMember(
                    projectId,
                    applicantId,
                    application.getJobPosition()
            );

            // 승인 알림 저장
            Notification approveNotification = createNotification(
                    application.getApplicant(),
                    application.getProject(),
                    projectCreatorId,
                    NotificationType.PROJECT_APPROVE,
                    application.getId()
            );
            notificationRepository.save(approveNotification);

            eventPublisher.publishEvent(new NotificationEvent(
                    applicantId,
                    projectId,
                    projectCreatorId,
                    NotificationType.PROJECT_APPROVE
            ));

            return ApplicationDecisionResponse.acceptResponseDto(
                    application.getId(),
                    application.getProject().getId(),
                    application.getApplicant().getId(),
                    application.getStatus()
            );
        }

        // 거절 알림 저장
        Notification rejectNotification = createNotification(
                application.getApplicant(),
                application.getProject(),
                projectCreatorId,
                NotificationType.PROJECT_REJECT,
                application.getId()
        );
        notificationRepository.save(rejectNotification);

        eventPublisher.publishEvent(new NotificationEvent(
                applicantId,
                projectId,
                projectCreatorId,
                NotificationType.PROJECT_REJECT
        ));

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
                .filter(application -> application.getStatus() == ApplicationStatus.PENDING)
                .map(AppliedProjectResponse::from)
                .toList();
    }
}
