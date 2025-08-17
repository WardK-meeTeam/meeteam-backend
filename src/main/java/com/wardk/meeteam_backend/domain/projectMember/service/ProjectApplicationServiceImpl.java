package com.wardk.meeteam_backend.domain.projectMember.service;

import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.SubCategoryRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectMember.entity.ApplicationStatus;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMemberApplication;
import com.wardk.meeteam_backend.domain.projectMember.entity.WeekDay;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectApplicationRepository;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationDecisionRequest;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationDecisionResponse;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationRequest;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        SubCategory subCategory = subCategoryRepository.findBySubCategory(request.getSubCategory())
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

        return ApplicationResponse.responseDto(
                saved.getId(),
                project.getId(),
                member.getId()
        );
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

        // decision이 ACCEPTED인 경우 프로젝트 멤버로 등록 후 리턴
        if (application.getStatus() == ApplicationStatus.ACCEPTED) {
            projectMemberService.addMember(
                    application.getProject().getId(),
                    application.getApplicant().getId(),
                    application.getSubCategory()
            );

            return ApplicationDecisionResponse.acceptResponseDto(
                    application.getId(),
                    application.getProject().getId(),
                    application.getApplicant().getId(),
                    application.getStatus()
            );
        }

        return ApplicationDecisionResponse.rejectResponseDto(
                application.getId(),
                application.getStatus()
        );
    }
}