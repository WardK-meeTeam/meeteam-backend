package com.wardk.meeteam_backend.domain.notification;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.notification.service.SSENotificationService;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final SSENotificationService notificationService;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;


    /**
     * 트랜잭션 커밋 이후 발행된 NotificationEvent를 비동기로 수신하여 처리하는 메서드입니다.
     *
     * @param e 알림 이벤트 객체 (수신자 ID, 발신자 ID, 프로젝트 ID, 알림 유형 포함)
     * @throws CustomException 이벤트에 포함된 수신자 또는 프로젝트를 찾을 수 없는 경우 발생
     *
     * 1) 이벤트에 담긴 receiverId로 수신자(Member)를 조회합니다.
     * 2) 이벤트에 담긴 projectId로 프로젝트(Project)를 조회합니다.
     * 3) 조회된 엔티티와 이벤트 정보를 기반으로 NotificationService를 호출하여 알림을 생성/전송합니다.
     *
     * 비동기(@Async)로 실행되며, 트랜잭션이 성공적으로 커밋된 이후(AFTER_COMMIT)에만 동작합니다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(NotificationEvent e) {


        Member receiver = memberRepository.findById(e.getReceiverId())
                .orElseThrow(
                        () -> {
                            log.error("[알림]수신자를 찾을 없음");
                            return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                        }
                );

        Project project = projectRepository.findById(e.getProjectId())
                .orElseThrow(
                        () -> {
                            log.error("[알림] 프로젝트를 찾을 수 없음");
                            return new CustomException(ErrorCode.PROJECT_NOT_FOUND);
                        }
                );

        // actor 엔티티를 직접 조회하지 않고, id만 넘김
        notificationService.notifyTo(receiver, e.getType(), project, e.getActorId(), e.getApplicationId());

    }
}
