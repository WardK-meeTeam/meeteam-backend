package com.wardk.meeteam_backend.domain.codereview.service;

import com.wardk.meeteam_backend.domain.chat.entity.ChatThread;
import com.wardk.meeteam_backend.domain.chat.repository.ChatThreadRepository;
import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import com.wardk.meeteam_backend.domain.codereview.repository.PrReviewJobRepository;
import com.wardk.meeteam_backend.domain.llm.service.LlmOrchestrator;
import com.wardk.meeteam_backend.domain.llm.service.LlmReviewService;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequestFile;
import com.wardk.meeteam_backend.domain.pr.repository.PullRequestFileRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrReviewService {

    private final PullRequestFileRepository fileRepository;
    private final PrReviewJobRepository prReviewJobRepository;
    private final ChatThreadRepository chatThreadRepository;
    private final LlmReviewService llmReviewService;
    // private final ExecutorService asyncTaskExecutor;
    private final LlmOrchestrator llmOrchestrator;

    /**
     * PR에 대한 리뷰 작업을 생성하고 비동기 처리를 시작합니다.
     */
    @Transactional
    public PrReviewJob createReviewJob(PullRequest pullRequest) {

        if (pullRequest == null) {
            throw new CustomException(ErrorCode.PR_NOT_FOUND);
        }

        log.info("PR 리뷰 작업 생성 시작: repo={}, pr=#{}, sha={}",
                pullRequest.getProjectRepo().getRepoFullName(),
                pullRequest.getPrNumber(),
                pullRequest.getHeadSha());

        // 이미 동일한 HEAD SHA에 대한 리뷰 작업이 있는지 확인
        Optional<PrReviewJob> existingJob = prReviewJobRepository.findByProjectRepoIdAndPullRequestIdAndHeadSha(
                pullRequest.getProjectRepo().getId(),
                pullRequest.getPrNumber(),
                pullRequest.getHeadSha());

        if (existingJob.isPresent()) {
            log.info("이미 존재하는 리뷰 작업 발견: id={}", existingJob.get().getId());
            PrReviewJob job = existingJob.get();

            // 완료된 작업이면 그대로 반환
            if (job.getStatus() == PrReviewJob.Status.SUCCEEDED ||
                job.getStatus() == PrReviewJob.Status.PARTIAL) {
                return job;
            }

            // 실패 또는 대기 중인 작업이면 상태 초기화 후 재사용
            if (job.getStatus() == PrReviewJob.Status.FAILED ||
                job.getStatus() == PrReviewJob.Status.QUEUED) {

                job.updateStatus(PrReviewJob.Status.QUEUED);
                job.recordError(null);
                return prReviewJobRepository.save(job);
            }
        }

        // 채팅 스레드 생성
        ChatThread chatThread = ChatThread.builder()
                .pullRequest(pullRequest)
                .title("PR #" + pullRequest.getPrNumber() + " 코드 리뷰")
                .memberId(Long.valueOf(1)) // 임의 수
                .build();
        chatThreadRepository.save(chatThread);

        // 리뷰 작업 생성
        PrReviewJob reviewJob = PrReviewJob.builder()
                .pullRequest(pullRequest)
                .projectRepo(pullRequest.getProjectRepo())
                .prNumber(pullRequest.getPrNumber())
                .headSha(pullRequest.getHeadSha())
                .status(PrReviewJob.Status.QUEUED)
                .chatThread(chatThread)
                .build();

        prReviewJobRepository.save(reviewJob);
        prReviewJobRepository.flush(); // 명시적으로 flush하여 DB에 즉시 반영
        log.info("PR 리뷰 작업 생성 완료: id={}", reviewJob.getId());

        // LLM 오케스트레이터 시작
        llmOrchestrator.startPrReview(reviewJob);

        return reviewJob;
    }

    /**
     * 특정 파일 타입을 리뷰에서 제외할지 결정합니다.
     */
    private boolean shouldSkipFile(String filename) {
        // 이미지, 바이너리 파일 등 제외
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".png") ||
                lowerFilename.endsWith(".jpg") ||
                lowerFilename.endsWith(".jpeg") ||
                lowerFilename.endsWith(".gif") ||
                lowerFilename.endsWith(".svg") ||
                lowerFilename.endsWith(".pdf") ||
                lowerFilename.endsWith(".zip") ||
                lowerFilename.endsWith(".jar");
    }
}