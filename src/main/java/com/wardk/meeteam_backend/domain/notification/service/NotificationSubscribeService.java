package com.wardk.meeteam_backend.domain.notification.service;


import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.notification.repository.EmitterRepository;
import com.wardk.meeteam_backend.domain.notification.repository.NotificationRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.notification.SseEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSubscribeService {

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


    // Event-Id 구조를 {memberId}_{timestamp} 구조로 만듬
    // 앞부분 식별자용 , 뒷부분 이벤트 발생 시각

    private String makeEmitterId(Long memberId) {
        return memberId + "_" + System.currentTimeMillis();
    }

    private String makeEventId(Long memberId) {
        return memberId + "_" + System.currentTimeMillis();
    }

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

}
