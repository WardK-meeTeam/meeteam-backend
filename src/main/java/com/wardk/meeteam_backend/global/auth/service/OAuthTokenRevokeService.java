package com.wardk.meeteam_backend.global.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * OAuth 제공자(Google, GitHub)의 Access Token을 철회하는 서비스.
 * 로그아웃 시 호출되어 OAuth 세션을 완전히 종료합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthTokenRevokeService {

    private final ClientRegistrationRepository clientRegistrationRepository;

    private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke";
    private static final String GITHUB_REVOKE_URL = "https://api.github.com/applications/{client_id}/token";

    /**
     * OAuth 토큰을 철회합니다.
     *
     * @param provider OAuth 제공자 (google, github)
     * @param accessToken 철회할 Access Token
     * @return 철회 성공 여부
     */
    public boolean revokeToken(String provider, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("OAuth 토큰이 없어 철회를 건너뜁니다.");
            return false;
        }

        try {
            if ("google".equalsIgnoreCase(provider)) {
                return revokeGoogleToken(accessToken);
            } else if ("github".equalsIgnoreCase(provider)) {
                return revokeGitHubToken(accessToken);
            } else {
                log.warn("지원하지 않는 OAuth 제공자입니다: {}", provider);
                return false;
            }
        } catch (Exception e) {
            log.error("OAuth 토큰 철회 중 오류 발생 (provider: {}): {}", provider, e.getMessage());
            return false;
        }
    }

    /**
     * Google OAuth 토큰 철회
     * https://developers.google.com/identity/protocols/oauth2/web-server#tokenrevoke
     */
    private boolean revokeGoogleToken(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", accessToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                GOOGLE_REVOKE_URL,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Google OAuth 토큰 철회 성공");
                return true;
            } else {
                log.warn("Google OAuth 토큰 철회 실패: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Google OAuth 토큰 철회 중 오류: {}", e.getMessage());
            return false;
        }
    }

    /**
     * GitHub OAuth 토큰 철회
     * https://docs.github.com/en/rest/apps/oauth-applications#delete-an-app-token
     */
    private boolean revokeGitHubToken(String accessToken) {
        ClientRegistration githubRegistration = clientRegistrationRepository.findByRegistrationId("github");
        if (githubRegistration == null) {
            log.warn("GitHub OAuth 설정을 찾을 수 없어 토큰 철회를 건너뜁니다.");
            return false;
        }

        String clientId = githubRegistration.getClientId();
        String clientSecret = githubRegistration.getClientSecret();

        if (clientId == null || clientSecret == null) {
            log.warn("GitHub OAuth 설정이 없어 토큰 철회를 건너뜁니다.");
            return false;
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(clientId, clientSecret);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        String requestBody = String.format("{\"access_token\":\"%s\"}", accessToken);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            String url = GITHUB_REVOKE_URL.replace("{client_id}", clientId);
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("GitHub OAuth 토큰 철회 성공");
                return true;
            } else {
                log.warn("GitHub OAuth 토큰 철회 실패: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("GitHub OAuth 토큰 철회 중 오류: {}", e.getMessage());
            return false;
        }
    }
}
