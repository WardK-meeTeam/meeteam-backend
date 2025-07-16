package com.wardk.meeteam_backend.global.loginRegister.service;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.global.loginRegister.dto.CustomOauth2UserDetails;
import com.wardk.meeteam_backend.global.loginRegister.dto.oauth.GoogleUserDetails;
import com.wardk.meeteam_backend.global.loginRegister.dto.oauth.OAuth2UserInfo;
import com.wardk.meeteam_backend.global.loginRegister.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOauth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);

            log.info("=== Google OAuth2 User Attributes ===");
            oAuth2User.getAttributes().forEach((key, value) -> {
                log.info("Key: {}, Value: {}, Type: {}", key, value, value != null ? value.getClass().getSimpleName() : "null");
            });
            log.info("=====================================");

            String provider = userRequest.getClientRegistration().getRegistrationId();
            OAuth2UserInfo oAuth2UserInfo = null;

            if(provider.equals("google")){
                log.info("구글 로그인 - 사용자 정보 파싱 시작");
                oAuth2UserInfo = new GoogleUserDetails(oAuth2User.getAttributes());
            } else {
                throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 Provider: " + provider);
            }

            // null 체크 추가
            if (oAuth2UserInfo == null) {
                throw new OAuth2AuthenticationException("OAuth2 사용자 정보를 파싱할 수 없습니다.");
            }

            String providerId = oAuth2UserInfo.getProviderId();
            String email = oAuth2UserInfo.getEmail();
            String name = oAuth2UserInfo.getName();

            // 이메일 null 체크
            if (email == null || email.isEmpty()) {
                throw new OAuth2AuthenticationException("OAuth2 사용자 이메일 정보가 없습니다.");
            }

            log.info("OAuth2 사용자 정보 - email: {}, name: {}, providerId: {}", email, name, providerId);

            Member findMember = memberRepository.findByEmail(email);
            Member member;

            if (findMember == null) {
                member = Member.builder()
                        .email(email)
                        .realName(name)
                        .provider(provider)
                        .providerId(providerId)
                        .role(UserRole.USER)
                        .build();
                memberRepository.save(member);
                log.info("새로운 OAuth2 사용자 생성: {}", email);
            } else {
                member = findMember;
                log.info("기존 OAuth2 사용자 로그인: {}", email);
            }

            return new CustomOauth2UserDetails(member, oAuth2User.getAttributes());

        } catch (Exception e) {
            log.error("OAuth2 사용자 로드 실패", e);
            OAuth2Error error = new OAuth2Error("user_load_error", "OAuth2 사용자 로드 중 오류 발생", null);
            throw new OAuth2AuthenticationException(error, e);
        }
    }
}
