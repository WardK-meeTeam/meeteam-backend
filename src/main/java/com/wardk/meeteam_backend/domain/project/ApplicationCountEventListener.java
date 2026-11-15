package com.wardk.meeteam_backend.domain.project;

import com.wardk.meeteam_backend.domain.application.ApplicationAppliedEvent;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

/**
 * 지원 커밋 후 프로젝트 누적 지원 수를 비동기로 +1 하는 리스너.
 * <p>
 * apply 트랜잭션 안에서 증가시키면 project 행 X-lock을 지원서 INSERT~커밋까지 쥐게 되어
 * 같은 프로젝트의 동시 지원이 전부 직렬화된다. 그래서 커밋 이후(AFTER_COMMIT) 별도
 * 트랜잭션으로 분리해 락 소유를 "UPDATE ~ 커밋"으로 최소화한다.
 * 지원 트랜잭션이 롤백되면 이벤트 자체가 소비되지 않으므로 카운터만 증가하는 일은 없다.
 * <p>
 * 트레이드오프: 카운터 반영이 수 ms 지연되고(최종 일관성), 커밋 직후 서버가 죽으면
 * 해당 건의 카운트가 유실될 수 있다. 누적 지원 수는 통계성 값이라 허용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationCountEventListener {

    private final ProjectRepository projectRepository;
    private final MeterRegistry meterRegistry;

    @Async("applicationCountExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ApplicationAppliedEvent event) {
        long updateStartNanos = System.nanoTime();
        projectRepository.increaseApplicationCount(event.projectId());
        recordLockTiming(event.projectId(), updateStartNanos, System.nanoTime());
    }

    /**
     * applicationCount UPDATE가 잡은 project 행 X-lock의 소유 시간을 측정합니다.
     * <p>
     * InnoDB는 커밋 시점에 행 락을 해제하므로, UPDATE 반환 직후부터 트랜잭션 완료까지가 락 소유 구간입니다.
     * <ul>
     *   <li>{@code project.apply.lock.acquire} — UPDATE 문 수행 시간 (다른 트랜잭션의 락 대기 포함)</li>
     *   <li>{@code project.apply.lock.held} — 락 획득 후 커밋 완료까지 소유 시간</li>
     * </ul>
     */
    private void recordLockTiming(Long projectId, long updateStartNanos, long lockAcquiredNanos) {
        meterRegistry.timer("project.apply.lock.acquire")
                .record(lockAcquiredNanos - updateStartNanos, TimeUnit.NANOSECONDS);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                long heldNanos = System.nanoTime() - lockAcquiredNanos;
                meterRegistry.timer("project.apply.lock.held")
                        .record(heldNanos, TimeUnit.NANOSECONDS);
                log.info("project 행 락 소유 시간 - projectId: {}, held: {}ms, committed: {}",
                        projectId, heldNanos / 1_000_000.0, status == STATUS_COMMITTED);
            }
        });
    }
}