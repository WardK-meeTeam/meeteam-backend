package com.wardk.meeteam_backend.global.auth.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth2 기존 회원 로그인 정보를 Redis에 임시 저장하기 위한 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginInfo {
    private Long memberId;
    private String oauthAccessToken;
    private String type;
}
