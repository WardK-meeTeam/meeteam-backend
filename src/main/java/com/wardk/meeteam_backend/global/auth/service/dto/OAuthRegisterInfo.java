package com.wardk.meeteam_backend.global.auth.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth2 신규 회원 가입 정보를 Redis에 임시 저장하기 위한 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthRegisterInfo {
    private String email;
    private String provider;
    private String providerId;
    private String oauthAccessToken;
    private String type;
}
