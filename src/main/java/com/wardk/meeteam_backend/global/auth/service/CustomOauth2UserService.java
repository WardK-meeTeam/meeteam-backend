package com.wardk.meeteam_backend.global.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * 일반 OAuth2(예: GitHub) 로그인을 처리하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class CustomOauth2UserService extends DefaultOAuth2UserService {

    private final OAuth2UserProcessor userProcessor;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String providerName = userRequest.getClientRegistration().getRegistrationId();
        String oauthAccessToken = userRequest.getAccessToken().getTokenValue();
        return userProcessor.process(providerName, oAuth2User, oauthAccessToken);
    }
}
