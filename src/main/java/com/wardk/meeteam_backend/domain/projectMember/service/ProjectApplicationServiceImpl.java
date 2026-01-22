package com.wardk.meeteam_backend.domain.projectMember.service;

import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.SubCategoryRepository;
import com.wardk.meeteam_backend.domain.notification.NotificationEvent;
import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.domain.notification.repository.NotificationRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
import com.wardk.meeteam_backend.domain.project.entity.Recruitment;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectMember.entity.ApplicationStatus;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMemberApplication;
import com.wardk.meeteam_backend.domain.projectMember.entity.WeekDay;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectApplicationRepository;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.web.projectMember.dto.*;
import io.micrometer.core.annotation.Counted;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectApplicationServiceImpl implements ProjectApplicationService {

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectApplicationRepository applicationRepository;
    private final ProjectMemberService projectMemberService;
    private final SubCategoryRepository subCategoryRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Counted("project.apply")
    @Override
    public ApplicationResponse apply(ApplicationRequest request, String applicantEmail) {

        Project project = projectRepository.findActiveById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        project.isCompleted();

        Member member = memberRepository.findOptionByEmail(applicantEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (applicationRepository.existsByProjectAndApplicant(project, member)) {
            throw new CustomException(ErrorCode.PROJECT_APPLICATION_ALREADY_EXISTS);
        }

        if (projectMemberRepository.existsByProjectIdAndMemberId(project.getId(), member.getId())) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
        }

        SubCategory subCategory = subCategoryRepository.findByName(request.getSubCategory())
                .orElseThrow(() -> new CustomException(ErrorCode.SUBCATEGORY_NOT_FOUND));

        List<WeekDay> weekDays = Arrays.stream(request.getAvailableDays().split(","))
                .map(String::trim)
                .map(WeekDay::valueOf)
                .toList();

        ProjectMemberApplication application = ProjectMemberApplication.createApplication
                        (project,
                        member,
                        subCategory,
                        request.getMotivation(),
                        request.getAvailableHoursPerWeek(),
                        weekDays,
                        request.getOfflineAvailable());

        ProjectMemberApplication memberApplication = applicationRepository.save(application);

        Long receiverId = project.getCreator().getId();
        Long actorId = member.getId();


        // 1. 팀장에게 지원 알림 저장
        Notification applyNotification = createNotification(
                project.getCreator(), project, actorId, NotificationType.PROJECT_APPLY, memberApplication.getId()
        );
        notificationRepository.save(applyNotification);

        // 2. 지원자에게 지원 완료 알림 저장
        Notification myApplyNotification = createNotification(
                member, project, actorId, NotificationType.PROJECT_MY_APPLY, memberApplication.getId()
        );
        notificationRepository.save(myApplyNotification);


        //프로젝트 리더 알림 발행 (SSE)
        eventPublisher.publishEvent(new NotificationEvent(
                receiverId,
                project.getId(),
                actorId,
                NotificationType.PROJECT_APPLY,
                memberApplication.getId()
        ));


        // 지원자 알림 발행 (SSE)
        eventPublisher.publishEvent(new NotificationEvent(
                actorId,
                project.getId(),
                actorId,
                NotificationType.PROJECT_MY_APPLY
        ));



        return ApplicationResponse.responseDto(memberApplication);
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
    public List<ProjectApplicationListResponse> getApplicationList(Long projectId, String requesterEmail) {

        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        return applicationRepository.findByProjectId(projectId).stream()
                .filter(application -> application.getStatus() == ApplicationStatus.PENDING)
                .map(ProjectApplicationListResponse::responseDto)
                .toList();
    }

    @Override
    public ApplicationDetailResponse getApplicationDetail(Long projectId, Long applicationId, String requesterEmail) {

        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        ProjectMemberApplication application = applicationRepository.findByIdWithApplicantAndSubCategory(projectId, applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        return ApplicationDetailResponse.responseDto(application);
    }

    @Override
    public ApplicationDecisionResponse decide(ApplicationDecisionRequest request, String requesterEmail) {

        ProjectMemberApplication application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        if (application.getProject().getStatus() == ProjectStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }

        if(!application.getProject().getCreator().getEmail().equals(requesterEmail)){
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        if(application.getStatus() != ApplicationStatus.PENDING) {
            throw new CustomException(ErrorCode.APPLICATION_ALREADY_DECIDED);
        }

        application.updateStatus(request.getDecision());


        Long projectId = application.getProject().getId(); // 프로젝트 id
        Long applicantId = application.getApplicant().getId(); // 지원자 id
        Long projectCreatorId = application.getProject().getCreator().getId(); // 프로젝트 팀장 id

        // decision이 ACCEPTED인 경우 프로젝트 멤버로 등록 후 리턴
        if (application.getStatus() == ApplicationStatus.ACCEPTED) {
            projectMemberService.addMember(
                    projectId,
                    applicantId,
                    application.getSubCategory()
            );

            // 승인 알림 저장
            Notification approveNotification = createNotification(
                    application.getApplicant(), // receiver: 지원자
                    application.getProject(),
                    projectCreatorId,           // actor: 팀장
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
                application.getApplicant(), // receiver: 지원자
                application.getProject(),
                projectCreatorId,           // actor: 팀장
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
    public List<AppliedProjectResponse> getAppliedProjects(CustomSecurityUserDetails userDetails) {

        List<ProjectMemberApplication> applications = applicationRepository.findAllByApplicantId(userDetails.getMemberId());

        return applications.stream()
                .filter(application -> application.getStatus() == ApplicationStatus.PENDING)
                .map(AppliedProjectResponse::responseDto)
                .toList();
    }
}