package com.wardk.meeteam_backend.web.pr.controller;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import com.wardk.meeteam_backend.domain.pr.repository.PullRequestRepository;
import com.wardk.meeteam_backend.domain.pr.service.PullRequestService;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.pr.dto.PullRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/prs")
@RequiredArgsConstructor
public class PullRequestController {

    private final PullRequestService prService;

    @GetMapping("/{owner}/{repo}/{prNumber}")
    public SuccessResponse<PullRequestResponse> get(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable int prNumber) {
        String repoFullName = owner + "/" + repo;
        PullRequestResponse response = prService.getPullRequest(repoFullName, prNumber);

        return SuccessResponse.onSuccess(response);
    }

    // TODO: 필요 시 보강 조회/관리용 엔드포인트 추가
}
