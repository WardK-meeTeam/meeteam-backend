package com.wardk.meeteam_backend.web.pr.controller;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import com.wardk.meeteam_backend.domain.pr.repository.PullRequestRepository;
import com.wardk.meeteam_backend.domain.pr.service.PullRequestService;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.pr.dto.PullRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/prs")
@RequiredArgsConstructor
public class PullRequestController {

    private final PullRequestService prService;

    /**
     * 특정 PR 조회
     * @param owner Ex) wardk
     * @param repo Ex) meeteam_backend
     * @param prNumber
     * @return
     */
    @Operation(summary = "특정 레포 PR 조회")
    @GetMapping("/{owner}/{repo}/{prNumber}")
    public SuccessResponse<PullRequestResponse> get(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable int prNumber,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {
        String repoFullName = owner + "/" + repo;
        PullRequestResponse response = prService.getPullRequest(repoFullName, prNumber);

        return SuccessResponse.onSuccess(response);
    }

    /**
     * 특정 프로젝트 내 모든 PR 조회
     * @param projectId
     * @return
     */
    @Operation(summary = "특정 프로젝트 내 모든 PR 조회")
    @GetMapping("/{projectId}")
    public SuccessResponse<List<PullRequestResponse>> getAllPullRequests(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        List<PullRequestResponse> responses = prService.getAllPullRequests(projectId);

        return SuccessResponse.onSuccess(responses);
    }

    /**
     *  특정 레포 내의 모든 PR 조회
     */
    @Operation(summary = "특정 레포 내의 모든 PR 조회")
    @GetMapping("/{owner}/{repo}")
    public SuccessResponse<List<PullRequestResponse>> getAllPullRequestsInRepo(
            @PathVariable String owner,
            @PathVariable String repo,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {
        String repoFullName = owner + "/" + repo;
        List<PullRequestResponse> responses = prService.getAllPullRequestsInRepo(repoFullName);

        return SuccessResponse.onSuccess(responses);
    }

    // TODO: 필요 시 보강 조회/관리용 엔드포인트 추가
}
