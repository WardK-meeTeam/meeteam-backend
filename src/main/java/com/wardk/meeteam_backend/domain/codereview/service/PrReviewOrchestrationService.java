package com.wardk.meeteam_backend.domain.codereview.service;

//import com.wardk.meeteam_backend.domain.chat.service.ChatService;
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
 * - 채팅 알림 메시지 전송
 * - 비동기 처리 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrReviewOrchestrationService {

    private final LlmOrchestrator llmOrchestrator;
    private final PrReviewJobService prReviewJobService;
    private final CodeReviewChatService codeReviewChatService;

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

            // 채팅방에 리뷰 시작 메시지 전송
            sendReviewStartMessage(reviewJobId);

            // LLM 오케스트레이터로 실제 리뷰 실행
            CompletableFuture<Void> reviewFuture = llmOrchestrator.startPrReview(reviewJobId);

            // 리뷰 완료 후 상태 업데이트 및 완료 메시지 전송
            return reviewFuture.whenComplete((result, throwable) -> {
                try {
                    if (throwable != null) {
                        log.error("PR 리뷰 실행 중 오류 발생: reviewJobId={}", reviewJobId, throwable);
                        prReviewJobService.updateReviewJobStatus(reviewJobId, PrReviewJob.Status.FAILED);
                        sendReviewFailureMessage(reviewJobId, throwable.getMessage());
                    } else {
                        log.info("PR 리뷰 완료: reviewJobId={}", reviewJobId);
                        prReviewJobService.updateReviewJobStatus(reviewJobId, PrReviewJob.Status.SUCCEEDED);
                        sendReviewCompleteMessage(reviewJobId);
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

    /**
     * 채팅방에 리뷰 시작 메시지를 전송합니다.
     */
    private void sendReviewStartMessage(Long reviewJobId) {
        try {
            PrReviewJob reviewJob = prReviewJobService.getReviewJob(reviewJobId);

            if (reviewJob.getChatRoom() != null) {
                int totalFiles = reviewJob.getPullRequest().getFiles().size();

                codeReviewChatService.addReviewStartMessage(
                    reviewJob.getChatRoom().getId(),
                    reviewJob.getPrNumber(),
                    totalFiles
                );

                log.info("PR #{} 리뷰 시작 메시지 전송 완료", reviewJob.getPrNumber());
            }
        } catch (Exception e) {
            log.error("리뷰 시작 메시지 전송 실패: reviewJobId={}", reviewJobId, e);
            // 메시지 전송 실패해도 리뷰 프로세스는 계속 진행
        }
    }

    /**
     * 채팅방에 리뷰 완료 메시지를 전송합니다.
     */
    private void sendReviewCompleteMessage(Long reviewJobId) {
        try {
            PrReviewJob reviewJob = prReviewJobService.getReviewJob(reviewJobId);

            if (reviewJob.getChatRoom() != null) {
                // 성공/실패 파일 수 계산 (LlmTask 조회를 통해)
                // 실제 구현에서는 LlmTaskRepository를 통해 조회하거나
                // reviewJob에서 관련 정보를 가져와야 합니다
                long successCount = calculateSuccessfulReviews(reviewJob);
                long failedCount = calculateFailedReviews(reviewJob);

                codeReviewChatService.addReviewCompleteMessage(
                    reviewJob.getChatRoom().getId(),
                    reviewJob.getPrNumber(),
                    successCount,
                    failedCount
                );

                log.info("PR #{} 리뷰 완료 메시지 전송 완료: success={}, failed={}",
                        reviewJob.getPrNumber(), successCount, failedCount);
            }
        } catch (Exception e) {
            log.error("리뷰 완료 메시지 전송 실패: reviewJobId={}", reviewJobId, e);
        }
    }

    /**
     * 채팅방에 리뷰 실패 메시지를 전송합니다.
     */
    private void sendReviewFailureMessage(Long reviewJobId, String errorMessage) {
        try {
            PrReviewJob reviewJob = prReviewJobService.getReviewJob(reviewJobId);

            if (reviewJob.getChatRoom() != null) {
                String failureMessage = String.format(
                    "❌ PR #%d 리뷰 중 오류가 발생했습니다.\n🔍 오류: %s",
                    reviewJob.getPrNumber(),
                    errorMessage != null ? errorMessage : "알 수 없는 오류"
                );

                // 시스템 메시지로 전송 (기존 addReviewCompleteMessage 메서드 활용)
                // 실제로는 별도의 실패 메시지 메서드가 있다면 사용
                codeReviewChatService.addReviewCompleteMessage(
                    reviewJob.getChatRoom().getId(),
                    reviewJob.getPrNumber(),
                    0, // 성공 0개
                    reviewJob.getPullRequest().getFiles().size() // 모든 파일 실패
                );

                log.info("PR #{} 리뷰 실패 메시지 전송 완료", reviewJob.getPrNumber());
            }
        } catch (Exception e) {
            log.error("리뷰 실패 메시지 전송 실패: reviewJobId={}", reviewJobId, e);
        }
    }

    /**
     * 성공한 리뷰 수를 계산합니다.
     * TODO: 실제 구현에서는 LlmTaskRepository를 통해 COMPLETED 상태의 태스크 수를 조회
     */
    private long calculateSuccessfulReviews(PrReviewJob reviewJob) {
        // 임시 구현 - 실제로는 LlmTask 조회 필요
        return reviewJob.getPullRequest().getFiles().size();
    }

    /**
     * 실패한 리뷰 수를 계산합니다.
     * TODO: 실제 구현에서는 LlmTaskRepository를 통해 FAILED 상태의 태스크 수를 조회
     */
    private long calculateFailedReviews(PrReviewJob reviewJob) {
        // 임시 구현 - 실제로는 LlmTask 조회 필요
        return 0;
    }
}
