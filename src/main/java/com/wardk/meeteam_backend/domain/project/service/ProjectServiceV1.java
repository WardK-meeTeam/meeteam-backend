package com.wardk.meeteam_backend.domain.project.service;

import com.wardk.meeteam_backend.domain.member.entity.JobType;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Category;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.CategoryRepository;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import com.wardk.meeteam_backend.global.loginRegister.FileStore;
import com.wardk.meeteam_backend.global.loginRegister.repository.MemberRepository;
import com.wardk.meeteam_backend.web.project.dto.ProjectPostRequsetDto;
import com.wardk.meeteam_backend.web.project.dto.ProjectPostResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProjectServiceV1 implements ProjectService{

    private final FileStore fileStore;
    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional
    @Override
    public ProjectPostResponseDto postProject(ProjectPostRequsetDto projectPostRequsetDto, MultipartFile file, String requesterEmail) {

        Member creator = memberRepository.findOptionByEmail(requesterEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        String storeFileName = null;
        if (file != null && !file.isEmpty()){
        storeFileName = fileStore.storeFile(file).getStoreFileName();
        }

        Category category = new Category(projectPostRequsetDto.getProjectCategory());

        categoryRepository.save(category);

        Project project = Project.builder()
                .name(projectPostRequsetDto.getProjectName())
                .platformCategory(projectPostRequsetDto.getPlatformCategory())
                .imageUrl(storeFileName)
                .offlineRequired(projectPostRequsetDto.getOfflineRequired())
                .description(projectPostRequsetDto.getDescription())
                .creator(creator)
                .build();

        project.addCategory(category);

        ProjectMember projectMember = ProjectMember.builder()
                .jobType(JobType.PM)
                .build();

        projectMember.assignMember(creator);
        project.joinMember(projectMember);

        Project projectSaved = projectRepository.save(project);
        projectMemberRepository.save(projectMember);

        ProjectPostResponseDto projectPostResponseDto = ProjectPostResponseDto.from(projectSaved);

        return projectPostResponseDto;
    }
}
