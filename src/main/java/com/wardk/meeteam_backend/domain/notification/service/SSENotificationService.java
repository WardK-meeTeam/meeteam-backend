package com.wardk.meeteam_backend.domain.notification.service;


import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.notification.NotificationEvent;
import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.domain.notification.repository.EmitterRepository;
import com.wardk.meeteam_backend.domain.notification.repository.NotificationRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;

import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.notification.SseEnvelope;
import com.wardk.meeteam_backend.web.notification.factory.NotificationPayloadFactory;
import com.wardk.meeteam_backend.web.notification.payload.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.util.StringUtils.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SSENotificationService {


    private static final long DEFAULT_TIMEOUT = 60L * 60L * 1000L; // 1h

    private final NotificationRepository notificationRepository;
    private final EmitterRepository emitterRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final NotificationPayloadFactory payloadFactory;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * NotificationEvent를 받아서 알림을 생성하고 실시간으로 전송하는 메서드입니다.
     * @param event 알림 이벤트 객체
     * 이 메서드에서 직접 엔티티를 조회하여 영속성 컨텍스트 내에서 처리합니다.
     * @Async로 실행되는 환경에서도 안전하게 엔티티를 사용할 수 있습니다.
     */
    @Transactional
    public void notifyTo(NotificationEvent event) {

        Member receiver = memberRepository.findById(event.getReceiverId())
                .orElseThrow(() -> {
                    log.error("[알림] 수신자를 찾을 수 없음: {}", event.getReceiverId());
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });

        Project project = projectRepository.findById(event.getProjectId())
                .orElseThrow(() -> {
                    log.error("[알림] 프로젝트를 찾을 수 없음: {}", event.getProjectId());
                    return new CustomException(ErrorCode.PROJECT_NOT_FOUND);
                });

        // 기존 notifyTo 메서드 호출
        notifyTo(receiver, project, event);
    }

    public void notifyTo(Member receiver,Project project, NotificationEvent event) {

        // actorId 가 꼭 필요한데 null이면 예외
        requireActor(event);
        Member actor = getActor(event);

        Payload payload = payloadFactory.create(event);

        SseEnvelope<Object> envelope = createEnvelope(event.getType(), payload);


        broadcast(receiver.getId(), event.getType(), envelope);
    }


    private Member getActor(NotificationEvent event) {
        Long actorId = event.getActorId();
        NotificationType type = event.getType();

        // actor 조회 (필요할 때만)
        Member actor = null;
        if (actorId != null && type.requiresActor()) {
            actor = memberRepository.findById(actorId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        }
        return actor;
    }

    private static void requireActor(NotificationEvent event) {
        NotificationType type = event.getType();
        Long actorId = event.getActorId();
        if (type.requiresActor() && actorId == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private void broadcast(Long receiverId, NotificationType type, Object envelope) {
        Map<String, SseEmitter> emitters =
                emitterRepository.findAllEmitterStartWithByMemberId(String.valueOf(receiverId));

        // 동일 이벤트는 한 번만 저장하고, 같은 eventId로 모든 emitter에 전송
        String eventId = makeEventId(receiverId); // 동일 이벤트 하나의 ID
        emitterRepository.saveEventCache(eventId, envelope); // 캐시에 1회만 저장

        emitters.forEach((emitterId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(eventId)
                        .name(type.name()) // 프론트에서 EventSource.addEventListener(type)
                        .data(envelope));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        });
    }


    private String makeEventId(Long memberId) {
        return memberId + "_" + System.currentTimeMillis();
    }

    @Transactional
    public void notifyTo2(Long memberId, NotificationType type, Long projectId, String projectName, LocalDate occurredAt) {


        Member receiver = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));


        // 1) DB 저장 (과거 기록 조회용)
        Notification notification = notificationRepository.save(
                Notification.builder()
                        .receiver(receiver)
                        .type(type)
                        .project(project)
                        .isRead(false)
                        .build()
        );

        ProjectEndedPayload payload = new ProjectEndedPayload(projectId, memberId, projectName, occurredAt);
        SseEnvelope<Object> envelope = createEnvelope(type, payload);
        broadcast(receiver.getId(), type, envelope);

    }


    private static SseEnvelope<Object> createEnvelope(NotificationType type, Payload payload) {


        SseEnvelope<Object> envelope = SseEnvelope.builder()
                .type(type)
                .data(payload)
                .createdAt(LocalDate.now())
                .build();

        return envelope;
    }
}

// 2) 타입별 payload 조립
//        Payload payload = switch (type) {
//            case PROJECT_SELF_APPLY -> new ApplySelfApplyPayloadStrategy( //지원자에게 알림
//                    receiver.getId(), // 지원한 사람 == 받는 사람
//                    project.getName(),
//                    LocalDate.now()
//            );
//            case PROJECT_APPLY -> { // 내 프로젝트에 누군가 지원 (팀장에게 알림)
//                if (actor == null) throw new CustomException(ErrorCode.RECRUITMENT_NOT_FOUND);
//                yield new ProjectApplicationSubmittedPayload(
//                        applicationId, // 지원서Id -> 지원서 상세보기 API 를 호출용
//                        project.getId(), // 프로젝트Id -> 지원서 상세보기 API 호출용
//                        receiver.getId(),// 팀장 Id
//                        actor.getId(), // 지원자Id
//                        actor.getRealName(), // 지원자이름
//                        project.getName(), //프로젝트 이름
//                        LocalDate.now()
//                );
//            }
//
//            case PROJECT_APPLICATION_APPROVED -> new SimpleMessagePayload(
//                    receiver.getId(),
//                    project.getId(),
//                    ApprovalResult.APPROVED,
//                    LocalDate.now()
//            );
//            case PROJECT_APPLICATION_REJECTED -> new SimpleMessagePayload(
//                    receiver.getId(),
//                    project.getId(),
//                    ApprovalResult.REJECTED,
//                    LocalDate.now()
//            );
//            default -> throw new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND);
//        };
