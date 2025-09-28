package com.wardk.meeteam_backend.web.codereview.controller;

import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import com.wardk.meeteam_backend.domain.codereview.service.PrReviewJobService;
import com.wardk.meeteam_backend.domain.codereview.service.PrReviewOrchestrationService;
import com.wardk.meeteam_backend.domain.codereview.service.PrReviewService;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import com.wardk.meeteam_backend.domain.pr.repository.PullRequestRepository;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.codereview.dto.PrReviewJobResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/codereviews")
@RequiredArgsConstructor
@Tag(name = "CodeReview", description = "코드리뷰 관련 API")
@Slf4j
public class CodeReviewController {

    private final PrReviewService prReviewService;
    private final PrReviewJobService prReviewJobService;
    private final PrReviewOrchestrationService orchestrationService;
    private final PullRequestRepository pullRequestRepository;

    /**
     * PR 리뷰 시작 - 통합 API
     */
    @PostMapping("/start")
    @Operation(summary = "PR 리뷰 시작", description = "PR에 대한 코드 리뷰를 시작합니다")
    public ResponseEntity<PrReviewJobResponse> startPrReview(
            @Parameter(description = "저장소 ID") @RequestParam Long repoId,
            @Parameter(description = "PR 번호") @RequestParam Integer prNumber,
            @AuthenticationPrincipal CustomSecurityUserDetails customSecurityUserDetails
            ) {

        try {
            // PR 조회
            PullRequest pullRequest = pullRequestRepository.findByProjectRepoIdAndPrNumber(repoId, prNumber)
                    .orElseThrow(() -> new CustomException(ErrorCode.PR_NOT_FOUND));

            // 리뷰 시작 (작업 생성 + 실행)
            PrReviewJob reviewJob = prReviewService.startPrReview(pullRequest, customSecurityUserDetails.getMemberId());
            PrReviewJobResponse response = PrReviewJobResponse.from(reviewJob);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("PR 리뷰 시작 실패: repoId={}, prNumber={}", repoId, prNumber, e);
            throw e;
        }
    }
}
