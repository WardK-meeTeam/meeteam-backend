package com.wardk.meeteam_backend.global.auth.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardk.meeteam_backend.global.auth.service.dto.OAuthLoginInfo;
import com.wardk.meeteam_backend.global.auth.service.dto.OAuthRegisterInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth2 일회용 코드 관리를 위한 Redis Repository
 * 리다이렉트 시 JWT 대신 UUID 기반 일회용 코드를 사용하여 보안을 강화합니다.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class OAuthCodeRepository {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "oauth:code:";
    private static final Duration REGISTER_TTL = Duration.ofMinutes(10);
    private static final Duration LOGIN_TTL = Duration.ofSeconds(60);

    /**
     * 신규 회원 가입 정보를 Redis에 저장하고 일회용 코드(UUID) 반환
     *
     * @param info 신규 회원 정보
     * @return 일회용 UUID 코드
     */
    public String saveRegisterInfo(OAuthRegisterInfo info) {
        String code = UUID.randomUUID().toString();
        String key = KEY_PREFIX + code;
        try {
            String json = objectMapper.writeValueAsString(info);
            stringRedisTemplate.opsForValue().set(key, json, REGISTER_TTL);
            log.info("OAuth 신규 회원 정보 저장 완료. code: {}, TTL: {}분", code, REGISTER_TTL.toMinutes());
        } catch (JsonProcessingException e) {
            log.error("OAuth 신규 회원 정보 직렬화 실패: {}", e.getMessage());
            throw new RuntimeException("OAuth 정보 저장 실패", e);
        }
        return code;
    }

    /**
     * 기존 회원 로그인 정보를 Redis에 저장하고 일회용 코드(UUID) 반환
     *
     * @param info 기존 회원 로그인 정보
     * @return 일회용 UUID 코드
     */
    public String saveLoginInfo(OAuthLoginInfo info) {
        String code = UUID.randomUUID().toString();
        String key = KEY_PREFIX + code;
        try {
            String json = objectMapper.writeValueAsString(info);
            stringRedisTemplate.opsForValue().set(key, json, LOGIN_TTL);
            log.info("OAuth 로그인 정보 저장 완료. code: {}, TTL: {}초", code, LOGIN_TTL.toSeconds());
        } catch (JsonProcessingException e) {
            log.error("OAuth 로그인 정보 직렬화 실패: {}", e.getMessage());
            throw new RuntimeException("OAuth 정보 저장 실패", e);
        }
        return code;
    }

    /**
     * 일회용 코드로 신규 회원 정보를 조회하고 즉시 삭제 (일회성 보장)
     *
     * @param code 일회용 UUID 코드
     * @return 신규 회원 정보 (없으면 empty)
     */
    public Optional<OAuthRegisterInfo> consumeRegisterInfo(String code) {
        String key = KEY_PREFIX + code;
        String json = stringRedisTemplate.opsForValue().getAndDelete(key);
        if (json == null) {
            log.warn("OAuth 신규 회원 정보를 찾을 수 없습니다. code: {}", code);
            return Optional.empty();
        }
        try {
            OAuthRegisterInfo info = objectMapper.readValue(json, OAuthRegisterInfo.class);
            log.info("OAuth 신규 회원 정보 조회 및 삭제 완료. code: {}", code);
            return Optional.of(info);
        } catch (JsonProcessingException e) {
            log.error("OAuth 신규 회원 정보 역직렬화 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 일회용 코드로 기존 회원 로그인 정보를 조회하고 즉시 삭제 (일회성 보장)
     *
     * @param code 일회용 UUID 코드
     * @return 기존 회원 로그인 정보 (없으면 empty)
     */
    public Optional<OAuthLoginInfo> consumeLoginInfo(String code) {
        String key = KEY_PREFIX + code;
        String json = stringRedisTemplate.opsForValue().getAndDelete(key);
        if (json == null) {
            log.warn("OAuth 로그인 정보를 찾을 수 없습니다. code: {}", code);
            return Optional.empty();
        }
        try {
            OAuthLoginInfo info = objectMapper.readValue(json, OAuthLoginInfo.class);
            log.info("OAuth 로그인 정보 조회 및 삭제 완료. code: {}", code);
            return Optional.of(info);
        } catch (JsonProcessingException e) {
            log.error("OAuth 로그인 정보 역직렬화 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
