package com.wardk.meeteam_backend.domain.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wardk.meeteam_backend.domain.recruitment.entity.RecruitmentState;
import com.wardk.meeteam_backend.domain.recruitment.entity.RecruitmentTechStack;
import com.wardk.meeteam_backend.domain.recruitment.repository.RecruitmentStateRepository;
import com.wardk.meeteam_backend.domain.recruitment.service.RecruitmentDomainService;
import com.wardk.meeteam_backend.domain.file.service.S3FileService;
import com.wardk.meeteam_backend.domain.job.entity.JobField;
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
import com.wardk.meeteam_backend.domain.notification.service.NotificationSaveService;
import com.wardk.meeteam_backend.domain.pr.entity.ProjectRepo;
import com.wardk.meeteam_backend.domain.pr.repository.ProjectRepoRepository;
import com.wardk.meeteam_backend.domain.project.entity.*;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.project.service.dto.ProjectEditCommand;
import com.wardk.meeteam_backend.domain.project.service.dto.ProjectPostCommand;
import com.wardk.meeteam_backend.domain.project.service.dto.RecruitmentEditCommand;
import com.wardk.meeteam_backend.domain.project.vo.RecruitmentDeadline;
import com.wardk.meeteam_backend.domain.projectlike.repository.ProjectLikeRepository;
import com.wardk.meeteam_backend.domain.application.entity.ApplicationStatus;
import com.wardk.meeteam_backend.domain.application.entity.ProjectApplication;
import com.wardk.meeteam_backend.domain.application.repository.ProjectApplicationRepository;
import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectMemberRole;
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
import com.wardk.meeteam_backend.web.mainpage.dto.response.ProjectCardResponse;
import com.wardk.meeteam_backend.web.project.dto.request.*;
import com.wardk.meeteam_backend.web.project.dto.response.*;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final NotificationSaveService notificationSaveService;
    private final ProjectApplicationRepository projectApplicationRepository;

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
        recruitmentStates.forEach(recruitment -> project.addRecruitment(recruitment));

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
    public ProjectDeleteResponse deleteProject(Long projectId, String requesterEmail) {
        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        project.delete();

        // 팀원 목록 조회
        List<Member> members = project.getMembers().stream()
                .map(pm -> pm.getMember())
                .toList();

        // 알림 저장 (서비스 레이어에서 동기적으로 저장)
        notificationSaveService.saveForProjectEnd(project, members);

        // 팀원 ID 목록 생성
        List<Long> membersId = members.stream()
                .map(Member::getId)
                .toList();

        // SSE 전송을 위한 이벤트 발행
        eventPublisher.publishEvent(new ProjectEndEvent(
                NotificationType.PROJECT_END,
                membersId,
                projectId,
                project.getName(),
                LocalDate.now()
        ));

        return ProjectDeleteResponse.responseDto(projectId, project.getName());
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
    public Page<ProjectCardResponse> searchProjects(
            ProjectSearchCondition condition, Pageable pageable,
            CustomSecurityUserDetails userDetails) {

        Page<Project> projects = projectRepository.findAllSlicedForSearchAtCondition(condition, pageable);

        return toProjectCardPage(projects, userDetails);
    }

    @Cacheable(
            value = "mainPageProjects",
            key = "'page_0_size_20_sort_' + #pageable.sort.toString() + '_category_all'",
            condition = "#pageable.pageNumber == 0 && #userDetails == null && #pageable.pageSize == 20 && #condition.projectCategory == null"
    )
    @Override
    public Page<ProjectCardResponse> searchMainPageProjects(CategoryCondition condition, Pageable pageable, CustomSecurityUserDetails userDetails) {
        Page<Project> projects = projectRepository.findProjectsFromMainPageCondition(condition, pageable);

        return toProjectCardPage(projects, userDetails);
    }

    private Page<ProjectCardResponse> toProjectCardPage(Page<Project> projects, CustomSecurityUserDetails userDetails) {
        List<Long> projectIds = projects.getContent().stream()
            .map(Project::getId)
            .toList();

        if (projectIds.isEmpty()) {
            return projects.map(p -> null);
        }

        // 배치 쿼리 1: 모집 현황 + 기술스택 한 번에 조회
        List<RecruitmentState> allRecruitments = recruitmentStateRepository.findAllByProjectIdsWithDetails(projectIds);
        Map<Long, List<RecruitmentState>> recruitmentMap = allRecruitments.stream()
            .collect(Collectors.groupingBy(rs -> rs.getProject().getId()));

        // 배치 쿼리 2: 좋아요 여부 한 번에 조회
        Set<Long> likedIds = findLikedProjectIds(userDetails, projectIds);

        return projects.map(project -> {
            List<RecruitmentState> recs = recruitmentMap.getOrDefault(project.getId(), Collections.emptyList());
            return ProjectCardResponse.from(project, recs, likedIds.contains(project.getId()));
        });
    }

    private Set<Long> findLikedProjectIds(CustomSecurityUserDetails userDetails, List<Long> projectIds) {
        if (userDetails == null) {
            return Collections.emptySet();
        }
        return projectLikeRepository.findLikedProjectIds(userDetails.getMemberId(), projectIds);
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
    public RecruitmentStatusResponse toggleRecruitmentStatus(Long projectId, String requesterEmail) {
        Project project = projectRepository.findByIdWithRecruitment(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        project.toggleRecruitmentStatus();
        projectRepository.save(project);
        return RecruitmentStatusResponse.from(project);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamManagementResponse getTeamManagement(Long projectId, String requesterEmail) {
        Project project = projectRepository.findByIdWithMembers(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }

        // 현재 팀원 수
        int currentMemberCount = project.getMembers().size();

        // 총 모집 정원
        int totalRecruitmentCount = project.getRecruitments().stream()
                .mapToInt(r -> r.getRecruitmentCount())
                .sum();

        // 대기중인 지원서 수
        long pendingApplicationCount = project.getApplications().stream()
                .filter(a -> a.getStatus() == ApplicationStatus.PENDING)
                .count();

        // 팀원 목록
        List<TeamManagementResponse.TeamMemberInfo> members = project.getMembers().stream()
                .map(pm -> TeamManagementResponse.TeamMemberInfo.builder()
                        .memberId(pm.getMember().getId())
                        .name(pm.getMember().getRealName())
                        .profileImageUrl(pm.getMember().getStoreFileName())
                        .jobFieldName(pm.getJobPosition().getJobField().getName())
                        .jobPositionName(pm.getJobPosition().getName())
                        .isLeader(pm.getRole() == ProjectMemberRole.LEADER)
                        .build())
                .toList();

        return TeamManagementResponse.builder()
                .currentMemberCount(currentMemberCount)
                .totalRecruitmentCount(totalRecruitmentCount)
                .pendingApplicationCount((int) pendingApplicationCount)
                .members(members)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectEditPrefillResponse getProjectEditPrefill(Long projectId, String requesterEmail) {
        Project project = projectRepository.findProjectForEdit(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        // 권한 확인: 리더만 수정 가능
        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_EDIT_FORBIDDEN);
        }

        // 리더 정보 조회
        ProjectMember leader = projectMemberRepository.findLeaderByProjectId(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));

        // 포지션별 대기 지원자 수 조회
        Map<Long, Long> pendingCountByPositionId = projectApplicationRepository
                .countPendingByProjectIdGroupByJobPositionId(projectId).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // 모집 포지션 정보 생성
        List<RecruitmentEditInfo> recruitmentInfos = project.getRecruitments().stream()
                .map(recruitment -> {
                    Long positionId = recruitment.getJobPosition().getId();
                    int pendingCount = pendingCountByPositionId.getOrDefault(positionId, 0L).intValue();
                    return RecruitmentEditInfo.from(recruitment, pendingCount);
                })
                .toList();

        return ProjectEditPrefillResponse.from(project, leader, recruitmentInfos);
    }

    @CacheEvict(value = "mainPageProjects", allEntries = true)
    @Override
    public ProjectEditResponse updateProject(Long projectId, ProjectEditCommand command, MultipartFile file, String requesterEmail) {
        Project project = projectRepository.findProjectForEdit(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        // 권한 확인: 리더만 수정 가능
        if (!project.getCreator().getEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.PROJECT_EDIT_FORBIDDEN);
        }

        // 모집 중단 상태에서는 수정 불가
        if (project.isSuspended()) {
            throw new CustomException(ErrorCode.PROJECT_EDIT_NOT_ALLOWED_SUSPENDED);
        }

        // 이미지 업로드
        Member creator = findMemberByEmail(requesterEmail);
        String imageUrl = uploadProjectImage(file, creator.getId());

        // 기본 정보 업데이트
        project.updateBasicInfo(
                command.name(),
                command.description(),
                command.projectCategory(),
                command.platformCategory(),
                command.githubRepositoryUrl(),
                command.communicationChannelUrl(),
                command.endDate(),
                imageUrl
        );

        // 모집 포지션 업데이트 및 자동 거절 처리
        int autoRejectedCount = updateRecruitments(project, command);

        // 프로젝트 모집 상태 자동 업데이트
        project.updateRecruitmentStatusBasedOnPositions();

        projectRepository.save(project);

        return ProjectEditResponse.from(project, autoRejectedCount);
    }

    /**
     * 모집 포지션을 업데이트합니다.
     *
     * @return 자동 거절된 지원자 수
     */
    private int updateRecruitments(Project project, ProjectEditCommand command) {
        int autoRejectedCount = 0;

        // 현재 모집 포지션 맵 생성 (recruitmentStateId -> RecruitmentState)
        Map<Long, RecruitmentState> currentRecruitmentMap = project.getRecruitments().stream()
                .collect(Collectors.toMap(RecruitmentState::getId, r -> r));

        // 요청에 포함된 모집 포지션 ID 목록
        Set<Long> requestedRecruitmentIds = command.recruitments().stream()
                .filter(r -> r.recruitmentStateId() != null)
                .map(RecruitmentEditCommand::recruitmentStateId)
                .collect(Collectors.toSet());

        // 삭제할 포지션 처리
        List<RecruitmentState> toRemove = new ArrayList<>();
        for (RecruitmentState recruitment : project.getRecruitments()) {
            if (!requestedRecruitmentIds.contains(recruitment.getId())) {
                // 승인 인원이 있으면 삭제 불가
                if (recruitment.getCurrentCount() > 0) {
                    throw new CustomException(ErrorCode.RECRUITMENT_HAS_APPROVED_MEMBERS);
                }

                // 대기 지원자 확인
                List<ProjectApplication> pendingApplications = projectApplicationRepository
                        .findPendingByProjectIdAndJobPositionId(project.getId(), recruitment.getJobPosition().getId());

                if (!pendingApplications.isEmpty()) {
                    // 대기 지원자가 있고 확인 플래그가 없으면 에러
                    if (!command.confirmDeletePositionsWithPendingApplicants()) {
                        throw new CustomException(ErrorCode.RECRUITMENT_HAS_PENDING_APPLICANTS);
                    }
                    // 대기 지원자 자동 거절 처리
                    for (ProjectApplication application : pendingApplications) {
                        application.updateStatus(ApplicationStatus.REJECTED);
                        autoRejectedCount++;
                    }
                }

                toRemove.add(recruitment);
            }
        }

        // 삭제 처리
        project.getRecruitments().removeAll(toRemove);

        // 기존 포지션 수정 및 신규 포지션 추가
        for (RecruitmentEditCommand recruitmentCmd : command.recruitments()) {
            if (recruitmentCmd.recruitmentStateId() != null) {
                // 기존 포지션 수정
                RecruitmentState recruitment = currentRecruitmentMap.get(recruitmentCmd.recruitmentStateId());
                if (recruitment != null) {
                    // 모집 인원 축소 검증
                    if (recruitmentCmd.recruitmentCount() < recruitment.getCurrentCount()) {
                        throw new CustomException(ErrorCode.RECRUITMENT_COUNT_BELOW_CURRENT);
                    }

                    // 모집 인원 업데이트
                    recruitment.updateRecruitmentCount(recruitmentCmd.recruitmentCount());

                    // 기술 스택 업데이트
                    List<TechStack> techStacks = techStackRepository.findByIdIn(recruitmentCmd.techStackIds());
                    List<RecruitmentTechStack> newTechStacks = techStacks.stream()
                            .map(RecruitmentTechStack::create)
                            .toList();
                    recruitment.replaceTechStacks(newTechStacks);

                    // 포지션 상태 업데이트 (인원 변경으로 인한 마감/재오픈)
                    if (recruitment.getCurrentCount() >= recruitment.getRecruitmentCount()) {
                        recruitment.close();
                    } else {
                        recruitment.reopen();
                    }
                }
            } else {
                // 신규 포지션 추가
                JobPosition jobPosition = jobPositionRepository.findByCode(recruitmentCmd.jobPositionCode())
                        .orElseThrow(() -> new CustomException(ErrorCode.JOB_POSITION_NOT_FOUND));

                RecruitmentState newRecruitment = RecruitmentState.createRecruitmentState(
                        jobPosition, recruitmentCmd.recruitmentCount());

                // 기술 스택 추가
                List<TechStack> techStacks = techStackRepository.findByIdIn(recruitmentCmd.techStackIds());
                for (TechStack techStack : techStacks) {
                    newRecruitment.addRecruitmentTechStack(RecruitmentTechStack.create(techStack));
                }

                project.addRecruitment(newRecruitment);
            }
        }

        return autoRejectedCount;
    }
}
