package com.wardk.meeteam_backend.domain.pr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wardk.meeteam_backend.domain.pr.entity.ProjectRepo;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequestFile;
import com.wardk.meeteam_backend.domain.pr.repository.ProjectRepoRepository;
import com.wardk.meeteam_backend.domain.pr.repository.PullRequestFileRepository;
import com.wardk.meeteam_backend.domain.pr.repository.PullRequestRepository;
import com.wardk.meeteam_backend.domain.pr.service.fetcher.PullRequestFetcher;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.pr.dto.PrData;
import com.wardk.meeteam_backend.web.pr.dto.PrFileData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PullRequestIngestionServiceImpl implements PullRequestIngestionService {

    private final PullRequestRepository pullRequestRepository;
    private final PullRequestFileRepository fileRepository;
    private final ProjectRepoRepository projectRepoRepository;
    private final PullRequestFetcher fetcher;

    @Override
    public void handlePullRequest(JsonNode payload) {

        String repoFullName = payload.path("repository").path("full_name").asText();
        Integer prNumber = payload.path("number").asInt();

        log.info("PR 수신: repo={}, prNumber={}", repoFullName, prNumber);

        ProjectRepo projectRepo = projectRepoRepository.findByRepoFullName(repoFullName)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_REPO_NOT_FOUND));

        PrData prData = fetcher.getPr(repoFullName, prNumber, payload);
        List<PrFileData> files = fetcher.listFiles(repoFullName, prNumber);

        PullRequest pr = pullRequestRepository.findByProjectRepoIdAndPrNumber(projectRepo.getId(), prNumber)
                .orElseGet(() -> {
                    PullRequest newPr = new PullRequest(prNumber);
                    newPr.setProjectRepo(projectRepo);
                    return newPr;
                });

        pr.updateFromPayload(payload.path("pull_request"));
//        pr.updateFromPayload(prData);

        pr.getFiles().clear();
        for (PrFileData f : files) {
            PullRequestFile file = PullRequestFile.createPullRequestFile(f);
            pr.addFile(file);
        }

        pullRequestRepository.save(pr);

        log.info("PR 저장 완료: id={}, repo={}, prNumber={}", pr.getId(), repoFullName, prNumber);
    }

    @Override
    public void handleMerged(JsonNode payload) {

        String repoFullName = payload.path("repository").path("full_name").asText();
        int prNumber = payload.path("pull_request").path("number").asInt();

        ProjectRepo projectRepo = projectRepoRepository.findByRepoFullName(repoFullName)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_REPO_NOT_FOUND));

        PullRequest pr = pullRequestRepository.findByProjectRepoIdAndPrNumber(projectRepo.getId(), prNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.PR_NOT_FOUND));

        pr.updateFromPayload(payload.path("pull_request"));
        pullRequestRepository.save(pr);

        log.info("PR 병합 처리 완료: id={}, repo={}, prNumber={}", pr.getId(), repoFullName, prNumber);
    }

    @Override
    public void handleClosed(JsonNode payload) {
        String repoFullName = payload.path("repository").path("full_name").asText();
        int prNumber = payload.path("pull_request").path("number").asInt();

        ProjectRepo projectRepo = projectRepoRepository.findByRepoFullName(repoFullName)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_REPO_NOT_FOUND));

        PullRequest pr = pullRequestRepository.findByProjectRepoIdAndPrNumber(projectRepo.getId(), prNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.PR_NOT_FOUND));

        pr.updateFromPayload(payload.path("pull_request"));
        pullRequestRepository.save(pr);
    }
}
