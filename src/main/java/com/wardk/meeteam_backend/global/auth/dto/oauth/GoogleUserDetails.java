package com.wardk.meeteam_backend.global.auth.dto.oauth;

import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class GoogleUserDetails implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleUserDetails(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            throw new CustomException(ErrorCode.OAUTH2_ATTRIBUTES_EMPTY);
        }
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
        throw new CustomException(ErrorCode.OAUTH2_PROVIDER_ID_NOT_FOUND);
    }

    @Override
    public String getEmail() {
        try {
            String email = (String) attributes.get("email");
            log.info("Google Email: {}", email);

            if (email == null || email.trim().isEmpty()) {
                throw new CustomException(ErrorCode.OAUTH2_EMAIL_NOT_FOUND);
            }

            return email;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google Email 추출 중 오류 발생", e);
            throw new CustomException(ErrorCode.OAUTH2_EMAIL_NOT_FOUND);
        }
    }

    @Override
    public String getName() {
        try {
            String name = (String) attributes.get("name");
            log.info("Google Name: {}", name);

            if (name == null || name.trim().isEmpty()) {
                name = "Google User"; // 기본값 설정
            }

            return name;
        } catch (Exception e) {
            log.error("Google Name 추출 중 오류 발생", e);
            return "Google User"; // 이름은 필수가 아니므로 기본값 반환
        }
    }
}
