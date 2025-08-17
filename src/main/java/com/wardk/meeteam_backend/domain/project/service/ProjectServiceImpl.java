package com.wardk.meeteam_backend.domain.project.service;

import com.wardk.meeteam_backend.domain.applicant.entity.ProjectCategoryApplication;
import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.member.repository.SubCategoryRepository;
//import com.wardk.meeteam_backend.domain.project.entity.Category;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectSkill;
//import com.wardk.meeteam_backend.domain.project.repository.CategoryRepository;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.domain.projectMember.service.ProjectMemberService;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import com.wardk.meeteam_backend.global.loginRegister.FileStore;
import com.wardk.meeteam_backend.web.project.dto.ProjectPostRequestDto;
import com.wardk.meeteam_backend.web.project.dto.ProjectPostResponseDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final FileStore fileStore;
    private final ProjectRepository projectRepository;
    //    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final SkillRepository skillRepository;
    private final ProjectMemberService projectMemberService;

    @Override
    @Transactional
    public ProjectPostResponseDto postProject(ProjectPostRequestDto projectPostRequestDto, MultipartFile file, String requesterEmail) {

        Member creator = memberRepository.findOptionByEmail(requesterEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        String storeFileName = getStoreFileName(file);

        Project project = Project.createProject(
                creator,
                projectPostRequestDto.getProjectName(),
                projectPostRequestDto.getDescription(),
                projectPostRequestDto.getProjectCategory(),
                projectPostRequestDto.getPlatformCategory(),
                storeFileName,
                projectPostRequestDto.getOfflineRequired(),
                projectPostRequestDto.getEndDate()
        );

        projectPostRequestDto.getRecruitments().forEach(recruitment -> {
            SubCategory subCategory = subCategoryRepository.findByName(recruitment.getSubCategory())
                    .orElseThrow(() -> new CustomException(ErrorCode.SUBCATEGORY_NOT_FOUND));

            ProjectCategoryApplication projectCategoryApplication = ProjectCategoryApplication.createProjectCategoryApplication(subCategory, recruitment.getRecruitmentCount());
            project.addRecruitment(projectCategoryApplication);
        });

        projectPostRequestDto.getProjectSkills().forEach(skillDto -> {
            Skill skill = skillRepository.findBySkillName(skillDto.getSkillName())
                    .orElseThrow(() -> new CustomException(ErrorCode.SKILL_NOT_FOUND));

            ProjectSkill projectSkill = ProjectSkill.createProjectSkill(skill);
            project.addProjectSkill(projectSkill);
        });

        projectRepository.save(project);

        SubCategory subCategory = subCategoryRepository.findByName(projectPostRequestDto.getSubCategory())
                .orElseThrow(() -> new CustomException(ErrorCode.SUBCATEGORY_NOT_FOUND));

        projectMemberService.addCreator(project.getId(), creator.getId(), subCategory);

        return ProjectPostResponseDto.from(project);
    }

    private String getStoreFileName(MultipartFile file) {
        String storeFileName = null;
        if (file != null && !file.isEmpty()) {
            storeFileName = fileStore.storeFile(file).getStoreFileName();
        }

        return storeFileName;
    }
}
