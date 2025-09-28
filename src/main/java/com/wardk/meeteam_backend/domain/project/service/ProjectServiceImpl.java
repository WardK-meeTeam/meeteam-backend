package com.wardk.meeteam_backend.domain.project.service;

import com.wardk.meeteam_backend.domain.applicant.entity.ProjectCategoryApplication;
import com.wardk.meeteam_backend.domain.applicant.repository.ProjectCategoryApplicationRepository;
import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.file.service.S3FileService;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.member.repository.SubCategoryRepository;
import com.wardk.meeteam_backend.domain.pr.entity.ProjectRepo;
import com.wardk.meeteam_backend.domain.pr.repository.ProjectRepoRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectSkill;
import com.wardk.meeteam_backend.domain.project.entity.Recruitment;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectLike.repository.ProjectLikeRepository;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.domain.projectMember.service.ProjectMemberService;
import com.wardk.meeteam_backend.domain.projectMember.service.ProjectMemberServiceImpl;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.github.GithubAppAuthService;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.web.mainpage.dto.MainPageProjectDto;
import com.wardk.meeteam_backend.web.mainpage.dto.SliceResponse;
import com.wardk.meeteam_backend.web.project.dto.*;
import com.wardk.meeteam_backend.web.projectCategoryApplication.dto.ProjectCounts;
import com.wardk.meeteam_backend.web.projectLike.dto.ProjectWithLikeDto;
import com.wardk.meeteam_backend.web.projectMember.dto.ProjectMemberListResponse;
import com.wardk.meeteam_backend.web.projectMember.dto.ProjectUpdateResponse;
import io.micrometer.core.annotation.Counted;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

//    private final FileUtil fileUtil;
    private final S3FileService s3FileService;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final SkillRepository skillRepository;
    private final ProjectMemberService projectMemberService;
    private final ProjectRepoRepository projectRepoRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final GithubAppAuthService githubAppAuthService;
    private final ProjectLikeRepository projectLikeRepository;
    private final ProjectCategoryApplicationRepository projectCategoryApplicationRepository;
    private final ProjectMemberServiceImpl projectMemberServiceImpl;


    @Counted("post.project")
    @Override
    public ProjectPostResponse postProject(ProjectPostRequest projectPostRequest, MultipartFile file, String requesterEmail) {

        Member creator = memberRepository.findOptionByEmail(requesterEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        String imageUrl = getImageUrl(file, creator.getId());

        LocalDate endDate = projectPostRequest.getEndDate();

        if(endDate != null && !endDate.isAfter(LocalDate.now())) {
            throw new CustomException(ErrorCode.INVALID_PROJECT_DATE);
        }

        Project project = Project.createProject(
                creator,
                projectPostRequest.getProjectName(),
                projectPostRequest.getDescription(),
                projectPostRequest.getProjectCategory(),
                projectPostRequest.getPlatformCategory(),
                imageUrl,
                projectPostRequest.getOfflineRequired(),
                endDate
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

//    @Override
//    public ProjectResponse getProjectV1(Long projectId) {
//
//        Project project = projectRepository.findById(projectId)
//                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
//
//
//        return ProjectResponse.responseDto(project);
//    }


    @Override
    public ProjectWithLikeDto getProjectV2(Long projectId) {

        ProjectWithLikeDto projectWithLikeDto = projectRepository.findProjectWithLikeCount(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));


        return projectWithLikeDto;
    }

    @Override
    public ProjectUpdateResponse updateProject(Long projectId, ProjectUpdateRequest request, MultipartFile file, String requesterEmail) {

        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        Member creator = memberRepository.findOptionByEmail(requesterEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (!creator.getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        if (endDate != null && startDate != null && !endDate.isAfter(startDate)) {
            throw new CustomException(ErrorCode.INVALID_PROJECT_DATE);
        }

        String imageUrl = getImageUrl(file, creator.getId());
        if (imageUrl == null) {
            imageUrl = project.getImageUrl();
        }

        project.updateProject(
                request.getName(),
                request.getDescription(),
                request.getProjectCategory(),
                request.getPlatformCategory(),
                imageUrl,
                request.getOfflineRequired(),
                request.getStatus(),
                startDate,
                endDate
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

        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        project.delete();

        return ProjectDeleteResponse.responseDto(projectId, project.getName());
    }

    @Override
    public List<ProjectRepoResponse> addRepo(Long projectId, ProjectRepoRequest request, String requesterEmail) {

        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        List<ProjectRepoResponse> responses = new ArrayList<>();

        for (String repoUrl :request.getRepoUrls()){
            String repoFullName = extractRepoFullName(repoUrl);

            if (projectRepoRepository.existsByRepoFullName(repoFullName)) {
                throw new CustomException(ErrorCode.PROJECT_REPO_ALREADY_EXISTS);
            }

            String[] parts = repoFullName.split("/");
            Long installationId = githubAppAuthService.fetchInstallationId(parts[0], parts[1]);

            ProjectRepo projectRepo = ProjectRepo.create(project, repoFullName, installationId);
            project.addRepo(projectRepo);

            projectRepoRepository.save(projectRepo);

            responses.add(ProjectRepoResponse.responseDto(projectRepo));
        }

        return responses;
    }

    @Override
    @Transactional
    public Page<ProjectConditionRequest> searchProject(
            ProjectSearchCondition condition, Pageable pageable,
            CustomSecurityUserDetails userDetails) {

        Page<Project> content = projectRepository.findAllSlicedForSearchAtCondition(condition, pageable);


        // 비로그인 -> isLiked 여부 항상 false;
        Page<ProjectConditionRequest> map = content.map(
                project ->  {

                    ProjectCounts totalCountsByProject = projectCategoryApplicationRepository.findTotalCountsByProject(project);
                    List<ProjectMemberListResponse> projectMembers = projectMemberServiceImpl.getProjectMembers(project.getId());

                    Long currentCount = totalCountsByProject.getCurrentCount();
                    Long recruitmentCount = totalCountsByProject.getRecruitmentCount();

                    boolean isLiked = false; // 비로그인인 경우 isLiked 무조건 false;
                    if (userDetails != null) { // 로그인이 경우
                        isLiked = projectLikeRepository.existsByMemberIdAndProjectId(userDetails.getMemberId(), project.getId());
                    }
                    return new ProjectConditionRequest(project, isLiked, currentCount, recruitmentCount, projectMembers);
                }
        );

        return map;
    }

    @Override
    public List<MyProjectResponse> getMyProject(CustomSecurityUserDetails userDetails) {

        List<ProjectMember> projectMembers = projectMemberRepository.findAllByMemberId(userDetails.getMemberId());

        return projectMembers.stream()
                .map(MyProjectResponse::responseDto)
                .toList();
    }

//    private String getStoreFileName(MultipartFile file) {
//        String storeFileName = null;
//        if (file != null && !file.isEmpty()) {
//            storeFileName = fileUtil.storeFile(file).getStoreFileName();
//        }
//
//        return storeFileName;
//    }

    private String getImageUrl(MultipartFile file, Long uploaderId) {
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = s3FileService.uploadFile(file, "images", uploaderId);
        }
        return imageUrl;
    }

    private String extractRepoFullName(String url) {
        if (url == null || url.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REPO_URL);
        }

        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || path.split("/").length < 3) {
                throw new CustomException(ErrorCode.INVALID_REPO_URL);
            }

            return path.substring(1);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FAILED_TO_PARSE_REPO_URL);
        }
    }


    @Transactional(readOnly = true)
    @Override
    public SliceResponse<MainPageProjectDto> getRecruitingProjectsByCategory(Long bigCategoryId, Pageable pageable) {
        if (bigCategoryId == null || bigCategoryId <= 0) {
            throw new CustomException(ErrorCode.MAIN_PAGE_CATEGORY_NOT_FOUND);
        }

        // 대분류별 + 모집중 상태 프로젝트 조회
        Slice<Project> projectSlice = projectRepository.findRecruitingProjectsByBigCategory(
                bigCategoryId,
                Recruitment.RECRUITING,
                pageable
        );

        // DTO 변환
        List<MainPageProjectDto> dtoList = projectSlice.getContent().stream()
                .map(MainPageProjectDto::responseDto)
                .collect(Collectors.toList());

        return SliceResponse.of(dtoList, projectSlice.hasNext(), projectSlice.getNumber());
    }

}
