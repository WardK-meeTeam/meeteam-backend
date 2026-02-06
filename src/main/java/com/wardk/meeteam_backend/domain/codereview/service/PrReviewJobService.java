package com.wardk.meeteam_backend.domain.codereview.service;


import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import com.wardk.meeteam_backend.domain.codereview.repository.PrReviewJobRepository;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;

import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * PR 리뷰 작업(Job) 생성 및 관리를 담당하는 서비스
 * - 리뷰 작업 생성/조회/상태 관리
 * - 채팅 스레드 생성 및 연결
 * - 중복 작업 확인 및 재사용
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PrReviewJobService {

    private final PrReviewJobRepository prReviewJobRepository;

    /**
     * PR 리뷰 작업을 생성하거나 기존 작업을 재사용합니다.
     */
    public PrReviewJob createOrReuseReviewJob(PullRequest pullRequest, Long memberId) {
        if (pullRequest == null) {
            throw new CustomException(ErrorCode.PR_NOT_FOUND);
        }

        log.info("PR 리뷰 작업 생성/재사용 확인: repo={}, pr=#{}, sha={}",
                pullRequest.getProjectRepo().getRepoFullName(),
                pullRequest.getPrNumber(),
                pullRequest.getHeadSha());

        // 이미 동일한 HEAD SHA에 대한 리뷰 작업이 있는지 확인
        Optional<PrReviewJob> existingJob = prReviewJobRepository.findByProjectRepoIdAndPrNumberAndHeadSha(
                pullRequest.getProjectRepo().getId(),
                pullRequest.getPrNumber(),
                pullRequest.getHeadSha());

        if (existingJob.isPresent()) {
            return handleExistingReviewJob(existingJob.get());
        }

        // 새로운 리뷰 작업 생성
        return createNewReviewJob(pullRequest, memberId);
    }

    /**
     * 기존 리뷰 작업 처리 로직
     */
    private PrReviewJob handleExistingReviewJob(PrReviewJob existingJob) {
        log.info("기존 리뷰 작업 발견: id={}, 상태={}", existingJob.getId(), existingJob.getStatus());

        // 완료된 작업이면 그대로 반환
        if (existingJob.getStatus() == PrReviewJob.Status.SUCCEEDED ||
                existingJob.getStatus() == PrReviewJob.Status.PARTIAL) {
            return existingJob;
        }

        // 실패 또는 대기 중인 작업이면 상태 초기화 후 재사용
        if (existingJob.getStatus() == PrReviewJob.Status.FAILED ||
                existingJob.getStatus() == PrReviewJob.Status.QUEUED) {

            existingJob.updateStatus(PrReviewJob.Status.QUEUED);
            existingJob.recordError(null);
            return prReviewJobRepository.save(existingJob);
        }

        // 진행 중인 작업이면 그대로 반환
        return existingJob;
    }

    /**
     * 새로운 PR 리뷰 작업 생성
     */
    @Transactional(propagation = REQUIRES_NEW)
    protected PrReviewJob createNewReviewJob(PullRequest pullRequest, Long memberId) {
        log.info("새로운 리뷰 작업 생성: pr=#{}", pullRequest.getPrNumber());

        // 리뷰 작업 생성
        PrReviewJob reviewJob = PrReviewJob.builder()
                .pullRequest(pullRequest)
                .headSha(pullRequest.getHeadSha())
                .status(PrReviewJob.Status.QUEUED)
                .build();

        PrReviewJob savedJob = prReviewJobRepository.save(reviewJob);
        // DB에 즉시 반영하여 다른 트랜잭션에서 조회 가능하도록
        prReviewJobRepository.flush();

        log.info("리뷰 작업 생성 완료: id={}", savedJob.getId());
        return savedJob;
    }



    /**
     * 리뷰 작업 상태 업데이트 (재시도 로직 포함)
     */
    @Transactional(propagation = REQUIRES_NEW)
    public void updateReviewJobStatus(Long reviewJobId, PrReviewJob.Status status) {
        // 최대 3번까지 재시도 (트랜잭션 타이밍 문제 해결)
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Optional<PrReviewJob> reviewJobOpt = prReviewJobRepository.findById(reviewJobId);

                if (reviewJobOpt.isPresent()) {
                    PrReviewJob reviewJob = reviewJobOpt.get();
                    reviewJob.updateStatus(status);
                    prReviewJobRepository.save(reviewJob);

                    log.info("리뷰 작업 상태 업데이트 완료: id={}, status={}", reviewJobId, status);
                    return;
                } else {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        log.warn("리뷰 작업 조회 실패, 재시도 {}/{}: id={}", retryCount, maxRetries, reviewJobId);
                        try {
                            Thread.sleep(100); // 100ms 대기 후 재시도
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                retryCount++;
                if (retryCount < maxRetries) {
                    log.warn("리뷰 작업 상태 업데이트 오류, 재시도 {}/{}: id={}, 오류: {}",
                            retryCount, maxRetries, reviewJobId, e.getMessage());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    throw e;
                }
            }
        }

        // 모든 재시도 실패 시
        throw new CustomException(ErrorCode.PR_NOT_FOUND);
    }

    /**
     * 리뷰 작업 조회
     */
    @Transactional(readOnly = true)
    public PrReviewJob getReviewJob(Long reviewJobId) {
        return prReviewJobRepository.findByIdWithAllAssociations(reviewJobId)
                .orElseThrow(() -> new CustomException(ErrorCode.PR_NOT_FOUND));
    }

    /**
     * PR로 최신 리뷰 작업 조회
     */
    @Transactional(readOnly = true)
    public Optional<PrReviewJob> getLatestReviewJobByPr(Long repoId, Integer prNumber) {
        return prReviewJobRepository.findLatestByRepoAndPrNumber(repoId, prNumber);
    }
}
