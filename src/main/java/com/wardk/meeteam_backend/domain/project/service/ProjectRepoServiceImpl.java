package com.wardk.meeteam_backend.domain.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wardk.meeteam_backend.domain.pr.entity.ProjectRepo;
import com.wardk.meeteam_backend.domain.pr.repository.ProjectRepoRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.github.GithubAppAuthService;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.project.dto.request.ProjectRepoRequest;
import com.wardk.meeteam_backend.web.project.dto.response.ProjectRepoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 프로젝트 GitHub 레포지토리 연동 서비스 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectRepoServiceImpl implements ProjectRepoService {

    private final ProjectRepository projectRepository;
    private final ProjectRepoRepository projectRepoRepository;
    private final GithubAppAuthService githubAppAuthService;
    private final WebClient.Builder webClientBuilder;

    @Override
    public List<ProjectRepoResponse> addRepo(Long projectId, ProjectRepoRequest request, String requesterEmail) {
        // 1단계: 엔티티 조회
        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        // 2단계: 권한 및 상태 검증
        project.validateNotCompleted();
        project.validateLeaderPermission(requesterEmail);

        // 3단계: 레포지토리 연결 처리
        List<ProjectRepoResponse> responses = new ArrayList<>();

        for (String repoUrl : request.getRepoUrls()) {
            String repoFullName = extractRepoFullName(repoUrl);

            if (projectRepoRepository.existsByRepoFullName(repoFullName)) {
                throw new CustomException(ErrorCode.PROJECT_REPO_ALREADY_EXISTS);
            }

            String[] parts = repoFullName.split("/");
            String owner = parts[0];
            String repo = parts[1];

            Long installationId = githubAppAuthService.fetchInstallationId(owner, repo);
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

            log.info("GitHub 레포 연결 - repo: {}, description: {}, starCount: {}",
                    repoFullName, description, starCount);

            ProjectRepo projectRepo = ProjectRepo.create(
                    project, repoFullName, installationId, description,
                    starCount, watcherCount, pushedAt, language
            );
            project.addRepo(projectRepo);
            projectRepoRepository.save(projectRepo);

            responses.add(ProjectRepoResponse.responseDto(projectRepo));
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectRepoResponse> findRepos(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        List<ProjectRepo> repos = projectRepoRepository.findAllByProjectId(projectId);

        return repos.stream()
                .map(ProjectRepoResponse::responseDto)
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
}