package com.wardk.meeteam_backend.domain.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wardk.meeteam_backend.domain.applicant.entity.RecruitmentState;
import com.wardk.meeteam_backend.domain.applicant.repository.RecruitmentStateRepository;
import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.file.service.S3FileService;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.notification.ProjectEndEvent;
import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.domain.pr.entity.ProjectRepo;
import com.wardk.meeteam_backend.domain.pr.repository.ProjectRepoRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectSkill;
import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectlike.repository.ProjectLikeRepository;
import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.projectmember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.domain.projectmember.service.ProjectMemberService;
import com.wardk.meeteam_backend.domain.projectmember.service.ProjectMemberServiceImpl;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.github.GithubAppAuthService;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.web.mainpage.dto.request.CategoryCondition;
import com.wardk.meeteam_backend.web.mainpage.dto.response.ProjectConditionMainPageResponse;
import com.wardk.meeteam_backend.web.project.dto.request.*;
import com.wardk.meeteam_backend.web.project.dto.response.*;
import com.wardk.meeteam_backend.web.recruitmentState.dto.response.ProjectCounts;
import com.wardk.meeteam_backend.web.projectlike.dto.response.ProjectWithLikeDto;
import com.wardk.meeteam_backend.web.projectmember.dto.request.*;
import com.wardk.meeteam_backend.web.projectmember.dto.response.*;
import com.wardk.meeteam_backend.web.projectmember.dto.request.*;
import com.wardk.meeteam_backend.web.projectmember.dto.response.*;
import io.micrometer.core.annotation.Counted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final S3FileService s3FileService;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final SkillRepository skillRepository;
    private final ProjectMemberService projectMemberService;
    private final ProjectRepoRepository projectRepoRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final GithubAppAuthService githubAppAuthService;
    private final ProjectLikeRepository projectLikeRepository;
    private final RecruitmentStateRepository recruitmentStateRepository;
    private final ProjectMemberServiceImpl projectMemberServiceImpl;
    private final WebClient.Builder webClientBuilder;
    private final ApplicationEventPublisher eventPublisher;


    @CacheEvict(value = "mainPageProjects", allEntries = true)
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
            RecruitmentState recruitmentState = RecruitmentState.createRecruitmentState(
                    recruitment.jobPosition(), recruitment.recruitmentCount());
            project.addRecruitment(recruitmentState);
        });

        projectPostRequest.getSkills().forEach(skillName -> {
            Skill skill = skillRepository.findBySkillName(skillName)
                    .orElseThrow(() -> new CustomException(ErrorCode.SKILL_NOT_FOUND));

            ProjectSkill projectSkill = ProjectSkill.createProjectSkill(skill);
            project.addProjectSkill(projectSkill);
        });

        projectRepository.save(project);

        JobPosition creatorJobPosition = projectPostRequest.getJobPosition();
        projectMemberService.addCreator(project.getId(), creator.getId(), creatorJobPosition);

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
    public ProjectWithLikeDto getProjectV2(Long projectId) {

        ProjectWithLikeDto projectWithLikeDto = projectRepository.findProjectWithLikeCount(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));


        return projectWithLikeDto;
    }

    @CacheEvict(value = "mainPageProjects", allEntries = true)
    @Override
    public ProjectUpdateResponse updateProject(Long projectId, ProjectUpdateRequest request, MultipartFile file, String requesterEmail) {

        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (project.getStatus() == ProjectStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }

        Member creator = memberRepository.findOptionByEmail(requesterEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
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

        List<RecruitmentState> recruitments = request.getRecruitments().stream()
                .map(recruitment -> RecruitmentState.createRecruitmentState(
                        recruitment.jobPosition(), recruitment.recruitmentCount()))
                .toList();

        List<ProjectSkill> skills = request.getSkills().stream()
                .map(skillName -> {
                    Skill skill = skillRepository.findBySkillName(skillName)
                            .orElseThrow(() -> new CustomException(ErrorCode.SKILL_NOT_FOUND));

                    return ProjectSkill.createProjectSkill(skill);
                }).toList();

        project.updateRecruitments(recruitments);
        project.updateProjectSkills(skills);

        return ProjectUpdateResponse.responseDto(projectId);
    }

    @CacheEvict(value = "mainPageProjects", allEntries = true)
    @Override
    public ProjectDeleteResponse deleteProject(Long projectId, String requesterEmail) {

        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        project.delete();

        List<Long> membersId = getMembersId(project);

        eventPublisher.publishEvent(new ProjectEndEvent(
                NotificationType.PROJECT_END,
                membersId,
                projectId,
                project.getName(),
                LocalDate.now()
        ));

        return ProjectDeleteResponse.responseDto(projectId, project.getName());
    }

    private static List<Long> getMembersId(Project project) {
        List<Long> membersId = project.getMembers()
                .stream()
                .map(p -> p.getMember().getId())
                .toList();
        return membersId;
    }

    @Override
    public List<ProjectRepoResponse> addRepo(Long projectId, ProjectRepoRequest request, String requesterEmail) {

        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (project.getStatus() == ProjectStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }

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

            String owner = parts[0];
            String repo = parts[1];

            if (installationId == null) {
                throw new CustomException(ErrorCode.GITHUB_APP_NOT_INSTALLED);
            }
            String installationToken = githubAppAuthService.getInstallationToken(installationId);

            JsonNode repoInfo = webClientBuilder.baseUrl("https://api.github.com").build()
                    .get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .headers(h -> h.setBearerAuth(installationToken))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();


            String description = repoInfo.get("description").asText();
            Long starCount = repoInfo.get("stargazers_count").asLong();
            Long watcherCount = repoInfo.get("watchers_count").asLong();
            LocalDateTime pushedAt = LocalDateTime.parse(repoInfo.get("pushed_at").asText().replace("Z", ""));
            String language = repoInfo.get("language").asText();

            log.info("description={}, starCount={}, watcherCount={}, pushedAt={}, language={}",
                    description, starCount, watcherCount, pushedAt, language);

            ProjectRepo projectRepo = ProjectRepo.create(project, repoFullName, installationId, description, starCount, watcherCount, pushedAt, language);
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

        Page<ProjectConditionRequest> map = content.map(
                project -> {

                    ProjectCounts totalCountsByProject = recruitmentStateRepository.findTotalCountsByProject(project);
                    List<ProjectMemberListResponse> projectMembers = projectMemberServiceImpl.getProjectMembers(project.getId());

                    Long currentCount = totalCountsByProject != null ? totalCountsByProject.getCurrentCount() : 0L;
                    Long recruitmentCount = totalCountsByProject != null ? totalCountsByProject.getRecruitmentCount() : 0L;
                    log.info("userDetails={}",userDetails);
                    boolean isLiked = false;
                    if (userDetails != null) {
                        isLiked = projectLikeRepository.existsByMemberIdAndProjectId(userDetails.getMemberId(), project.getId());
                    }
                    return new ProjectConditionRequest(project, isLiked, currentCount, recruitmentCount, projectMembers);
                }
        );

        return map;
    }

    @Cacheable(
            value = "mainPageProjects",
            key = "'page_0_size_20_sort_' + #pageable.sort.toString() + '_category_all'",
            condition = "#pageable.pageNumber == 0 && #userDetails == null && #pageable.pageSize == 20 && #condition.projectCategory == null"
    )
    @Override
    public Page<ProjectConditionMainPageResponse> searchMainPageProject(CategoryCondition condition, Pageable pageable, CustomSecurityUserDetails userDetails) {
        Page<Project> content = projectRepository.findProjectsFromMainPageCondition(condition, pageable);

        Page<ProjectConditionMainPageResponse> map = content.map(
                project -> {
                    ProjectCounts totalCountsByProject = recruitmentStateRepository.findTotalCountsByProject(project);
                    List<ProjectMemberListResponse> projectMembers = projectMemberServiceImpl.getProjectMembers(project.getId());

                    Long currentCount = totalCountsByProject != null ? totalCountsByProject.getCurrentCount() : 0L;
                    Long recruitmentCount = totalCountsByProject != null ? totalCountsByProject.getRecruitmentCount() : 0L;

                    boolean isLiked = false;
                    if (userDetails != null) {
                        isLiked = projectLikeRepository.existsByMemberIdAndProjectId(userDetails.getMemberId(), project.getId());
                    }

                    return new ProjectConditionMainPageResponse(project, isLiked, currentCount, recruitmentCount, projectMembers);
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
    public List<ProjectRepoResponse> getProjectRepos(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        List<ProjectRepo> repos = projectRepoRepository.findAllByProjectId(projectId);

        return repos.stream()
                .map(ProjectRepoResponse::responseDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectEndResponse endProject(Long projectId, String requesterEmail) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if(!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        if (project.getStatus() == ProjectStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }

        project.endProject();

        return ProjectEndResponse.responseDto(project.getId(), project.getStatus());
    }


}