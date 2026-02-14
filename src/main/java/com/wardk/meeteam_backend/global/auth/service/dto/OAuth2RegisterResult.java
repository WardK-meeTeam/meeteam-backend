package com.wardk.meeteam_backend.global.auth.service.dto;

import com.wardk.meeteam_backend.domain.member.entity.Member;

/**
 * OAuth2 회원가입 결과를 담는 DTO.
 * 서비스 레이어에서 컨트롤러로 결과를 전달합니다.
 * 엔티티를 직접 노출하지 않고 필요한 필드만 포함합니다.
 */
public record OAuth2RegisterResult(
        String username,
        Long memberId,
        String accessToken,
        String refreshToken
) {
    public static OAuth2RegisterResult of(Member member, String accessToken, String refreshToken) {
        return new OAuth2RegisterResult(
                member.getRealName(),
                member.getId(),
                accessToken,
                refreshToken
        );
    }
}