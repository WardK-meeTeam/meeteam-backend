package com.wardk.meeteam_backend.domain.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wardk.meeteam_backend.domain.recruitment.entity.RecruitmentState;
import com.wardk.meeteam_backend.domain.recruitment.entity.RecruitmentTechStack;
import com.wardk.meeteam_backend.domain.recruitment.repository.RecruitmentStateRepository;
import com.wardk.meeteam_backend.domain.recruitment.service.RecruitmentDomainService;
import com.wardk.meeteam_backend.domain.file.service.S3FileService;
import com.wardk.meeteam_backend.domain.job.entity.JobField;
import com.wardk.meeteam_backend.domain.job.entity.JobFieldCode;
import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.job.entity.JobPositionCode;
import com.wardk.meeteam_backend.domain.job.entity.TechStack;
import com.wardk.meeteam_backend.domain.job.repository.JobFieldRepository;
import com.wardk.meeteam_backend.domain.job.repository.JobFieldTechStackRepository;
import com.wardk.meeteam_backend.domain.job.repository.JobPositionRepository;
import com.wardk.meeteam_backend.domain.job.repository.TechStackRepository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.notification.ProjectEndEvent;
import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.domain.pr.entity.ProjectRepo;
import com.wardk.meeteam_backend.domain.pr.repository.ProjectRepoRepository;
import com.wardk.meeteam_backend.domain.project.entity.*;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.project.service.dto.ProjectPostCommand;
import com.wardk.meeteam_backend.domain.project.service.dto.RecruitmentCommand;
import com.wardk.meeteam_backend.domain.project.vo.RecruitmentDeadline;
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
    private final JobFieldRepository jobFieldRepository;
    private final JobPositionRepository jobPositionRepository;
    private final TechStackRepository techStackRepository;
    private final JobFieldTechStackRepository jobFieldTechStackRepository;
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
    private final RecruitmentDomainService recruitmentDomainService;

    @CacheEvict(value = "mainPageProjects", allEntries = true)
    @Counted("project.create")
    @Override
    public ProjectPostResponse createProject(ProjectPostCommand command, MultipartFile file, String requesterEmail) {
        Member creator = findMemberByEmail(requesterEmail);
        String imageUrl = uploadProjectImage(file, creator.getId());

        // 값 객체로 마감 정책 검증 캡슐화
        RecruitmentDeadline deadline = new RecruitmentDeadline(
                command.recruitmentDeadlineType(),
                command.endDate()
        );

        // 프로젝트 생성
        Project project = Project.create(command, creator, deadline, imageUrl);

        // 모집 정보 생성 및 추가 (도메인 서비스 위임)
        List<RecruitmentState> recruitmentStates = recruitmentDomainService.createRecruitmentStates(command.recruitments());
        recruitmentStates.forEach(project::addRecruitment);

        projectRepository.save(project);

        // 생성자를 프로젝트 멤버로 추가
        addCreatorAsProjectMember(project, creator, command.creatorJobPositionCode());

        return ProjectPostResponse.from(project);
    }

    private Member findMemberByEmail(String email) {
        return memberRepository.findOptionByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private String uploadProjectImage(MultipartFile file, Long uploaderId) {
        if (file != null && !file.isEmpty()) {
            return s3FileService.uploadFile(file, "images", uploaderId);
        }
        return null;
    }

    private void addCreatorAsProjectMember(Project project, Member creator, JobPositionCode creatorJobPositionCode) {
        JobPosition creatorPosition = jobPositionRepository
                .findByCode(creatorJobPositionCode)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_POSITION_NOT_FOUND));
        projectMemberService.addCreator(project.getId(), creator.getId(), creatorPosition);
    }

    @Override
    public List<ProjectListResponse> findAllProjects() {
        List<Project> projects = projectRepository.findAllWithCreatorAndSkills();
        return projects.stream()
                .map(ProjectListResponse::responseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public ProjectDetailResponse findProjectById(Long projectId, Long memberId) {
        Project project = projectRepository.findProjectDetailById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        // 좋아요 여부 확인 (비로그인 시 false)
        boolean isLiked = memberId != null
                && projectLikeRepository.existsByMemberIdAndProjectId(memberId, projectId);

        // 리더 여부 확인 (비로그인 시 false)
        boolean isLeader = memberId != null
                && project.getCreator().getId().equals(memberId);

        return ProjectDetailResponse.from(project, isLiked, isLeader);
    }

    @CacheEvict(value = "mainPageProjects", allEntries = true)
    @Override
    public ProjectUpdateResponse updateProject(Long projectId, ProjectUpdateRequest request, MultipartFile file, String requesterEmail) {
        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

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

        String imageUrl = uploadProjectImage(file, creator.getId());
        if (imageUrl == null) {
            imageUrl = project.getImageUrl();
        }
        project.updateProject(
                request.getName(),
                request.getDescription(),
                request.getProjectCategory(),
                request.getPlatformCategory(),
                imageUrl,
                startDate,
                endDate
        );

        List<RecruitmentState> recruitments = request.getRecruitments().stream()
                .map(recruitment -> {
                    JobField jobField = jobFieldRepository.findByCode(recruitment.jobFieldCode())
                            .orElseThrow(() -> new CustomException(ErrorCode.JOB_FIELD_NOT_FOUND));
                    JobPosition jobPosition = jobPositionRepository.findByCode(recruitment.jobPositionCode())
                            .orElseThrow(() -> new CustomException(ErrorCode.JOB_POSITION_NOT_FOUND));

                    if (!jobPosition.getJobField().getId().equals(jobField.getId())) {
                        throw new CustomException(ErrorCode.IS_NOT_ALLOWED_POSITION);
                    }

                    RecruitmentState recruitmentState = RecruitmentState.createRecruitmentState(
                            jobPosition, recruitment.recruitmentCount());

                    for (Long techStackId : recruitment.techStackIds()) {
                        TechStack techStack = techStackRepository.findById(techStackId)
                                .orElseThrow(() -> new CustomException(ErrorCode.TECH_STACK_NOT_FOUND));

                        if (!jobFieldTechStackRepository.existsByJobFieldIdAndTechStackId(jobField.getId(), techStack.getId())) {
                            throw new CustomException(ErrorCode.TECH_STACK_IS_NOT_MATCHING);
                        }

                        recruitmentState.addRecruitmentTechStack(RecruitmentTechStack.create(techStack));
                    }

                    return recruitmentState;
                })
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

        if (project.getRecruitmentStatus() == Recruitment.CLOSED) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        List<ProjectRepoResponse> responses = new ArrayList<>();

        for (String repoUrl : request.getRepoUrls()) {
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
    @Transactional(readOnly = true)
    public Page<ProjectConditionRequest> searchProjects(
            ProjectSearchCondition condition, Pageable pageable,
            CustomSecurityUserDetails userDetails) {

        Page<Project> projects = projectRepository.findAllSlicedForSearchAtCondition(condition, pageable);

        return projects.map(project -> {
            ProjectCounts totalCounts = recruitmentStateRepository.findTotalCountsByProject(project);
            List<ProjectMemberListResponse> projectMembers = projectMemberServiceImpl.getProjectMembers(project.getId());

            Long currentCount = totalCounts != null ? totalCounts.getCurrentCount() : 0L;
            Long recruitmentCount = totalCounts != null ? totalCounts.getRecruitmentCount() : 0L;

            boolean isLiked = userDetails != null
                    && projectLikeRepository.existsByMemberIdAndProjectId(userDetails.getMemberId(), project.getId());

            return new ProjectConditionRequest(project, isLiked, currentCount, recruitmentCount, projectMembers);
        });
    }

    @Cacheable(
            value = "mainPageProjects",
            key = "'page_0_size_20_sort_' + #pageable.sort.toString() + '_category_all'",
            condition = "#pageable.pageNumber == 0 && #userDetails == null && #pageable.pageSize == 20 && #condition.projectCategory == null"
    )
    @Override
    public Page<ProjectConditionMainPageResponse> searchMainPageProjects(CategoryCondition condition, Pageable pageable, CustomSecurityUserDetails userDetails) {
        Page<Project> projects = projectRepository.findProjectsFromMainPageCondition(condition, pageable);

        return projects.map(project -> {
            ProjectCounts totalCounts = recruitmentStateRepository.findTotalCountsByProject(project);
            List<ProjectMemberListResponse> projectMembers = projectMemberServiceImpl.getProjectMembers(project.getId());

            Long currentCount = totalCounts != null ? totalCounts.getCurrentCount() : 0L;
            Long recruitmentCount = totalCounts != null ? totalCounts.getRecruitmentCount() : 0L;

            boolean isLiked = userDetails != null
                    && projectLikeRepository.existsByMemberIdAndProjectId(userDetails.getMemberId(), project.getId());

            return new ProjectConditionMainPageResponse(project, isLiked, currentCount, recruitmentCount, projectMembers);
        });
    }


    @Override
    public List<MyProjectResponse> findMyProjects(CustomSecurityUserDetails userDetails) {
        List<ProjectMember> projectMembers = projectMemberRepository.findAllByMemberId(userDetails.getMemberId());
        return projectMembers.stream()
                .map(MyProjectResponse::responseDto)
                .toList();
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
    public List<ProjectRepoResponse> findProjectRepos(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        List<ProjectRepo> repos = projectRepoRepository.findAllByProjectId(projectId);

        return repos.stream()
                .map(ProjectRepoResponse::responseDto)
                .toList();
    }

    @Override
    public ProjectEndResponse endProject(Long projectId, String requesterEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        validateIsCompleted(project);
        project.endProject();
        return ProjectEndResponse.responseDto(projectId);
    }

    private static void validateIsCompleted(Project project) {
        if (project.isCompleted()) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }
    }
}
