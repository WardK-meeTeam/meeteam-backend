package com.wardk.meeteam_backend.web.auth.dto;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter

public class CustomOauth2UserDetails implements UserDetails, OAuth2User, OidcUser { // ★ 2. OidcUser 구현 추가

    private final Member member;
    private final Map<String, Object> attributes;
    private final boolean isNewMember;
    private final OidcIdToken idToken; // ★ 3. OIDC 관련 필드 추가
    private final OidcUserInfo userInfo;
    private final String oauthAccessToken; // OAuth 제공자의 Access Token (로그아웃 시 토큰 철회용)

    // 생성자 수정
    public CustomOauth2UserDetails(Member member, Map<String, Object> attributes, boolean isNewMember,
                                   OidcIdToken idToken, OidcUserInfo userInfo, String oauthAccessToken) {
        this.member = member;
        this.attributes = attributes;
        this.isNewMember = isNewMember;
        this.idToken = idToken;
        this.userInfo = userInfo;
        this.oauthAccessToken = oauthAccessToken;
    }

    // 일반 OAuth2용 생성자 (GitHub 등)
    public CustomOauth2UserDetails(Member member, Map<String, Object> attributes, boolean isNewMember, String oauthAccessToken) {
        this(member, attributes, isNewMember, null, null, oauthAccessToken);
    }

    // OidcUser 인터페이스의 필수 메서드 구현
    @Override
    public Map<String, Object> getClaims() {
        return attributes;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }

    // --- 기존 UserDetails, OAuth2User 메서드들 ---
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return member.getEmail();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_"  + member.getRole().name()));
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getEmail();
    }
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}