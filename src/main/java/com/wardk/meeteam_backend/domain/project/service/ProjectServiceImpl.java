package com.wardk.meeteam_backend.domain.project.service;

import com.wardk.meeteam_backend.domain.applicant.entity.ProjectCategoryApplication;
import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.member.repository.SubCategoryRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectSkill;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectMember.service.ProjectMemberService;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import com.wardk.meeteam_backend.global.loginRegister.FileStore;
import com.wardk.meeteam_backend.web.project.dto.*;
import com.wardk.meeteam_backend.web.projectMember.dto.ProjectMemberListResponse;
import com.wardk.meeteam_backend.web.projectMember.dto.ProjectUpdateResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final FileStore fileStore;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final SkillRepository skillRepository;
    private final ProjectMemberService projectMemberService;

    @Override
    public ProjectPostResponse postProject(ProjectPostRequest projectPostRequest, MultipartFile file, String requesterEmail) {

        Member creator = memberRepository.findOptionByEmail(requesterEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        String storeFileName = getStoreFileName(file);

        Project project = Project.createProject(
                creator,
                projectPostRequest.getProjectName(),
                projectPostRequest.getDescription(),
                projectPostRequest.getProjectCategory(),
                projectPostRequest.getPlatformCategory(),
                storeFileName,
                projectPostRequest.isOfflineRequired(),
                projectPostRequest.getEndDate()
        );


        projectPostRequest.getRecruitments().forEach(recruitment -> {
            SubCategory subCategory = subCategoryRepository.findByName(recruitment.getSubCategory())
                    .orElseThrow(() -> new CustomException(ErrorCode.SUBCATEGORY_NOT_FOUND));

            ProjectCategoryApplication projectCategoryApplication = ProjectCategoryApplication.createProjectCategoryApplication(subCategory, recruitment.getRecruitmentCount());
            project.addRecruitment(projectCategoryApplication);
        });

        projectPostRequest.getProjectSkills().forEach(skillDto -> {
            Skill skill = skillRepository.findBySkillName(skillDto.getSkillName())
                    .orElseThrow(() -> new CustomException(ErrorCode.SKILL_NOT_FOUND));

            ProjectSkill projectSkill = ProjectSkill.createProjectSkill(skill);
            project.addProjectSkill(projectSkill);
        });

        projectRepository.save(project);

        SubCategory subCategory = subCategoryRepository.findByName(projectPostRequest.getSubCategory())
                .orElseThrow(() -> new CustomException(ErrorCode.SUBCATEGORY_NOT_FOUND));

        projectMemberService.addCreator(project.getId(), creator.getId(), subCategory);

        return ProjectPostResponse.from(project);
    }

    @Override
    public List<ProjectListResponse> getProjectList() {

        List<Project> projects = projectRepository.findAllWithCreatorAndSkills();

        return projects.stream()
                .map(ProjectListResponse::responseDto)
                .toList();
    }

    @Override
    public ProjectResponse getProject(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));


        return ProjectResponse.responseDto(project);
    }

    @Override
    public ProjectUpdateResponse updateProject(Long projectId, ProjectUpdateRequest request, MultipartFile file, String requesterEmail) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if(!project.getCreator().getEmail().equals(requesterEmail)){
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        String storeFileName = getStoreFileName(file);

        project.updateProject(
                request.getName(),
                request.getDescription(),
                request.getProjectCategory(),
                request.getPlatformCategory(),
                storeFileName,
                request.isOfflineRequired(),
                request.getStatus(),
                request.getStartDate(),
                request.getEndDate()
        );

        List<ProjectCategoryApplication> recruitments = request.getRecruitments().stream()
                .map(recruitment -> {
                    SubCategory subCategory = subCategoryRepository.findByName(recruitment.getSubCategory())
                            .orElseThrow(() -> new CustomException(ErrorCode.SUBCATEGORY_NOT_FOUND));

                    return ProjectCategoryApplication.createProjectCategoryApplication(subCategory, recruitment.getRecruitmentCount());
                }).toList();

        List<ProjectSkill> skills = request.getSkills().stream()
                .map(skillDto -> {
                    Skill skill = skillRepository.findBySkillName(skillDto.getSkillName())
                            .orElseThrow(() -> new CustomException(ErrorCode.SKILL_NOT_FOUND));

                    return ProjectSkill.createProjectSkill(skill);
                }).toList();

        project.updateRecruitments(recruitments);
        project.updateProjectSkills(skills);

        return ProjectUpdateResponse.responseDto(projectId);
    }

    @Override
    public ProjectDeleteResponse deleteProject(Long projectId, String requesterEmail) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if(!project.getCreator().getEmail().equals(requesterEmail)){
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        projectRepository.delete(project);

        return ProjectDeleteResponse.responseDto(projectId, project.getName());
    }

    private String getStoreFileName(MultipartFile file) {
        String storeFileName = null;
        if (file != null && !file.isEmpty()) {
            storeFileName = fileStore.storeFile(file).getStoreFileName();
        }

        return storeFileName;
    }
}
