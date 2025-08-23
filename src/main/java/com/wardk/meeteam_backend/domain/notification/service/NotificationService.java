package com.wardk.meeteam_backend.domain.notification.service;


import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.domain.notification.repository.EmitterRepository;
import com.wardk.meeteam_backend.domain.notification.repository.NotificationRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import com.wardk.meeteam_backend.web.notification.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.util.StringUtils.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {


    private static final long DEFAULT_TIMEOUT = 60L * 60L * 1000L; // 1h

    private final NotificationRepository notificationRepository;
    private final EmitterRepository emitterRepository;
    private final MemberRepository memberRepository;

    // ====== 구독 ======
    public SseEmitter subscribe(String email, String lastEventId) {

        Member member = memberRepository.findOptionByEmail(email)
                .orElseThrow( () -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Long memberId = member.getId();

        String emitterId = makeEmitterId(memberId);
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // 저장 및 수명 관리
        emitterRepository.save(emitterId, emitter);
        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));
        emitter.onError(e -> emitterRepository.deleteById(emitterId));

        // 1) 연결 확인용 더미 이벤트(503 방지)
        String eventId = makeEventId(memberId);
        sendToClient(emitter, "connect","connected");


        // 2) 클라이언트가 Last-Event-ID를 보냈다면, 미수신 캐시 재전송
        if (hasText(lastEventId)) {
            Map<String, Object> cachedEvents =
                    emitterRepository.findAllEventCacheStartWithByMemberId(String.valueOf(memberId));

            cachedEvents.entrySet().stream()
                    .filter(entry -> entry.getKey().compareTo(lastEventId) > 0) // last 이후만
                    .forEach(entry -> sendToClient(emitter, entry.getKey(), entry.getValue()));
        }

        return emitter;
    }

    // ====== 알림 생성 + 실시간 전송 ======
    @Transactional
    public void notifyTo(Member receiver, NotificationType type,Project project, Long actorId) {


        // actorId 가 꼭 필요한데 null이면 예외
        if (type.requiresActor() && actorId == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }


        // actor 조회 (필요할 때만)
        Member actor = null;
        if (actorId != null && type.requiresActor()) {
            actor = memberRepository.findById(actorId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        }

        // 메시지 생성
        String finalMessage = switch (type) {
            case PROJECT_MY_APPLY -> String.format("[%s] 에 지원이 완료되었습니다.", project.getName());
            case PROJECT_APPLY -> {
                if (actor == null) throw new CustomException(ErrorCode.ACTOR_NOT_FOUND);
                yield String.format("%s님이 [%s]에 지원했어요.", actor.getRealName(), project.getName());
            }
            case PROJECT_APPROVE -> String.format("[%s] 지원이 승인되었습니다.", project.getName());
            case PROJECT_REJECT -> String.format("[%s] 지원이 거절되었습니다.", project.getName());
        };



        // 1) DB 저장 (과거 기록 조회용)
        Notification notification = notificationRepository.save(
                Notification.builder()
                        .receiver(receiver)
                        .type(type)
                        .message(finalMessage)
                        .project(project)
                        .isRead(false)
                        .build()
        );


        // 2) 타입별 payload 조립
        Object payload = switch (type) {
            case PROJECT_MY_APPLY -> new ApplyNotiPayload(
                    receiver.getId(), // 지원한 사람 == 받는 사람
                    project.getName(),
                    finalMessage,
                    LocalDate.now()
            );
            case PROJECT_APPLY -> { // 내 프로젝트에 누군가 지원
                if (actor == null) throw new CustomException(ErrorCode.RECRUITMENT_NOT_FOUND);
                yield new NewApplicantPayload(
                        actor.getId(), // 지원자Id
                        actor.getRealName(), // 지원자이름
                        project.getName(), //프로젝트 이름
                        finalMessage,
                        LocalDate.now()
                );
            }
            case PROJECT_APPROVE -> new SimpleMessagePayload(
                    receiver.getId(),
                    project.getId(),
                    ApprovalResult.APPROVED,
                    finalMessage,
                    LocalDate.now()
            );
            case PROJECT_REJECT -> new SimpleMessagePayload(
                    receiver.getId(),
                    project.getId(),
                    ApprovalResult.REJECTED,
                    finalMessage,
                    LocalDate.now()
            );
            default -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND);
        };


        SseEnvelope<Object> envelope = SseEnvelope.builder()
                .type(type)
                .data(payload)
                .createdAt(LocalDate.now())
                .build();

        broadcast(receiver.getId(), type, envelope);


    }

    private void broadcast(Long receiverId, NotificationType type, Object envelope) {
        Map<String, SseEmitter> emitters =
                emitterRepository.findAllEmitterStartWithByMemberId(String.valueOf(receiverId));

        emitters.forEach((emitterId, emitter) -> {
            String eventId = makeEventId(receiverId);
            emitterRepository.saveEventCache(eventId, envelope); // 유실 대비: envelope 통째로 저장
            try {
                emitter.send(SseEmitter.event()
                        .id(eventId)
                        .name(type.name())   // ★ 프론트에서 이벤트명으로도 분기 가능
                        .data(envelope));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        });
    }

    // ====== 공용 전송 헬퍼 ======
    private void sendToClient(SseEmitter emitter, String eventId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .data(data));
        } catch (IOException ex) {
            // 전송 실패 시 emitter 제거
            emitter.completeWithError(ex);
        }
    }

    private String makeEmitterId(Long memberId) {
        return memberId + "_" + System.currentTimeMillis();
    }

    private String makeEventId(Long memberId) {
        return memberId + "_" + System.currentTimeMillis();
    }
}
