package com.wardk.meeteam_backend.domain.llm.service;

import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import com.wardk.meeteam_backend.domain.llm.LlmConcurrencyLimiter;
import com.wardk.meeteam_backend.domain.llm.entity.LlmTask;
import com.wardk.meeteam_backend.domain.llm.entity.LlmTaskResult;
import com.wardk.meeteam_backend.domain.llm.repository.LlmTaskRepository;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequestFile;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LlmOrchestratorImpl implements LlmOrchestrator {

    private final LlmTaskRepository llmTaskRepository;
    private final LlmReviewService llmReviewService;
    private final LlmSummaryService llmSummaryService;
    private final Executor asyncTaskExecutor;
    private final LlmConcurrencyLimiter limiter;

    /**
     * PR 리뷰 작업을 시작합니다.
     * 이 메서드는 비동기로 동작하며, 리뷰 작업 전체 오케스트레이션을 담당합니다.
     */
    @Override
    @Async("asyncTaskExecutor")
    public CompletableFuture<Void> startPrReview(PrReviewJob reviewJob) {
        log.info("PR 리뷰 시작: PR #{} - 저장소: {} - 커밋: {}",
                reviewJob.getPrNumber(),
                reviewJob.getProjectRepo().getRepoFullName(),
                reviewJob.getHeadSha());

        try {
            // // 메인 오케스트레이션 태스크 생성
            LlmTask orchestrationTask = createOrchestrationTask(reviewJob);
            llmTaskRepository.save(orchestrationTask);

            // 오케스트레이션 태스크 실행 (비동기)
            return CompletableFuture.runAsync(() -> {
                try {
                    orchestratePrReview(orchestrationTask);
                } catch (Exception e) {
                    log.error("PR 리뷰 오케스트레이션 중 오류 발생: PR #{}", reviewJob.getPrNumber(), e);
                    orchestrationTask.completeWithError("오케스트레이션 실패: " + e.getMessage());
                    llmTaskRepository.save(orchestrationTask);
                }
            }, asyncTaskExecutor);

        } catch (Exception e) {
            log.error("PR 리뷰 시작 중 오류 발생: PR #{}", reviewJob.getPrNumber(), e);
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    /**
     * 오케스트레이션 태스크를 생성합니다.
     */
    private LlmTask createOrchestrationTask(PrReviewJob job) {
        return LlmTask.builder()
                .prReviewJob(job)
                .taskType(LlmTask.TaskType.ORCHESTRATION)
                .status(LlmTask.TaskStatus.CREATED)
                .priority(LlmTask.Priority.HIGH)
                .startedAt(LocalDateTime.now())
                .build();
    }

    /**
     * PR 리뷰 작업을 오케스트레이션합니다.
     * 1. 파일별 리뷰 태스크 생성
     * 2. 병렬 실행 및 진행 모니터링
     * 3. 완료 후 요약 생성
     */
    public void orchestratePrReview(LlmTask orchestrationTask) {
        PrReviewJob reviewJob = orchestrationTask.getPrReviewJob();
        List<PullRequestFile> files = reviewJob.getPullRequest().getFiles();

        log.info("오케스트레이션 진행 중: PR #{}, 파일 수: {}",
                reviewJob.getPrNumber(), files.size());

        // 오케스트레이션 태스크 상태 업데이트
        orchestrationTask.updateStatus(LlmTask.TaskStatus.RUNNING);
        llmTaskRepository.save(orchestrationTask);

        // 1. 각 파일에 대한 LLM 태스크 생성 및 저장
        List<LlmTask> fileTasks = files.stream()
                .map(file -> createFileReviewTask(reviewJob, file))
                .collect(Collectors.toList());

        llmTaskRepository.saveAll(fileTasks);

        // 2. 파일 리뷰 태스크 병렬 실행
        List<CompletableFuture<LlmTaskResult>> fileReviewFutures = new ArrayList<>();

        for (LlmTask fileTask : fileTasks) {
            CompletableFuture<LlmTaskResult> future = executeFileReviewAsync(fileTask);
            fileReviewFutures.add(future);
        }

        // 3. 모든 파일 리뷰 완료 대기 및 결과 수집
        CompletableFuture<Void> allFileReviewsCompleted = CompletableFuture.allOf(
                fileReviewFutures.toArray(new CompletableFuture[0]));

        // // 4. 모든 파일 리뷰 완료 후 요약 태스크 생성 및 실행
        CompletableFuture<LlmTaskResult> summaryFuture = allFileReviewsCompleted
                .thenApplyAsync(v -> {

                    // 성공, 실패 태스크 카운트
                    long successCount = fileTasks.stream()
                            .filter(task -> task.getStatus() == LlmTask.TaskStatus.COMPLETED)
                            .count();

                    long failedCount = fileTasks.stream()
                            .filter(task -> task.getStatus() == LlmTask.TaskStatus.FAILED)
                            .count();

                    log.info("파일 리뷰 완료: PR #{}, 성공: {}, 실패: {}",
                            reviewJob.getPrNumber(), successCount, failedCount);

                    // 요약 태스크 생성
                    if (successCount > 0) {
                        return executeSummaryTask(reviewJob, fileTasks);
                    } else {
                        throw new RuntimeException("모든 파일 리뷰가 실패했습니다");
                    }
                }, asyncTaskExecutor)
                .exceptionally(ex -> {
                    log.error("요약 태스크 실행 중 오류 발생: PR #{}", reviewJob.getPrNumber(), ex);
                    return null;
                });

        try {
            // 요약 태스크 완료 대기
            LlmTaskResult summaryResult = summaryFuture.join();

            // 오케스트레이션 태스크 완료 처리
            if (summaryResult != null) {
                orchestrationTask.updateStatus(LlmTask.TaskStatus.COMPLETED);
                llmTaskRepository.save(orchestrationTask);
            } else {
                orchestrationTask.completeWithError("요약 생성 실패");
            }

            llmTaskRepository.save(orchestrationTask);

            log.info("PR 리뷰 완료: PR #{}", reviewJob.getPrNumber());

        } catch (Exception e) {
            log.error("PR 리뷰 오케스트레이션 완료 처리 중 오류 발생: PR #{}", reviewJob.getPrNumber(), e);
            orchestrationTask.completeWithError("오케스트레이션 완료 실패: " + e.getMessage());
            llmTaskRepository.save(orchestrationTask);
        }
    }

    /**
     * 파일 리뷰 태스크를 생성합니다.
     */
    @Override
    public LlmTask createFileReviewTask(PrReviewJob reviewJob, PullRequestFile file) {
        return LlmTask.builder()
                .prReviewJob(reviewJob)
                .pullRequestFile(file)
                .taskType(LlmTask.TaskType.FILE_REVIEW)
                .status(LlmTask.TaskStatus.CREATED)
                .priority(LlmTask.Priority.NORMAL)
                .build();
    }

    /**
     * 파일 리뷰를 비동기적으로 실행한다.
     * - 동시성 제한(세마포어) + 지연 분산(jitter)
     * - per-call 가드 타임아웃(기본 55s)
     * - 지수 백오프(1s, 2s, 4s) + 소폭 지터
     * - 상태 저장 최소화
     */
    private CompletableFuture<LlmTaskResult> executeFileReviewAsync(LlmTask fileTask) {
        return CompletableFuture.supplyAsync(() -> {
            // 1) 실행 상태로 전환 (최초 1회)
            fileTask.updateStatus(LlmTask.TaskStatus.RUNNING);
            llmTaskRepository.save(fileTask);

            Exception last = null;

            for (int attempt = 1; attempt <= 3; attempt++) {
                log.info("파일 리뷰 시작 (시도 #{}/{}): PR #{}, 파일: {}",
                        attempt, 3,
                        fileTask.getPrReviewJob().getPrNumber(),
                        fileTask.getPullRequestFile().getFileName());

                try {
                    // 2) 동시성 제한 + 소폭 지연 분산
                    limiter.acquire();
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(100, 401)); // 100~400ms

                        // 3) LLM 호출에 가드 타임아웃 적용
                        CompletableFuture<LlmTaskResult> call = CompletableFuture.supplyAsync(
                                () -> llmReviewService.reviewFile(fileTask),
                                asyncTaskExecutor // 전용 풀 사용
                        );

                        LlmTaskResult result = call.get(Duration.ofSeconds(55).toMillis(), TimeUnit.MILLISECONDS);

                        // 4) 성공 처리 (저장 1회)
                        fileTask.completeWithSuccess(result);
                        llmTaskRepository.save(fileTask);

                        log.info("파일 리뷰 완료: PR #{}, 파일: {}",
                                fileTask.getPrReviewJob().getPrNumber(),
                                fileTask.getPullRequestFile().getFileName());

                        return result;

                    } finally {
                        limiter.release(); // ★ 반드시 해제
                    }

                } catch (TimeoutException te) {
                    last = te;
                    log.warn("파일 리뷰 가드 타임아웃(약 {}s): PR #{}, 파일: {}",
                            Duration.ofSeconds(attempt).getSeconds(),
                            fileTask.getPrReviewJob().getPrNumber(),
                            fileTask.getPullRequestFile().getFileName());

                } catch (ExecutionException ee) {
                    last = (ee.getCause() instanceof Exception) ? (Exception) ee.getCause() : ee;
                    log.warn("파일 리뷰 중 예외 (시도 #{}/{}): PR #{}, 파일: {}, 원인: {}",
                            attempt, 3,
                            fileTask.getPrReviewJob().getPrNumber(),
                            fileTask.getPullRequestFile().getFileName(),
                            last.toString());

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    last = ie;
                    break;
                }

                // 5) 재시도 백오프: 1s, 2s, 4s (최대 4s) + 지터(200~600ms)
                if (attempt < 3) {
                    long base = Math.min(4000L, 1000L << (attempt - 1));
                    long jitter = ThreadLocalRandom.current().nextLong(200, 601);
                    try {
                        Thread.sleep(base + jitter);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        last = ie;
                        break;
                    }
                }
            }

            // 6) 최종 실패 처리 (저장 1회)
            String msg = "리뷰 실패 (최대 재시도 초과)" + (last != null ? ": " + last.getMessage() : "");
            fileTask.completeWithError(msg);
            llmTaskRepository.save(fileTask);

            throw new RuntimeException(msg, last);
        }, asyncTaskExecutor);
    }

    /**
     * 요약 태스크를 실행합니다.
     */
    private LlmTaskResult executeSummaryTask(PrReviewJob reviewJob, List<LlmTask> fileTasks) {
        try {
            // 요약 태스크 생성
            LlmTask summaryTask = LlmTask.builder()
                    .prReviewJob(reviewJob)
                    .taskType(LlmTask.TaskType.PR_SUMMARY)
                    .status(LlmTask.TaskStatus.CREATED)
                    .priority(LlmTask.Priority.HIGH)
                    .build();

            llmTaskRepository.save(summaryTask);

            // 요약 태스크 상태 업데이트
            summaryTask.updateStatus(LlmTask.TaskStatus.RUNNING);
            llmTaskRepository.save(summaryTask);

            log.info("PR 요약 시작: PR #{}", reviewJob.getPrNumber());

            // 요약 생성 실행
            LlmTaskResult result = llmSummaryService.createEmptySummaryResult(summaryTask);

            // 요약 태스크 상태 업데이트
            summaryTask.completeWithSuccess(result);
            llmTaskRepository.save(summaryTask);

            log.info("PR 요약 완료: PR #{}", reviewJob.getPrNumber());

            return result;

        } catch (Exception e) {
            log.error("PR 요약 생성 중 오류 발생: PR #{}", reviewJob.getPrNumber(), e);
            throw new RuntimeException("PR 요약 생성 실패", e);
        }
    }

    /**
     * 현재 실행 중인 태스크 상태를 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public LlmTaskStatus getTaskStatus(Long taskId) {
        LlmTask task = llmTaskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNSUPPORTED_TASK_TYPE));

        PrReviewJob job = task.getPrReviewJob();

        // 자식 태스크 상태 통계
        // List<LlmTask> childTasks = llmTaskRepository.findByParentTaskId(taskId);

        // int totalTasks = childTasks.size();
        // int completedTasks = (int) childTasks.stream()
        // .filter(t -> t.getStatus() == LlmTask.TaskStatus.COMPLETED)
        // .count();
        // int failedTasks = (int) childTasks.stream()
        // .filter(t -> t.getStatus() == LlmTask.TaskStatus.FAILED)
        // .count();
        // int runningTasks = (int) childTasks.stream()
        // .filter(t -> t.getStatus() == LlmTask.TaskStatus.RUNNING)
        // .count();

        return LlmTaskStatus.builder()
                .taskId(task.getId())
                .prNumber(job.getPrNumber())
                .status(task.getStatus().name())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                // .totalChildTasks(totalTasks)
                // .completedChildTasks(completedTasks)
                // .failedChildTasks(failedTasks)
                // .runningChildTasks(runningTasks)
                // .progressPercentage(totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0)
                .build();
    }
}
