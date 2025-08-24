package com.wardk.meeteam_backend.domain.notification.repository;


import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Duration;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;

@Repository
@RequiredArgsConstructor
public class EmitterRepositoryImpl implements EmitterRepository{

    // emitterId = "{memberId}_{timestamp}"
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>(); // in-memory: active SSE connections per tab/device

    /**
     * RedisTemplate<String, Object> + Serializer → 자동 직렬화/역직렬화 (바이너리 or JSON)
     * StringRedisTemplate → 내가 직접 문자열(JSON 포함) 로 바꿔서 넣어야 함
     */
    private final StringRedisTemplate stringRedisTemplate;          // ZSET 용(문자열)
    private final RedisTemplate<String, Object> redisObjectTemplate; // 저장될때는 바이너리로 저장되지만 객체 자체로 바로 넣을수 있다.

    @Override
    public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    @Override
    public void saveEventCache(String eventId, Object event) {

        String[] parts = eventId.split("_", 2);

        if (parts.length < 2) return;

        String memberId = parts[0];
        long timestamp;
        try {
            timestamp = Long.parseLong(parts[1]);

        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.INVALID_EVENT_ID);
        }

        String zkey = "eventCache:" + memberId;
        String vkey = "eventCache:evt:" + eventId;

        // 1) payload: 객체 그대로 저장 (TTL 5일)
        redisObjectTemplate.opsForValue().set(vkey, event, Duration.ofDays(1));

        // 2) ZSET 인덱스(멤버=eventId, score=timestamp)
        stringRedisTemplate.opsForZSet().add(zkey, eventId, timestamp);

        // 3) ZSET 자체 TTL
        stringRedisTemplate.expire(zkey, Duration.ofDays(7));
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

        String zkey = "eventCache:" + memberIdPrefix;
        Set<TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().rangeWithScores(zkey, 0, -1);

        if (tuples == null || tuples.isEmpty()) return Map.of();

        Map<String, Object> result = new LinkedHashMap<>();
        for (TypedTuple<String> t : tuples) {
            String eventId = t.getValue();
            if (eventId == null) continue;
            String vkey = "eventCache:evt:" + eventId;

            Object payload = redisObjectTemplate.opsForValue().get(vkey); // ← 역직렬화 자동
            if (payload != null) {
                result.put(eventId, payload);
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> findEventCacheAfterByMemberId(String memberId, long afterTs) {

        String zkey = "eventCache:" + memberId;

        // afterTs 이후(엄밀히는 strictly greater)만 재전송: (afterTs, +inf]
        Set<TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet()
                        .rangeByScoreWithScores(zkey, afterTs + 1, Double.POSITIVE_INFINITY);

        if (tuples == null || tuples.isEmpty()) return Map.of();

        Map<String, Object> result = new LinkedHashMap<>();
        for (TypedTuple<String> t : tuples) {
            String eventId = t.getValue();
            if (eventId == null) continue;

            String vkey = "eventCache:evt:" + eventId;
            Object payload = redisObjectTemplate.opsForValue().get(vkey); // 자동 역직렬화
            if (payload != null) {
                result.put(eventId, payload);
            }
        }
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

        String zkey = "eventCache:" + memberIdPrefix;
        Set<String> eventIds = stringRedisTemplate.opsForZSet().range(zkey, 0, -1);
        if (eventIds != null) {
            for (String eid : eventIds) {
                stringRedisTemplate.delete("eventCache:evt:" + eid);
            }
        }
        stringRedisTemplate.delete(zkey);


    }
}
