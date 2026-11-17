package com.wardk.meeteam_backend.domain.application;

/**
 * 프로젝트 지원 트랜잭션이 커밋된 뒤 발행되는 이벤트.
 * <p>
 * 프로젝트 누적 지원 수 증가({@code ApplicationCountEventListener})가 이 이벤트를 구독한다.
 * AFTER_COMMIT 단계에서만 소비되므로, 지원 트랜잭션이 롤백되면 카운터는 증가하지 않는다.
 */
public record ApplicationAppliedEvent(Long projectId) {

    public static ApplicationAppliedEvent of(Long projectId) {
        return new ApplicationAppliedEvent(projectId);
    }
}