package com.wardk.meeteam_backend.global.loginRegister.dto.oauth;

import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class GitHubUserDetails implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GitHubUserDetails(Map<String, Object> attributes) {
        this.attributes = attributes;
        log.info("GitHubUserDetails 생성 - attributes: {}", attributes);

        if (attributes == null || attributes.isEmpty()) {
            throw new CustomException(ErrorCode.OAUTH2_ATTRIBUTES_EMPTY);
        }
    }

    @Override
    public String getProvider() {
        return "github";
    }

    @Override
    public String getProviderId() {
        try {
            Object id = attributes.get("id");
            if (id != null) {
                String stringValue = String.valueOf(id).trim();
                if (!stringValue.isEmpty() && !stringValue.equals("null")) {
                    log.info("GitHub ProviderId found: {}", stringValue);
                    return stringValue;
                }
            }

            log.error("GitHub ProviderId not found in attributes: {}", attributes);
            throw new CustomException(ErrorCode.OAUTH2_PROVIDER_ID_NOT_FOUND);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("GitHub ProviderId 추출 중 오류 발생", e);
            throw new CustomException(ErrorCode.OAUTH2_PROVIDER_ID_NOT_FOUND);
        }
    }

    @Override
    public String getEmail() {
        try {
            String email = (String) attributes.get("email");
            log.info("GitHub Email (primary): {}", email);

            // GitHub에서 이메일이 null이거나 비어있는 경우 처리
            if (email == null || email.trim().isEmpty()) {
                // login을 이메일 대신 사용 (임시 해결책)
                String login = (String) attributes.get("login");
                if (login != null && !login.trim().isEmpty()) {
                    email = login + "@github.local"; // 임시 이메일 생성
                    log.warn("GitHub 이메일이 없어 임시 이메일 생성: {}", email);
                    return email;
                }

                throw new CustomException(ErrorCode.OAUTH2_EMAIL_NOT_FOUND);
            }

            return email;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("GitHub Email 추출 중 오류 발생", e);
            throw new CustomException(ErrorCode.OAUTH2_EMAIL_NOT_FOUND);
        }
    }

    @Override
    public String getName() {
        try {
            String name = (String) attributes.get("name");
            log.info("GitHub Name: {}", name);

            // GitHub에서 name이 null인 경우 login 사용
            if (name == null || name.trim().isEmpty()) {
                name = (String) attributes.get("login");
                log.info("GitHub Name 대신 login 사용: {}", name);
            }

            if (name == null || name.trim().isEmpty()) {
                name = "GitHub User"; // 기본값 설정
            }

            return name;
        } catch (Exception e) {
            log.error("GitHub Name 추출 중 오류 발생", e);
            return "GitHub User"; // 이름은 필수가 아니므로 기본값 반환
        }
    }
}
