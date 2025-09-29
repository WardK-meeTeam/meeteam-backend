package com.wardk.meeteam_backend.domain.codereview.service;

import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * PR 리뷰 서비스 - 파사드 패턴
 * PrReviewJobService와 PrReviewOrchestrationService를 조합하여
 * 클라이언트에게 간단한 인터페이스를 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrReviewService {

    private final PrReviewJobService prReviewJobService;
    private final PrReviewOrchestrationService orchestrationService;

    /**
     * PR 리뷰 프로세스를 시작합니다.
     * 1. 리뷰 작업 생성/재사용 (별도 트랜잭션)
     * 2. 비동기로 리뷰 실행
     */
    @Transactional(propagation = Propagation.NEVER) // 트랜잭션 없이 실행하여 하위 서비스들이 독립적으로 트랜잭션 관리
    public PrReviewJob startPrReview(PullRequest pullRequest, Long memberId) {
        log.info("PR 리뷰 프로세스 시작: repo={}, pr=#{}",
                pullRequest.getProjectRepo().getRepoFullName(),
                pullRequest.getPrNumber());

        // 1. 리뷰 작업 생성 또는 기존 작업 재사용 (새 트랜잭션에서 실행)
        PrReviewJob reviewJob = createOrReuseReviewJobInTransaction(pullRequest, memberId);

        // 2. 이미 완료된 작업이면 바로 반환
        if (reviewJob.getStatus() == PrReviewJob.Status.SUCCEEDED ||
                reviewJob.getStatus() == PrReviewJob.Status.PARTIAL) {
            log.info("이미 완료된 리뷰 작업: id={}", reviewJob.getId());
            return reviewJob;
        }

        // 3. 진행 중인 작업이면 바로 반환
        if (reviewJob.getStatus() == PrReviewJob.Status.RUNNING) {
            log.info("이미 진행 중인 리뷰 작업: id={}", reviewJob.getId());
            return reviewJob;
        }

        // 4. 새 작업이거나 재시작할 작업이면 비동기로 실행
        log.info("리뷰 작업 비동기 실행 시작: id={}", reviewJob.getId());
        CompletableFuture<Void> reviewFuture = orchestrationService.startReviewAsync(reviewJob.getId());

        // 비동기 실행 시작 후 즉시 반환 (완료를 기다리지 않음)
        return reviewJob;
    }

    /**
     * 새 트랜잭션에서 리뷰 작업 생성/재사용
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected PrReviewJob createOrReuseReviewJobInTransaction(PullRequest pullRequest, Long memberId) {
        return prReviewJobService.createOrReuseReviewJob(pullRequest, memberId);
    }

    /**
     * 리뷰 작업 상태 조회
     */
    @Transactional(readOnly = true)
    public PrReviewJob getReviewJob(Long reviewJobId) {
        return prReviewJobService.getReviewJob(reviewJobId);
    }
}