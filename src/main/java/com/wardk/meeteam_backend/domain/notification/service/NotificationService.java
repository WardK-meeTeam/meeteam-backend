package com.wardk.meeteam_backend.domain.notification.service;


import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.domain.notification.repository.EmitterRepository;
import com.wardk.meeteam_backend.domain.notification.repository.NotificationRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;

import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
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
    // emitter 를 client 에게 반환 ( 서버로 부터 이벤트를 client 가 받을 수 있게 된다.)
    public SseEmitter subscribe(String email, String lastEventId) {

        Member member = memberRepository.findOptionByEmail(email)
                .orElseThrow( () -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Long memberId = member.getId();

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT); // 1시간 타임아웃으로 SSE 생성

        // 저장 및 수명 관리
        String emitterId = makeEmitterId(memberId); // "{memberId}_{timestamp}"
        emitterRepository.save(emitterId, emitter); // 메모리에 emitter 등록

        //즉시 실행이 아니라 미래에 발생할 이벤트에 대비한 예약 실행 (바로 실행되는게 아니라 미리 등록해 놓는것이다.)
        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId)); // 정상 종료 시 정리
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId)); // 타임아웃 시 정리
        emitter.onError(e -> emitterRepository.deleteById(emitterId)); // 에러 시 정리

        // 1) 연결 확인용 더미 이벤트(503 방지)
        sendPing(emitter);


        // ── 재연결: Last-Event-ID 이후의 미수신 이벤트 재전송 (ZSET score 범위 조회)
        if (hasText(lastEventId)) {
            long afterTs = extractTs(lastEventId);
            if (afterTs != Long.MIN_VALUE) {
                Map<String, Object> cachedEvents =
                        emitterRepository.findEventCacheAfterByMemberId(String.valueOf(memberId), afterTs); // Redis에서 시간순 Map
                cachedEvents.forEach((eid, payload) -> sendReplay(emitter, eid, payload)); // 이벤트명(name) 포함 재전송
            } else {
                log.warn("Skip replay due to non-numeric Last-Event-ID: {}", lastEventId);
            }
        }

        return emitter;
    }

    // ====== 알림 생성 + 실시간 전송 ======
    /**
     * 특정 사용자에게 알림을 생성하고 실시간으로 전송하는 메서드입니다.
     *
     * @param receiver 알림을 받을 대상 사용자
     * @param type 알림 유형 (지원, 승인, 거절 등)
     * @param project 알림과 연관된 프로젝트
     * @param actorId 알림을 발생시킨 주체의 ID (일부 유형에서는 필수)
     * @throws CustomException actorId가 필요한 알림 유형인데 null이거나,
     *                         존재하지 않는 사용자/프로젝트일 경우 발생
     *
     * 1) 알림 메시지를 생성하고 DB에 저장합니다.
     * 2) 알림 유형에 따라 payload 객체를 생성합니다.
     * 3) SSE를 통해 구독 중인 클라이언트에게 실시간 전송합니다.
     */
    @Transactional
    public void notifyTo(Member receiver, NotificationType type, Project project, Long actorId, Long applicantId) {


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
            case PROJECT_MY_APPLY -> new ApplyNotiPayload( //지원자에게 알림
                    receiver.getId(), // 지원한 사람 == 받는 사람
                    project.getName(),
                    finalMessage,
                    LocalDate.now()
            );
            case PROJECT_APPLY -> { // 내 프로젝트에 누군가 지원 (팀장에게 알림)
                if (actor == null) throw new CustomException(ErrorCode.RECRUITMENT_NOT_FOUND);
                yield new NewApplicantPayload(
                        applicantId, // 지원서Id -> 지원서 상세보기 API 를 호출용
                        project.getId(), // 프로젝트Id -> 지원서 상세보기 API 호출용
                        receiver.getId(),
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
            default -> throw new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND);
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

    // 초기 연결 확인용 핑 이벤트: id를 주지 않아 브라우저의 Last-Event-ID가 갱신되지 않도록 함
    private void sendPing(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("PING")
                    .data("connected"));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private long extractTs(String eventId) {
        if (eventId == null || eventId.isBlank()) return Long.MIN_VALUE;
        int idx = eventId.lastIndexOf('_');
        if (idx >= 0 && idx + 1 < eventId.length()) {
            try {
                return Long.parseLong(eventId.substring(idx + 1));
            } catch (NumberFormatException e) {
                log.warn("Invalid eventId format (suffix not numeric): {}", eventId);
                return Long.MIN_VALUE;
            }
        } else {
            try {
                return Long.parseLong(eventId);
            } catch (NumberFormatException e) {
                log.warn("Invalid eventId format (not numeric): {}", eventId);
                return Long.MIN_VALUE;
            }
        }
    }

    // Event-Id 구조를 {memberId}_{timestamp} 구조로 만듬
    // 앞부분 식별자용 , 뒷부분 이벤트 발생 시각
    private String makeEmitterId(Long memberId) {
        return memberId + "_" + System.currentTimeMillis();
    }

    private String makeEventId(Long memberId) {
        return memberId + "_" + System.currentTimeMillis();
    }
}
