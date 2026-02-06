package com.wardk.meeteam_backend.domain.codereview.service;

import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import com.wardk.meeteam_backend.domain.llm.service.LlmOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * PR 리뷰 실행 및 오케스트레이션을 담당하는 서비스
 * - 리뷰 작업 시작
 * - 비동기 처리 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrReviewOrchestrationService {

    private final LlmOrchestrator llmOrchestrator;
    private final PrReviewJobService prReviewJobService;

    /**
     * PR 리뷰를 비동기로 시작합니다.
     */
    @Async
    @Transactional(propagation = Propagation.NEVER) // 비동기 메서드에서는 트랜잭션을 새로 시작
    public CompletableFuture<Void> startReviewAsync(Long reviewJobId) {
        try {
            log.info("PR 리뷰 비동기 시작: reviewJobId={}", reviewJobId);

            // 리뷰 작업 상태를 RUNNING으로 업데이트 (별도 트랜잭션)
            prReviewJobService.updateReviewJobStatus(reviewJobId, PrReviewJob.Status.RUNNING);

            // LLM 오케스트레이터로 실제 리뷰 실행
            CompletableFuture<Void> reviewFuture = llmOrchestrator.startPrReview(reviewJobId);

            // 리뷰 완료 후 상태 업데이트
            return reviewFuture.whenComplete((result, throwable) -> {
                try {
                    if (throwable != null) {
                        log.error("PR 리뷰 실행 중 오류 발생: reviewJobId={}", reviewJobId, throwable);
                        prReviewJobService.updateReviewJobStatus(reviewJobId, PrReviewJob.Status.FAILED);
                    } else {
                        log.info("PR 리뷰 완료: reviewJobId={}", reviewJobId);
                        prReviewJobService.updateReviewJobStatus(reviewJobId, PrReviewJob.Status.SUCCEEDED);
                    }
                } catch (Exception e) {
                    log.error("리뷰 완료 후 처리 중 오류: reviewJobId={}", reviewJobId, e);
                }
            });

        } catch (Exception e) {
            log.error("PR 리뷰 비동기 시작 중 오류: reviewJobId={}", reviewJobId, e);
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }
}