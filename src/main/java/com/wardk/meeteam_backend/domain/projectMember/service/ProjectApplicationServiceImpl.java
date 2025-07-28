package com.wardk.meeteam_backend.domain.projectMember.service;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectApplication;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectApplicationRepository;
import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import com.wardk.meeteam_backend.global.loginRegister.repository.MemberRepository;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationRequest;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectApplicationServiceImpl implements ProjectApplicationService {

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final ProjectApplicationRepository applicationRepository;

    @Override
    public ApplicationResponse apply(ApplicationRequest request, String applicantEmail) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        Member member = memberRepository.findOptionByEmail(applicantEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (applicationRepository.existsByProjectAndApplicant(project, member)) {
            throw new CustomException(ErrorCode.PROJECT_APPLICATION_ALREADY_EXISTS);
        }

        project.getMembers().stream()
                .filter(pm -> pm.getMember().getId().equals(member.getId()))
                .findFirst()
                .ifPresent(pm -> {
                    throw new CustomException(ErrorCode.ALREADY_PROJECT_MEMBER);
                });

        String availableDays = String.join(",", request.getAvailableDays());

        ProjectApplication application = ProjectApplication.builder()
                .project(project)
                .applicant(member)
                .jobType(request.getJobType())
                .availableDays(availableDays)
                .availableHoursPerWeek(request.getAvailableHoursPerWeek())
                .motivation(request.getMotivation())
                .offlineAvailable(request.getOfflineAvailable())
                .build();

        ProjectApplication saved = applicationRepository.save(application);

        return ApplicationResponse.responseDto(
                saved.getId(),
                project.getId(),
                member.getId()
        );
    }
}
