package com.wardk.meeteam_backend.global.auth.service;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.global.auth.dto.CustomOauth2UserDetails;
import com.wardk.meeteam_backend.global.auth.dto.oauth.GitHubUserDetails;
import com.wardk.meeteam_backend.global.auth.dto.oauth.GoogleUserDetails;
import com.wardk.meeteam_backend.global.auth.dto.oauth.OAuth2UserInfo;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

            if(provider.equals("google")) {
                log.info("구글 로그인 - 사용자 정보 파싱 시작");
                oAuth2UserInfo = new GoogleUserDetails(oAuth2User.getAttributes());
            }else if(provider.equals("github")){
                log.info("깃허브 로그인 - 사용자 정보 파싱 시작");
                oAuth2UserInfo = new GitHubUserDetails(oAuth2User.getAttributes());
            }else {
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

            Member findMember = findOrCreateMember(email, name, providerId, provider);  // ← Provider 기반 조회


            return new CustomOauth2UserDetails(findMember, oAuth2User.getAttributes());

        } catch (Exception e) {
            log.error("OAuth2 사용자 로드 실패", e);
            OAuth2Error error = new OAuth2Error("user_load_error", "OAuth2 사용자 로드 중 오류 발생", null);
            throw new OAuth2AuthenticationException(error, e);
        }
    }

    /**
     * Provider + ProviderId 기반으로 회원 조회 또는 생성
     */
    private Member findOrCreateMember(String email, String name, String providerId, String provider) {
        log.info("회원 조회/생성 시작: provider={}, providerId={}, email={}", provider, providerId, email);

        // 1순위: Provider + ProviderId로 조회 (가장 확실한 방법)
        Optional<Member> existingMember = memberRepository.findByProviderAndProviderId(provider, providerId);
        if (existingMember.isPresent()) {
            log.info("기존 회원 조회 성공: id={}, email={}", existingMember.get().getId(), existingMember.get().getEmail());
            return existingMember.get();
        }

        // 2순위: 신규 회원 생성
        log.info("새로운 OAuth2 사용자 생성: email={}, provider={}, providerId={}", email, provider, providerId);

        try {
            Member newMember = Member.builder()
                    .email(email)
                    .realName(name)
                    .provider(provider)
                    .providerId(providerId)
                    .role(UserRole.USER)
                    .build();

            Member savedMember = memberRepository.save(newMember);
            log.info("새 회원 생성 성공: id={}, email={}", savedMember.getId(), email);
            return savedMember;

        } catch (Exception e) {
            log.error("새로운 OAuth2 사용자 생성 중 오류 발생: email={}, provider={}, providerId={}",
                    email, provider, providerId, e);

            // 동시성 문제로 인해 다른 스레드에서 생성되었을 가능성 체크
            Optional<Member> concurrentMember = memberRepository.findByProviderAndProviderId(provider, providerId);
            if (concurrentMember.isPresent()) {
                log.info("동시성 문제로 다른 스레드에서 생성된 회원 발견: id={}", concurrentMember.get().getId());
                return concurrentMember.get();
            }

            // 이메일 중복으로 실패한 경우 기존 회원 조회 시도
            return memberRepository.findByEmail(email)
                    .orElseThrow(() -> new OAuth2AuthenticationException("사용자 생성 실패: " + e.getMessage()));
        }
    }

}
