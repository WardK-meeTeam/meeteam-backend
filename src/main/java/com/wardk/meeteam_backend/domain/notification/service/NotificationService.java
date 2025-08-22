package com.wardk.meeteam_backend.domain.notification.service;


import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.notification.NotificationPayload;
import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.domain.notification.repository.EmitterRepository;
import com.wardk.meeteam_backend.domain.notification.repository.NotificationRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
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
        sendToClient(emitter, eventId, NotificationPayload.system("EventStream Created. memberId=" + memberId));

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
    public void notifyTo(Long receiverId, NotificationType type, String message, Project project) {
        // 1) DB 저장 (과거 기록 조회용)
        Notification notification = notificationRepository.save(
                Notification.builder()
                        .receiver(Member.builder().id(receiverId).build())
                        .type(type)
                        .message(message)
                        .project(project)
                        .isRead(false)
                        .build()
        );

        // 2) SSE 실시간 전송 (여러 탭/기기 고려: 같은 userId prefix의 모든 emitter로)
        NotificationPayload payload = NotificationPayload.from(notification);
        Map<String, SseEmitter> emitters =
                emitterRepository.findAllEmitterStartWithByMemberId(String.valueOf(receiverId));

        emitters.forEach((emitterId, emitter) -> {
            String eventId = makeEventId(receiverId);
            emitterRepository.saveEventCache(eventId, payload); // 유실 대비 캐시
            sendToClient(emitter, eventId, payload);
        });
    }

    // ====== 공용 전송 헬퍼 ======
    private void sendToClient(SseEmitter emitter, String eventId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name("notification")
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
