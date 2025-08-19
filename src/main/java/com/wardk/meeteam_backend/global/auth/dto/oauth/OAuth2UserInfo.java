package com.wardk.meeteam_backend.global.auth.dto.oauth;

public interface OAuth2UserInfo {
    String getProvider(); // 소셜 로그인 제공자 (예: "google", "github" 등)
    String getProviderId(); // 소셜 로그인 제공자 ID
    String getEmail(); // 이메일
    String getName(); // 이름
}
