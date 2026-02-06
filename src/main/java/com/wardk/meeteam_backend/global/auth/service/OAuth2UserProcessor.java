package com.wardk.meeteam_backend.global.auth.service;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.global.config.OAuth2Properties;
import com.wardk.meeteam_backend.web.auth.dto.CustomOauth2UserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * OAuth2/OIDC 사용자 정보를 처리하는 핵심 로직을 담당하는 클래스.
 * 두 종류의 서비스(일반, OIDC)에서 공통으로 사용됩니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserProcessor {

    private final MemberRepository memberRepository;
    private final OAuth2Properties oAuth2Properties;

    public OAuth2User process(String providerName, OAuth2User oAuth2User, String oauthAccessToken) {
        ParsedUserInfo userInfo = parseAttributes(providerName, oAuth2User.getAttributes());

        Optional<Member> memberOptional = memberRepository.findByProviderAndProviderId(providerName, userInfo.providerId());

        Member member;
        boolean isNewMember;

        if (memberOptional.isPresent()) {
            member = memberOptional.get();
            isNewMember = false;
            log.info("기존 회원입니다: {}", member.getEmail());
        } else {
            isNewMember = true;
            member = Member.createOAuth2Guest(
                userInfo.email(),
                userInfo.name(),
                providerName,
                userInfo.providerId()
            );
            log.info("신규 회원입니다. 임시 회원 정보를 생성합니다: {}", userInfo.email());
        }

        // 구글(OIDC)과 깃허브(OAuth2) 사용자를 구분하여 적절한 CustomUserDetails 객체 생성
        if (oAuth2User instanceof OidcUser) {
            DefaultOidcUser oidcUser = (DefaultOidcUser) oAuth2User;
            return new CustomOauth2UserDetails(member, oAuth2User.getAttributes(), isNewMember,
                oidcUser.getIdToken(), oidcUser.getUserInfo(), oauthAccessToken);
        } else {
            return new CustomOauth2UserDetails(member, oAuth2User.getAttributes(), isNewMember, oauthAccessToken);
        }
    }

    private ParsedUserInfo parseAttributes(String providerName, Map<String, Object> attributes) {
        String providerId, email, name;
        OAuth2Properties.Providers providers = oAuth2Properties.getProviders();
        if (providers.getGoogle().getName().equals(providerName)) {
            OAuth2Properties.Providers.Google googleProps = providers.getGoogle();
            providerId = (String) attributes.get(googleProps.getIdAttribute());
            email = (String) attributes.get(googleProps.getEmailAttribute());
            name = (String) attributes.get(googleProps.getUserNameAttribute());

        } else if (providers.getGithub().getName().equals(providerName)) {
            OAuth2Properties.Providers.Github githubProps = providers.getGithub();
            providerId = String.valueOf(attributes.get(githubProps.getIdAttribute())); // GitHub ID는 숫자일 수 있으므로 String으로 변환
            email = (String) attributes.get(githubProps.getEmailAttribute());
            name = (String) attributes.get(githubProps.getUserNameAttribute());

            // Github는 이메일이 null일 수 있으므로, providerId 이용한 임시 이메일 생성
            if (email == null) {
                email = providerId + "@users.noreply.github.com";
                log.warn("GitHub 이메일 정보가 없어 임시 이메일을 생성했습니다: {}", email);
            }
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 Provider: " + providerName);
        }

        return new ParsedUserInfo(providerId, email, name);
    }

    /**
     * 파싱된 사용자 정보를 담기 위한 간단한 record 클래스
     */
    private record ParsedUserInfo(String providerId, String email, String name) {}
}
