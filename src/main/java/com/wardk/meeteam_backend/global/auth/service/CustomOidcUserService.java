package com.wardk.meeteam_backend.global.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * OIDC(예: Google) 로그인을 처리하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final OAuth2UserProcessor userProcessor;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String providerName = userRequest.getClientRegistration().getRegistrationId();
        String oauthAccessToken = userRequest.getAccessToken().getTokenValue();
        // OidcUser도 OAuth2User의 하위 타입이므로 process 메서드에 전달 가능
        return (OidcUser) userProcessor.process(providerName, oidcUser, oauthAccessToken);
    }
}
