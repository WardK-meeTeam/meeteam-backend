package com.wardk.meeteam_backend.domain.llm.service;

import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import com.wardk.meeteam_backend.domain.codereview.repository.PrReviewJobRepository;

import com.wardk.meeteam_backend.domain.llm.LlmConcurrencyLimiter;
import com.wardk.meeteam_backend.domain.llm.entity.LlmTask;
import com.wardk.meeteam_backend.domain.llm.entity.LlmTaskResult;
import com.wardk.meeteam_backend.domain.llm.repository.LlmTaskRepository;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequestFile;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmOrchestratorImpl implements LlmOrchestrator {

    private final LlmTaskRepository llmTaskRepository;
    private final PrReviewJobRepository prReviewJobRepository;
    private final LlmReviewService llmReviewService;
    private final LlmSummaryService llmSummaryService;
    // 기존 Executor를 Virtual Thread Executor로 교체
    // private final Executor asyncTaskExecutor = Executors.newVirtualThreadPerTaskExecutor();
    // 플랫폼 스레드 기반 Executor로 변경
    private final Executor asyncTaskExecutor = Executors.newCachedThreadPool();
    private final LlmConcurrencyLimiter limiter;

    /**
     * PR 리뷰 작업을 시작합니다.
     * 이 메서드는 비동기로 동작하며, 리뷰 작업 전체 오케스트레이션을 담당합니다.
     */
    @Override
    public CompletableFuture<Void> startPrReview(Long reviewJobId) {
        try {
            // PrReviewJob을 새로운 트랜잭션에서 조회
            PrReviewJob reviewJob = getPrReviewJobById(reviewJobId);

            log.info("PR 리뷰 시작: PR #{} - 저장소: {} - 커밋: {}",
                    reviewJob.getPrNumber(),
                    reviewJob.getProjectRepo().getRepoFullName(),
                    reviewJob.getHeadSha());

            // 메인 오케스트레이션 태스크 생성 (별도 트랜잭션)
            LlmTask orchestrationTask = createOrchestrationTask(reviewJob);

            // 오케스트레이션 태스크 실행 (비동기)
            return CompletableFuture.runAsync(() -> {
                try {
                    orchestratePrReview(orchestrationTask);
                } catch (Exception e) {
                    log.error("PR 리뷰 오케스트레이션 중 오류 발생: PR #{}", reviewJob.getPrNumber(), e);
                    updateOrchestrationTaskWithError(orchestrationTask.getId(), "오케스트레이션 실패: " + e.getMessage());
                }
            }, asyncTaskExecutor);

        } catch (Exception e) {
            log.error("PR 리뷰 시작 중 오류 발생: reviewJobId={}", reviewJobId, e);
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    /**
     * PrReviewJob을 ID로 조회합니다.
     */
    @Transactional(readOnly = true)
    protected PrReviewJob getPrReviewJobById(Long reviewJobId) {
        return prReviewJobRepository.findByIdWithAllAssociations(reviewJobId)
                .orElseThrow(() -> new CustomException(ErrorCode.PR_NOT_FOUND));
    }

    /**
     * 트랜잭션 내에서 오케스트레이션 태스크를 생성합니다.
     */
    @Transactional
    protected LlmTask createOrchestrationTask(PrReviewJob job) {
        LlmTask task = LlmTask.builder()
                .prReviewJob(job)
                .taskType(LlmTask.TaskType.ORCHESTRATION)
                .status(LlmTask.TaskStatus.CREATED)
                .priority(LlmTask.Priority.HIGH)
                .startedAt(LocalDateTime.now())
                .build();
        return llmTaskRepository.save(task);
    }

    /**
     * 트랜잭션 내에서 오케스트레이션 태스크 오류 업데이트
     */
    @Transactional
    protected void updateOrchestrationTaskWithError(Long taskId, String errorMessage) {
        LlmTask task = llmTaskRepository.findById(taskId).orElse(null);
        if (task != null) {
            task.completeWithError(errorMessage);
            llmTaskRepository.save(task);
        }
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

        // 오케스트레이션 태스크 상태 업데이트 (별도 트랜잭션)
        updateOrchestrationTaskStatus(orchestrationTask.getId(), LlmTask.TaskStatus.RUNNING);

        // 1. 각 파일에 대한 LLM 태스크 생성 및 저장 (별도 트랜잭션)
        List<LlmTask> fileTasks = createFileReviewTasksWithTransaction(reviewJob, files);

        // 2. 파일 리뷰 태스크 병렬 실행 - CompletableFuture 리스트로 변경
        List<CompletableFuture<LlmTaskResult>> fileReviewFutures = new ArrayList<>();

        for (LlmTask fileTask : fileTasks) {
            CompletableFuture<LlmTaskResult> future = executeFileReviewAsync(fileTask);
            fileReviewFutures.add(future);
        }

        // 3. 모든 파일 리뷰 완료 대기 및 결과 수집 - 타입 안전한 배열 생성
        CompletableFuture<Void> allFileReviewsCompleted = CompletableFuture.allOf(
                fileReviewFutures.toArray(new CompletableFuture[fileReviewFutures.size()]));

        // 4. 모든 파일 리뷰 완료 후 요약 태스크 생성 및 실행
        CompletableFuture<LlmTaskResult> summaryFuture = allFileReviewsCompleted
                .handle((v, throwable) -> {
                    // 완료된 파일 태스크들을 다시 조회하여 최신 상태 확인
                    List<LlmTask> updatedFileTasks = getUpdatedFileTasks(fileTasks);

                    // 성공, 실패 태스크 카운트
                    long successCount = updatedFileTasks.stream()
                            .filter(task -> task.getStatus() == LlmTask.TaskStatus.COMPLETED)
                            .count();

                    long failedCount = updatedFileTasks.stream()
                            .filter(task -> task.getStatus() == LlmTask.TaskStatus.FAILED)
                            .count();

                    log.info("파일 리뷰 완료: PR #{}, 성공: {}, 실패: {}",
                            reviewJob.getPrNumber(), successCount, failedCount);

                    // 성공한 파일이 하나라도 있으면 요약 진행
                    if (successCount > 0) {
                        try {
                            return executeSummaryTask(reviewJob, updatedFileTasks);
                        } catch (Exception e) {
                            log.error("요약 태스크 실행 중 오류 발생: PR #{}", reviewJob.getPrNumber(), e);
                            return null;
                        }
                    } else {
                        log.warn("모든 파일 리뷰가 실패했지만 부분 완료로 처리: PR #{}", reviewJob.getPrNumber());
                        return null; // 빈 요약으로 처리
                    }
                });

        try {
            // 요약 태스크 완료 대기
            LlmTaskResult summaryResult = summaryFuture.join();

            // 오케스트레이션 태스크 완료 처리 (별도 트랜잭션)
            if (summaryResult != null) {
                updateOrchestrationTaskStatus(orchestrationTask.getId(), LlmTask.TaskStatus.COMPLETED);
            } else {
                updateOrchestrationTaskWithError(orchestrationTask.getId(), "요약 생성 실패");
            }

            log.info("PR 리뷰 완료: PR #{}", reviewJob.getPrNumber());

        } catch (Exception e) {
            log.error("PR 리뷰 오케스트레이션 완료 처리 중 오류 발생: PR #{}", reviewJob.getPrNumber(), e);
            updateOrchestrationTaskWithError(orchestrationTask.getId(), "오케스트레이션 완료 실패: " + e.getMessage());
        }
    }

    /**
     * 트랜잭션 내에서 오케스트레이션 태스크 상태 업데이트
     */
    @Transactional
    protected void updateOrchestrationTaskStatus(Long taskId, LlmTask.TaskStatus status) {
        LlmTask task = llmTaskRepository.findById(taskId).orElse(null);
        if (task != null) {
            task.updateStatus(status);
            llmTaskRepository.save(task);
        }
    }

    /**
     * 트랜잭션 내에서 파일 리뷰 태스크들을 생성
     */
    @Transactional
    protected List<LlmTask> createFileReviewTasksWithTransaction(PrReviewJob reviewJob, List<PullRequestFile> files) {
        List<LlmTask> fileTasks = files.stream()
                .map(file -> createFileReviewTask(reviewJob, file))
                .collect(Collectors.toList());

        return llmTaskRepository.saveAll(fileTasks);
    }

    /**
     * 파일 태스크들의 최신 상태를 조회
     */
    @Transactional(readOnly = true)
    protected List<LlmTask> getUpdatedFileTasks(List<LlmTask> fileTasks) {
        List<Long> taskIds = fileTasks.stream()
                .map(LlmTask::getId)
                .collect(Collectors.toList());
        return llmTaskRepository.findAllById(taskIds);
    }

    /**
     * 파일 리뷰 태스크를 생성합니다.
     */
    @Override
    public LlmTask createFileReviewTask(PrReviewJob reviewJob, PullRequestFile file) {
        return LlmTask.builder()
                .prReviewJob(reviewJob)
                .pullRequestFileId(file.getId())
                .pullRequestFileName(file.getFileName())
                .taskType(LlmTask.TaskType.FILE_REVIEW)
                .status(LlmTask.TaskStatus.CREATED)
                .priority(LlmTask.Priority.NORMAL)
                .build();
    }

    private CompletableFuture<LlmTaskResult> executeFileReviewAsync(LlmTask fileTask) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1) 동시성 제한
                limiter.acquire();

                try {
                    // 2) 실행 상태로 전환 (별도 트랜잭션)
                    updateFileTaskStatus(fileTask.getId(), LlmTask.TaskStatus.RUNNING);

                    log.info("파일 리뷰 시작: PR #{}, 파일: {}",
                            fileTask.getPrReviewJob().getPrNumber(),
                            fileTask.getPullRequestFile().getFileName());

                    // 3) LLM 호출 - 직접 호출로 단순화
                    LlmTaskResult result = llmReviewService.reviewFile(fileTask);

                    // 4) 성공 처리 (별도 트랜잭션)
                    updateFileTaskWithSuccess(fileTask.getId(), result);

                    log.info("파일 리뷰 완료: PR #{}, 파일: {}",
                            fileTask.getPrReviewJob().getPrNumber(),
                            fileTask.getPullRequestFile().getFileName());

                    return result;

                } finally {
                    limiter.release(); // ★ 반드시 해제
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                String msg = "리뷰 중단됨: " + ie.getMessage();
                updateFileTaskWithError(fileTask.getId(), msg);
                throw new RuntimeException(msg, ie);

            } catch (Exception e) {
                String msg = "리뷰 실패: " + e.getMessage();
                updateFileTaskWithError(fileTask.getId(), msg);

                log.warn("파일 리뷰 중 예외: PR #{}, 파일: {}, 원인: {}",
                        fileTask.getPrReviewJob().getPrNumber(),
                        fileTask.getPullRequestFile().getFileName(),
                        e.toString());

                throw new RuntimeException(msg, e);
            }
        }, asyncTaskExecutor);
    }

    /**
     * 트랜잭션 내에서 파일 태스크 상태 업데이트
     */
    @Transactional
    protected void updateFileTaskStatus(Long taskId, LlmTask.TaskStatus status) {
        LlmTask task = llmTaskRepository.findById(taskId).orElse(null);
        if (task != null) {
            task.updateStatus(status);
            llmTaskRepository.save(task);
        }
    }

    /**
     * 트랜잭션 내에서 파일 태스크 성공 처리
     */
    @Transactional
    protected void updateFileTaskWithSuccess(Long taskId, LlmTaskResult result) {
        LlmTask task = llmTaskRepository.findById(taskId).orElse(null);
        if (task != null) {
            task.completeWithSuccess(result);
            llmTaskRepository.save(task);
        }
    }



    /**
     * 트랜잭션 내에서 파일 태스크 실패 처리
     */

    @Transactional
    protected void updateFileTaskWithError(Long taskId, String errorMessage) {
        LlmTask task = llmTaskRepository.findById(taskId).orElse(null);
        if (task != null) {
            task.completeWithError(errorMessage);
            llmTaskRepository.save(task);
            llmTaskRepository.save(task);
        }
    }

    /**
     * 요약 태스크를 실행합니다.
     * 낙관적 잠금 오류 방지를 위한 안전한 처리
     */
    private LlmTaskResult executeSummaryTask(PrReviewJob reviewJob, List<LlmTask> fileTasks) {
        try {
            // 요약 태스크 생성 (별도 트랜잭션으로 안전하게)
            LlmTask summaryTask = createSummaryTaskWithTransaction(reviewJob);

            log.info("PR 요약 시작: PR #{}", reviewJob.getPrNumber());

            // 요약 생성 실행
            LlmTaskResult result = llmSummaryService.createEmptySummaryResult(summaryTask);

            // 요약 태스크 완료 처리 (별도 트랜잭션으로 안전하게)
            updateSummaryTaskWithSuccess(summaryTask.getId(), result);

            log.info("PR 요약 완료: PR #{}", reviewJob.getPrNumber());

            return result;

        } catch (Exception e) {
            log.error("PR 요약 생성 중 오류 발생: PR #{}", reviewJob.getPrNumber(), e);
            throw new RuntimeException("PR 요약 생성 실패", e);
        }
    }

    /**
     * 트랜잭션 내에서 요약 태스크 생성 (낙관적 잠금 오류 방지)
     */
    @Transactional
    protected LlmTask createSummaryTaskWithTransaction(PrReviewJob reviewJob) {
        LlmTask summaryTask = LlmTask.builder()
                .prReviewJob(reviewJob)
                .taskType(LlmTask.TaskType.PR_SUMMARY)
                .status(LlmTask.TaskStatus.CREATED)
                .priority(LlmTask.Priority.HIGH)
                .build();

        LlmTask savedTask = llmTaskRepository.save(summaryTask);

        // 실행 상태로 즉시 업데이트
        savedTask.updateStatus(LlmTask.TaskStatus.RUNNING);
        return llmTaskRepository.save(savedTask);
    }

    /**
     * 트랜잭션 내에서 요약 태스크 성공 처리 (낙관적 잠금 오류 방지)
     */
    @Transactional
    protected void updateSummaryTaskWithSuccess(Long taskId, LlmTaskResult result) {
        try {
            // 최신 엔티티를 다시 조회하여 낙관적 잠금 오류 방지
            LlmTask task = llmTaskRepository.findById(taskId).orElse(null);
            if (task != null) {
                task.completeWithSuccess(result);
                llmTaskRepository.save(task);
            }
        } catch (Exception e) {
            log.warn("요약 태스크 성공 업데이트 실패 (무시됨): taskId={}, 원인: {}", taskId, e.getMessage());
            // 낙관적 잠금 실패는 무시 (이미 다른 스레드에서 처리되었을 가능성)
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
