package com.wardk.meeteam_backend.domain.projectMember.service;

import com.wardk.meeteam_backend.domain.member.entity.JobType;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import com.wardk.meeteam_backend.global.loginRegister.repository.MemberRepository;
import com.wardk.meeteam_backend.web.projectMember.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public void addMember(Long projectId, Long memberId, JobType jobType) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        boolean alreadyExists = project.getMembers().stream()
                .anyMatch(pm -> pm.getMember().getId().equals(member.getId()));

        if (alreadyExists) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
        }

        ProjectMember projectMember = ProjectMember.builder()
                .jobType(jobType)
                .build();

        projectMember.assignMember(member);
        project.joinMember(projectMember);

        projectMemberRepository.save(projectMember);
    }

    @Override
    public List<ProjectMemberListResponse> getProjectMembers(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        List<ProjectMember> members = project.getMembers();

        return members.stream()
                .map(pm -> {
                    Member member = pm.getMember();
                    return ProjectMemberListResponse.responseDto(
                            pm.getId(),
                            member.getNickName(),
                            member.getEmail(),
                            pm.getJobType()
                    );
                })
                .toList();
    }

    @Override
    @Transactional
    public DeleteResponse deleteProjectMember(DeleteRequest request, String requesterEmail) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if(!project.getCreator().getEmail().equals(requesterEmail)){
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        ProjectMember projectMember = project.getMembers().stream()
                .filter(pm -> pm.getMember().getId().equals(request.getMemberId()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));

        String email = projectMember.getMember().getEmail();
        Long memberId = projectMember.getMember().getId();

        projectMemberRepository.delete(projectMember);

        return DeleteResponse.responseDto(
                project.getId(),
                memberId,
                email
        );
    }

    @Override
    @Transactional
    public RoleUpdateResponse updateRole(RoleUpdateRequest request, String requesterEmail) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if(!project.getCreator().getEmail().equals(requesterEmail)){
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        ProjectMember projectMember = project.getMembers().stream()
                .filter(pm -> pm.getMember().getId().equals(request.getMemberId()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));

        projectMember.updateJobType(request.getJobType());

        return RoleUpdateResponse.responseDto(
                project.getId(),
                request.getMemberId(),
                request.getJobType()
        );
    }

    @Override
    @Transactional
    public UpdateOwnerResponse updateOwner(UpdateOwnerRequest request, String requesterEmail) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if(!project.getCreator().getEmail().equals(requesterEmail)){
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        ProjectMember projectMember = project.getMembers().stream()
                .filter(pm -> pm.getMember().getId().equals(request.getNewOwnerId()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));

        Member oldOwner = project.getCreator();
        Member newOwner = projectMember.getMember();

        if(oldOwner.equals(newOwner)){
            throw new CustomException(ErrorCode.CREATOR_TRANSFER_SELF_DENIED);
        }

        project.setCreator(newOwner);

        return UpdateOwnerResponse.responseDto(
                project.getId(),
                oldOwner.getId(),
                newOwner.getId()
        );
    }

    @Override
    @Transactional
    public WithdrawResponse withdraw(WithdrawRequest request, String requesterEmail) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        Member member = memberRepository.findOptionByEmail(requesterEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (project.getCreator().equals(member)) {
            throw new CustomException(ErrorCode.CREATOR_WITHDRAW_FORBIDDEN);
        }

        ProjectMember projectMember = project.getMembers().stream()
                .filter(pm -> pm.getMember().equals(member))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));

        projectMemberRepository.delete(projectMember);

        return WithdrawResponse.responseDto(
                project.getId(),
                member.getId()
        );
    }
}
