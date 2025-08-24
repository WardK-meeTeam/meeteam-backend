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
import lombok.extern.slf4j.Slf4j;
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

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT); // 1시간 타임아웃으로 SSE 생성

        // 저장 및 수명 관리
        String emitterId = makeEmitterId(memberId); // "{memberId}_{timestamp}"
        emitterRepository.save(emitterId, emitter); // 메모리에 emitter 등록
        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId)); // 정상 종료 시 정리
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId)); // 타임아웃 시 정리
        emitter.onError(e -> emitterRepository.deleteById(emitterId)); // 에러 시 정리

        // 1) 연결 확인용 더미 이벤트(503 방지)
        sendToClient(emitter, "connect","connected"); // 503 방지용 더미 이벤트


        // ── 재연결: Last-Event-ID 이후의 미수신 이벤트 재전송 (ZSET score 범위 조회)
        if (hasText(lastEventId)) {
            long afterTs = extractTs(lastEventId);
            Map<String, Object> cachedEvents =
                    emitterRepository.findEventCacheAfterByMemberId(String.valueOf(memberId), afterTs); // Redis에서 시간순 Map

            cachedEvents.forEach((eid, payload) -> sendReplay(emitter, eid, payload)); // 이벤트명(name) 포함 재전송
        }

        return emitter;
    }

    // ====== 알림 생성 + 실시간 전송 ======
    @Transactional
    public void notifyTo(Member receiver, NotificationType type, Project project, Long actorId) {


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

    // ====== 공용 전송 헬퍼 ======
    private void sendToClient(SseEmitter emitter, String eventId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .data(data)); // 공용(단순) 전송
        } catch (IOException ex) {
            // 전송 실패 시 emitter 제거
            emitter.completeWithError(ex);
        }
    }

    private void sendReplay(SseEmitter emitter, String eventId, Object data) {
        try {
            if (data instanceof SseEnvelope<?> env && env.getType() != null) { // 저장해둔 타입이 있으면
                emitter.send(SseEmitter.event()
                        .id(eventId)
                        .name(env.getType().name()) // 재전송에도 이벤트명 부여
                        .data(data));
            } else {
                throw new CustomException(ErrorCode.NO_MATCHING_TYPE);
            }
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private long extractTs(String eventId) {
        if (eventId == null) return Long.MIN_VALUE;
        int idx = eventId.lastIndexOf('_');
        if (idx >= 0 && idx + 1 < eventId.length()) {
            try {
                return Long.parseLong(eventId.substring(idx + 1));
            } catch (NumberFormatException ignore) {
                log.error("Invalid eventId format: \" "+ eventId);
                throw new CustomException(ErrorCode.INVALID_EVENT_ID);
            }
        } else {
            // eventId가 순수 timestamp일 수도 있는 경우
            try {
                return Long.parseLong(eventId);
            } catch (NumberFormatException ignore) {

                log.error("Invalid eventId format: \" "+ eventId);
                throw new CustomException(ErrorCode.INVALID_EVENT_ID);

            }
        }
    }

    private String makeEmitterId(Long memberId) {
        return memberId + "_" + System.currentTimeMillis();
    }

    private String makeEventId(Long memberId) {
        return memberId + "_" + System.currentTimeMillis();
    }
}
