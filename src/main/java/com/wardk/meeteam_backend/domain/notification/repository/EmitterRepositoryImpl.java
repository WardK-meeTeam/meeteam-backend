package com.wardk.meeteam_backend.domain.notification.repository;


import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class EmitterRepositoryImpl implements EmitterRepository{


    // emitterId = "{memberId}_{timestamp}"
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // eventId = "{memberId}_{timestamp}"
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();

    @Override
    public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    @Override
    public void saveEventCache(String eventId, Object event) {
        eventCache.put(eventId, event);
    }

    @Override
    public Map<String, SseEmitter> findAllEmitterStartWithByMemberId(String memberIdPrefix) {
        // 같은 userId로 열린 여러 탭/기기(emitter)를 모두 반환
        Map<String, SseEmitter> result = new ConcurrentHashMap<>();
        emitters.forEach((key, emitter) -> {
            if (key.startsWith(memberIdPrefix)) {
                result.put(key, emitter);
            }
        });
        return result;
    }

    @Override
    public Map<String, Object> findAllEventCacheStartWithByMemberId(String memberIdPrefix) {
        // 미수신 이벤트 캐시(Last-Event-ID 재전송용)
        Map<String, Object> result = new ConcurrentHashMap<>();
        eventCache.forEach((key, event) -> {
            if (key.startsWith(memberIdPrefix)) {
                result.put(key, event);
            }
        });
        return result;
    }

    @Override
    public void deleteById(String emitterId) {
        emitters.remove(emitterId);
    }

    @Override
    public void deleteAllEmitterStartWithId(String memberIdPrefix) {
        emitters.keySet().removeIf(key -> key.startsWith(memberIdPrefix));
    }

    @Override
    public void deleteAllEventCacheStartWithId(String memberIdPrefix) {
        eventCache.keySet().removeIf(key -> key.startsWith(memberIdPrefix));
    }
}
