package com.wardk.meeteam_backend.domain.notification.repository;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface EmitterRepository {


    SseEmitter save(String emitterId, SseEmitter sseEmitter);

    void saveEventCache(String eventId, Object event);

    Map<String, SseEmitter> findAllEmitterStartWithByMemberId(String memberIdPrefix);

    Map<String, Object> findAllEventCacheStartWithByMemberId(String memberIdPrefix);

    void deleteById(String emitterId);

    void deleteAllEmitterStartWithId(String memberIdPrefix);

    void deleteAllEventCacheStartWithId(String memberIdPrefix);
}
