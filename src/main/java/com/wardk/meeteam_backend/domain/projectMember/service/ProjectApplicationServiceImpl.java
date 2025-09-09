package com.wardk.meeteam_backend.domain.projectMember.service;

import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.SubCategoryRepository;
import com.wardk.meeteam_backend.domain.notification.NotificationEvent;
import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectMember.entity.ApplicationStatus;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMemberApplication;
import com.wardk.meeteam_backend.domain.projectMember.entity.WeekDay;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectApplicationRepository;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.web.projectMember.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ApplicationResponse apply(ApplicationRequest request, String applicantEmail) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

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

        WeekDay weekDay = WeekDay.valueOf(request.getAvailableDay().toUpperCase());

        ProjectMemberApplication application = ProjectMemberApplication.createApplication
                        (project,
                        member,
                        subCategory,
                        request.getMotivation(),
                        request.getAvailableHoursPerWeek(),
                        weekDay,
                        request.getOfflineAvailable());

        ProjectMemberApplication saved = applicationRepository.save(application);

        Long receiverId = project.getCreator().getId();
        Long actorId = member.getId();

        //프로젝트 리더 알림
        eventPublisher.publishEvent(new NotificationEvent(
                receiverId,
                project.getId(),
                actorId,
                NotificationType.PROJECT_APPLY
        ));


        // 지원자 알림
        eventPublisher.publishEvent(new NotificationEvent(
                actorId,
                project.getId(),
                actorId,
                NotificationType.PROJECT_MY_APPLY
        ));



        return ApplicationResponse.responseDto(saved);
    }



    @Override
    public List<ProjectApplicationListResponse> getApplicationList(Long projectId, String requesterEmail) {

        Project project = projectRepository.findById(projectId)
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

        Project project = projectRepository.findById(projectId)
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