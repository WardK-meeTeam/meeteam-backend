package com.wardk.meeteam_backend.global.auth.service;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.global.config.OAuth2Properties;
import com.wardk.meeteam_backend.web.auth.dto.CustomOauth2UserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOauth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final OAuth2Properties oAuth2Properties;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        ParsedUserInfo userInfo = parseAttributes(provider, oAuth2User.getAttributes());

        if (userInfo.email() == null || userInfo.email().isEmpty()) {
            throw new OAuth2AuthenticationException("OAuth2 사용자 이메일 정보가 없습니다.");
        }

        // Provider와 ProviderId로 사용자를 조회
        Optional<Member> memberOptional = memberRepository.findByProviderAndProviderId(provider, userInfo.providerId());
        if (memberOptional.isPresent()) {
            Member member = memberOptional.get();
            log.info("기존 회원입니다: {}", member.getEmail());
            // isNewMember 플래그를 false로 설정하여 반환
            return new CustomOauth2UserDetails(member, oAuth2User.getAttributes(), false);
        } else {
            log.info("신규 회원입니다. 임시 회원 정보를 생성합니다: {}", userInfo.email());
            Member tempMember = Member.builder()
                .email(userInfo.email())
                .provider(provider)
                .providerId(userInfo.providerId())
                .role(UserRole.OAUTH2_GUEST) // 임시 권한 부여
                .build();
            // isNewMember 플래그를 true로 설정하여 반환
            return new CustomOauth2UserDetails(tempMember, oAuth2User.getAttributes(), true);
        }
    }

    /**
     * ★ 3. Provider별로 다른 attributes를 파싱하여 표준화된 DTO로 반환하는 메서드
     */
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

