package com.wardk.meeteam_backend.domain.projectMember.service;

import com.wardk.meeteam_backend.domain.applicant.entity.ProjectCategoryApplication;
import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.global.aop.Retry;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.web.projectMember.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public void addCreator(Long projectId, Long memberId, SubCategory subCategory) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (projectMemberRepository.existsByProjectIdAndMemberId(projectId, memberId)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
        }

        ProjectMember projectMember = ProjectMember.createProjectMember(member, subCategory);
        project.joinMember(projectMember);

        projectMemberRepository.save(projectMember);
    }

    @Retry
    @Override
    public void addMember(Long projectId, Long memberId, SubCategory subCategory) {

        Project project = projectRepository.findByIdWithRecruitment(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (projectMemberRepository.existsByProjectIdAndMemberId(projectId, memberId)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
        }

        ProjectMember projectMember = ProjectMember.createProjectMember(member, subCategory);
        project.joinMember(projectMember);

        ProjectCategoryApplication projectCategoryApplication = project.getRecruitments().stream()
                .filter(r -> r.getSubCategory().getId().equals(subCategory.getId()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.RECRUITMENT_NOT_FOUND));

        projectCategoryApplication.increaseCurrentCount();

        projectMemberRepository.save(projectMember);
    }

    @Override
    public List<ProjectMemberListResponse> getProjectMembers(Long projectId) {

        Project project = projectRepository.findByIdWithMembers(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        Long creatorId = project.getCreator().getId();

        return project.getMembers().stream()
                .map(pm -> {
                    Member member = pm.getMember();
                    return ProjectMemberListResponse.responseDto(
                            member.getId(),
                            member.getRealName(),
                            member.getStoreFileName(),
                            creatorId.equals(member.getId())
                    );
                }).toList();
    }

    @Override
    public DeleteResponse deleteProjectMember(DeleteRequest request, String requesterEmail) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        Member creator = project.getCreator();

        if (!creator.getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        if (creator.getId().equals(request.getMemberId())) {
            throw new CustomException(ErrorCode.CREATOR_DELETE_FORBIDDEN);
        }

        if (!projectMemberRepository.existsByProjectIdAndMemberId(request.getProjectId(), request.getMemberId())) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_NOT_FOUND);
        }

        projectMemberRepository.deleteByProjectIdAndMemberId(request.getProjectId(), request.getMemberId());

        return DeleteResponse.responseDto(
                project.getId(),
                request.getMemberId()
        );
    }

    @Override
    public WithdrawResponse withdraw(WithdrawRequest request, String requesterEmail) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        Member member = memberRepository.findOptionByEmail(requesterEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (project.getCreator().getId().equals(member.getId())) {
            throw new CustomException(ErrorCode.CREATOR_DELETE_FORBIDDEN);
        }

        ProjectMember projectMember = projectMemberRepository.findByProjectIdAndMemberId(project.getId(), member.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));

        projectMemberRepository.delete(projectMember);

        return WithdrawResponse.responseDto(
                project.getId(),
                member.getId()
        );
    }
}
