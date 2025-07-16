package com.wardk.meeteam_backend.global.loginRegister.dto.oauth;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class GoogleUserDetails implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleUserDetails(Map<String, Object> attributes) {
        this.attributes = attributes;
        log.info("GoogleUserDetails 생성 - attributes: {}", attributes);
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getProviderId() {
        // 모든 가능한 ID 속성 확인
        String[] possibleIds = {"sub", "id", "user_id", "google_id"};

        for (String key : possibleIds) {
            Object value = attributes.get(key);
            if (value != null && !value.toString().isEmpty()) {
                log.info("ProviderId found - key: {}, value: {}", key, value);
                return String.valueOf(value);
            }
        }

        // 모든 속성 출력
        log.error("ProviderId not found in attributes: {}", attributes);
        throw new IllegalArgumentException("Google OAuth2 response does not contain valid provider ID");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }
}
